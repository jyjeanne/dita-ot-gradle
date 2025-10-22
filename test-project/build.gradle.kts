plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.0"
    id("de.undercouch.download") version "5.5.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val ditaOtVersion = "3.6"

// Download and install DITA-OT for integration testing
val downloadDitaOt by tasks.registering(de.undercouch.gradle.tasks.download.Download::class) {
    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(File(layout.buildDirectory.asFile.get(), "dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extractDitaOt by tasks.registering(Copy::class) {
    dependsOn(downloadDitaOt)
    from(zipTree(downloadDitaOt.get().dest))
    into(File(layout.buildDirectory.asFile.get(), "dita-extract"))

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

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    dependsOn(installDitaOt)

    ditaOt(File(layout.buildDirectory.asFile.get(), "dita-ot"))
    input("src/test.ditamap")
    transtype("html5")
}

defaultTasks("dita")
