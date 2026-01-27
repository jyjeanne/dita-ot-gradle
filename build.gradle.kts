import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("de.undercouch.download") version "5.5.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.50.0"
}

group = "io.github.jyjeanne"
version = "2.8.3"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "2.1"
        languageVersion = "2.1"
    }
}

defaultTasks("check")

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Common dependencies
    implementation("commons-io:commons-io:2.20.0")

    // Kotlin tests (Kotest)
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")

    // For jsoup (HTML parsing in tests)
    testImplementation("org.jsoup:jsoup:1.21.2")
}

// DITA-OT download configuration
val ditaOtVersion = "3.6"

val downloadDitaOt by tasks.registering(Download::class) {
    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(layout.buildDirectory.file("dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extractDitaOt by tasks.registering(Copy::class) {
    dependsOn(downloadDitaOt)
    from(zipTree(downloadDitaOt.map { it.dest }))
    into(layout.buildDirectory.dir("dita-extract"))
    doNotTrackState("DITA-OT extraction is not tracked for performance")

    // This task is only used for testing, not part of plugin functionality
    notCompatibleWithConfigurationCache("DITA-OT extraction task uses doLast with script context")

    doLast {
        val buildDir = layout.buildDirectory.asFile.get()
        val ditaOtDir = File(buildDir, "dita-extract/dita-ot-$ditaOtVersion")
        val targetDir = File(buildDir, "dita-ot")
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        ditaOtDir.renameTo(targetDir)
    }
}

val installDitaOt by tasks.registering {
    dependsOn(extractDitaOt)
}

tasks.test {
    dependsOn(installDitaOt)

    systemProperty("dita.home",
        System.getProperty("dita.home",
            File(layout.buildDirectory.asFile.get(), "dita-ot").canonicalPath))
    systemProperty("examples.dir",
        File(projectDir, "examples").absolutePath)

    // Use JUnit Platform for Kotest
    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

gradlePlugin {
    website.set("https://github.com/jyjeanne/dita-ot-gradle")
    vcsUrl.set("https://github.com/jyjeanne/dita-ot-gradle.git")

    plugins {
        create("ditaOtPlugin") {
            id = "io.github.jyjeanne.dita-ot-gradle"
            displayName = "DITA-OT Gradle Plugin"
            implementationClass = "com.github.jyjeanne.DitaOtPlugin"
            description = "A Gradle plugin for publishing DITA documents with DITA Open Toolkit. Continuation of com.github.eerohele.dita-ot-gradle with Kotlin migration. Supports incremental builds, continuous mode, multiple output formats, and DITAVAL filtering. Faster than running DITA-OT directly thanks to the Gradle Daemon."
            tags.set(listOf("dita", "dita-ot", "documentation", "publishing", "technical-writing"))
        }
    }
}
