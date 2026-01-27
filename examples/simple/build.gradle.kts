// Load the DITA-OT Gradle plugin.
plugins {
    // Using version 2.8.3 of the plugin with Configuration Cache support
    // For latest version, see: https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.3"
}

// The DITA-OT Gradle plugin adds a task called "dita" into your Gradle build-
// file. Let's make it the default task so that you can just type "gradle"
// on the command line to run DITA-OT.
defaultTasks("dita")

// Configure the dita task
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    // Tell Gradle where your DITA-OT installation is.
    //
    // In this case, we read DITA-OT installation location from a parameter
    // passed to the build script. You could just pass the path as a String or
    // a file.
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))

    // Point DITA-OT to the files you want to publish.
    input("dita/root.ditamap")

    // Tell DITA-OT what you want it to produce.
    transtype("pdf")

    // Point DITA-OT to the DITAVAL file you want to use.
    filter("dita/root.ditaval")

    // Give DITA-OT additional parameters using the type-safe Kotlin DSL (v2.1.0+)
    //
    // For a list of the parameters DITA-OT understands, see:
    // https://www.dita-ot.org/dev/parameters/
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
