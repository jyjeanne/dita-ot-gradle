package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gradle task for installing DITA-OT plugins.
 *
 * This task allows installing plugins from:
 * - Plugin registry (by name/ID)
 * - Local ZIP files
 * - Remote URLs
 * - GitHub repositories
 *
 * **Usage (Groovy DSL):**
 * ```groovy
 * tasks.register('installPlugins', DitaOtInstallPluginTask) {
 *     ditaOtDir = file("$buildDir/dita-ot/dita-ot-4.2.3")
 *     plugins = ['org.dita.pdf2', 'com.example.custom-plugin']
 *     retries = 3  // Optional: retry failed installations
 *     quiet = true // Optional: suppress progress output
 * }
 * ```
 *
 * **Usage (Kotlin DSL):**
 * ```kotlin
 * tasks.register<DitaOtInstallPluginTask>("installPlugins") {
 *     ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-4.2.3"))
 *     plugins.set(listOf("org.dita.pdf2", "com.example.custom-plugin"))
 *     retries.set(3)  // Optional: retry failed installations
 *     quiet.set(true) // Optional: suppress progress output
 * }
 * ```
 *
 * **Plugin Sources:**
 * - Registry: `org.dita.pdf2` (installs from DITA-OT plugin registry)
 * - URL: `https://example.com/plugin.zip`
 * - Local: `file:///path/to/plugin.zip` or absolute path
 * - GitHub: `github.com/owner/repo` (requires DITA-OT 3.5+)
 *
 * **Configuration Options:**
 * - `ditaOtDir`: DITA-OT installation directory (required)
 * - `plugins`: List of plugin IDs, URLs, or file paths (required)
 * - `force`: Force reinstall existing plugins (default: false)
 * - `failOnError`: Fail build on installation error (default: true)
 * - `retries`: Number of retry attempts for failed installations (default: 0)
 * - `quiet`: Suppress progress output (default: false)
 *
 * @since 2.4.0
 */
abstract class DitaOtInstallPluginTask @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    companion object {
        /** Task name constant */
        const val TASK_NAME = "installDitaOtPlugins"

