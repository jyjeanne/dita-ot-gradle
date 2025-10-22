package com.github.jyjeanne

import groovy.lang.Closure
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task for publishing DITA documents using DITA-OT.
 *
 * This task allows you to transform DITA maps into various output formats
 * (HTML5, PDF, XHTML, etc.) using the DITA Open Toolkit.
 */
open class DitaOtTask : DefaultTask() {

    @get:Internal
    val options: Options = Options()

    init {
        options.output = project.layout.buildDirectory.asFile.get()
    }

    // Configuration methods (for both Groovy and Kotlin DSL)

    fun devMode(d: Boolean) {
        options.devMode = d
    }

    fun ditaOt(d: Any) {
        options.ditaOt = project.file(d)
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

    fun properties(p: Closure<*>) {
        options.properties = p
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
    fun getDitaHome(): File {
        return options.ditaOt ?: throw GradleException(Messages.ditaHomeError)
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
        } ?: project.layout.buildDirectory.asFile.get()

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
        val ditaHome = getDitaHome()
        val classpathToUse = options.classpath ?: getDefaultClasspath()

        antBuilder(classpathToUse).execute { antProject ->
            getInputFiles().files.forEach { inputFile ->
                val associatedPropertyFile = getAssociatedFile(inputFile, FileExtensions.PROPERTIES)

                options.transtype.forEach { transtype ->
                    val outputDir = getOutputDirectory(inputFile, transtype)

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

                            // Apply user-defined properties
                            if (options.properties != null) {
                                options.properties!!.delegate = ant
                                options.properties!!.call()
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
                    (antProject as groovy.lang.GroovyObject).invokeMethod("ant",
                        arrayOf(mapOf("antfile" to antBuildFile.absolutePath), propertyClosure))
                }
            }
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
