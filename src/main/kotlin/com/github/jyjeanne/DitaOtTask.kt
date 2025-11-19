package com.github.jyjeanne

import groovy.lang.Closure
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task for publishing DITA documents using DITA-OT.
 *
 * This task allows you to transform DITA maps into various output formats
 * (HTML5, PDF, XHTML, etc.) using the DITA Open Toolkit.
 *
 * **Configuration Cache**: This task is NOT compatible with Gradle's configuration cache due to:
 * - Dynamic classpath computation that depends on DITA-OT directory structure
 * - Complex ANT integration through IsolatedAntBuilder
 * - Groovy Closure-based properties that require serialization
 * Run with `--no-configuration-cache` flag if you encounter cache-related errors.
 *
 * @since 1.0.0
 */
@CacheableTask
open class DitaOtTask : DefaultTask() {

    @get:Internal
    val options: Options = Options()

    // Pre-computed default output directory (accessed during configuration time)
    @get:Internal
    private val defaultBuildDirectory: File = project.layout.buildDirectory.asFile.get()

    init {
        options.output = defaultBuildDirectory
    }

    // Configuration methods (for both Groovy and Kotlin DSL)

    fun devMode(d: Boolean) {
        options.devMode = d
    }

    fun ditaOt(d: Any?) {
        options.ditaOt = if (d != null) project.file(d) else null
    }

    fun classpath(vararg classpath: Any) {
        options.classpath = project.files(*classpath)
    }

    fun input(i: Any) {
        options.input = i
    }

    fun filter(f: Any) {
        options.filter = f
    }

    fun output(o: String) {
        options.output = project.file(o)
    }

    fun temp(t: String) {
        options.temp = project.file(t)
    }

    /**
     * Configure DITA-OT properties using Groovy Closure (for Groovy DSL).
     * Example:
     * ```groovy
     * properties {
     *     property(name: 'processing-mode', value: 'strict')
     * }
     * ```
     */
    fun properties(p: Closure<*>) {
        options.properties = p
    }

    /**
     * Configure DITA-OT properties using Kotlin DSL.
     * Example:
     * ```kotlin
     * properties {
     *     "processing-mode" to "strict"
     *     "args.cssroot" to "$projectDir/css"
     * }
     * ```
     */
    fun properties(block: PropertyBuilder.() -> Unit) {
        val builder = PropertyBuilder()
        builder.block()
        options.kotlinProperties = builder.build()
    }

    fun transtype(vararg t: String) {
        options.transtype = t.toList()
    }

    fun singleOutputDir(s: Boolean) {
        options.singleOutputDir = s
    }

    fun useAssociatedFilter(a: Boolean) {
        options.useAssociatedFilter = a
    }

    @Internal
    fun getDefaultClasspath(): FileTree {
        return Classpath.compile(project, getDitaHome()).asFileTree
    }

