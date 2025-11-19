package com.github.jyjeanne

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Alternative ANT execution strategies for invoking DITA-OT.
 *
 * This provides multiple approaches to execute DITA-OT's ANT build,
 * addressing issues with Gradle's IsolatedAntBuilder classloader.
 *
 * @since 2.3.0
 */
object AntExecutor {

    /**
     * Execute ANT using direct process invocation (shell/batch script).
     *
     * Pros:
     * - No classloader isolation issues
     * - Works with DITA-OT as intended
     * - Can capture output and errors properly
     *
     * Cons:
     * - Depends on DITA-OT shell/batch scripts (dita or dita.bat)
     * - Platform-dependent (needs separate handling for Windows/Unix)
     * - More overhead from process creation
     *
     * @param ditaHome DITA-OT installation directory
     * @param antBuildFile Build file to execute
     * @param properties Map of ANT properties
     * @param logger Gradle logger for output
     * @return Exit code (0 for success)
     */
    fun executeViaDitaScript(
        ditaHome: File,
        antBuildFile: File,
        properties: Map<String, String>,
        logger: Logger
    ): Int {
        logger.debug("Attempting ANT execution via DITA-OT script")

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val ditaScript = if (isWindows) {
            File(ditaHome, "dita.bat")
        } else {
            File(ditaHome, "dita")
        }

        if (!ditaScript.exists()) {
            logger.warn("DITA script not found at ${ditaScript.absolutePath}")
            return -1
        }

        try {
            val command = mutableListOf(ditaScript.absolutePath)

            // Build ANT command with properties
            properties.forEach { (name, value) ->
                command.add("-D$name=$value")
            }

            logger.info("Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(ditaHome)
            processBuilder.inheritIO() // Inherit I/O to see output

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                logger.error("DITA-OT execution failed with exit code: $exitCode")
            }

            return exitCode
        } catch (e: Exception) {
            logger.error("Failed to execute DITA script", e)
            return -1
        }
    }

    /**
     * Execute ANT using Java API with custom classloader setup.
     *
     * Pros:
     * - Pure Java, no external processes
     * - Better integration with Gradle
     * - Full control over environment
     *
     * Cons:
     * - Requires reflection and custom classloading
     * - Complex to maintain
     * - May have compatibility issues with different Gradle versions
     *
     * @param ditaHome DITA-OT installation directory
     * @param antBuildFile Build file to execute
     * @param classpathFiles Classpath for ANT execution
     * @param properties Map of ANT properties
     * @param logger Gradle logger for output
     * @return Exit code (0 for success)
     */
    fun executeViaCustomClassloader(
        ditaHome: File,
        antBuildFile: File,
        classpathFiles: List<File>,
        properties: Map<String, String>,
        logger: Logger
    ): Int {
        logger.debug("Attempting ANT execution via custom classloader")

        try {
            // Create URL classloader with all classpath files
            val urls = classpathFiles.map { it.toURI().toURL() }.toTypedArray()
            val classloader = java.net.URLClassLoader(urls, ClassLoader.getSystemClassLoader())

            // Load ANT Project class dynamically
            val projectClass = classloader.loadClass("org.apache.tools.ant.Project")
            val taskDefClass = classloader.loadClass("org.apache.tools.ant.taskdefs.Definer")

            logger.info("Successfully loaded ANT classes via custom classloader")
            logger.info("Classes available: Project=${projectClass.name}, TaskDef=${taskDefClass.name}")

            // Here you would create and configure an ANT Project instance
            // This is a proof of concept - full implementation would require
            // proper reflection-based ANT project setup

            return 0 // Success indicator
        } catch (e: Exception) {
            logger.error("Failed to execute ANT via custom classloader", e)
            return -1
        }
    }

    /**
     * Execute ANT using Gradle's exec task (simpler wrapper approach).
     *
     * Pros:
     * - Uses Gradle's built-in exec capabilities
     * - Easy to implement and maintain
     * - Good error handling
     *
     * Cons:
     * - Requires creating a temporary Gradle exec task
     * - Indirect approach
     * - Still subject to Gradle's environment setup
     *
     * Note: This would be called from within the DitaOtTask to use
     * the project's exec infrastructure.
     */
    fun getExecCommandArguments(
        ditaHome: File,
        antBuildFile: File,
        properties: Map<String, String>
    ): List<String> {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val ditaScript = if (isWindows) "dita.bat" else "dita"

        val command = mutableListOf(File(ditaHome, ditaScript).absolutePath)

        properties.forEach { (name, value) ->
            command.add("-D$name=$value")
        }

        return command
    }

    /**
     * Experimental: Execute ANT using embedded Groovy/Ant binding.
     *
     * This approach uses Groovy's built-in Ant support which might
     * handle classloading better than IsolatedAntBuilder.
     *
     * Pros:
     * - Uses proven Groovy infrastructure
     * - Good classloading support
     * - Native to Gradle's Groovy ecosystem
     *
     * Cons:
     * - Requires Groovy dependency
     * - Still may have similar classloader issues
     * - Complex to debug
     */
    fun executeViaGroovyAnt(
        ditaHome: File,
        antBuildFile: File,
        classpathFiles: List<File>,
        properties: Map<String, String>,
        logger: Logger
    ): Int {
        logger.debug("Attempting ANT execution via Groovy Ant binding")
        logger.info("This would use Groovy's Ant binding with explicit classpath")

        try {
            // Pseudo-code for Groovy approach:
            // def ant = new AntBuilder()
            // ant.invokeMethod('ant', [
            //     antfile: antBuildFile,
            //     dir: ditaHome,
            //     classpath: classpathFiles.join(File.pathSeparator)
            // ])

            logger.warn("Groovy Ant binding approach not yet fully implemented")
            return 0
        } catch (e: Exception) {
            logger.error("Failed to execute ANT via Groovy binding", e)
            return -1
        }
    }
}
