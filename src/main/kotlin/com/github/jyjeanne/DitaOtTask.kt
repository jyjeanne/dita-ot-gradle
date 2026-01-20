package com.github.jyjeanne

import groovy.lang.Closure
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Gradle task for publishing DITA documents using DITA-OT.
 *
 * This task allows you to transform DITA maps into various output formats
 * (HTML5, PDF, XHTML, etc.) using the DITA Open Toolkit.
 *
 * **Configuration Cache**: This task is compatible with Gradle's configuration cache
 * when using the DITA_SCRIPT execution strategy (default). The task uses Gradle's
 * Provider API to ensure all project references are resolved during configuration time.
 *
 * @since 1.0.0
 */
@CacheableTask
abstract class DitaOtTask @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    // ==================== Properties (Configuration Cache Compatible) ====================

    /**
     * DITA-OT installation directory.
     *
     * Marked as @Internal (not @InputDirectory) because:
     * 1. The DITA-OT directory is a tool/runtime, not input data
     * 2. Using @InputDirectory causes Gradle to infer implicit dependencies
     *    between tasks that share the same DITA-OT installation
     * 3. When output directories are inside or adjacent to ditaOtDir,
     *    Gradle incorrectly assumes tasks depend on each other
     *
     * For up-to-date checks on DITA-OT changes, use devMode(true) which
     * includes the DITA-OT directory in getInputFileTree().
     *
     * @see devMode
     * @see getInputFileTree
     */
    @get:Internal
    abstract val ditaOtDir: DirectoryProperty

    /**
     * Input files (DITA maps or topics) to process.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * DITAVAL filter file (optional).
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val filterFile: ConfigurableFileCollection

    /**
     * Output directory for generated content.
     */
    @get:Internal
    abstract val outputDir: DirectoryProperty

    /**
     * Temporary directory for DITA-OT processing.
     */
    @get:Internal
    abstract val tempDir: DirectoryProperty

    /**
     * Output formats (transtypes) to generate.
     */
    @get:Input
    abstract val transtypes: ListProperty<String>

    /**
     * Custom DITA-OT properties.
     */
    @get:Input
    @get:Optional
    abstract val ditaProperties: MapProperty<String, String>

    /**
     * Enable dev mode (include DITA-OT directory in up-to-date checks).
     */
    @get:Input
    abstract val devMode: Property<Boolean>

    /**
     * Use single output directory for all inputs.
     */
    @get:Input
    abstract val singleOutputDir: Property<Boolean>

    /**
     * Use associated DITAVAL filter files.
     */
    @get:Input
    abstract val useAssociatedFilter: Property<Boolean>

    /**
     * ANT execution strategy.
     */
    @get:Input
    abstract val antExecutionStrategy: Property<String>

    /**
     * Enable visual progress reporting during transformation.
     * Default: true
     */
    @get:Input
    abstract val showProgress: Property<Boolean>

    /**
     * Progress display style.
     * Options: DETAILED, SIMPLE, MINIMAL, QUIET
     * Default: DETAILED
     */
    @get:Input
    abstract val progressStyle: Property<String>

    /**
     * Custom classpath for DITA-OT (optional).
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val customClasspath: ConfigurableFileCollection

    // ==================== Legacy Options (for backward compatibility) ====================

    @get:Internal
    val options: Options = Options()

    // Groovy Closure for properties (not serializable, marked @Internal)
    @get:Internal
    var groovyProperties: Closure<*>? = null

    // ==================== Initialization ====================

    init {
        // Set defaults
        devMode.convention(false)
        singleOutputDir.convention(false)
        useAssociatedFilter.convention(false)
        transtypes.convention(listOf(Options.DEFAULT_TRANSTYPE))
        antExecutionStrategy.convention(Options.Companion.AntExecutionStrategy.DITA_SCRIPT.name)
        outputDir.convention(projectLayout.buildDirectory)
        tempDir.convention(projectLayout.buildDirectory.dir("dita-temp"))
        showProgress.convention(true)
        progressStyle.convention(ProgressReporter.ProgressStyle.DETAILED.name)
    }

    // ==================== Configuration Methods (DSL) ====================

    fun devMode(d: Boolean) {
        devMode.set(d)
        options.devMode = d
    }

    fun ditaOt(d: Any?) {
        if (d != null) {
            when (d) {
                is File -> {
                    ditaOtDir.set(d)
                    options.ditaOt = d
                }
                is Directory -> {
                    ditaOtDir.set(d)
                    options.ditaOt = d.asFile
                }
                is RegularFile -> {
                    ditaOtDir.set(d.asFile)
                    options.ditaOt = d.asFile
                }
                is Provider<*> -> {
                    // Handle Provider<Directory> - resolve the value now since the provider
                    // should have a value at task configuration time (after dependent tasks configure)
                    // We need to get the actual File to avoid circular provider chains in Gradle
                    @Suppress("UNCHECKED_CAST")
                    val value = (d as Provider<*>).get()
                    val file = when (value) {
                        is Directory -> value.asFile
                        is File -> value
                        else -> projectLayout.projectDirectory.dir(value.toString()).asFile
                    }
                    ditaOtDir.set(file)
                    options.ditaOt = file
                }
                is String -> {
                    val file = projectLayout.projectDirectory.dir(d).asFile
                    ditaOtDir.set(file)
                    options.ditaOt = file
                }
                else -> {
                    val file = projectLayout.projectDirectory.dir(d.toString()).asFile
                    ditaOtDir.set(file)
                    options.ditaOt = file
                }
            }
        }
    }

    fun classpath(vararg classpath: Any) {
        customClasspath.from(*classpath)
        options.classpath = objectFactory.fileCollection().from(*classpath)
    }

    fun input(i: Any) {
        inputFiles.from(i)
        options.input = i
    }

    fun filter(f: Any) {
        val file = when (f) {
            is File -> f
            is String -> projectLayout.projectDirectory.file(f).asFile
            else -> projectLayout.projectDirectory.file(f.toString()).asFile
        }
        filterFile.from(file)
        options.filter = f
    }

    fun output(o: String) {
        val dir = projectLayout.projectDirectory.dir(o)
        outputDir.set(dir)
        options.output = dir.asFile
    }

    fun temp(t: String) {
        val dir = projectLayout.projectDirectory.dir(t)
        tempDir.set(dir)
        options.temp = dir.asFile
    }

    /**
     * Configure DITA-OT properties using Groovy Closure (for Groovy DSL).
     * Note: Groovy Closures are not configuration cache compatible.
     * Use the Kotlin DSL properties method for full compatibility.
     */
    fun properties(p: Closure<*>) {
        groovyProperties = p
        options.properties = p
    }

    /**
     * Configure DITA-OT properties using Kotlin DSL.
     * This method is configuration cache compatible.
     */
    fun properties(block: PropertyBuilder.() -> Unit) {
        val builder = PropertyBuilder()
        builder.block()
        val props = builder.build()
        ditaProperties.putAll(props)
        options.kotlinProperties = props
    }

    fun transtype(vararg t: String) {
        transtypes.set(t.toList())
        options.transtype = t.toList()
    }

    fun singleOutputDir(s: Boolean) {
        singleOutputDir.set(s)
        options.singleOutputDir = s
    }

    fun useAssociatedFilter(a: Boolean) {
        useAssociatedFilter.set(a)
        options.useAssociatedFilter = a
    }

    /**
     * Configure ANT execution strategy.
     */
    fun antExecutionStrategy(strategy: String) {
        try {
            Options.Companion.AntExecutionStrategy.valueOf(strategy)
            antExecutionStrategy.set(strategy)
            options.antExecutionStrategy = Options.Companion.AntExecutionStrategy.valueOf(strategy)
            logger.info("ANT execution strategy set to: $strategy")
        } catch (e: IllegalArgumentException) {
            throw GradleException(
                "Invalid ANT execution strategy: $strategy. " +
                "Valid options are: ${Options.Companion.AntExecutionStrategy.values().joinToString(", ")}",
                e
            )
        }
    }

    /**
     * Enable or disable visual progress reporting.
     */
    fun showProgress(show: Boolean) {
        showProgress.set(show)
    }

    /**
     * Configure progress display style.
     * @param style One of: DETAILED, SIMPLE, MINIMAL, QUIET
     */
    fun progressStyle(style: String) {
        try {
            ProgressReporter.ProgressStyle.valueOf(style)
            progressStyle.set(style)
        } catch (e: IllegalArgumentException) {
            throw GradleException(
                "Invalid progress style: $style. " +
                "Valid options are: ${ProgressReporter.ProgressStyle.values().joinToString(", ")}",
                e
            )
        }
    }

    // ==================== Computed Properties ====================

    /**
     * Resolve DITA-OT home directory with validation.
     */
    fun resolveDitaHome(): File {
        val ditaHome = ditaOtDir.asFile.orNull ?: throw GradleException(Messages.ditaHomeError)

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
     */
    fun detectDitaOtVersion(): String {
        return try {
            val ditaHome = resolveDitaHome()
            val versionFile = File(ditaHome, "VERSION")
            if (versionFile.exists()) {
                versionFile.readText().trim()
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            logger.debug("Could not detect DITA-OT version: ${e.message}")
            "unknown"
        }
    }

    /**
     * Get input file tree for up-to-date checks.
     * Includes parent directories of input files and optionally DITA-OT directory in dev mode.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getInputFileTree(): FileCollection {
        val outputDirName = FilenameUtils.getBaseName(outputDir.asFile.orNull?.path ?: "")
        val collection = objectFactory.fileCollection()

        // Add parent directories of all input files
        inputFiles.files.forEach { file ->
            val parentDir = file.parentFile
            if (parentDir != null && parentDir.exists()) {
                val tree = objectFactory.fileTree()
                tree.from(parentDir)
                tree.exclude("**/.gradle/**", outputDirName)
                collection.from(tree)
            }
        }

        // Add filter file if specified
        if (!filterFile.isEmpty) {
            collection.from(filterFile)
        }

        // In dev mode, include DITA-OT directory for up-to-date checks
        if (devMode.get()) {
            val ditaHome = ditaOtDir.asFile.orNull
            if (ditaHome != null && ditaHome.exists()) {
                val tree = objectFactory.fileTree()
                tree.from(ditaHome)
                tree.exclude(
                    "temp",
                    "lib/dost-configuration.jar",
                    "lib/org.dita.dost.platform/plugin.properties"
                )
                collection.from(tree)
            }
        }

        return collection
    }

    /**
     * Get all output directories for this task.
     */
    @OutputDirectories
    fun getOutputDirectories(): Set<File> {
        return inputFiles.files.flatMap { file ->
            transtypes.get().map { transtype ->
                getOutputDirectory(file, transtype)
            }
        }.toSet()
    }

    /**
     * Get the output directory for the given DITA map.
     */
    fun getOutputDirectory(inputFile: File, transtype: String): File {
        val baseOutputDir = if (singleOutputDir.get() || inputFiles.files.size == 1) {
            outputDir.asFile.orNull
        } else {
            val basename = FilenameUtils.getBaseName(inputFile.path)
            File(outputDir.asFile.orNull, basename)
        } ?: projectLayout.buildDirectory.asFile.get()

        return if (transtype.isNotEmpty() && transtypes.get().size > 1) {
            File(baseOutputDir, transtype)
        } else {
            baseOutputDir
        }
    }

    /**
     * Get DITAVAL filter file for the given input file.
     */
    fun getDitavalFile(inputFile: File): File {
        return if (!filterFile.isEmpty) {
            filterFile.files.first()
        } else {
            getAssociatedFile(inputFile, FileExtensions.DITAVAL)
        }
    }

    /**
     * Create default classpath for DITA-OT.
     */
    fun createDefaultClasspath(): FileCollection {
        // Note: This method creates a new FileCollection during execution
        // For configuration cache compatibility, prefer using customClasspath property
        val ditaHome = resolveDitaHome()
        return Classpath.compileWithObjectFactory(objectFactory, ditaHome)
    }

    // ==================== Task Action ====================

    @TaskAction
    fun render() {
        val startTime = System.currentTimeMillis()
        val ditaHome = resolveDitaHome()
        val inputs = inputFiles.files
        val types = transtypes.get().toTypedArray()

        // Detect and log DITA-OT version
        val ditaOtVersion = detectDitaOtVersion()

        // Log transformation start
        logger.lifecycle("Starting DITA-OT transformation")
        logger.info("DITA-OT version: $ditaOtVersion")
        logger.info("DITA-OT home: ${ditaHome.absolutePath}")
        logger.info("Input files: ${inputs.size} file(s)")
        logger.info("Output formats: ${types.joinToString(", ")}")
        logger.info("Output directory: ${outputDir.asFile.orNull?.absolutePath}")
        logger.info("ANT execution strategy: ${antExecutionStrategy.get()}")

        // Warn if DITA-OT version is old
        if (ditaOtVersion != "unknown") {
            val majorVersion = ditaOtVersion.split(".").firstOrNull()?.toIntOrNull()
            if (majorVersion != null && majorVersion < 3) {
                logger.warn("DITA-OT version $ditaOtVersion is old. Version 3.0+ is recommended for best results.")
            }
        }

        if (inputs.isEmpty()) {
            logger.warn("No input files specified - task will be skipped")
            return
        }

        // WORKAROUND: Use DITA_SCRIPT strategy by default
        val strategy = Options.Companion.AntExecutionStrategy.valueOf(antExecutionStrategy.get())
        if (strategy == Options.Companion.AntExecutionStrategy.DITA_SCRIPT) {
            logger.info("Using DITA_SCRIPT strategy (configuration cache compatible)")
            val success = renderViaDitaScript(ditaHome, inputs, types, startTime)

            if (!success) {
                throw GradleException("DITA-OT transformation failed with DITA_SCRIPT strategy")
            }
        } else {
            // Legacy: Use IsolatedAntBuilder (not configuration cache compatible)
            logger.warn("Using deprecated IsolatedAntBuilder strategy - NOT configuration cache compatible")
            logger.warn("Recommendation: Use antExecutionStrategy(\"DITA_SCRIPT\") for configuration cache support")

            renderViaIsolatedAntBuilder(ditaHome, inputs, types)
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
        logger.lifecycle("Status:           SUCCESS")
        logger.lifecycle("DITA-OT version:  $ditaOtVersion")
        logger.lifecycle("Files processed:  ${inputs.size}")
        logger.lifecycle("Formats:          ${types.joinToString(", ")}")
        logger.lifecycle("Output size:      ${String.format("%.2f", outputSizeMB)} MB")
        logger.lifecycle("Duration:         ${String.format("%.2f", durationSeconds)}s")
        logger.lifecycle("═══════════════════════════════════════════════════════")
    }

    // ==================== Execution Strategies ====================

    /**
     * Execute DITA-OT via native script (configuration cache compatible).
     */
    private fun renderViaDitaScript(
        ditaHome: File,
        inputs: Set<File>,
        types: Array<String>,
        startTime: Long
    ): Boolean {
        var hasErrors = false

        // Create progress reporter if enabled
        val progressReporter = if (showProgress.get()) {
            val style = try {
                ProgressReporter.ProgressStyle.valueOf(progressStyle.get())
            } catch (e: IllegalArgumentException) {
                ProgressReporter.ProgressStyle.DETAILED
            }
            ProgressReporter(logger, style)
        } else {
            null
        }

        inputs.forEach { inputFile ->
            logger.info("Processing: ${inputFile.name}")

            if (!inputFile.exists()) {
                throw GradleException("Input file does not exist: ${inputFile.absolutePath}")
            }

            types.forEach { transtype ->
                val outDir = getOutputDirectory(inputFile, transtype)
                logger.info("  -> Generating $transtype output to: ${outDir.absolutePath}")

                // Build properties map
                val properties = mutableMapOf<String, String>()

                // User-defined properties from Groovy Closure (for backward compatibility)
                if (groovyProperties != null) {
                    val capturedProps = GroovyPropertyCapture.captureFromClosure(groovyProperties)
                    properties.putAll(capturedProps)
                    logger.debug("Captured ${capturedProps.size} properties from Groovy closure")
                }

                // User-defined properties from Provider API (these override closure properties)
                if (ditaProperties.isPresent) {
                    properties.putAll(ditaProperties.get())
                }

                // Determine filter file
                val filter = if (!filterFile.isEmpty || useAssociatedFilter.get()) {
                    getDitavalFile(inputFile)
                } else {
                    null
                }

                // Execute via DITA script workaround
                try {
                    val exitCode = AntExecutor.executeViaDitaScript(
                        ditaHome = ditaHome,
                        inputFile = inputFile,
                        transtype = transtype,
                        outputDir = outDir,
                        tempDir = tempDir.asFile.get(),
                        filterFile = filter,
                        properties = properties,
                        logger = logger,
                        progressReporter = progressReporter
                    )

                    if (exitCode == 0) {
                        logger.info("  [OK] Successfully generated $transtype output")
                    } else {
                        logger.error("  [FAIL] Failed to generate $transtype output (exit code: $exitCode)")
                        hasErrors = true
                    }

                    // Reset progress reporter for next transformation
                    progressReporter?.reset()
                } catch (e: Exception) {
                    logger.error("  [FAIL] Failed to generate $transtype output", e)
                    hasErrors = true
                    progressReporter?.reset()
                }
            }
        }

        return !hasErrors
    }

    /**
     * Execute DITA-OT via IsolatedAntBuilder (legacy, not configuration cache compatible).
     */
    private fun renderViaIsolatedAntBuilder(
        ditaHome: File,
        inputs: Set<File>,
        types: Array<String>
    ) {
        val classpathToUse = if (!customClasspath.isEmpty) {
            customClasspath
        } else {
            createDefaultClasspath()
        }
        logger.debug("Classpath: ${classpathToUse.files.size} entries")

        val builder = services.get(IsolatedAntBuilder::class.java)
        builder.withClasspath(classpathToUse.files)

        builder.execute { antProject ->
            inputs.forEach { inputFile ->
                logger.info("Processing: ${inputFile.name}")

                if (!inputFile.exists()) {
                    throw GradleException("Input file does not exist: ${inputFile.absolutePath}")
                }

                val associatedPropertyFile = getAssociatedFile(inputFile, FileExtensions.PROPERTIES)

                types.forEach { transtype ->
                    val outDir = getOutputDirectory(inputFile, transtype)
                    logger.info("  -> Generating $transtype output to: ${outDir.absolutePath}")

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
                                "location" to outDir.absolutePath
                            ))
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.TEMP_DIR,
                                "location" to tempDir.asFile.get().absolutePath
                            ))
                            ant.invokeMethod("property", mapOf(
                                "name" to Properties.TRANSTYPE,
                                "value" to transtype
                            ))

                            // DITAVAL filter
                            if (!filterFile.isEmpty || useAssociatedFilter.get()) {
                                ant.invokeMethod("property", mapOf(
                                    "name" to Properties.ARGS_FILTER,
                                    "location" to getDitavalFile(inputFile).absolutePath
                                ))
                            }

                            // Apply user-defined properties from Groovy Closure
                            if (groovyProperties != null) {
                                groovyProperties!!.delegate = ant
                                groovyProperties!!.call()
                            }

                            // Apply user-defined properties from Provider API
                            if (ditaProperties.isPresent) {
                                ditaProperties.get().forEach { (name, value) ->
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

                    // Execute Ant task
                    try {
                        (antProject as groovy.lang.GroovyObject).invokeMethod("ant",
                            arrayOf(mapOf("antfile" to antBuildFile.absolutePath), propertyClosure))
                        logger.info("  [OK] Successfully generated $transtype output")
                    } catch (e: Exception) {
                        logger.error("  [FAIL] Failed to generate $transtype output", e)
                        throw GradleException("DITA-OT transformation failed for $transtype: ${e.message}", e)
                    }
                }
            }
        }
    }

    // ==================== Utility Methods ====================

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
