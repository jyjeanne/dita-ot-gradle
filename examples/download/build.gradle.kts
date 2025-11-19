// See the 'simple' example for comments on the basic properties.

import de.undercouch.gradle.tasks.download.Download

plugins {
    id("de.undercouch.download") version "4.1.1"
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.2"
}

val ditaOtVersion = "3.4"

// Download and install DITA-OT

val downloadDitaOt by tasks.registering(Download::class) {
    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(File(layout.buildDirectory.asFile.get(), "dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extract by tasks.registering(Copy::class) {
    dependsOn(downloadDitaOt)
    from(zipTree(downloadDitaOt.get().dest))
    into(layout.buildDirectory)
}

val ditaHome = "${layout.buildDirectory.asFile.get()}/dita-ot-$ditaOtVersion"

// Install DITA-OT plugins from DITA-OT Plugin Registry

val pluginIds = listOf("org.lwdita", "org.dita.normalize")

val pluginTasks = pluginIds.map { id ->
    tasks.register<Exec>(id) {
        // Install plugin if not already installed, otherwise build will fail
        onlyIf { !file("$ditaHome/plugins/$id").exists() }
        outputs.dir("$ditaHome/plugins/$id")
        workingDir(file(ditaHome))
        commandLine("bin/dita", "--install", id)
    }
}

val install by tasks.registering {
    dependsOn(extract)
    dependsOn(pluginTasks)
}

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    dependsOn(install)
    ditaOt(ditaHome)
    input("dita/root.ditamap")
    transtype("markdown")
}

defaultTasks("dita")
