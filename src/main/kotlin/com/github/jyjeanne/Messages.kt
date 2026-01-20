package com.github.jyjeanne

/**
 * Centralized error messages with contextual suggestions.
 *
 * All error messages should:
 * 1. Clearly state what went wrong
 * 2. Provide context (file paths, values, etc.)
 * 3. Suggest actionable fixes
 * 4. Include documentation links where appropriate
 *
 * @since 1.0.0
 */
object Messages {

    // ==================== DITA-OT Configuration Errors ====================

    val ditaHomeError = """
        DITA-OT directory not configured.

        Please configure it in your build.gradle:

        dita {
            ditaOt '/path/to/dita-ot'
            input 'my.ditamap'
            transtype 'html5'
        }

        Or in build.gradle.kts:

        tasks.named<DitaOtTask>("dita") {
            ditaOt(file("/path/to/dita-ot"))
            input("my.ditamap")
            transtype("html5")
        }

        To download DITA-OT automatically, use DitaOtDownloadTask:

        tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
            version.set("4.2.3")
        }

        Documentation: https://github.com/jyjeanne/dita-ot-gradle
    """.trimIndent()

    fun ditaHomeNotFound(path: String) = """
        DITA-OT directory does not exist: $path

        Possible causes:
        1. The path is incorrect
        2. DITA-OT has not been downloaded/installed yet
        3. The directory was deleted

        Solutions:
        A) Download DITA-OT automatically:
           tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
               version.set("4.2.3")
           }

        B) Download manually from: https://www.dita-ot.org/download

        C) Check the configured path: $path
    """.trimIndent()

    fun invalidDitaHome(path: String, missingFile: String) = """
        Invalid DITA-OT directory: $path

        Expected file not found: $missingFile

        This does not appear to be a valid DITA-OT installation.

        A valid DITA-OT directory should contain:
        - build.xml (ANT build file)
        - bin/dita or bin/dita.bat (command-line tool)
        - plugins/ (plugins directory)
        - lib/ (libraries)

        Solutions:
        1. Verify you're pointing to the root DITA-OT directory (e.g., dita-ot-4.2.3)
        2. Re-download DITA-OT if the installation is corrupted
        3. Check file permissions
    """.trimIndent()

    // ==================== Input File Errors ====================

    fun inputFileNotFound(path: String) = """
        Input file does not exist: $path

        Please verify:
        1. The file path is correct
        2. The file exists
        3. You have read permissions

        Tip: Use absolute paths or paths relative to the project root.

        Example:
        dita {
            input 'src/docs/manual.ditamap'
        }
    """.trimIndent()

    fun inputFileInvalid(path: String, reason: String) = """
        Invalid input file: $path

        Reason: $reason

        DITA-OT supports these input types:
        - DITA maps (.ditamap)
        - DITA topics (.dita, .xml)
        - DITA bookmaps (.bookmap)

        Ensure the file is valid XML and conforms to DITA DTD/Schema.
    """.trimIndent()

    // ==================== Transtype Errors ====================

    fun invalidTranstype(transtype: String, available: List<String>) = """
        Invalid output format (transtype): $transtype

        Available transtypes in your DITA-OT installation:
        ${available.joinToString("\n") { "  - $it" }}

        Common transtypes:
        - html5      (HTML5 output)
        - pdf        (PDF using FOP)
        - xhtml      (XHTML output)
        - eclipsehelp (Eclipse Help)

        To see all available transtypes, run:
        dita --transtypes

        To install additional transtypes, use DitaOtInstallPluginTask.
    """.trimIndent()

    // ==================== Transformation Errors ====================

    fun transformationFailed(transtype: String, exitCode: Int, logFile: String?) = """
        DITA-OT transformation failed for transtype: $transtype

        Exit code: $exitCode
        ${if (logFile != null) "Log file: $logFile" else ""}

        Common causes and solutions:

        Exit code 1 - General error:
        - Check your DITA content for XML errors
        - Verify all referenced files exist
        - Check DITA-OT log for specific error messages

        Exit code 2 - Configuration error:
        - Verify transtype '$transtype' is installed
        - Check property values for typos

        Exit code 255 (Windows) - Script error:
        - Usually indicates success on Windows despite exit code
        - Check if output was actually generated

        Troubleshooting steps:
        1. Run with --info or --debug for more details
        2. Check the DITA-OT log in the temp directory
        3. Validate your DITA content with 'dita --validate'
        4. Try running DITA-OT directly from command line
    """.trimIndent()

