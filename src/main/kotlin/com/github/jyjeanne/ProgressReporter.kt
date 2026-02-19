package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * Progress reporter for DITA-OT transformations.
 *
 * Parses DITA-OT output to detect processing stages and displays
 * visual progress indicators during transformation.
 *
 * @param logger The Gradle logger for output
 * @param style The progress display style (DETAILED, SIMPLE, MINIMAL, QUIET)
 * @param showWarnings Whether to display warnings during transformation (default: false)
 *        When false, warnings are still collected for the summary count but not displayed.
 *        When true, warnings are displayed in all modes except QUIET.
 *        This is useful to suppress verbose FOP warnings during PDF generation.
 *
 * @since 2.8.0
 */
class ProgressReporter(
    private val logger: Logger,
    private val style: ProgressStyle = ProgressStyle.DETAILED,
    private val showWarnings: Boolean = false
) {
    /**
     * Progress display style.
     */
    enum class ProgressStyle {
        /** Show detailed progress with stage names and file counts */
        DETAILED,
        /** Show simple progress bar only */
        SIMPLE,
        /** Show minimal output (stages only) */
        MINIMAL,
        /** No progress output, only errors */
        QUIET
    }

    /**
     * Processing stages in DITA-OT transformation.
     */
    enum class Stage(val displayName: String, val progressPercent: Int) {
        INIT("Initializing", 0),
        PREPROCESS("Preprocessing", 10),
        PREPROCESS_DEBUG_FILTER("Filtering debug attributes", 15),
        PREPROCESS_MAPREF("Resolving map references", 20),
        PREPROCESS_BRANCH_FILTER("Applying branch filters", 25),
        PREPROCESS_KEYREF("Resolving key references", 30),
        PREPROCESS_COPY_FILES("Copying files", 35),
        PREPROCESS_CONREF("Resolving content references", 40),
        PREPROCESS_CODEREF("Resolving code references", 45),
        PREPROCESS_MAP_LINK("Building map links", 50),
        PREPROCESS_CHUNK("Chunking topics", 55),
        PREPROCESS_MOVE_META("Moving metadata", 60),
        PREPROCESS_MAP_PULL("Processing map pull", 65),
        TRANSFORM("Transforming", 70),
        GENERATE_OUTER("Generating outer content", 75),
        GENERATE_INNER("Generating content", 80),
        COPY_RESOURCES("Copying resources", 85),
        CSS_COPY("Copying CSS", 90),
        FINALIZE("Finalizing", 95),
        COMPLETE("Complete", 100);

        companion object {
            /**
             * Detect stage from DITA-OT log message.
             * Order matters: more specific checks come before general ones.
             */
            fun fromLogMessage(message: String): Stage? {
                val lowerMessage = message.lowercase()
                return when {
                    // Completion checks first (highest priority)
                    lowerMessage.contains("build successful") || lowerMessage.contains("build finished") -> COMPLETE

                    // Preprocessing stages (specific before general)
                    lowerMessage.contains("debug-filter") -> PREPROCESS_DEBUG_FILTER
                    lowerMessage.contains("mapref") -> PREPROCESS_MAPREF
                    lowerMessage.contains("branch-filter") -> PREPROCESS_BRANCH_FILTER
                    lowerMessage.contains("conkeyref") -> PREPROCESS_KEYREF // conkeyref before keyref
                    lowerMessage.contains("keyref") -> PREPROCESS_KEYREF
                    lowerMessage.contains("copy-files") || lowerMessage.contains("copying files") -> PREPROCESS_COPY_FILES
                    lowerMessage.contains("coderef") -> PREPROCESS_CODEREF // coderef before conref
                    lowerMessage.contains("conref") -> PREPROCESS_CONREF
                    lowerMessage.contains("maplink") || lowerMessage.contains("map-link") -> PREPROCESS_MAP_LINK
                    lowerMessage.contains("chunk") -> PREPROCESS_CHUNK
                    lowerMessage.contains("move-meta") -> PREPROCESS_MOVE_META
                    lowerMessage.contains("mappull") || lowerMessage.contains("map-pull") -> PREPROCESS_MAP_PULL
                    lowerMessage.contains("preprocess") -> PREPROCESS

                    // Transformation stages (specific before general)
                    lowerMessage.contains("dita.xsl.html5") || lowerMessage.contains("dita.xsl.xhtml") -> GENERATE_INNER
                    lowerMessage.contains("xslt") && !lowerMessage.contains("preprocess") -> GENERATE_INNER
                    lowerMessage.contains("outer") && lowerMessage.contains("generat") -> GENERATE_OUTER
                    lowerMessage.contains("transform") -> TRANSFORM

                    // Output stages
                    lowerMessage.contains("copying css") || lowerMessage.contains("copy-css") ||
                        (lowerMessage.contains(".css") && lowerMessage.contains("copy")) -> CSS_COPY
                    lowerMessage.contains("copy") && lowerMessage.contains("resource") -> COPY_RESOURCES

                    // Init (last, most general)
                    lowerMessage.contains("init") && !lowerMessage.contains("processing") -> INIT

                    else -> null
                }
            }
        }
    }

    // Progress bar constants
    companion object {
        private const val BAR_WIDTH = 30
        private const val FILLED_CHAR = "="
        private const val EMPTY_CHAR = " "
        private const val HEAD_CHAR = ">"

        // Pattern to match DITA-OT log messages
        private val LOG_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]\\s+(\\w+):\\s+(.*)$")
        private val FILE_PATTERN = Pattern.compile("(Processing|Reading|Writing|Transforming)\\s+(.+\\.(dita|ditamap|xml|html|pdf))")

        /**
         * Pattern for DITA-OT error messages using structured message codes ONLY.
         *
         * DITA-OT message code format: [PREFIX][NUMBER][SEVERITY]
         * Prefixes: DOTA (Ant/core), DOTJ (Java), DOTX (XSLT),
         *           INDX (Index), PDFJ (PDF/Java), PDFX (PDF/XSL-FO), XEPJ (XEP)
         * Severity: E=Error, F=Fatal
         *
         * Generic markers ([ERROR], Error:, Exception) from third-party libraries
         * (Apache FOP, Batik) are excluded — they cause false positives and
         * double-counting of stack traces.
         */
        private val ERROR_PATTERN = Pattern.compile(
            "\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]"
        )
        private val WARNING_PATTERN = Pattern.compile(
            "\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]"
        )
    }

    @Volatile
    private var currentStage = Stage.INIT
    private val filesProcessed = AtomicInteger(0)
    private val errors: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile
    private var lastProgressLine = ""
    private val isTerminalSupported = System.console() != null

    /**
     * Process output stream from DITA-OT and report progress.
     *
     * @param inputStream The stdout/stderr from DITA-OT process
     * @param onComplete Callback when processing is complete
     */
    fun processOutput(inputStream: InputStream, onComplete: (Boolean) -> Unit) {
        var hasErrors = false

        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (!Thread.currentThread().isInterrupted && line != null) {
                    processLine(line)
                    // Count as error if line contains a DITA-OT error/fatal code
                    if (ERROR_PATTERN.matcher(line).find()) {
                        hasErrors = true
                    }
                    line = reader.readLine()
                }
            }

            onComplete(!hasErrors)
        } catch (e: InterruptedException) {
            // Thread was interrupted, exit gracefully
            Thread.currentThread().interrupt()
            onComplete(false)
        } catch (e: Exception) {
            // Check if this is an interrupt wrapped in IOException
            if (Thread.currentThread().isInterrupted) {
                onComplete(false)
            } else {
                logger.error("Error processing DITA-OT output: ${e.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Process a single line of DITA-OT output.
     *
     * DITA-OT message severity is determined by the suffix:
     * - E = Error (e.g., DOTJ012E)
     * - F = Fatal (e.g., DOTJ001F)
     * - W = Warning (e.g., DOTJ031W)
     * - I = Info (e.g., DOTJ031I) - NOT an error, just informational
     *
     * Info messages like DOTJ031I ("No rule for X was found in DITAVAL file")
     * are informational only and should not be treated as errors.
     */
    private fun processLine(line: String) {
        // Detect stage changes
        val newStage = Stage.fromLogMessage(line)
        if (newStage != null && newStage.ordinal > currentStage.ordinal) {
            currentStage = newStage
            updateProgress(currentStage)
        }

        // Count processed files
        val fileMatcher = FILE_PATTERN.matcher(line)
        if (fileMatcher.find()) {
            filesProcessed.incrementAndGet()
        }

        // Collect errors and warnings (DITA-OT message codes only)
        if (ERROR_PATTERN.matcher(line).find()) {
            errors.add(line)
            if (style != ProgressStyle.QUIET) {
                clearProgressLine()
                logger.error(line)
            }
        } else if (WARNING_PATTERN.matcher(line).find()) {
            // Always collect warnings for summary count
            warnings.add(line)
            // Display warnings if showWarnings is enabled (except in QUIET mode)
            if (showWarnings && style != ProgressStyle.QUIET) {
                clearProgressLine()
                logger.warn(line)
            }
        } else if (style == ProgressStyle.DETAILED) {
            // In detailed mode, show all INFO messages
            val logMatcher = LOG_PATTERN.matcher(line)
            if (logMatcher.matches()) {
                val level = logMatcher.group(2)
                if (level == "INFO") {
                    logger.debug(line)
                }
            }
        }
    }

    /**
     * Update the progress display.
     */
    private fun updateProgress(stage: Stage) {
        if (style == ProgressStyle.QUIET) return

        val percent = stage.progressPercent
        val progressBar = buildProgressBar(percent)
        val fileCount = if (filesProcessed.get() > 0) " (${filesProcessed.get()} files)" else ""

        when (style) {
            ProgressStyle.DETAILED -> {
                val progressLine = "  $progressBar $percent% - ${stage.displayName}$fileCount"
                printProgressLine(progressLine)
            }
            ProgressStyle.SIMPLE -> {
                val progressLine = "  $progressBar $percent%"
                printProgressLine(progressLine)
            }
            ProgressStyle.MINIMAL -> {
                if (stage == Stage.INIT || stage == Stage.PREPROCESS ||
                    stage == Stage.TRANSFORM || stage == Stage.COMPLETE) {
                    clearProgressLine()
                    logger.lifecycle("  ${stage.displayName}...")
                }
            }
            ProgressStyle.QUIET -> { /* No output */ }
        }
    }

    /**
     * Build a visual progress bar.
     */
    private fun buildProgressBar(percent: Int): String {
        val filled = (BAR_WIDTH * percent / 100).coerceIn(0, BAR_WIDTH)
        val empty = BAR_WIDTH - filled

        return buildString {
            append("[")
            if (filled > 0) {
                append(FILLED_CHAR.repeat(filled - 1))
                append(if (percent < 100) HEAD_CHAR else FILLED_CHAR)
            }
            append(EMPTY_CHAR.repeat(empty))
            append("]")
        }
    }

    /**
     * Print progress line with carriage return for in-place updates.
     */
    private fun printProgressLine(line: String) {
        if (isTerminalSupported) {
            // Use carriage return for in-place update
            print("\r${line.padEnd(lastProgressLine.length)}")
            System.out.flush()
        } else {
            // For non-terminal output, use logger
            if (line != lastProgressLine) {
                logger.lifecycle(line)
            }
        }
        lastProgressLine = line
    }

    /**
     * Clear the current progress line.
     */
    private fun clearProgressLine() {
        if (isTerminalSupported && lastProgressLine.isNotEmpty()) {
            print("\r${" ".repeat(lastProgressLine.length)}\r")
            System.out.flush()
        }
        lastProgressLine = ""
    }

    /**
     * Print the final summary.
     * Thread-safe: takes snapshots of errors/warnings to avoid race conditions.
     */
    fun printSummary(success: Boolean, durationMs: Long) {
        clearProgressLine()

        // Take thread-safe snapshots to avoid race conditions
        val errorSnapshot: List<String>
        val warningSnapshot: List<String>
        synchronized(errors) { errorSnapshot = errors.toList() }
        synchronized(warnings) { warningSnapshot = warnings.toList() }
        val stageSnapshot = currentStage
        val filesSnapshot = filesProcessed.get()

        if (style == ProgressStyle.QUIET && success && errorSnapshot.isEmpty()) {
            return
        }

        val durationSec = durationMs / 1000.0

        if (style != ProgressStyle.QUIET) {
            logger.lifecycle("")
            if (success) {
                logger.lifecycle("  ${buildProgressBar(100)} 100% - Complete ($filesSnapshot files, ${String.format("%.1f", durationSec)}s)")
            } else {
                logger.lifecycle("  ${buildProgressBar(stageSnapshot.progressPercent)} ${stageSnapshot.progressPercent}% - Failed at ${stageSnapshot.displayName}")
            }
        }

        // Show warning count in summary (even if warnings weren't displayed during processing)
        // This lets users know warnings exist and can enable showWarnings for details
        if (warningSnapshot.isNotEmpty() && style != ProgressStyle.QUIET) {
            logger.lifecycle("")
            logger.lifecycle("  Warnings: ${warningSnapshot.size}")
            // Show hint only in DETAILED mode (SIMPLE/MINIMAL users want minimal output)
            if (!showWarnings && style == ProgressStyle.DETAILED) {
                logger.lifecycle("    (use showWarnings(true) to display warning details)")
            }
        }

        if (errorSnapshot.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("  Errors: ${errorSnapshot.size}")
            errorSnapshot.take(5).forEach { logger.error("    $it") }
            if (errorSnapshot.size > 5) {
                logger.error("    ... and ${errorSnapshot.size - 5} more errors")
            }
        }
    }

    /**
     * Get the current progress percentage.
     */
    fun getProgressPercent(): Int = currentStage.progressPercent

    /**
     * Get the number of files processed.
     */
    fun getFilesProcessed(): Int = filesProcessed.get()

    /**
     * Get the number of errors encountered.
     * Thread-safe.
     */
    fun getErrorCount(): Int = synchronized(errors) { errors.size }

    /**
     * Get the number of warnings encountered.
     * Thread-safe.
     */
    fun getWarningCount(): Int = synchronized(warnings) { warnings.size }

    /**
     * Check if any errors were encountered.
     * Thread-safe.
     */
    fun hasErrors(): Boolean = synchronized(errors) { errors.isNotEmpty() }

    /**
     * Reset the progress reporter for a new transformation.
     * Call this between processing multiple files.
     */
    fun reset() {
        currentStage = Stage.INIT
        filesProcessed.set(0)
        synchronized(errors) { errors.clear() }
        synchronized(warnings) { warnings.clear() }
        lastProgressLine = ""
    }
}
