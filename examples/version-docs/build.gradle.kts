/**
 * Product Version Documentation Example (using built-in tasks)
 *
 * This example demonstrates how to:
 * 1. Generate documentation for multiple product versions from a single source
 * 2. Use DITA conditional processing (profiling) for version-specific content
 * 3. Apply version-specific branding and styling
 * 4. Run parallel builds for all supported versions
 *
 * Usage:
 *   ./gradlew buildAllVersions         # Build all versions in parallel
 *   ./gradlew buildV1                  # Build v1.x documentation
 *   ./gradlew buildV2                  # Build v2.x documentation
 *   ./gradlew buildV3                  # Build v3.x (latest) documentation
 *   ./gradlew buildLatest              # Build only the latest version
 *
 * No external plugins required! Everything is built-in.
 */

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.0"
}

// ============================================================================
// Configuration
// ============================================================================

val ditaOtVersion: String = project.findProperty("ditaOtVersion")?.toString() ?: "4.2.3"
val ditaOtHome = layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion")

// Supported product versions
val productVersions = listOf("v1", "v2", "v3")
val latestVersion = "v3"
val versionLabels = mapOf(
    "v1" to "1.x (LTS)",
    "v2" to "2.x",
    "v3" to "3.x (Latest)"
)

// ============================================================================
// DITA-OT Setup Tasks (built-in)
// ============================================================================

val downloadDitaOt by tasks.registering(com.github.jyjeanne.DitaOtDownloadTask::class) {
    description = "Download DITA-OT from GitHub releases"
    group = "DITA-OT Setup"

    version.set(ditaOtVersion)
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))

    // Configure retries for reliability
    retries.set(3)
    connectTimeout.set(30000)
    readTimeout.set(60000)
}

// ============================================================================
// Version-Specific Build Tasks
// ============================================================================

// Create HTML build task for each version
val htmlTasks = productVersions.map { version ->
    tasks.register<com.github.jyjeanne.DitaOtTask>("build${version.uppercase()}") {
        description = "Build ${versionLabels[version]} HTML documentation"
        group = "Documentation"

        dependsOn(downloadDitaOt)

        ditaOt(ditaOtHome)
        input("content/guide.ditamap")
        output("build/output/$version/html")
        transtype("html5")
        filter("filters/$version.ditaval")

        // Version-specific properties
        ditaProperties.put("nav-toc", "partial")
        ditaProperties.put("args.gen.task.lbl", "YES")

        doFirst {
            logger.lifecycle("Building documentation for version ${versionLabels[version]}...")
        }
    }
}

// Create PDF build task for each version
val pdfTasks = productVersions.map { version ->
    tasks.register<com.github.jyjeanne.DitaOtTask>("build${version.uppercase()}Pdf") {
        description = "Build ${versionLabels[version]} PDF documentation"
        group = "Documentation"

        dependsOn(downloadDitaOt)

        ditaOt(ditaOtHome)
        input("content/guide.ditamap")
        output("build/output/$version/pdf")
        transtype("pdf")
        filter("filters/$version.ditaval")

        ditaProperties.put("outputFile.base", "guide-$version")

        doFirst {
            logger.lifecycle("Building PDF for version ${versionLabels[version]}...")
        }
    }
}

// ============================================================================
// Aggregate Tasks
// ============================================================================

val buildAllVersions by tasks.registering {
    description = "Build HTML documentation for all product versions in parallel"
    group = "Documentation"

    dependsOn(htmlTasks)

    doLast {
        logger.lifecycle("")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("Multi-Version Build Complete!")
        logger.lifecycle("=" .repeat(60))
        productVersions.forEach { version ->
            logger.lifecycle("  ${versionLabels[version]}: build/output/$version/html/")
        }
        logger.lifecycle("=" .repeat(60))
    }
}

val buildAllPdfs by tasks.registering {
    description = "Build PDF documentation for all product versions in parallel"
    group = "Documentation"

    dependsOn(pdfTasks)
}

val buildLatest by tasks.registering {
    description = "Build only the latest version documentation"
    group = "Documentation"

    dependsOn("build${latestVersion.uppercase()}")
}

val release by tasks.registering {
    description = "Build all versions in HTML and PDF formats"
    group = "Documentation"

    dependsOn(buildAllVersions, buildAllPdfs)

    doLast {
        logger.lifecycle("")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("Release Build Complete!")
        logger.lifecycle("=" .repeat(60))
        productVersions.forEach { version ->
            logger.lifecycle("  ${versionLabels[version]}:")
            logger.lifecycle("    HTML: build/output/$version/html/")
            logger.lifecycle("    PDF:  build/output/$version/pdf/")
        }
        logger.lifecycle("=" .repeat(60))
    }
}

// ============================================================================
// Utility Tasks
// ============================================================================

val listVersions by tasks.registering {
    description = "List all supported product versions"
    group = "Documentation"

    doLast {
        logger.lifecycle("Supported product versions:")
        productVersions.forEach { version ->
            val isLatest = if (version == latestVersion) " [LATEST]" else ""
            logger.lifecycle("  - $version (${versionLabels[version]})$isLatest")
        }
    }
}

val cleanDocs by tasks.registering(Delete::class) {
    description = "Clean generated documentation"
    group = "Documentation"

    delete(layout.buildDirectory.dir("output"))
}

val cleanAll by tasks.registering(Delete::class) {
    description = "Clean all build outputs including DITA-OT"
    group = "Documentation"

    delete(layout.buildDirectory)
}

// ============================================================================
// Default Task
// ============================================================================

defaultTasks("buildAllVersions")
