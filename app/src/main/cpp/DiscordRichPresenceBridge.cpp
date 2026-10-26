#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <mutex>
#include <string>

#include <cdiscord.h>

namespace {
constexpr const char* kLogTag = "DerbisDiscord";
std::mutex g_mutex;
Discord_Client g_client{};
bool g_started = false;

Discord_String string_view(std::string& value) {
    return Discord_String{
        reinterpret_cast<uint8_t*>(value.data()),
        value.size()
    };
}

void update_callback(Discord_ClientResult* result, void*) {
    if (result == nullptr) return;
    const auto type = Discord_ClientResult_Type(result);
    if (type != Discord_ErrorType_None) {
        __android_log_print(ANDROID_LOG_WARN, kLogTag, "Discord Rich Presence update failed: %d", type);
    }
}

bool ensure_started(uint64_t application_id) {
    if (g_started) return true;
    if (application_id == 0) return false;
    Discord_SetFreeThreaded();
    Discord_Client_Init(&g_client);
    Discord_Client_SetApplicationId(&g_client, application_id);
    Discord_Client_Connect(&g_client);
    g_started = true;
    return true;
}

void clear_locked() {
    if (g_started) Discord_Client_ClearRichPresence(&g_client);
}
} // namespace

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_luc4n3x_derbis_feature_discord_DerbisDiscordRichPresence_nativeStart(
    JNIEnv*, jobject, jlong application_id
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return ensure_started(static_cast<uint64_t>(application_id)) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_luc4n3x_derbis_feature_discord_DerbisDiscordRichPresence_nativeUpdate(
    JNIEnv* env,
    jobject,
    jstring title,
    jstring artist,
    jboolean playing
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_started || !playing) {
        clear_locked();
        return;
    }

    const char* title_chars = title == nullptr ? "" : env->GetStringUTFChars(title, nullptr);
    const char* artist_chars = artist == nullptr ? "" : env->GetStringUTFChars(artist, nullptr);
    std::string details = title_chars == nullptr ? "" : title_chars;
    std::string state = artist_chars == nullptr ? "" : artist_chars;
    if (title_chars != nullptr) env->ReleaseStringUTFChars(title, title_chars);
    if (artist_chars != nullptr) env->ReleaseStringUTFChars(artist, artist_chars);

    Discord_Activity activity{};
    Discord_Activity_Init(&activity);
    Discord_Activity_SetType(&activity, Discord_ActivityTypes_Listening);
    std::string name = "Derbis";
    auto name_value = string_view(name);
    auto details_value = string_view(details);
    auto state_value = string_view(state);
    Discord_Activity_SetName(&activity, name_value);
    if (!details.empty()) Discord_Activity_SetDetails(&activity, &details_value);
    if (!state.empty()) Discord_Activity_SetState(&activity, &state_value);
    Discord_Activity_SetSupportedPlatforms(&activity, Discord_ActivityGamePlatforms_Android);
    Discord_Client_UpdateRichPresence(&g_client, &activity, update_callback, nullptr, nullptr);
    Discord_Activity_Drop(&activity);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_luc4n3x_derbis_feature_discord_DerbisDiscordRichPresence_nativeClear(
    JNIEnv*, jobject
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    clear_locked();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_luc4n3x_derbis_feature_discord_DerbisDiscordRichPresence_nativeStop(
    JNIEnv*, jobject
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_started) return;
    Discord_Client_ClearRichPresence(&g_client);
    Discord_Client_Disconnect(&g_client);
    Discord_Client_Drop(&g_client);
    g_started = false;
}