    @Deprecated("Use getDefaultClasspath() instead", ReplaceWith("getDefaultClasspath()"))
    @Suppress("UNUSED_PARAMETER")
    fun getDefaultClasspath(project: Project): FileTree {
        logger.warn("getDefaultClasspath(project) is deprecated. Use getDefaultClasspath() instead.")
        return getDefaultClasspath()
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getDitaHome(): File {
        val ditaHome = options.ditaOt ?: throw GradleException(Messages.ditaHomeError)

        // Validate DITA-OT directory
        if (!ditaHome.exists()) {
            throw GradleException("DITA-OT directory does not exist: ${ditaHome.absolutePath}")
        }

        if (!ditaHome.isDirectory) {
            throw GradleException("DITA-OT path is not a directory: ${ditaHome.absolutePath}")
        }

        val buildXml = File(ditaHome, "build.xml")
        if (!buildXml.exists()) {
            throw GradleException("""
                Invalid DITA-OT directory: build.xml not found in ${ditaHome.absolutePath}

                Make sure you're pointing to the root DITA-OT directory, not a subdirectory.
            """.trimIndent())
        }

        return ditaHome
    }

    /**
     * Detect DITA-OT version from the installation.
     * Returns version string or "unknown" if version cannot be determined.
     */
    fun detectDitaOtVersion(): String {
        return try {
            val ditaHome = getDitaHome()
            val versionFile = File(ditaHome, "VERSION")
            if (versionFile.exists()) {
                versionFile.readText().trim()
            } else {
                // Try reading from lib directory JAR manifest
                val libDir = File(ditaHome, "lib")
                val dostJar = libDir.listFiles()?.find { it.name.startsWith("dost") }
                if (dostJar != null) {
                    logger.debug("Found DOST JAR: ${dostJar.name}")
                }
                "unknown"
            }
        } catch (e: Exception) {
            logger.debug("Could not detect DITA-OT version: ${e.message}")
            "unknown"
        }
    }

    /**
     * Get input files for up-to-date check.
     *
     * By default, all files under all input directories are included in the
     * up-to-date check, apart from the build directory.
     *
     * If devMode is true, the DITA-OT directory is also checked. That's useful
     * for stylesheet developers who don't want to use --rerun-tasks every time
     * they make a change to the DITA-OT plugin they're developing.
     *
     * @since 0.1.0
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    fun getInputFileTree(): Set<Any> {
        val outputDir = FilenameUtils.getBaseName(options.output?.path ?: "")

        val inputFiles = mutableSetOf<Any>()

        getInputFiles().files.forEach { file ->
            inputFiles.add(project.fileTree(file.parent ?: ".").apply {
                exclude("**/.gradle/**", outputDir)
            })
        }

        // DITAVAL file might be outside the DITA map directory, so we add that separately
        if (options.filter != null) {
            inputFiles.add(project.files(options.filter).asFileTree)
        }

        if (options.devMode) {
            inputFiles.add(project.fileTree(getDitaHome()).apply {
                exclude(
                    "temp",
                    "lib/dost-configuration.jar",
                    "lib/org.dita.dost.platform/plugin.properties"
                )
            })
        }

        return inputFiles
    }

    @OutputDirectories
    fun getOutputDirectories(): Set<File> {
        return getInputFiles().files.flatMap { file ->
            options.transtype.map { transtype ->
                getOutputDirectory(file, transtype)
            }
        }.toSet()
    }

    @SkipWhenEmpty
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getInputFiles(): FileCollection {
        return if (options.input == null) {
            project.files()
        } else {
            project.files(options.input)
        }
    }

    fun getDitavalFile(inputFile: File): File {
        return if (options.filter != null) {
            project.file(options.filter!!)
        } else {
            getAssociatedFile(inputFile, FileExtensions.DITAVAL)
        }
    }

    /**
     * Get the output directory for the given DITA map.
     *
     * If the user has given an output directory, use that. Otherwise,
     * use the basename of the input DITA file.
     *
     * Example: if the name input DITA file is `root`, the output directory
     * is `${buildDir}/root`.
     *
     * @param inputFile Input DITA file.
     * @param transtype DITA transtype.
     * @since 0.1.0
     */
    fun getOutputDirectory(inputFile: File, transtype: String): File {
        val baseOutputDir = if (options.singleOutputDir || getInputFiles().files.size == 1) {
            options.output
        } else {
            val basename = FilenameUtils.getBaseName(inputFile.path)
            File(options.output, basename)
        } ?: defaultBuildDirectory

        return if (transtype.isNotEmpty() && options.transtype.size > 1) {
            File(baseOutputDir, transtype)
        } else {
            baseOutputDir
        }
    }

    private fun antBuilder(classpath: FileCollection): IsolatedAntBuilder {
        val builder = services.get(IsolatedAntBuilder::class.java)

        builder.withClasspath(classpath.files)

        return builder
    }

    @TaskAction
    fun render() {
        val startTime = System.currentTimeMillis()
        val ditaHome = getDitaHome()
        val inputFiles = getInputFiles().files
        val transtypes = options.transtype

        // Detect and log DITA-OT version
        val ditaOtVersion = detectDitaOtVersion()

        // Log transformation start
        logger.lifecycle("Starting DITA-OT transformation")
        logger.info("DITA-OT version: $ditaOtVersion")
        logger.info("DITA-OT home: ${ditaHome.absolutePath}")
        logger.info("Input files: ${inputFiles.size} file(s)")
        logger.info("Output formats: ${transtypes.joinToString(", ")}")
        logger.info("Output directory: ${options.output?.absolutePath ?: defaultBuildDirectory.absolutePath}")

        // Warn if DITA-OT version is old
        if (ditaOtVersion != "unknown") {
            val majorVersion = ditaOtVersion.split(".").firstOrNull()?.toIntOrNull()
            if (majorVersion != null && majorVersion < 3) {
                logger.warn("DITA-OT version $ditaOtVersion is old. Version 3.0+ is recommended for best results.")
            }
        }

        if (inputFiles.isEmpty()) {
            logger.warn("No input files specified - task will be skipped")
            return
        }

        val classpathToUse = options.classpath ?: getDefaultClasspath()
        logger.debug("Classpath: ${classpathToUse.files.size} entries")

        // Debug: Log all classpath files
        if (logger.isDebugEnabled) {
            classpathToUse.files.forEach { file ->
                logger.debug("  - ${file.absolutePath}")
            }
        }

        antBuilder(classpathToUse).execute { antProject ->
            inputFiles.forEach { inputFile ->
                logger.info("Processing: ${inputFile.name}")

                if (!inputFile.exists()) {
                    throw GradleException("Input file does not exist: ${inputFile.absolutePath}")
                }

                val associatedPropertyFile = getAssociatedFile(inputFile, FileExtensions.PROPERTIES)

                transtypes.forEach { transtype ->
                    val outputDir = getOutputDirectory(inputFile, transtype)
                    logger.info("  → Generating $transtype output to: ${outputDir.absolutePath}")

                    // Use GroovyObject invokeMethod directly - works reliably across Gradle versions
                    val antBuildFile = File(ditaHome, "build.xml")

                    // Create property closure
                    val propertyClosure = object : Closure<Unit>(this) {
                        fun doCall() {
                            val ant = delegate as groovy.lang.GroovyObject

                            // Set required DITA-OT properties
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.ARGS_INPUT,
                                "location" to inputFile.absolutePath
                            ))
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.OUTPUT_DIR,
                                "location" to outputDir.absolutePath
                            ))
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.TEMP_DIR,
                                "location" to options.temp.absolutePath
                            ))
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.TRANSTYPE,
                                "value" to transtype
                            ))

                            // DITAVAL filter
                            if (options.filter != null || options.useAssociatedFilter) {
                                ant.invokeMethod("property", mapOf(
                                    "name" to Properties.ARGS_FILTER,
                                    "location" to getDitavalFile(inputFile).absolutePath
                                ))
                            }

                            // Apply user-defined properties from Groovy Closure
                            if (options.properties != null) {
                                options.properties!!.delegate = ant
                                options.properties!!.call()
                            }

                            // Apply user-defined properties from Kotlin DSL
                            if (options.kotlinProperties != null) {
                                options.kotlinProperties!!.forEach { (name, value) ->
                                    ant.invokeMethod("property", mapOf(
                                        "name" to name,
                                        "value" to value
                                    ))
                                }
                            }

                            // Load associated property file if it exists
                            if (associatedPropertyFile.exists()) {
                                ant.invokeMethod("property", mapOf(
                                    "file" to associatedPropertyFile.absolutePath
                                ))
                            }
                        }
                    }

                    // Execute Ant task - invokeMethod with correct parameter types
                    try {
                        (antProject as groovy.lang.GroovyObject).invokeMethod("ant",
                            arrayOf(mapOf("antfile" to antBuildFile.absolutePath), propertyClosure))
                        logger.info("  ✓ Successfully generated $transtype output")
                    } catch (e: Exception) {
                        logger.error("  ✗ Failed to generate $transtype output", e)
                        throw GradleException("DITA-OT transformation failed for $transtype: ${e.message}", e)
                    }
                }
            }
        }

        // Calculate output metrics
        val duration = System.currentTimeMillis() - startTime
        val durationSeconds = duration / 1000.0
        val outputDirs = getOutputDirectories()
        val totalOutputSize = outputDirs.sumOf { calculateDirectorySize(it) }
        val outputSizeMB = totalOutputSize / (1024.0 * 1024.0)

        // Log transformation completion with summary
        logger.lifecycle("")
        logger.lifecycle("═══════════════════════════════════════════════════════")
        logger.lifecycle("DITA-OT Transformation Report")
        logger.lifecycle("═══════════════════════════════════════════════════════")
        logger.lifecycle("Status:           ✓ SUCCESS")
        logger.lifecycle("DITA-OT version:  $ditaOtVersion")
        logger.lifecycle("Files processed:  ${inputFiles.size}")
        logger.lifecycle("Formats:          ${transtypes.joinToString(", ")}")
        logger.lifecycle("Output size:      ${String.format("%.2f", outputSizeMB)} MB")
        logger.lifecycle("Duration:         ${String.format("%.2f", durationSeconds)}s")
        logger.lifecycle("═══════════════════════════════════════════════════════")
    }

    /**
     * Calculate total size of a directory and its contents recursively.
     */
    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L

        return try {
            dir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            logger.debug("Could not calculate size of ${dir.absolutePath}: ${e.message}")
            0L
        }
    }

    companion object {
        /**
         * Get a file "associated" with a given file.
         *
         * An "associated" file is a file in the same directory as the
         * input file that has the exact same basename but with the given extension.
         *
         * Example: if the input file is `subdir/root.ditamap` and the given
         * extension is ".properties", the associated file is
         * `subdir/root.properties`.
         *
         * @param inputFile Input file.
         * @param extension The extension of the associated file.
         * @since 0.2.0
         */
        @JvmStatic
        fun getAssociatedFile(inputFile: File, extension: String): File {
            val absPath = inputFile.absolutePath
            val dirname = FilenameUtils.getFullPathNoEndSeparator(absPath)
            val basename = FilenameUtils.getBaseName(absPath)

            return File(FilenameUtils.concat(dirname, basename) + extension)
        }
    }
}
