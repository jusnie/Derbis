import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licensee)
    alias(libs.plugins.androidx.baselineprofile)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun envOrProperty(name: String, propertyName: String = name): String {
    val gradleProperty = findProperty(propertyName) as? String
    val localProperty = if (localProperties.containsKey(propertyName)) {
        localProperties.getProperty(propertyName)
    } else {
        null
    }
    val environmentValue = System.getenv(name)
    return (gradleProperty ?: localProperty ?: environmentValue ?: "").trim()
}

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

fun isPerformanceTaskRequested(): Boolean =
    gradle.startParameter.taskNames.any { task ->
        task.contains("baselineprofile", ignoreCase = true) || task.contains("benchmark", ignoreCase = true)
    }

fun isPrDiagnosticsTaskRequested(): Boolean =
    gradle.startParameter.taskNames.any { task ->
        task.contains("PrDiagnostics", ignoreCase = true)
    }

fun isPrDiagnosticsArtifactTaskRequested(): Boolean =
    gradle.startParameter.taskNames.any { task ->
        task.contains("assemblePrDiagnostics", ignoreCase = true) ||
            task.contains("bundlePrDiagnostics", ignoreCase = true) ||
            task.contains("packagePrDiagnostics", ignoreCase = true) ||
            task.contains("installPrDiagnostics", ignoreCase = true)
    }

fun isReleaseTaskRequested(): Boolean =
    !isPerformanceTaskRequested() && (
        isPrDiagnosticsArtifactTaskRequested() ||
            gradle.startParameter.taskNames.any { task ->
                task.contains("Release", ignoreCase = true) || task.equals("bundle", ignoreCase = true) || task.equals("assemble", ignoreCase = true)
            }
        )

