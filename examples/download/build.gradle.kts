/**
 * DITA-OT Download Example (using built-in tasks)
 *
 * This example demonstrates how to:
 * 1. Download DITA-OT automatically using DitaOtDownloadTask
 * 2. Install plugins using DitaOtInstallPluginTask
 * 3. Run transformations
 *
 * Usage:
 *   ./gradlew dita                    # Download, install plugins, transform
 *   ./gradlew downloadDitaOt          # Just download DITA-OT
 *   ./gradlew installPlugins          # Install plugins
 *
 * No external plugins required! Everything is built-in.
 */

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.3"
}

val ditaOtVersion = "4.2.3"

// ============================================================================
// Task: Download DITA-OT (built-in)
// ============================================================================

val downloadDitaOt by tasks.registering(com.github.jyjeanne.DitaOtDownloadTask::class) {
    version.set(ditaOtVersion)
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))

    // Optional: Configure retries and timeouts
    retries.set(3)
    connectTimeout.set(30000)
    readTimeout.set(60000)
}

// ============================================================================
// Task: Install Plugins (built-in)
// ============================================================================

val installPlugins by tasks.registering(com.github.jyjeanne.DitaOtInstallPluginTask::class) {
    dependsOn(downloadDitaOt)

    ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion"))
    plugins.set(listOf("org.lwdita", "org.dita.normalize"))

    // Optional: Configure retries
    retries.set(2)
}

// ============================================================================
// Task: DITA Transformation
// ============================================================================

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    dependsOn(installPlugins)

    ditaOt(layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion"))
    input("dita/root.ditamap")
    transtype("markdown")
}

defaultTasks("dita")
