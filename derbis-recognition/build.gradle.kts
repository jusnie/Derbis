plugins {
    alias(libs.plugins.kotlin.jvm)
}

val isFdroidBuild = providers.gradleProperty("derbisFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)

kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
