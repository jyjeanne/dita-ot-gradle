plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.3"
}

repositories {
    mavenCentral()
}

val ditaOtVersion = "4.2.3"

// Download DITA-OT task to project root dita-ot folder
tasks.register<com.github.jyjeanne.DitaOtDownloadTask>("downloadDitaOt") {
    description = "Download DITA-OT from GitHub releases"
    group = "documentation"
    version(ditaOtVersion)
    destinationDir("dita-ot")
}

// Install DITA-OT plugins from registry
tasks.register<com.github.jyjeanne.DitaOtInstallPluginTask>("installPlugins") {
    description = "Install DITA-OT plugins from registry"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOtDir.set(file("dita-ot/dita-ot-$ditaOtVersion"))
    plugins.set(listOf(
        "net.infotexture.dita-bootstrap"  // Bootstrap 5 styled HTML output
    ))
}

// Validate DITA content using DitaOtValidateTask (fixed in v2.8.1 for DITA-OT 4.x)
tasks.register<com.github.jyjeanne.DitaOtValidateTask>("validateDita") {
    description = "Validate DITA content for errors"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOtDir(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    strictMode.set(true)
    processingMode.set("strict")
}

// Check links in DITA content (internal and external)
tasks.register<com.github.jyjeanne.DitaLinkCheckTask>("checkLinks") {
    description = "Check internal and external links in DITA content"
    group = "documentation"
    inputFiles.from(fileTree("src/dita") {
        include("**/*.dita", "**/*.ditamap", "**/*.bookmap")
    })
    checkExternal.set(true)
    failOnBroken.set(true)
}

// HTML5 output (fast feedback during writing)
tasks.register<com.github.jyjeanne.DitaOtTask>("ditaHtml") {
    description = "Build HTML5 output"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/docs/html5")
    transtype("html5")
    properties {
        property("args.gen.task.lbl", "YES")
        property("clean.temp", "YES")
    }
}

// PDF with conditional filtering
tasks.register<com.github.jyjeanne.DitaOtTask>("ditaPdf") {
    description = "Build PDF output"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/docs/pdf")
    transtype("pdf")
    properties {
        property("args.filter", file("src/dita/ditaval/pdf.ditaval").absolutePath)
        property("clean.temp", "YES")
    }
}

// Markdown output (for GitHub/GitLab wikis)
tasks.register<com.github.jyjeanne.DitaOtTask>("ditaMarkdown") {
    description = "Build Markdown output"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/docs/markdown")
    transtype("markdown_github")
    properties {
        property("clean.temp", "YES")
    }
}

// XHTML output (legacy web format)
tasks.register<com.github.jyjeanne.DitaOtTask>("ditaXhtml") {
    description = "Build XHTML output"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/docs/xhtml")
    transtype("xhtml")
    properties {
        property("clean.temp", "YES")
    }
}

// Bootstrap 5 HTML output (modern responsive web format)
tasks.register<com.github.jyjeanne.DitaOtTask>("ditaBootstrap") {
    description = "Build Bootstrap 5 HTML output"
    group = "documentation"
    dependsOn("installPlugins")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/docs/bootstrap")
    transtype("html5-bootstrap")
    properties {
        property("clean.temp", "YES")
        property("bootstrap.css.shortdesc", "YES")
        property("bootstrap.icon.hd", "bi-book")
        property("nav-toc", "full")
    }
}

// Build all formats
tasks.register("ditaAll") {
    description = "Build all documentation formats (HTML5, PDF, Markdown, XHTML, Bootstrap)"
    group = "documentation"
    dependsOn("ditaHtml", "ditaPdf", "ditaMarkdown", "ditaXhtml", "ditaBootstrap")
}

// Default build task
tasks.register("buildDocs") {
    description = "Build default documentation (HTML5)"
    group = "documentation"
    dependsOn("ditaHtml")
}

// =============================================================================
// Complete Publication Workflow
// =============================================================================

// Step 1: Download DITA-OT (reuses existing task)
// Step 2: Validate DITA content (reuses existing task)
// Step 3: Check links (reuses existing task)

// Step 4: Publish HTML, PDF, and Bootstrap for release
tasks.register("publishDocs") {
    description = "Publish documentation (HTML5, PDF, and Bootstrap)"
    group = "documentation"
    dependsOn("ditaHtml", "ditaPdf", "ditaBootstrap")
}

// Step 5: Verify generated output files exist and are not empty
tasks.register("verifyOutput") {
    description = "Verify generated documentation files exist and are not empty"
    group = "documentation"
    dependsOn("publishDocs")

    doLast {
        val errors = mutableListOf<String>()
        val verified = mutableListOf<String>()

        println("")
        println("═══════════════════════════════════════════════════════")
        println("Output Verification")
        println("═══════════════════════════════════════════════════════")

        // Verify HTML5 output
        val html5Dir = file("build/docs/html5")
        val html5Index = file("build/docs/html5/index.html")

        if (!html5Dir.exists()) {
            errors.add("HTML5 output directory does not exist: ${html5Dir.absolutePath}")
        } else if (!html5Index.exists()) {
            errors.add("HTML5 index.html does not exist")
        } else if (html5Index.length() == 0L) {
            errors.add("HTML5 index.html is empty")
        } else {
            verified.add("HTML5: index.html (${html5Index.length()} bytes)")

            // Count HTML files
            val htmlFiles = html5Dir.walkTopDown().filter { it.extension == "html" }.count()
            verified.add("HTML5: $htmlFiles HTML files generated")
        }

        // Verify PDF output
        val pdfDir = file("build/docs/pdf")
        val pdfFile = pdfDir.listFiles()?.find { it.extension == "pdf" }

        if (!pdfDir.exists()) {
            errors.add("PDF output directory does not exist: ${pdfDir.absolutePath}")
        } else if (pdfFile == null) {
            errors.add("No PDF file found in ${pdfDir.absolutePath}")
        } else if (pdfFile.length() == 0L) {
            errors.add("PDF file is empty: ${pdfFile.name}")
        } else {
            val sizeKb = pdfFile.length() / 1024
            verified.add("PDF: ${pdfFile.name} (${sizeKb} KB)")
        }

        // Verify Bootstrap HTML output
        val bootstrapDir = file("build/docs/bootstrap")
        val bootstrapToc = file("build/docs/bootstrap/toc.html")

        if (!bootstrapDir.exists()) {
            errors.add("Bootstrap output directory does not exist: ${bootstrapDir.absolutePath}")
        } else if (!bootstrapToc.exists()) {
            errors.add("Bootstrap toc.html does not exist")
        } else if (bootstrapToc.length() == 0L) {
            errors.add("Bootstrap toc.html is empty")
        } else {
            verified.add("Bootstrap: toc.html (${bootstrapToc.length()} bytes)")

            // Count HTML files
            val bootstrapFiles = bootstrapDir.walkTopDown().filter { it.extension == "html" }.count()
            verified.add("Bootstrap: $bootstrapFiles HTML files generated")
        }

        // Print results
        println("")
        println("Verified outputs:")
        verified.forEach { println("  ✓ $it") }

        if (errors.isNotEmpty()) {
            println("")
            println("Errors:")
            errors.forEach { println("  ✗ $it") }
            println("")
            println("═══════════════════════════════════════════════════════")
            println("Status: FAILED")
            println("═══════════════════════════════════════════════════════")
            throw GradleException("Output verification failed with ${errors.size} error(s)")
        }

        println("")
        println("═══════════════════════════════════════════════════════")
        println("Status: PASSED")
        println("═══════════════════════════════════════════════════════")
    }
}

// Complete publication workflow task
tasks.register("publishWorkflow") {
    description = "Complete publication workflow: download, install plugins, validate, check links, publish, verify"
    group = "documentation"

    // Define workflow steps with dependencies
    dependsOn("downloadDitaOt")

    doFirst {
        println("")
        println("═══════════════════════════════════════════════════════════════")
        println("  DITA Documentation Publication Workflow")
        println("═══════════════════════════════════════════════════════════════")
        println("")
        println("  Steps:")
        println("    1. Download DITA-OT $ditaOtVersion")
        println("    2. Install DITA-OT plugins (Bootstrap)")
        println("    3. Validate DITA content")
        println("    4. Check internal and external links")
        println("    5. Publish HTML5, PDF, and Bootstrap")
        println("    6. Verify output files")
        println("")
        println("═══════════════════════════════════════════════════════════════")
    }
}

// Wire up the workflow dependencies
tasks.named("installPlugins") {
    mustRunAfter("downloadDitaOt")
}

tasks.named("validateDita") {
    mustRunAfter("installPlugins")
}

tasks.named("checkLinks") {
    mustRunAfter("validateDita")
}

tasks.named("ditaHtml") {
    mustRunAfter("checkLinks")
}

tasks.named("ditaPdf") {
    mustRunAfter("checkLinks")
}

tasks.named("ditaBootstrap") {
    mustRunAfter("checkLinks")
}

tasks.named("publishDocs") {
    mustRunAfter("checkLinks")
}

tasks.named("verifyOutput") {
    mustRunAfter("publishDocs")
}

tasks.named("publishWorkflow") {
    finalizedBy("installPlugins", "validateDita", "checkLinks", "publishDocs", "verifyOutput")
}