        /** Process timeout in milliseconds (5 minutes) */
        private const val PROCESS_TIMEOUT = 300_000L
    }

    // ==================== Properties ====================

    /**
     * DITA-OT installation directory.
     */
    @get:Internal
    abstract val ditaOtDir: DirectoryProperty

    /**
     * List of plugins to install.
     * Can be plugin IDs, URLs, or file paths.
     */
    @get:Input
    abstract val plugins: ListProperty<String>

    /**
     * Whether to force reinstall existing plugins.
     * Defaults to false.
     */
    @get:Input
    abstract val force: Property<Boolean>

    /**
     * Whether to fail the build if any plugin installation fails.
     * Defaults to true.
     */
    @get:Input
    abstract val failOnError: Property<Boolean>

    /**
     * Number of retries for failed plugin installations.
     * Defaults to 0 (no retries).
     */
    @get:Input
    abstract val retries: Property<Int>

    /**
     * Whether to suppress progress output.
     * Defaults to false.
     */
    @get:Input
    abstract val quiet: Property<Boolean>

    // ==================== Internal Properties ====================

    /**
     * Get the DITA executable path.
     */
    @get:Internal
    val ditaExecutable: File
        get() = Platform.getDitaExecutable(ditaOtDir.asFile.get())

    // ==================== Initialization ====================

    init {
        group = "DITA-OT Setup"
        description = "Installs DITA-OT plugins"

        // Set defaults
        force.convention(false)
        failOnError.convention(true)
        retries.convention(0)
        quiet.convention(false)
    }

    // ==================== DSL Methods ====================

    /**
     * Set DITA-OT directory (Groovy DSL friendly).
     */
    fun ditaOtDir(dir: Any) {
        when (dir) {
            is File -> ditaOtDir.set(dir)
            is String -> ditaOtDir.set(projectLayout.projectDirectory.dir(dir))
            else -> ditaOtDir.set(projectLayout.projectDirectory.dir(dir.toString()))
        }
    }

    /**
     * Add plugins to install (Groovy DSL friendly).
     */
    fun plugins(vararg pluginIds: String) {
        plugins.set(pluginIds.toList())
    }

    /**
     * Add a single plugin to the list.
     */
    fun plugin(pluginId: String) {
        val current = plugins.getOrElse(emptyList())
        plugins.set(current + pluginId)
    }

    // ==================== Task Action ====================

    @TaskAction
    fun installPlugins() {
        val ditaHome = ditaOtDir.asFile.get()
        val pluginList = plugins.get()
        val isQuiet = quiet.get()

        // Validate DITA-OT installation
        validateDitaOtInstallation(ditaHome)

        if (!isQuiet) {
            logger.lifecycle("DITA-OT Plugin Installation")
            logger.lifecycle("  DITA-OT: ${ditaHome.absolutePath}")
            logger.lifecycle("  Plugins: ${pluginList.size}")
        }

        if (pluginList.isEmpty()) {
            if (!isQuiet) {
                logger.lifecycle("  No plugins specified - nothing to install")
            }
            return
        }

        val results = mutableMapOf<String, InstallResult>()

        pluginList.forEach { plugin ->
            if (!isQuiet) {
                logger.lifecycle("")
                logger.lifecycle("Installing: $plugin")
            }

            val result = installPluginWithRetry(ditaHome, plugin)
            results[plugin] = result

            when (result) {
                is InstallResult.Success -> {
                    if (!isQuiet) {
                        logger.lifecycle("  ✓ Successfully installed: ${result.pluginId}")
                    }
                }
                is InstallResult.AlreadyInstalled -> {
                    if (!isQuiet) {
                        logger.lifecycle("  → Already installed: ${result.pluginId}")
                    }
                }
                is InstallResult.Failed -> {
                    logger.error("  ✗ Failed to install: $plugin")
                    logger.error("    Error: ${result.message}")
                    if (failOnError.get()) {
                        throw GradleException(
                            """
                            Plugin installation failed: $plugin

                            Error: ${result.message}

                            Troubleshooting:
                            1. Check plugin ID is correct
                            2. Verify network connectivity
                            3. For URLs, ensure the file is accessible
                            4. For local files, verify the path exists
                            5. Try running: ${ditaExecutable.absolutePath} --install $plugin
                            """.trimIndent()
                        )
                    }
                }
            }
        }

        // Summary
        val successful = results.values.count { it is InstallResult.Success }
        val alreadyInstalled = results.values.count { it is InstallResult.AlreadyInstalled }
        val failed = results.values.count { it is InstallResult.Failed }

        if (!isQuiet) {
            logger.lifecycle("")
            logger.lifecycle("═══════════════════════════════════════════════════════")
            logger.lifecycle("Plugin Installation Summary")
            logger.lifecycle("═══════════════════════════════════════════════════════")
            logger.lifecycle("Installed:         $successful")
            logger.lifecycle("Already present:   $alreadyInstalled")
            logger.lifecycle("Failed:            $failed")
            logger.lifecycle("═══════════════════════════════════════════════════════")
        }

        if (failed > 0 && failOnError.get()) {
            throw GradleException("$failed plugin(s) failed to install")
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
     * Install a plugin with retry support.
     */
    private fun installPluginWithRetry(ditaHome: File, plugin: String): InstallResult {
        val maxRetries = retries.get()
        val isQuiet = quiet.get()
        var lastResult: InstallResult = InstallResult.Failed(plugin, "No attempt made")
        var attempt = 0

        while (attempt <= maxRetries) {
            if (attempt > 0 && !isQuiet) {
                val delay = calculateRetryDelay(attempt)
                logger.lifecycle("  Retry $attempt/$maxRetries after ${delay}ms...")
                Thread.sleep(delay)
            }

            lastResult = installPlugin(ditaHome, plugin)

            // Return immediately on success or already installed
            if (lastResult is InstallResult.Success || lastResult is InstallResult.AlreadyInstalled) {
                return lastResult
            }

            // Log failure and continue retrying
            if (lastResult is InstallResult.Failed && attempt < maxRetries && !isQuiet) {
                logger.warn("  Installation attempt ${attempt + 1} failed: ${lastResult.message}")
            }

            attempt++
        }

        return lastResult
    }

    /**
     * Calculate retry delay with exponential backoff.
     * Base delay: 1 second, max delay: 30 seconds.
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = 1000L
        val maxDelay = 30000L
        val delay = baseDelay * (1L shl (attempt - 1).coerceAtMost(5))
        return delay.coerceAtMost(maxDelay)
    }

    /**
     * Install a single plugin.
     */
    private fun installPlugin(ditaHome: File, plugin: String): InstallResult {
        return try {
            // Check if already installed (for registry plugins)
            if (!force.get() && isPluginInstalled(ditaHome, plugin)) {
                return InstallResult.AlreadyInstalled(extractPluginId(plugin))
            }

            // Build install command
            val command = mutableListOf<String>()

            val isWindows = Platform.isWindows
            if (isWindows) {
                command.add("cmd")
                command.add("/c")
            }

            command.add(ditaExecutable.absolutePath)
            command.add("install")

            if (force.get()) {
                command.add("--force")
            }

            command.add(plugin)

            logger.debug("Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(ditaHome)
            processBuilder.redirectErrorStream(true)

            // Set environment
            val env = processBuilder.environment()
            env["DITA_HOME"] = ditaHome.absolutePath

            val process = processBuilder.start()

            // Capture output with size limit (max 100KB to prevent memory issues)
            val maxOutputSize = 100 * 1024
            val output = StringBuilder()
            var outputTruncated = false

            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (output.length < maxOutputSize) {
                        output.appendLine(line)
                    } else if (!outputTruncated) {
                        output.appendLine("... (output truncated)")
                        outputTruncated = true
                    }
                    logger.debug("  $line")
                }
            }

            // Wait for process with timeout
            val completed = process.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                return InstallResult.Failed(plugin, "Installation timed out after ${PROCESS_TIMEOUT / 1000} seconds")
            }

            val exitCode = process.exitValue()

            if (exitCode == 0) {
                InstallResult.Success(extractPluginId(plugin))
            } else {
                InstallResult.Failed(plugin, "Exit code: $exitCode\n$output")
            }
        } catch (e: Exception) {
            InstallResult.Failed(plugin, e.message ?: "Unknown error")
        }
    }

    /**
     * Check if a plugin is already installed.
     */
    private fun isPluginInstalled(ditaHome: File, plugin: String): Boolean {
        // For registry plugins, check plugins directory
        val pluginId = extractPluginId(plugin)
        val pluginsDir = File(ditaHome, "plugins")

        if (!pluginsDir.exists()) return false

        // Check for exact match or prefix match
        return pluginsDir.listFiles()?.any { dir ->
            dir.isDirectory && (dir.name == pluginId || dir.name.startsWith("$pluginId-"))
        } ?: false
    }

    /**
     * Extract plugin ID from various formats.
     */
    private fun extractPluginId(plugin: String): String {
        return when {
            // URL format
            plugin.startsWith("http://") || plugin.startsWith("https://") -> {
                val filename = plugin.substringAfterLast("/")
                filename.removeSuffix(".zip")
            }
            // File path format
            plugin.startsWith("file://") || plugin.contains("/") || plugin.contains("\\") -> {
                val filename = File(plugin.removePrefix("file://")).name
                filename.removeSuffix(".zip")
            }
            // GitHub format (github.com/owner/repo)
            plugin.startsWith("github.com/") -> {
                plugin.substringAfterLast("/")
            }
            // Plugin ID format
            else -> plugin
        }
    }

    /**
     * Result of plugin installation.
     */
    sealed class InstallResult {
        data class Success(val pluginId: String) : InstallResult()
        data class AlreadyInstalled(val pluginId: String) : InstallResult()
        data class Failed(val plugin: String, val message: String) : InstallResult()
    }
}
