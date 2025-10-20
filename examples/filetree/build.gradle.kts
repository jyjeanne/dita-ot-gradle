// See the 'simple' example for comments on the basic properties.

plugins {
    id("com.github.jyjeanne.dita-ot-gradle") version "1.0-SNAPSHOT"
}

defaultTasks("dita")

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))

    // Publish all .ditamap files in the 'dita' directory.
    val ditaFiles = fileTree("dita") {
        include("*.ditamap")
    }
    input(ditaFiles)

    transtype("xhtml")
}