val isFdroidBuild = providers.gradleProperty("derbisFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)
// Public InnerTube client identifier, not a private developer credential. Keep this
// aligned with DerbisExtractor's Android client so F-Droid can rebuild from source.
val publicYoutubeInnertubeApiKey = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
val youtubeInnertubeApiKey = if (isFdroidBuild) {
    publicYoutubeInnertubeApiKey
} else {
    val configured = envOrProperty("YOUTUBE_INNERTUBE_API_KEY", "youtubeInnertubeApiKey")
    if (configured.isNotBlank()) configured else publicYoutubeInnertubeApiKey
}
val canvasResolverUrl = if (isFdroidBuild) {
    ""
} else {
    envOrProperty("DERBIS_CANVAS_RESOLVER_URL", "derbisCanvasResolverUrl")
}
val canvasClientKey = if (isFdroidBuild) {
    ""
} else {
    envOrProperty("DERBIS_CANVAS_CLIENT_KEY", "derbisCanvasClientKey")
}
val derbisCanvasCatalogUrl = if (isFdroidBuild) {
    ""
} else {
    envOrProperty("DERBIS_CANVAS_CATALOG_URL", "derbisCanvasCatalogUrl")
}
val derbisCanvasCatalogKey = if (isFdroidBuild) {
    ""
} else {
    envOrProperty("DERBIS_CANVAS_CATALOG_KEY", "derbisCanvasCatalogKey")
}
val derbisJamRelayUrl = if (isFdroidBuild) {
    ""
} else {
    envOrProperty("DERBIS_JAM_RELAY_URL", "derbisJamRelayUrl")
}
// Spotify client IDs are public identifiers for installed apps. OAuth is protected by PKCE;
// never add a Spotify client secret to Android, this repository, or GitHub Actions.
val spotifyClientId = if (isFdroidBuild) "" else envOrProperty("SPOTIFY_CLIENT_ID", "spotifyClientId")
    .ifBlank { "27265553b42446a7b5d8ab9027dd0bb2" }
val spotifyRedirectUri = if (isFdroidBuild) "" else envOrProperty("SPOTIFY_REDIRECT_URI", "spotifyRedirectUri")
    .ifBlank { "derbis-spotify://callback" }
// The Discord Social SDK is a proprietary build-time dependency supplied by Discord's portal.
// It stays out of source control and is downloaded only by authorized upstream release builds.
val discordSocialSdkAar = file("libs/discord_partner_sdk.aar")
val discordSocialSdkAvailable = !isFdroidBuild && discordSocialSdkAar.isFile
val discordApplicationId = if (isFdroidBuild) "" else envOrProperty("DISCORD_APPLICATION_ID", "discordApplicationId")
    .ifBlank { "1563840846372012102" }
val releaseStoreFilePath = envOrProperty("DERBIS_KEYSTORE_FILE", "derbisStoreFile").ifBlank { "app/derbis-release.jks" }
val releaseStorePassword = envOrProperty("DERBIS_KEYSTORE_PASSWORD", "derbisStorePassword").ifBlank { "derbis123" }
val releaseKeyAlias = envOrProperty("DERBIS_KEY_ALIAS", "derbisKeyAlias").ifBlank { "derbis" }
val releaseKeyPassword = envOrProperty("DERBIS_KEY_PASSWORD", "derbisKeyPassword").ifBlank { "derbis123" }
val releaseStoreFile = rootProject.file(releaseStoreFilePath)
val releaseSigningAvailable = releaseStoreFile.isFile && releaseStorePassword.isNotBlank() && releaseKeyAlias.isNotBlank() && releaseKeyPassword.isNotBlank()

if (isFdroidBuild && isPrDiagnosticsTaskRequested()) {
    throw GradleException("PR diagnostics and F-Droid builds are mutually exclusive.")
}

if (isReleaseTaskRequested() && !isFdroidBuild && youtubeInnertubeApiKey.isBlank()) {
    throw GradleException("Missing YOUTUBE_INNERTUBE_API_KEY. Set it as a GitHub Actions secret or in local.properties as youtubeInnertubeApiKey.")
}

if (isPrDiagnosticsArtifactTaskRequested() && canvasResolverUrl.isBlank()) {
    throw GradleException("Missing DERBIS_CANVAS_RESOLVER_URL. Configure DERBIS_CANVAS_RESOLVER_URL as a GitHub Actions secret or in local.properties as derbisCanvasResolverUrl.")
}

if (isPrDiagnosticsArtifactTaskRequested() && canvasClientKey.isBlank()) {
    throw GradleException("Missing DERBIS_CANVAS_CLIENT_KEY. Configure DERBIS_CANVAS_CLIENT_KEY as a GitHub Actions secret or in local.properties as derbisCanvasClientKey.")
}

fun normalizedVersionName(value: String): String {
    val clean = value.trim().removePrefix("v").removePrefix("V")
    val match = Regex("\\d+(?:\\.\\d+){0,3}(?:[-+][0-9A-Za-z.-]+)?").find(clean)?.value
    return match ?: clean.ifBlank { "1.1.1" }
}

fun generatedVersionCode(versionName: String): Int {
    val parts = Regex("\\d+")
        .findAll(versionName)
        .mapNotNull { it.value.toIntOrNull() }
        .take(4)
        .toList()
    val major = parts.getOrElse(0) { 0 }.coerceIn(0, 999)
    val minor = parts.getOrElse(1) { 0 }.coerceIn(0, 99)
    val patch = parts.getOrElse(2) { 0 }.coerceIn(0, 99)
    val build = parts.getOrElse(3) { 0 }.coerceIn(0, 99)
    return major * 1_000_000 + minor * 10_000 + patch * 100 + build
}

fun githubTagVersionName(): String? {
    val refType = System.getenv("GITHUB_REF_TYPE").orEmpty()
    val refName = System.getenv("GITHUB_REF_NAME").orEmpty()
    val ref = System.getenv("GITHUB_REF").orEmpty()
    return when {
        refType == "tag" && refName.isNotBlank() -> refName
        ref.startsWith("refs/tags/") -> ref.substringAfterLast("/")
        else -> null
    }
}

val derbisVersionName = normalizedVersionName(
    (findProperty("derbisVersionName") as? String)
        ?: System.getenv("DERBIS_VERSION_NAME")
        ?: githubTagVersionName()
        ?: "1.1.1"
)

val derbisVersionCode = ((findProperty("derbisVersionCode") as? String)
    ?: System.getenv("DERBIS_VERSION_CODE"))
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: maxOf(generatedVersionCode(derbisVersionName), 3_000_203)

android {
    namespace = "com.yasuda.derbis"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.derbis.app"
        minSdk = 26
        targetSdk = 37
        versionCode = derbisVersionCode
        versionName = derbisVersionName
        vectorDrawables.useSupportLibrary = true
        manifestPlaceholders["upstreamUpdatesEnabled"] = (!isFdroidBuild).toString()
        manifestPlaceholders["discordApplicationId"] = discordApplicationId
        manifestPlaceholders["discordSocialSdkAvailable"] = discordSocialSdkAvailable.toString()
        buildConfigField("String", "UPDATE_REPOSITORY", "\"jusnie/Derbis\"")
        buildConfigField("String", "UPDATE_RELEASES_URL", "\"https://api.github.com/repos/jusnie/Derbis/releases?per_page=30\"")
        buildConfigField("boolean", "UPSTREAM_UPDATES_ENABLED", (!isFdroidBuild).toString())
        buildConfigField("boolean", "REMOTE_ANNOUNCEMENTS_ENABLED", (!isFdroidBuild).toString())
        buildConfigField("String", "YOUTUBE_INNERTUBE_API_KEY", buildConfigString(youtubeInnertubeApiKey))
        buildConfigField("String", "CANVAS_RESOLVER_URL", buildConfigString(canvasResolverUrl))
        buildConfigField("String", "CANVAS_CLIENT_KEY", buildConfigString(canvasClientKey))
        buildConfigField("String", "DERBIS_CANVAS_CATALOG_URL", buildConfigString(derbisCanvasCatalogUrl))
        buildConfigField("String", "DERBIS_CANVAS_CATALOG_KEY", buildConfigString(derbisCanvasCatalogKey))
        buildConfigField("String", "JAM_RELAY_URL", buildConfigString(derbisJamRelayUrl))
        buildConfigField("String", "SPOTIFY_CLIENT_ID", buildConfigString(spotifyClientId))
        buildConfigField("String", "SPOTIFY_REDIRECT_URI", buildConfigString(spotifyRedirectUri))
        buildConfigField("boolean", "DISCORD_SOCIAL_SDK_AVAILABLE", discordSocialSdkAvailable.toString())
        buildConfigField("String", "DISCORD_APPLICATION_ID", buildConfigString(discordApplicationId))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

    sourceSets.getByName("main").kotlin.directories.add(
        if (isFdroidBuild) "src/fdroid/java" else "src/upstream/java"
    )
    if (!isFdroidBuild) {
        sourceSets.getByName("main").jniLibs.directories.add("src/upstream/jniLibs")
    }
    sourceSets.getByName("debug").kotlin.directories.add("src/noDiagnostics/java")
    sourceSets.getByName("release").kotlin.directories.add("src/noDiagnostics/java")
    sourceSets.configureEach {
        if (name == "benchmark" || name == "nonMinifiedRelease" || name == "sidecarRelease") {
            kotlin.directories.add("src/noDiagnostics/java")
        }
    }
    if (isFdroidBuild) {
        sourceSets.getByName("debug").manifest.srcFile("src/fdroid/AndroidManifest.xml")
        sourceSets.getByName("release").manifest.srcFile("src/fdroid/AndroidManifest.xml")
    }

    signingConfigs {
        getByName("debug")
        create("release") {
            if (releaseSigningAvailable) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("sidecarRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
        create("prDiagnostics") {
            initWith(getByName("release"))
            applicationIdSuffix = ".prdiagnostics"
            versionNameSuffix = "-pr-diagnostics"
            isDebuggable = false
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "INTERNAL_DIAGNOSTICS", "true")
            buildConfigField("String", "INTERNAL_DIAGNOSTICS_MARKER", "\"DERBIS_PR_DIAGNOSTICS_V1\"")
            buildConfigField(
                "String",
                "INTERNAL_DIAGNOSTICS_COMMIT",
                buildConfigString(System.getenv("GITHUB_SHA").orEmpty().take(40))
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
        prefab = discordSocialSdkAvailable
    }

    if (discordSocialSdkAvailable) {
        defaultConfig {
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-std=c++17")
                }
            }
        }
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    dependenciesInfo {
        includeInApk = !isFdroidBuild
        includeInBundle = !isFdroidBuild
    }

    lint {
        // The whole player layer is built on media3, whose APIs are annotated
        // @UnstableApi. UnsafeOptInUsageError targets library authors who expose
        // unstable APIs to consumers and is redundant for an app that
        // deliberately depends on media3, so disable just this check while
        // keeping every other lint rule enforced.
        disable += "UnsafeOptInUsageError"
    }

    packaging {
        jniLibs {
            if (isFdroidBuild) {
                // F-Droid's build server has no NDK strip tool, while GitHub's
                // runner does. Preserve this prebuilt library in both builds so
                // the upstream-signed and F-Droid-rebuilt APKs are identical.
                keepDebugSymbols += "**/libdatastore_shared_counter.so"
            }
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}


kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-javalite")
}

dependencies {
    if (discordSocialSdkAvailable) {
        implementation(files(discordSocialSdkAar))
    }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    if (!isFdroidBuild) implementation(libs.androidx.fragment.ktx)
    if (!isFdroidBuild) implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.googlefonts)
    implementation(libs.androidx.media3.exoplayer)
    if (!isFdroidBuild) implementation(project(":derbis-native-audio"))
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.session)
    if (!isFdroidBuild) implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
    implementation(libs.androidx.media.compat)
    implementation(libs.androidx.media3.datasource.okhttp)
    if (!isFdroidBuild) {
        implementation(libs.androidx.media3.datasource.cronet) {
            exclude(group = "org.chromium.net", module = "cronet-api")
            exclude(group = "com.google.android.gms", module = "play-services-cronet")
        }
        implementation(libs.chromium.cronet.embedded)
    }
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.newpipe.extractor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.kolor.utilities)
    implementation(libs.timber)
    debugImplementation(libs.chucker.no.op)
    releaseImplementation(libs.chucker.no.op)
    add("prDiagnosticsImplementation", libs.chucker.no.op)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.json)
    testImplementation(libs.okhttp.tls)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    baselineProfile(project(":baselineprofile"))
}
