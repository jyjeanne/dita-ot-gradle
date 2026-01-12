/**
 * Multi-Language Documentation Example
 *
 * This example demonstrates how to:
 * 1. Build documentation in multiple languages from localized DITA sources
 * 2. Run parallel builds for all languages
 * 3. Share common assets across languages
 * 4. Organize output by locale
 *
 * Usage:
 *   ./gradlew buildAllLanguages        # Build all languages in parallel
 *   ./gradlew buildEnglish             # Build English only
 *   ./gradlew buildFrench              # Build French only
 *   ./gradlew buildGerman              # Build German only
 *   ./gradlew release                  # Build all languages + PDF
 */

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.2"
    id("de.undercouch.download") version "5.5.0"
}

// ============================================================================
// Configuration
// ============================================================================

val ditaOtVersion: String = project.findProperty("ditaOtVersion")?.toString() ?: "4.2.3"
val ditaOtDir = layout.buildDirectory.dir("dita-ot-$ditaOtVersion")

// Supported languages
val languages = listOf("en", "fr", "de")
val languageNames = mapOf(
    "en" to "English",
    "fr" to "French",
    "de" to "German"
)

// ============================================================================
// DITA-OT Setup Tasks
// ============================================================================

val downloadDitaOt by tasks.registering(de.undercouch.gradle.tasks.download.Download::class) {
    description = "Download DITA-OT from GitHub releases"
    group = "DITA-OT Setup"

    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(layout.buildDirectory.file("dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extractDitaOt by tasks.registering(Copy::class) {
    description = "Extract DITA-OT zip archive"
    group = "DITA-OT Setup"

    dependsOn(downloadDitaOt)
    doNotTrackState("DITA-OT contains files that cannot be hashed")

    from(zipTree(layout.buildDirectory.file("dita-ot-$ditaOtVersion.zip")))
    into(layout.buildDirectory)
}

// ============================================================================
// Language-Specific Build Tasks
// ============================================================================

// Create HTML build task for each language
val htmlTasks = languages.map { lang ->
    tasks.register<com.github.jyjeanne.DitaOtTask>("build${languageNames[lang]}") {
        description = "Build ${languageNames[lang]} HTML documentation"
        group = "Documentation"

        dependsOn(extractDitaOt)

        ditaOt(ditaOtDir)
        input("content/$lang/guide.ditamap")
        output(layout.buildDirectory.dir("output/$lang/html"))
        transtype("html5")

        // Language-specific properties
        ditaProperties.put("args.gen.task.lbl", "YES")
        ditaProperties.put("nav-toc", "partial")

        doFirst {
            logger.lifecycle("Building ${languageNames[lang]} documentation...")
        }
    }
}

// Create PDF build task for each language
val pdfTasks = languages.map { lang ->
    tasks.register<com.github.jyjeanne.DitaOtTask>("build${languageNames[lang]}Pdf") {
        description = "Build ${languageNames[lang]} PDF documentation"
        group = "Documentation"

        dependsOn(extractDitaOt)

        ditaOt(ditaOtDir)
        input("content/$lang/guide.ditamap")
        output(layout.buildDirectory.dir("output/$lang/pdf"))
        transtype("pdf")

        ditaProperties.put("outputFile.base", "guide-$lang")

        doFirst {
            logger.lifecycle("Building ${languageNames[lang]} PDF...")
        }
    }
}

// ============================================================================
// Aggregate Tasks
// ============================================================================

val buildAllLanguages by tasks.registering {
    description = "Build HTML documentation for all languages in parallel"
    group = "Documentation"

    dependsOn(htmlTasks)

    doLast {
        logger.lifecycle("")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("Multi-Language Build Complete!")
        logger.lifecycle("=" .repeat(60))
        languages.forEach { lang ->
            logger.lifecycle("  ${languageNames[lang]}: build/output/$lang/html/")
        }
        logger.lifecycle("=" .repeat(60))
    }
}

val buildAllPdfs by tasks.registering {
    description = "Build PDF documentation for all languages in parallel"
    group = "Documentation"

    dependsOn(pdfTasks)
}

val release by tasks.registering {
    description = "Build all languages in HTML and PDF formats"
    group = "Documentation"

    dependsOn(buildAllLanguages, buildAllPdfs)

    doLast {
        logger.lifecycle("")
        logger.lifecycle("=" .repeat(60))
        logger.lifecycle("Release Build Complete!")
        logger.lifecycle("=" .repeat(60))
        languages.forEach { lang ->
            logger.lifecycle("  ${languageNames[lang]}:")
            logger.lifecycle("    HTML: build/output/$lang/html/")
            logger.lifecycle("    PDF:  build/output/$lang/pdf/")
        }
        logger.lifecycle("=" .repeat(60))
    }
}

// ============================================================================
// Utility Tasks
// ============================================================================

val listLanguages by tasks.registering {
    description = "List all supported languages"
    group = "Documentation"

    doLast {
        logger.lifecycle("Supported languages:")
        languages.forEach { lang ->
            logger.lifecycle("  - $lang (${languageNames[lang]})")
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

defaultTasks("buildAllLanguages")
