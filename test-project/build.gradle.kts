plugins {
    // Must match `version` in the root build.gradle.kts: CI publishes the plugin
    // to mavenLocal and this build resolves it from there (see settings.gradle.kts).
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.6"
}

repositories {
    mavenLocal()
    mavenCentral()
}

// CI runs this build once per DITA-OT version: ./gradlew verifyOutput -PditaOtVersion=4.4.2
val ditaOtVersion: String = providers.gradleProperty("ditaOtVersion").getOrElse("3.6")

// Download and extract DITA-OT with the plugin's own task (also exercises it in CI).
// It extracts to build/dita-ot/dita-ot-<version>.
val downloadDitaOt = tasks.register<com.github.jyjeanne.DitaOtDownloadTask>("downloadDitaOt") {
    version.set(ditaOtVersion)
    retries.set(3)
}

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    dependsOn(downloadDitaOt)

    ditaOt(layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/test.ditamap")
    output("build/out")
    transtype("html5")
}

// Fail the build unless the transformation really produced HTML5 output.
tasks.register("verifyOutput") {
    dependsOn("dita")
    val outDir = layout.buildDirectory.dir("out")
    doLast {
        val dir = outDir.get().asFile
        listOf("index.html", "test-topic.html").forEach { name ->
            val file = File(dir, name)
            check(file.isFile && file.length() > 0) {
                "Expected HTML5 output $file was not produced (DITA-OT $ditaOtVersion)"
            }
        }
        logger.lifecycle("Verified HTML5 output for DITA-OT $ditaOtVersion in $dir")
    }
}

defaultTasks("verifyOutput")
