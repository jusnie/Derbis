#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <mutex>
#include <string>

#include <cdiscord.h>

namespace {
constexpr const char* kLogTag = "DerbisDiscord";
constexpr jint kStateUnavailable = 0;
constexpr jint kStateDisconnected = 1;
constexpr jint kStateSigningIn = 2;
constexpr jint kStateConnecting = 3;
constexpr jint kStateConnected = 4;
constexpr jint kStateError = 5;

std::recursive_mutex g_mutex;
Discord_Client g_client{};
Discord_AuthorizationCodeVerifier g_code_verifier{};
bool g_client_initialized = false;
bool g_code_verifier_active = false;
uint64_t g_application_id = 0;
jint g_connection_state = kStateDisconnected;
jint g_token_type = static_cast<jint>(Discord_AuthorizationTokenType_User);
std::string g_error;
std::string g_access_token;
std::string g_refresh_token;
std::string g_pending_refresh_token;

Discord_String string_view(std::string& value) {
    return Discord_String{
        reinterpret_cast<uint8_t*>(value.data()),
        value.size()
    };
}

std::string copy_string(Discord_String value) {
    if (value.ptr == nullptr || value.size == 0) return "";
    return std::string(reinterpret_cast<const char*>(value.ptr), value.size);
}

jstring new_jstring(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

std::string copy_jstring(JNIEnv* env, jstring value) {
    if (value == nullptr) return "";
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return "";
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

bool presence_link_allowed(const std::string& value) {
    if (value.empty() || value.size() > 256 || value.find_first_of(" \r\n\t") != std::string::npos) return false;
    return value.rfind("https://derbis-web.vercel.app/t/", 0) == 0 ||
        value.rfind("https://music.youtube.com/channel/", 0) == 0;
}

void add_presence_button(Discord_Activity* activity, const char* label, std::string& url) {
    if (!presence_link_allowed(url)) return;
    Discord_ActivityButton button{};
    Discord_ActivityButton_Init(&button);
    std::string button_label(label);
    Discord_ActivityButton_SetLabel(&button, string_view(button_label));
    Discord_ActivityButton_SetUrl(&button, string_view(url));
    Discord_Activity_AddButton(activity, &button);
    Discord_ActivityButton_Drop(&button);
}

void clear_code_verifier_locked() {
    if (!g_code_verifier_active) return;
    Discord_AuthorizationCodeVerifier_Drop(&g_code_verifier);
    g_code_verifier = Discord_AuthorizationCodeVerifier{};
    g_code_verifier_active = false;
}

void set_error_locked(const char* detail) {
    g_connection_state = kStateError;
    g_error = detail;
}

void on_status_changed(
    Discord_Client_Status status,
    Discord_Client_Error error,
    int32_t,
    void*
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (error != Discord_Client_Error_None) {
        set_error_locked("Discord could not connect");
        return;
    }
    switch (status) {
        case Discord_Client_Status_Ready:
            g_connection_state = kStateConnected;
            g_error.clear();
            break;
        case Discord_Client_Status_Connected:
        case Discord_Client_Status_Connecting:
        case Discord_Client_Status_Reconnecting:
            if (g_connection_state != kStateSigningIn) g_connection_state = kStateConnecting;
            break;
        case Discord_Client_Status_Disconnected:
            if (g_connection_state != kStateDisconnected && g_connection_state != kStateSigningIn) {
                set_error_locked("Discord disconnected");
            }
            break;
        default:
            break;
    }
}

bool ensure_client_initialized_locked(uint64_t application_id) {
    if (g_client_initialized) {
        return application_id == g_application_id && application_id != 0;
    }
    if (application_id == 0) return false;
    Discord_SetFreeThreaded();
    Discord_Client_Init(&g_client);
    Discord_Client_SetApplicationId(&g_client, application_id);
    Discord_Client_SetStatusChangedCallback(&g_client, on_status_changed, nullptr, nullptr);
    g_application_id = application_id;
    g_client_initialized = true;
    g_connection_state = kStateDisconnected;
    g_error.clear();
    return true;
}

void on_token_updated(Discord_ClientResult* result, void*) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (result == nullptr || !Discord_ClientResult_Successful(result)) {
        set_error_locked("Discord could not start the linked session");
        return;
    }
    g_connection_state = kStateConnecting;
    Discord_Client_Connect(&g_client);
}

void on_token_exchange(
    Discord_ClientResult* result,
    Discord_String access_token,
    Discord_String refresh_token,
    Discord_AuthorizationTokenType token_type,
    int32_t,
    Discord_String,
    void*
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    clear_code_verifier_locked();
    if (result == nullptr || !Discord_ClientResult_Successful(result)) {
        set_error_locked("Discord sign-in was not approved");
        return;
    }
    g_access_token = copy_string(access_token);
    g_refresh_token = copy_string(refresh_token);
    g_token_type = static_cast<jint>(token_type);
    if (g_access_token.empty() || g_refresh_token.empty()) {
        set_error_locked("Discord returned an incomplete session");
        return;
    }
    auto token = string_view(g_access_token);
    g_connection_state = kStateConnecting;
    Discord_Client_UpdateToken(
        &g_client,
        token_type,
        token,
        on_token_updated,
        nullptr,
        nullptr
    );
}

void on_authorization(
    Discord_ClientResult* result,
    Discord_String code,
    Discord_String redirect_uri,
    void*
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (result == nullptr || !Discord_ClientResult_Successful(result) || !g_code_verifier_active) {
        clear_code_verifier_locked();
        set_error_locked("Discord sign-in was cancelled or failed");
        return;
    }
    Discord_String verifier{};
    Discord_AuthorizationCodeVerifier_Verifier(&g_code_verifier, &verifier);
    g_connection_state = kStateConnecting;
    Discord_Client_GetToken(
        &g_client,
        g_application_id,
        code,
        verifier,
        redirect_uri,
        on_token_exchange,
        nullptr,
        nullptr
    );
}

void update_callback(Discord_ClientResult* result, void*) {
    if (result == nullptr || !Discord_ClientResult_Successful(result)) {
        __android_log_print(ANDROID_LOG_WARN, kLogTag, "Discord Rich Presence update failed");
    }
}

void clear_locked() {
    if (g_client_initialized && g_connection_state == kStateConnected) {
        Discord_Client_ClearRichPresence(&g_client);
    }
}
} // namespace

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeInitialize(
    JNIEnv*, jobject, jlong application_id
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    return ensure_client_initialized_locked(static_cast<uint64_t>(application_id)) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeBeginSignIn(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (!g_client_initialized || g_application_id == 0 || g_connection_state == kStateSigningIn) {
        return JNI_FALSE;
    }
    clear_code_verifier_locked();
    Discord_Client_CreateAuthorizationCodeVerifier(&g_client, &g_code_verifier);
    g_code_verifier_active = true;

    Discord_AuthorizationArgs args{};
    Discord_AuthorizationArgs_Init(&args);
    Discord_AuthorizationArgs_SetClientId(&args, g_application_id);
    Discord_String scopes{};
    Discord_Client_GetDefaultPresenceScopes(&scopes);
    Discord_AuthorizationArgs_SetScopes(&args, scopes);
    Discord_AuthorizationCodeChallenge challenge{};
    Discord_AuthorizationCodeChallenge_Init(&challenge);
    Discord_AuthorizationCodeVerifier_Challenge(&g_code_verifier, &challenge);
    Discord_AuthorizationArgs_SetCodeChallenge(&args, &challenge);
    g_connection_state = kStateSigningIn;
    g_error.clear();
    Discord_Client_Authorize(&g_client, &args, on_authorization, nullptr, nullptr);
    Discord_AuthorizationCodeChallenge_Drop(&challenge);
    Discord_AuthorizationArgs_Drop(&args);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeRestoreSession(
    JNIEnv* env,
    jobject,
    jstring refresh_token
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (!g_client_initialized || g_application_id == 0 || refresh_token == nullptr) return JNI_FALSE;
    const char* raw_token = env->GetStringUTFChars(refresh_token, nullptr);
    g_pending_refresh_token = raw_token == nullptr ? "" : raw_token;
    if (raw_token != nullptr) env->ReleaseStringUTFChars(refresh_token, raw_token);
    if (g_pending_refresh_token.empty()) return JNI_FALSE;
    auto token = string_view(g_pending_refresh_token);
    g_connection_state = kStateConnecting;
    g_error.clear();
    Discord_Client_RefreshToken(
        &g_client,
        g_application_id,
        token,
        on_token_exchange,
        nullptr,
        nullptr
    );
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeDisconnect(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    clear_locked();
    clear_code_verifier_locked();
    if (g_client_initialized) Discord_Client_Disconnect(&g_client);
    g_access_token.clear();
    g_refresh_token.clear();
    g_pending_refresh_token.clear();
    g_error.clear();
    g_connection_state = kStateDisconnected;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeConnectionState(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    return g_client_initialized ? g_connection_state : kStateUnavailable;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeConnectionError(
    JNIEnv* env, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    return new_jstring(env, g_error);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeTakeAccessToken(
    JNIEnv* env, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    std::string value = std::move(g_access_token);
    g_access_token.clear();
    return new_jstring(env, value);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeTakeRefreshToken(
    JNIEnv* env, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    std::string value = std::move(g_refresh_token);
    g_refresh_token.clear();
    return new_jstring(env, value);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeTakeTokenType(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    return g_token_type;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeUpdate(
    JNIEnv* env,
    jobject,
    jstring title,
    jstring artist,
    jstring artwork_url,
    jstring artist_thumbnail_url,
    jstring song_url,
    jstring artist_url,
    jstring artwork_text,
    jlong started_at_epoch_ms,
    jlong ends_at_epoch_ms,
    jboolean playing
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (!g_client_initialized || g_connection_state != kStateConnected || !playing) {
        clear_locked();
        return;
    }

    std::string details = copy_jstring(env, title);
    std::string state = copy_jstring(env, artist);
    std::string image_key = copy_jstring(env, artwork_url);
    std::string small_image = copy_jstring(env, artist_thumbnail_url);
    std::string listen_url = copy_jstring(env, song_url);
    std::string profile_url = copy_jstring(env, artist_url);
    std::string image_text = copy_jstring(env, artwork_text);
    if (image_key.rfind("https://", 0) != 0) image_key = "derbis_listening";
    if (image_text.empty()) image_text = "Listening on Derbis";

    Discord_Activity activity{};
    Discord_Activity_Init(&activity);
    Discord_Activity_SetType(&activity, Discord_ActivityTypes_Listening);
    std::string name = "Derbis";
    auto name_value = string_view(name);
    auto details_value = string_view(details);
    auto state_value = string_view(state);
    auto image_key_value = string_view(image_key);
    auto image_text_value = string_view(image_text);
    auto listen_url_value = string_view(listen_url);
    auto profile_url_value = string_view(profile_url);
    Discord_Activity_SetName(&activity, name_value);
    if (!details.empty()) Discord_Activity_SetDetails(&activity, &details_value);
    if (!state.empty()) Discord_Activity_SetState(&activity, &state_value);
    if (presence_link_allowed(listen_url)) Discord_Activity_SetDetailsUrl(&activity, &listen_url_value);
    if (presence_link_allowed(profile_url)) Discord_Activity_SetStateUrl(&activity, &profile_url_value);
    Discord_Activity_SetSupportedPlatforms(&activity, Discord_ActivityGamePlatforms_Android);

    if (started_at_epoch_ms > 0) {
        Discord_ActivityTimestamps timestamps{};
        Discord_ActivityTimestamps_Init(&timestamps);
        Discord_ActivityTimestamps_SetStart(&timestamps, static_cast<uint64_t>(started_at_epoch_ms));
        if (ends_at_epoch_ms > started_at_epoch_ms) {
            Discord_ActivityTimestamps_SetEnd(&timestamps, static_cast<uint64_t>(ends_at_epoch_ms));
        }
        Discord_Activity_SetTimestamps(&activity, &timestamps);
        Discord_ActivityTimestamps_Drop(&timestamps);
    }

    Discord_ActivityAssets assets{};
    Discord_ActivityAssets_Init(&assets);
    Discord_ActivityAssets_SetLargeImage(&assets, &image_key_value);
    Discord_ActivityAssets_SetLargeText(&assets, &image_text_value);
    if (presence_link_allowed(listen_url)) Discord_ActivityAssets_SetLargeUrl(&assets, &listen_url_value);
    if (small_image.rfind("https://", 0) == 0) {
        auto small_image_value = string_view(small_image);
        Discord_ActivityAssets_SetSmallImage(&assets, &small_image_value);
        if (!state.empty()) Discord_ActivityAssets_SetSmallText(&assets, &state_value);
        if (presence_link_allowed(profile_url)) Discord_ActivityAssets_SetSmallUrl(&assets, &profile_url_value);
    }
    Discord_Activity_SetAssets(&activity, &assets);
    add_presence_button(&activity, "Listen on Derbis", listen_url);
    add_presence_button(&activity, "View Artist", profile_url);
    Discord_Client_UpdateRichPresence(&g_client, &activity, update_callback, nullptr, nullptr);
    Discord_ActivityAssets_Drop(&assets);
    Discord_Activity_Drop(&activity);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeClear(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    clear_locked();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yasuda_derbis_feature_discord_DerbisDiscordRichPresence_nativeRelease(
    JNIEnv*, jobject
) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    clear_locked();
    clear_code_verifier_locked();
    if (g_client_initialized) {
        Discord_Client_Disconnect(&g_client);
        Discord_Client_Drop(&g_client);
    }
    g_client = Discord_Client{};
    g_client_initialized = false;
    g_application_id = 0;
    g_access_token.clear();
    g_refresh_token.clear();
    g_pending_refresh_token.clear();
    g_error.clear();
    g_connection_state = kStateDisconnected;
}
