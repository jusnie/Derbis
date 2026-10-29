plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.licensee) apply false
    alias(libs.plugins.ruler) apply false
}

project(":app").pluginManager.withPlugin("com.android.application") {
    project(":app").dependencies.add("implementation", "com.github.YASUDA:DerbisNexus:1.0.0")
    project(":app").dependencies.add(
        "implementation",
        project(":app").dependencies.project(":derbis-recognition")
    )
}
