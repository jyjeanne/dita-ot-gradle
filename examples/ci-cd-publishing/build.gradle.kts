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
    id("io.github.jyjeanne.dita-ot-gradle") version "2.4.0"
}

// ============================================================================
// Configuration (can be overridden via gradle.properties or -P flags)
// ============================================================================

val ditaOtVersion: String = project.findProperty("ditaOtVersion")?.toString() ?: "4.2.3"
val ditaOtDir = layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion")

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

    ditaOt(ditaOtDir)
    input("docs/guide.ditamap")
    output(layout.buildDirectory.dir("output/html"))
    transtype("html5")
    filter("docs/release.ditaval")

    // HTML5 specific properties
    ditaProperties.put("nav-toc", "partial")
    ditaProperties.put("args.copycss", "yes")
    ditaProperties.put("args.css", "theme.css")
    ditaProperties.put("args.csspath", "css")

    doFirst {
        logger.lifecycle("Generating HTML documentation...")
    }
}

// ============================================================================
// Task: Generate PDF Documentation
// ============================================================================

val generatePdf by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    description = "Generate PDF documentation"
    group = "Documentation"

    dependsOn(downloadDitaOt)

    ditaOt(ditaOtDir)
    input("docs/guide.ditamap")
    output(layout.buildDirectory.dir("output/pdf"))
    transtype("pdf")
    filter("docs/release.ditaval")

    // PDF specific properties
    ditaProperties.put("args.chapter.layout", "BASIC")
    ditaProperties.put("outputFile.base", "user-guide")

    doFirst {
        logger.lifecycle("Generating PDF documentation...")
    }
}

// ============================================================================
// Task: Generate All Documentation
// ============================================================================

val generateDocs by tasks.registering {
    description = "Generate all documentation formats (HTML and PDF)"
    group = "Documentation"

    dependsOn(generateHtml, generatePdf)

    doLast {
        logger.lifecycle("")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("Documentation Generation Complete!")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("HTML: build/output/html/")
        logger.lifecycle("PDF:  build/output/pdf/")
        logger.lifecycle("=" .repeat(60))
    }
}

// ============================================================================
// Task: Verify Output
// ============================================================================

val verifyDocs by tasks.registering {
    description = "Verify documentation was generated correctly"
    group = "Documentation"

    dependsOn(generateDocs)

    doLast {
        val htmlDir = layout.buildDirectory.dir("output/html").get().asFile
        val pdfDir = layout.buildDirectory.dir("output/pdf").get().asFile

        val htmlFiles = htmlDir.walkTopDown().filter { it.extension == "html" }.toList()
        val pdfFiles = pdfDir.walkTopDown().filter { it.extension == "pdf" }.toList()

        if (htmlFiles.isEmpty()) {
            throw GradleException("No HTML files generated!")
        }

        logger.lifecycle("Verification Results:")
        logger.lifecycle("  HTML files: ${htmlFiles.size}")
        logger.lifecycle("  PDF files: ${pdfFiles.size}")
        logger.lifecycle("Verification: PASSED")
    }
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