    // ==================== Plugin Installation Errors ====================

    fun pluginNotFound(pluginId: String) = """
        Plugin not found: $pluginId

        The plugin could not be found in the DITA-OT plugin registry.

        Possible solutions:
        1. Verify the plugin ID is correct
        2. Check if the plugin is available: https://www.dita-ot.org/plugins
        3. Use a direct URL to the plugin ZIP file
        4. Install from a local file path

        Examples:
        plugins.set(listOf(
            "org.dita.pdf2",                              // Registry
            "https://example.com/my-plugin.zip",          // URL
            "/path/to/local/plugin.zip"                   // Local file
        ))
    """.trimIndent()

    fun pluginInstallFailed(pluginId: String, error: String) = """
        Failed to install plugin: $pluginId

        Error: $error

        Troubleshooting:
        1. Check network connectivity (for remote plugins)
        2. Verify the plugin is compatible with your DITA-OT version
        3. Check available disk space
        4. Try installing manually: dita --install $pluginId

        If the plugin requires dependencies, install them first:
        plugins.set(listOf("dependency-plugin", "$pluginId"))
    """.trimIndent()

    // ==================== Download Errors ====================

    fun downloadFailed(url: String, statusCode: Int) = """
        Failed to download DITA-OT

        URL: $url
        HTTP Status: $statusCode

        Possible causes:
        ${when (statusCode) {
            404 -> "- The requested DITA-OT version does not exist"
            403 -> "- Access forbidden (rate limiting or authentication required)"
            500, 502, 503 -> "- GitHub server error (try again later)"
            else -> "- Network or server error"
        }}

        Solutions:
        1. Verify the DITA-OT version exists: https://github.com/dita-ot/dita-ot/releases
        2. Check your network connection
        3. Try using a different version
        4. Download manually and configure ditaOt path
    """.trimIndent()

    fun downloadNetworkError(url: String, error: String) = """
        Network error while downloading DITA-OT

        URL: $url
        Error: $error

        Possible causes:
        - No internet connection
        - Firewall blocking the connection
        - Proxy configuration required
        - DNS resolution failure

        Solutions:
        1. Check your internet connection
        2. Configure proxy settings if required
        3. Download manually from: https://www.dita-ot.org/download
    """.trimIndent()

    // ==================== Classpath Errors ====================

    fun classpathError(ditaHome: String, details: String) = """
        Failed to build DITA-OT classpath

        DITA-OT directory: $ditaHome
        Error: $details

        This usually indicates a corrupted or incomplete DITA-OT installation.

        Solutions:
        1. Re-download DITA-OT using DitaOtDownloadTask with forceReinstall = true
        2. Check that all required JAR files exist in lib/ directory
        3. Verify file permissions
    """.trimIndent()

    // ==================== Configuration Cache Warnings ====================

    val configurationCacheWarning = """
        Configuration cache compatibility warning

        The current configuration uses features that are not compatible with
        Gradle's configuration cache (Groovy closures for properties).

        For full configuration cache support, use the Kotlin DSL:

        tasks.named<DitaOtTask>("dita") {
            properties {
                "nav-toc" to "full"
                "args.css" to "custom.css"
            }
        }

        Or use antExecutionStrategy("DITA_SCRIPT") for the default strategy.
    """.trimIndent()

    // ==================== Version Warnings ====================

    fun oldDitaOtVersionWarning(version: String) = """
        DITA-OT version $version is outdated.

        Recommendation: Upgrade to DITA-OT 4.x for:
        - Better performance
        - More output formats
        - Improved error messages
        - Security fixes

        Download: https://www.dita-ot.org/download
    """.trimIndent()

    // ==================== Help Messages ====================

    val quickStartGuide = """
        DITA-OT Gradle Plugin Quick Start
        ==================================

        1. Apply the plugin:
           plugins {
               id("io.github.jyjeanne.dita-ot-gradle") version "2.4.0"
           }

        2. Download DITA-OT:
           tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
               version.set("4.2.3")
           }

        3. Configure transformation:
           dita {
               ditaOt("build/dita-ot/dita-ot-4.2.3")
               input("src/docs/manual.ditamap")
               transtype("html5", "pdf")
               output("build/docs")
           }

        4. Run: ./gradlew dita

        Documentation: https://github.com/jyjeanne/dita-ot-gradle
    """.trimIndent()
}
