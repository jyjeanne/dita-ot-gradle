/**
 * DITA-OT Gradle Plugin Documentation Generator
 *
 * Cross-platform build script that works on both Windows and Linux/macOS.
 *
 * Note: This build script is NOT configuration cache compatible because it
 * uses doLast blocks with captured variables for essential file operations.
 * This is a special-purpose build for generating the plugin's documentation.
 *
 * Usage:
 *   ./gradlew buildDocs       - Generate both HTML and PDF
 *   ./gradlew generateHtml    - Generate HTML only
 *   ./gradlew generatePdf     - Generate PDF only
 *   ./gradlew cleanDocs       - Clean generated documentation
 */

plugins {
    id("de.undercouch.download") version "5.5.0"
}

val ditaOtVersion = "4.2.3"

// OS detection
val isWindows = System.getProperty("os.name").lowercase().contains("windows")

// Use a short path to avoid Windows command line length limits with DITA-OT classpath
val ditaOtHome = File(System.getProperty("user.home"), ".dita-ot/dita-ot-$ditaOtVersion")

// DITA executable path (platform-specific)
val ditaExecutable = if (isWindows) {
    File(ditaOtHome, "bin/dita.bat")
} else {
    File(ditaOtHome, "bin/dita")
}

// Use project-relative paths
val projectRoot: File = projectDir.parentFile.parentFile  // Go up to dita-ot-gradle root
val ditaSourceDir = File(projectRoot, "docs/dita")
val mainBookmap = File(ditaSourceDir, "plugin-documentation-version-2.3.2.bookmap")
val htmlOutputDir = File(projectRoot, "docs/html")
val pdfOutputDir = File(projectRoot, "docs/pdf")

val downloadDitaOt by tasks.registering(de.undercouch.gradle.tasks.download.Download::class) {
    group = "setup"
    description = "Download DITA-OT distribution"
    src("https://github.com/dita-ot/dita-ot/releases/download/$ditaOtVersion/dita-ot-$ditaOtVersion.zip")
    dest(layout.buildDirectory.file("downloads/dita-ot-$ditaOtVersion.zip"))
    overwrite(false)
}

val extractDitaOt by tasks.registering(Copy::class) {
    group = "setup"
    description = "Extract DITA-OT to user home directory"
    dependsOn(downloadDitaOt)
    from(zipTree(layout.buildDirectory.file("downloads/dita-ot-$ditaOtVersion.zip")))
    into(ditaOtHome.parentFile)
    onlyIf { !ditaOtHome.exists() }
}

// Make DITA executable on Unix systems
val setupDitaOt by tasks.registering {
    group = "setup"
    description = "Setup DITA-OT (set executable permissions on Unix)"
    dependsOn(extractDitaOt)

    doLast {
        if (!isWindows && ditaExecutable.exists()) {
            ditaExecutable.setExecutable(true)
        }
    }
}

// Create properties file for HTML generation
val createHtmlProperties by tasks.registering {
    group = "documentation"
    description = "Create properties file for HTML generation"
    dependsOn(setupDitaOt)

    val propsFile = File(ditaOtHome, "html.properties")
    outputs.file(propsFile)

    doLast {
        propsFile.writeText(buildString {
            appendLine("args.input=${mainBookmap.absolutePath.replace("\\", "/")}")
            appendLine("output.dir=${htmlOutputDir.absolutePath.replace("\\", "/")}")
            appendLine("transtype=html5")
            appendLine("nav-toc=full")
            appendLine("args.css=css/custom.css")
            appendLine("args.copycss=yes")
            appendLine("args.csspath=css")
            appendLine("args.cssroot=${ditaSourceDir.absolutePath.replace("\\", "/")}")
        })
    }
}

// Create properties file for PDF generation
val createPdfProperties by tasks.registering {
    group = "documentation"
    description = "Create properties file for PDF generation"
    dependsOn(setupDitaOt)

    val propsFile = File(ditaOtHome, "pdf.properties")
    outputs.file(propsFile)

    doLast {
        propsFile.writeText(buildString {
            appendLine("args.input=${mainBookmap.absolutePath.replace("\\", "/")}")
            appendLine("output.dir=${pdfOutputDir.absolutePath.replace("\\", "/")}")
            appendLine("transtype=pdf")
        })
    }
}

val generateHtml by tasks.registering(Exec::class) {
    group = "documentation"
    description = "Generate HTML5 documentation"
    dependsOn(createHtmlProperties)
    workingDir(ditaOtHome)

    // Set DITA_HOME environment variable to override any system setting
    environment("DITA_HOME", ditaOtHome.absolutePath)

    // Cross-platform command execution
    if (isWindows) {
        commandLine("cmd", "/c", ditaExecutable.absolutePath, "--propertyfile=html.properties")
    } else {
        commandLine(ditaExecutable.absolutePath, "--propertyfile=html.properties")
    }

    // DITA-OT may return non-zero exit codes even on success (especially on Windows)
    isIgnoreExitValue = true

    doLast {
        val files = htmlOutputDir.listFiles() ?: emptyArray()
        if (htmlOutputDir.exists() && files.size > 1) {
            println("HTML generated: ${htmlOutputDir.absolutePath}")
        } else {
            throw GradleException("HTML generation failed - output directory is empty or missing")
        }
    }
}

val generatePdf by tasks.registering(Exec::class) {
    group = "documentation"
    description = "Generate PDF documentation"
    dependsOn(createPdfProperties)
    workingDir(ditaOtHome)

    // Set DITA_HOME environment variable to override any system setting
    environment("DITA_HOME", ditaOtHome.absolutePath)

    // Cross-platform command execution
    if (isWindows) {
        commandLine("cmd", "/c", ditaExecutable.absolutePath, "--propertyfile=pdf.properties")
    } else {
        commandLine(ditaExecutable.absolutePath, "--propertyfile=pdf.properties")
    }

    // DITA-OT may return non-zero exit codes even on success (especially on Windows)
    isIgnoreExitValue = true

    doLast {
        val pdfFiles = pdfOutputDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()
        if (pdfFiles.isNotEmpty()) {
            println("PDF generated: ${pdfFiles.first().absolutePath}")
        } else {
            throw GradleException("PDF generation failed - no PDF file found in output directory")
        }
    }
}

val buildDocs by tasks.registering {
    group = "documentation"
    description = "Generate both HTML and PDF documentation"
    dependsOn(generateHtml, generatePdf)

    doLast {
        println()
        println("Documentation generated successfully!")
        println("  HTML: ${htmlOutputDir.absolutePath}")
        println("  PDF:  ${pdfOutputDir.absolutePath}")
    }
}

val cleanDocs by tasks.registering(Delete::class) {
    group = "documentation"
    description = "Clean generated documentation"
    delete(htmlOutputDir)
    delete(pdfOutputDir)
}

defaultTasks("buildDocs")
