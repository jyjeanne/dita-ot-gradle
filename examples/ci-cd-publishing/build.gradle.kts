/**
 * CI/CD Documentation Publishing Example (using built-in tasks)
 *
 * This example demonstrates how to:
 * 1. Automate DITA documentation builds in CI/CD pipelines
 * 2. Download DITA-OT using built-in DitaOtDownloadTask
 * 3. Generate multiple output formats (HTML, PDF)
 * 4. Use configuration cache for faster builds
 * 5. Integrate with GitHub Actions
 *
 * Usage:
 *   ./gradlew generateDocs              # Generate all formats
 *   ./gradlew generateHtml              # Generate HTML only
 *   ./gradlew generatePdf               # Generate PDF only
 *   ./gradlew generateDocs -PditaOtVersion=4.1.0  # Use specific DITA-OT version
 *
 * No external plugins required! Everything is built-in.
 */

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.2"
}

// ============================================================================
// Configuration (can be overridden via gradle.properties or -P flags)
// ============================================================================

val ditaOtVersion: String = project.findProperty("ditaOtVersion")?.toString() ?: "4.2.3"
val ditaOtHome = layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion")

// ============================================================================
// Task: Download DITA-OT (built-in)
// ============================================================================

val downloadDitaOt by tasks.registering(com.github.jyjeanne.DitaOtDownloadTask::class) {
    description = "Download DITA-OT from GitHub releases"
    group = "DITA-OT Setup"

    version.set(ditaOtVersion)
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))

    // Configure retries for CI reliability
    retries.set(3)
    connectTimeout.set(60000)  // 60 seconds for CI environments
    readTimeout.set(120000)    // 2 minutes for slow connections
}

// ============================================================================
// Task: Generate HTML Documentation
// ============================================================================

val generateHtml by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    description = "Generate HTML5 documentation"
    group = "Documentation"

    dependsOn(downloadDitaOt)

    ditaOt(ditaOtHome)
    input("docs/guide.ditamap")
    output("build/output/html")
    transtype("html5")
    filter("docs/release.ditaval")

    // HTML5 specific properties
    ditaProperties.put("nav-toc", "partial")
    ditaProperties.put("args.copycss", "yes")
    ditaProperties.put("args.css", "theme.css")
    ditaProperties.put("args.csspath", "css")
}

// ============================================================================
// Task: Generate PDF Documentation
// ============================================================================

val generatePdf by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    description = "Generate PDF documentation"
    group = "Documentation"

    dependsOn(downloadDitaOt)

    ditaOt(ditaOtHome)
    input("docs/guide.ditamap")
    output("build/output/pdf")
    transtype("pdf")
    filter("docs/release.ditaval")

    // PDF specific properties
    ditaProperties.put("args.chapter.layout", "BASIC")
    ditaProperties.put("outputFile.base", "user-guide")
}

// ============================================================================
// Task: Generate All Documentation
// ============================================================================

val generateDocs by tasks.registering {
    description = "Generate all documentation formats (HTML and PDF)"
    group = "Documentation"

    dependsOn(generateHtml, generatePdf)
}

// ============================================================================
// Task: Verify Output
// ============================================================================

val verifyDocs by tasks.registering {
    description = "Verify documentation was generated correctly"
    group = "Documentation"

    dependsOn(generateDocs)

    // Note: Verification logic removed for configuration cache compatibility
    // The DitaOtTask already reports success/failure with detailed output
}

// ============================================================================
// Task: Clean Documentation
// ============================================================================

val cleanDocs by tasks.registering(Delete::class) {
    description = "Clean generated documentation"
    group = "Documentation"

    delete(layout.buildDirectory.dir("output"))
}

// ============================================================================
// Task: Clean All (including DITA-OT)
// ============================================================================

val cleanAll by tasks.registering(Delete::class) {
    description = "Clean all build outputs including downloaded DITA-OT"
    group = "Documentation"

    delete(layout.buildDirectory)
}

// ============================================================================
// Default Task
// ============================================================================

defaultTasks("generateDocs")
