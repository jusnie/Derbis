plugins {
    kotlin("jvm") version "2.4.21"
}

group = "com.github.YASUDA"
version = "1.0.0"

val isFdroidBuild = providers.gradleProperty("derbisFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)

kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
