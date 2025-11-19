// Use Saxon-PE instead of Saxon-HE

import de.undercouch.gradle.tasks.download.Download

plugins {
    id("de.undercouch.download") version "4.1.1"
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

val downloadSaxonPE by tasks.registering(Download::class) {
    src("https://www.saxonica.com/download/SaxonPE9-9-1-5J.zip")
    dest(file("${layout.buildDirectory.asFile.get()}/SaxonPE9-9-1-5J.zip"))
    overwrite(false)
}

val extractSaxonPE by tasks.registering(Copy::class) {
    dependsOn(downloadSaxonPE)
    from(zipTree(downloadSaxonPE.get().dest))
    into(layout.buildDirectory)
}

val installSaxonPE by tasks.registering {
    dependsOn(extractSaxonPE)
}

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    dependsOn(installSaxonPE)
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))
    input("dita/root.ditamap")
    transtype("html5")

    val defaultCp = getDefaultClasspath()
    val filteredCp = defaultCp.filter { !it.name.matches(Regex(".*Saxon-HE.*\\.jar")) }
    classpath(filteredCp, file("${layout.buildDirectory.asFile.get()}/saxon9pe.jar"))
}

defaultTasks("dita")
