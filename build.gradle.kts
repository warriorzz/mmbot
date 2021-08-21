plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
    application
}

group = "com.github.warriorzz"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://schlaubi.jfrog.io/artifactory/envconf/")
    mavenCentral()
}

dependencies {
    implementation("dev.kord", "kord-core", "0.8.0-M4")
    implementation("dev.kord.x", "emoji", "0.5.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.1")
    implementation("org.litote.kmongo", "kmongo-coroutine-serialization", "4.2.8")
    implementation("dev.schlaubi", "envconf", "1.1")
    implementation("org.slf4j", "slf4j-simple", "1.7.31")

    implementation("de.nycode", "docky-kotlin-client", "1.0.6")
    implementation("io.ktor", "ktor-client-cio", "1.6.2")
}

application {
    mainClass.set("com.github.warriorzz.bot.LauncherKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

ktlint {
    verbose.set(true)
    filter {
        disabledRules.add("no-wildcard-imports")
        disabledRules.add("no-multi-spaces")
        disabledRules.add("indent")

        exclude("**/build/**")
    }
}
