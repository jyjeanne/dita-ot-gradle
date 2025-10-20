import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("de.undercouch.download") version "5.5.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.50.0"
}

group = "com.github.jyjeanne"
version = "0.7.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.9"
        languageVersion = "1.9"
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
    implementation("commons-io:commons-io:2.8.0")

    // Kotlin tests (Kotest)
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")

    // For jsoup (HTML parsing in tests)
    testImplementation("org.jsoup:jsoup:1.13.1")
}

// DITA-OT download configuration
val ditaOtVersion = "3.6"

val downloadDitaOt by tasks.registering(Download::class) {
    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(File(layout.buildDirectory.asFile.get(), "dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extractDitaOt by tasks.registering(Copy::class) {
    dependsOn(downloadDitaOt)
    from(zipTree(downloadDitaOt.get().dest))
    into(File(layout.buildDirectory.asFile.get(), "dita-extract"))
    doNotTrackState("DITA-OT extraction is not tracked for performance")

    doLast {
        val ditaOtDir = File(layout.buildDirectory.asFile.get(), "dita-extract/dita-ot-$ditaOtVersion")
        val targetDir = File(layout.buildDirectory.asFile.get(), "dita-ot")
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
            id = "com.github.jyjeanne.dita-ot-gradle"
            displayName = "DITA-OT Gradle Plugin"
            implementationClass = "com.github.jyjeanne.DitaOtPlugin"
            description = "A Gradle plugin for running DITA Open Toolkit"
            tags.set(listOf("dita", "dita-ot", "documentation"))
        }
    }
}
