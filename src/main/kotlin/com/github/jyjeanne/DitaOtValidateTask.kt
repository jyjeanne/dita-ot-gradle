package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Gradle task for validating DITA content without full transformation.
 *
 * This task validates DITA maps and topics using DITA-OT's preprocessing
 * to catch errors early without the overhead of full transformation.
 *
 * **Usage (Groovy DSL):**
 * ```groovy
 * tasks.register('validateDita', DitaOtValidateTask) {
 *     ditaOtDir = layout.buildDirectory.dir('dita-ot/dita-ot-4.2.3')
 *     input 'docs/guide.ditamap'
 *     strictMode = true  // Fail on warnings
 * }
 * ```
 *
 * **Usage (Kotlin DSL):**
 * ```kotlin
 * tasks.register<DitaOtValidateTask>("validateDita") {
 *     ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-4.2.3"))
 *     input("docs/guide.ditamap")
 *     strictMode.set(true)  // Fail on warnings
 * }
 * ```
 *
 * **Validation Checks:**
 * - XML well-formedness
 * - DTD/Schema validity
 * - Reference integrity (conrefs, topicrefs, xrefs)
 * - Key definitions and references
 * - Image and resource references
 *
 * @since 2.5.0
 */
abstract class DitaOtValidateTask @Inject constructor(
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    companion object {
        /** Task name constant */
        const val TASK_NAME = "validateDita"

        /** Default process timeout in milliseconds (10 minutes) */
        private const val DEFAULT_TIMEOUT = 600_000L

        /** Valid processing modes */
        private val VALID_PROCESSING_MODES = setOf("strict", "lax", "skip")

        /** Maximum output size (200KB) */
        private const val MAX_OUTPUT_SIZE = 200 * 1024

        /**
         * Pattern for DITA-OT error messages.
         * DITA-OT message format: DOT[component][number][severity]
         * Severity codes: E=Error, F=Fatal, W=Warning, I=Info
         * Only matches messages ending with E (Error) or F (Fatal).
         */
        private val ERROR_PATTERN = Pattern.compile(
            "\\[DOT[A-Z]\\d{3,4}[EF]\\]|(?<!\\w)ERROR(?!\\w)|(?<!\\w)FATAL(?!\\w)|(?<![A-Z])Error:|(?<![a-z])error:",
            Pattern.CASE_INSENSITIVE
        )

        /**
         * Pattern for DITA-OT warning messages.
         * Only matches messages ending with W (Warning).
         */
        private val WARNING_PATTERN = Pattern.compile(
            "\\[DOT[A-Z]\\d{3,4}W\\]|(?<!\\w)WARN(?!ING\\w)|Warning:|warning:",
            Pattern.CASE_INSENSITIVE
        )

        /**
         * Pattern for DITA-OT informational messages.
         * Messages ending with I are informational and should not be treated as errors.
         * Example: DOTJ031I - missing DITAVAL rule (informational, not an error)
         */
        private val INFO_PATTERN = Pattern.compile(
            "\\[DOT[A-Z]\\d{3,4}I\\]",
            Pattern.CASE_INSENSITIVE
        )

        /** Pattern to extract file location from error messages */
        private val FILE_LOCATION_PATTERN = Pattern.compile(
            "(?:file:/*)?([^:]+\\.(?:dita|ditamap|xml))(?::(\\d+))?",
            Pattern.CASE_INSENSITIVE
        )
    }

    // ==================== Properties ====================

    /**
     * DITA-OT installation directory.
     */
    @get:Internal
    abstract val ditaOtDir: DirectoryProperty

    /**
     * Input files (DITA maps or topics) to validate.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * DITAVAL filter file (optional).
     * Used to filter content during validation.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val filterFile: ConfigurableFileCollection

    /**
     * Strict mode - fail on warnings in addition to errors.
     * Defaults to false.
     */
    @get:Input
    abstract val strictMode: Property<Boolean>

    /**
     * Whether to fail the build on validation errors.
     * Defaults to true.
     */
    @get:Input
    abstract val failOnError: Property<Boolean>

    /**
     * Whether to suppress progress output.
     * Defaults to false.
     */
    @get:Input
    abstract val quiet: Property<Boolean>

    /**
     * Processing mode for DITA-OT.
     * Options: "strict", "lax", "skip"
     * Defaults to "strict".
     */
    @get:Input
    abstract val processingMode: Property<String>

    /**
     * Timeout for validation process in milliseconds.
     * Defaults to 600000 (10 minutes).
     */
    @get:Input
    abstract val validationTimeout: Property<Long>

    // ==================== Internal Properties ====================

    /**
     * Get the DITA executable path.
     */
    @get:Internal
    val ditaExecutable: File
        get() = Platform.getDitaExecutable(ditaOtDir.asFile.get())

    // ==================== Initialization ====================

    init {
        group = "Verification"
        description = "Validates DITA content without full transformation"

        // Set defaults
        strictMode.convention(false)
        failOnError.convention(true)
        quiet.convention(false)
        processingMode.convention("strict")
        validationTimeout.convention(DEFAULT_TIMEOUT)
    }

    // ==================== DSL Methods ====================

    /**
     * Set DITA-OT directory (Groovy DSL friendly).
     * Supports File, Directory, Provider<Directory>, and String types.
     */
    fun ditaOtDir(dir: Any) {
        when (dir) {
            is File -> ditaOtDir.set(dir)
            is Directory -> ditaOtDir.set(dir)
            is Provider<*> -> {
                // Handle Provider<Directory> - resolve the value now since the provider
                // should have a value at task configuration time (after dependent tasks configure)
                @Suppress("UNCHECKED_CAST")
                val value = (dir as Provider<*>).get()
                when (value) {
                    is Directory -> ditaOtDir.set(value)
                    is File -> ditaOtDir.set(value)
                    else -> ditaOtDir.set(projectLayout.projectDirectory.dir(value.toString()))
                }
            }
            is String -> ditaOtDir.set(projectLayout.projectDirectory.dir(dir))
            else -> ditaOtDir.set(projectLayout.projectDirectory.dir(dir.toString()))
        }
    }

    /**
     * Add input file to validate (Groovy DSL friendly).
     */
    fun input(i: Any) {
        inputFiles.from(i)
    }

    /**
     * Set DITAVAL filter file (Groovy DSL friendly).
     */
    fun filter(f: Any) {
        val file = when (f) {
            is File -> f
            is String -> projectLayout.projectDirectory.file(f).asFile
            else -> projectLayout.projectDirectory.file(f.toString()).asFile
        }
        filterFile.from(file)
    }

    // ==================== Task Action ====================

    @TaskAction
    fun validate() {
        val ditaHome = ditaOtDir.asFile.get()
        val inputs = inputFiles.files
        val isQuiet = quiet.get()

        // Validate DITA-OT installation
        validateDitaOtInstallation(ditaHome)

        // Validate processing mode
        val mode = processingMode.get()
        if (mode !in VALID_PROCESSING_MODES) {
            throw GradleException(
                """
                Invalid processing mode: '$mode'

                Valid options are: ${VALID_PROCESSING_MODES.joinToString(", ")}
                """.trimIndent()
            )
        }

        if (!isQuiet) {
            logger.lifecycle("DITA Content Validation")
            logger.lifecycle("  DITA-OT: ${ditaHome.absolutePath}")
            logger.lifecycle("  Files to validate: ${inputs.size}")
            logger.lifecycle("  Strict mode: ${strictMode.get()}")
            logger.lifecycle("  Processing mode: ${processingMode.get()}")
        }

        if (inputs.isEmpty()) {
            if (!isQuiet) {
                logger.lifecycle("  No input files specified - nothing to validate")
            }
            return
        }

        val allResults = mutableListOf<ValidationResult>()

        inputs.forEach { inputFile ->
            if (!isQuiet) {
                logger.lifecycle("")
                logger.lifecycle("Validating: ${inputFile.name}")
            }

            val result = validateFile(ditaHome, inputFile)
            allResults.add(result)

            if (!isQuiet) {
                logValidationResult(result)
            }
        }

        // Print summary
        printSummary(allResults)

        // Check if we should fail
        val totalErrors = allResults.sumOf { it.errors.size }
        val totalWarnings = allResults.sumOf { it.warnings.size }

        if (failOnError.get()) {
            if (totalErrors > 0) {
                throw GradleException(
                    buildErrorMessage(totalErrors, totalWarnings, allResults)
                )
            }
            if (strictMode.get() && totalWarnings > 0) {
                throw GradleException(
                    """
                    Validation failed in strict mode: $totalWarnings warning(s) found.

                    Disable strict mode to allow warnings:
                        strictMode.set(false)

                    Or fix the warnings listed above.
                    """.trimIndent()
                )
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Validate that DITA-OT is properly installed.
     */
    private fun validateDitaOtInstallation(ditaHome: File) {
        if (!ditaHome.exists()) {
            throw GradleException(
                """
                DITA-OT directory does not exist: ${ditaHome.absolutePath}

                Please ensure DITA-OT is installed. You can use DitaOtDownloadTask:

                tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
                    version.set("4.2.3")
                    destinationDir.set(layout.buildDirectory.dir("dita-ot"))
                }

                Then add: dependsOn(downloadDitaOt)
                """.trimIndent()
            )
        }

        if (!ditaExecutable.exists()) {
            throw GradleException(
                """
                DITA executable not found: ${ditaExecutable.absolutePath}

                This task requires DITA-OT 3.0+ with the dita command-line tool.
                """.trimIndent()
            )
        }
    }

    /**
     * Validate a single DITA file.
     */
    private fun validateFile(ditaHome: File, inputFile: File): ValidationResult {
        val errors = mutableListOf<ValidationMessage>()
        val warnings = mutableListOf<ValidationMessage>()
        val timeoutMs = validationTimeout.get()

        // Create temp directory for validation output
        val tempDir = File(ditaHome, "temp/validate-${System.currentTimeMillis()}")

        return try {
            // Build validation command
            // We use a minimal transformation that validates content
            val command = buildValidationCommand(ditaHome, inputFile, tempDir)

            logger.debug("Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(ditaHome)
            processBuilder.redirectErrorStream(true)

            // Set environment
            val env = processBuilder.environment()
            env["DITA_HOME"] = ditaHome.absolutePath

            val process = processBuilder.start()

            // Capture output
            val output = StringBuilder()
            var outputTruncated = false

            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (output.length < MAX_OUTPUT_SIZE) {
                        output.appendLine(line)
                    } else if (!outputTruncated) {
                        output.appendLine("... (output truncated)")
                        outputTruncated = true
                    }

                    // Parse for errors and warnings
                    parseOutputLine(line, inputFile, errors, warnings)

                    logger.debug("  $line")
                }
            }

            // Wait for process with timeout
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                errors.add(ValidationMessage(
                    inputFile.absolutePath,
                    null,
                    "Validation timed out after ${timeoutMs / 1000} seconds"
                ))
            }

            val exitCode = process.exitValue()

            // If exit code is non-zero and we haven't captured specific errors,
            // add a general error
            if (exitCode != 0 && errors.isEmpty()) {
                // Extract meaningful error from output
                // IMPORTANT: Exclude INFO messages (like DOTJ031I) which may contain
                // "ERROR" keyword in their description but are not actual errors
                val errorLines = output.lines()
                    .filter { line ->
                        ERROR_PATTERN.matcher(line).find() &&
                        !INFO_PATTERN.matcher(line).find()  // Skip info messages
                    }
                    .take(5)

                if (errorLines.isNotEmpty()) {
                    errorLines.forEach { line ->
                        errors.add(ValidationMessage(
                            extractFileFromMessage(line) ?: inputFile.absolutePath,
                            extractLineNumber(line),
                            line.trim()
                        ))
                    }
                } else {
                    errors.add(ValidationMessage(
                        inputFile.absolutePath,
                        null,
                        "Validation failed with exit code: $exitCode"
                    ))
                }
            }

            ValidationResult(
                file = inputFile,
                success = errors.isEmpty(),
                errors = errors,
                warnings = warnings,
                output = output.toString()
            )
        } catch (e: Exception) {
            errors.add(ValidationMessage(
                inputFile.absolutePath,
                null,
                e.message ?: "Unknown error during validation"
            ))
            ValidationResult(
                file = inputFile,
                success = false,
                errors = errors,
                warnings = warnings,
                output = ""
            )
        } finally {
            // Clean up temp directory
            cleanupTempDirectory(tempDir)
        }
    }

    /**
     * Clean up temporary validation directory.
     */
    private fun cleanupTempDirectory(tempDir: File) {
        try {
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                logger.debug("Cleaned up temp directory: ${tempDir.absolutePath}")
            }
        } catch (e: Exception) {
            logger.debug("Failed to clean up temp directory: ${e.message}")
        }
    }

    /**
     * Build the DITA-OT command for validation.
     */
    private fun buildValidationCommand(ditaHome: File, inputFile: File, tempDir: File): List<String> {
        val command = mutableListOf<String>()

        if (Platform.isWindows) {
            command.add("cmd")
            command.add("/c")
        }

        command.add(ditaExecutable.absolutePath)

        // Use 'dita' transtype for validation (compatible with DITA-OT 3.x+)
        // Note: 'preprocess' transtype was removed in DITA-OT 4.x
        // The 'dita' transtype performs preprocessing and outputs normalized DITA
        // Using separate arguments for cross-platform compatibility with paths containing spaces
        command.add("--input")
        command.add(inputFile.absolutePath)

        command.add("--format")
        command.add("dita")

        // Use the provided temporary output directory
        command.add("--output")
        command.add(tempDir.absolutePath)

        // Set processing mode
        command.add("--processing-mode")
        command.add(processingMode.get())

        // Add filter if specified
        filterFile.files.firstOrNull()?.let { filter ->
            command.add("--filter")
            command.add(filter.absolutePath)
        }

        // Verbose output for better error messages
        command.add("-v")

        return command
    }

    /**
     * Parse an output line for errors and warnings.
     *
     * DITA-OT message severity is determined by the suffix:
     * - E = Error (e.g., DOTJ012E)
     * - F = Fatal (e.g., DOTJ001F)
     * - W = Warning (e.g., DOTJ031W)
     * - I = Info (e.g., DOTJ031I) - NOT an error, just informational
     *
     * Info messages like DOTJ031I ("No rule for X was found in DITAVAL file")
     * are informational only and should not cause validation to fail.
     */
    private fun parseOutputLine(
        line: String,
        defaultFile: File,
        errors: MutableList<ValidationMessage>,
        warnings: MutableList<ValidationMessage>
    ) {
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) return

        // Skip informational messages (ending with I) - they are not errors
        if (INFO_PATTERN.matcher(trimmedLine).find()) {
            logger.debug("  [INFO] $trimmedLine")
            return
        }

        when {
            ERROR_PATTERN.matcher(trimmedLine).find() -> {
                errors.add(ValidationMessage(
                    extractFileFromMessage(trimmedLine) ?: defaultFile.absolutePath,
                    extractLineNumber(trimmedLine),
                    trimmedLine
                ))
            }
            WARNING_PATTERN.matcher(trimmedLine).find() -> {
                warnings.add(ValidationMessage(
                    extractFileFromMessage(trimmedLine) ?: defaultFile.absolutePath,
                    extractLineNumber(trimmedLine),
                    trimmedLine
                ))
            }
        }
    }

    /**
     * Extract file path from error message.
     */
    private fun extractFileFromMessage(message: String): String? {
        val matcher = FILE_LOCATION_PATTERN.matcher(message)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    /**
     * Extract line number from error message.
     */
    private fun extractLineNumber(message: String): Int? {
        val matcher = FILE_LOCATION_PATTERN.matcher(message)
        return if (matcher.find() && matcher.groupCount() >= 2) {
            matcher.group(2)?.toIntOrNull()
        } else {
            // Try to find line number in format "line X" or ":X:"
            val linePattern = Pattern.compile("(?:line\\s+|:(\\d+):)")
            val lineMatcher = linePattern.matcher(message)
            if (lineMatcher.find()) {
                lineMatcher.group(1)?.toIntOrNull()
            } else {
                null
            }
        }
    }

    /**
     * Log validation result for a single file.
     */
    private fun logValidationResult(result: ValidationResult) {
        if (result.success && result.warnings.isEmpty()) {
            logger.lifecycle("  ✓ Valid")
        } else {
            result.errors.forEach { error ->
                val location = if (error.line != null) "${error.file}:${error.line}" else error.file
                logger.error("  ✗ ERROR [$location]: ${error.message}")
            }
            result.warnings.forEach { warning ->
                val location = if (warning.line != null) "${warning.file}:${warning.line}" else warning.file
                logger.warn("  ⚠ WARN [$location]: ${warning.message}")
            }
            if (result.errors.isEmpty()) {
                logger.lifecycle("  ✓ Valid (with ${result.warnings.size} warning(s))")
            }
        }
    }

    /**
     * Print validation summary.
     */
    private fun printSummary(results: List<ValidationResult>) {
        val totalFiles = results.size
        val validFiles = results.count { it.success && it.warnings.isEmpty() }
        val filesWithWarnings = results.count { it.success && it.warnings.isNotEmpty() }
        val invalidFiles = results.count { !it.success }
        val totalErrors = results.sumOf { it.errors.size }
        val totalWarnings = results.sumOf { it.warnings.size }

        if (!quiet.get()) {
            logger.lifecycle("")
            logger.lifecycle("═══════════════════════════════════════════════════════")
            logger.lifecycle("DITA Validation Summary")
            logger.lifecycle("═══════════════════════════════════════════════════════")
            logger.lifecycle("Files validated:    $totalFiles")
            logger.lifecycle("Valid:              $validFiles")
            logger.lifecycle("Valid with warnings: $filesWithWarnings")
            logger.lifecycle("Invalid:            $invalidFiles")
            logger.lifecycle("───────────────────────────────────────────────────────")
            logger.lifecycle("Total errors:       $totalErrors")
            logger.lifecycle("Total warnings:     $totalWarnings")
            logger.lifecycle("───────────────────────────────────────────────────────")

            val status = when {
                totalErrors > 0 -> "FAILED"
                strictMode.get() && totalWarnings > 0 -> "FAILED (strict mode)"
                totalWarnings > 0 -> "PASSED (with warnings)"
                else -> "PASSED"
            }
            logger.lifecycle("Status:             $status")
            logger.lifecycle("═══════════════════════════════════════════════════════")
        }
    }

    /**
     * Build detailed error message for failure.
     */
    private fun buildErrorMessage(
        totalErrors: Int,
        totalWarnings: Int,
        results: List<ValidationResult>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("DITA validation failed: $totalErrors error(s), $totalWarnings warning(s)")
        sb.appendLine()

        results.filter { it.errors.isNotEmpty() }.forEach { result ->
            sb.appendLine("File: ${result.file.name}")
            result.errors.take(10).forEach { error ->
                val location = if (error.line != null) ":${error.line}" else ""
                sb.appendLine("  - ${error.file}$location")
                sb.appendLine("    ${error.message}")
            }
            if (result.errors.size > 10) {
                sb.appendLine("  ... and ${result.errors.size - 10} more errors")
            }
            sb.appendLine()
        }

        sb.appendLine("Troubleshooting:")
        sb.appendLine("  1. Check the file paths and ensure all referenced files exist")
        sb.appendLine("  2. Verify XML syntax is well-formed")
        sb.appendLine("  3. Ensure conrefs and xrefs point to valid targets")
        sb.appendLine("  4. Run with --info for detailed DITA-OT output")

        return sb.toString()
    }

    // ==================== Data Classes ====================

    /**
     * Validation result for a single file.
     */
    data class ValidationResult(
        val file: File,
        val success: Boolean,
        val errors: List<ValidationMessage>,
        val warnings: List<ValidationMessage>,
        val output: String
    )

    /**
     * A validation message (error or warning).
     */
    data class ValidationMessage(
        val file: String,
        val line: Int?,
        val message: String
    )
}
