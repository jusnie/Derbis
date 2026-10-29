pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.spotify.ruler") {
                useModule("com.spotify.ruler:ruler-gradle-plugin:${requested.version ?: "2.0.0-beta-3"}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("third_party/DerbisExtractor") {
    dependencySubstitution {
        substitute(module("com.github.YASUDA:DerbisExtractor")).using(project(":"))
    }
}

includeBuild("third_party/DerbisNexus") {
    dependencySubstitution {
        substitute(module("com.github.YASUDA:DerbisNexus")).using(project(":"))
    }
}

rootProject.name = "Derbis"
include(":app")
include(":baselineprofile")
include(":derbis-recognition")
include(":derbis-native-audio")
