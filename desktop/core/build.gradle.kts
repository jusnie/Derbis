plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("../third_party/DerbisNexus/src/main/kotlin"))
    }
    sourceSets.named("test") {
        kotlin.srcDir(rootProject.file("../third_party/DerbisNexus/src/test/kotlin"))
    }
}

dependencies {
    api(libs.derbis.extractor)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(libs.okhttp.brotli)
    runtimeOnly(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
