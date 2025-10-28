// See the 'simple' example for comments on the basic properties.

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.0" apply false
}

// Set 'dita' as the default task for this project.
defaultTasks("dita")

subprojects {
    // Enable the DITA-OT Gradle plugin for all subprojects.
    apply(plugin = "io.github.jyjeanne.dita-ot-gradle")

    // Use the same DITA-OT installation for all subprojects.
    tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
        ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))
    }
}
