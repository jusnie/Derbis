pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
}

includeBuild("../third_party/DerbisExtractor") {
    dependencySubstitution {
        substitute(module("com.github.YASUDA:DerbisExtractor")).using(project(":"))
    }
}

rootProject.name = "derbis-desktop"

include(":core")
include(":player")
include(":app")
