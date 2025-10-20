package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * DEPRECATED: This task is deprecated and should not be used.
 * Plugin installation should be done via the dita task configuration instead.
 */
@Deprecated("Use dita task configuration instead", level = DeprecationLevel.WARNING)
open class DitaOtSetupTask : DefaultTask() {

    @get:InputFiles
    var classpath: FileCollection? = null

    @get:InputFile
    var dir: File? = null

    @get:Internal
    var plugins: List<Any>? = null

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun plugins(vararg plugins: Any) {
        logger.warn("The \"ditaOt\" task is deprecated.")
        this.plugins = plugins.toList()
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    @Suppress("UNUSED_PARAMETER")
    fun dir(dir: Any) {
        // Note: This references project.ditaOt.dir which may not exist
        // Keeping for backward compatibility even though it's deprecated
        logger.warn("The \"ditaOt\" task is deprecated.")
        // project.ditaOt.dir = project.file(dir)
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun classpath(vararg classpath: Any) {
        logger.warn("The \"ditaOt\" task is deprecated.")
        this.classpath = project.files(*classpath)
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun getDefaultClasspath(ditaHome: File): FileTree {
        logger.warn("The \"ditaOt\" task is deprecated.")
        return Classpath.compile(project, ditaHome).asFileTree
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun getDefaultClasspath(project: Project): FileTree {
        logger.warn("The \"ditaOt\" task is deprecated.")
        val ditaOtDir = this.dir ?: throw IllegalStateException("DITA-OT directory not set")
        return Classpath.compile(project, ditaOtDir).asFileTree
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun getPluginClasspath(ditaHome: File): FileTree {
        logger.warn("The \"ditaOt\" task is deprecated.")
        return Classpath.pluginClasspath(project, ditaHome).asFileTree
    }

    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun getPluginClasspath(project: Project): FileTree {
        logger.warn("The \"ditaOt\" task is deprecated.")
        val ditaOtDir = this.dir ?: throw IllegalStateException("DITA-OT directory not set")
        return Classpath.pluginClasspath(project, ditaOtDir).asFileTree
    }

    @TaskAction
    @Deprecated("The ditaOt task is deprecated", level = DeprecationLevel.WARNING)
    fun install() {
        logger.warn("The \"ditaOt\" task is deprecated.")
        val pluginsList = this.plugins
        val ditaOtDir = this.dir

        if (pluginsList != null && ditaOtDir != null) {
            // Note: Original code references Ant.getBuilder() which doesn't exist
            // This functionality is likely broken in the original Groovy version as well
            // Keeping a comment for documentation purposes
            logger.error("Plugin installation via ditaOt task is not supported. " +
                    "This task is deprecated and the Ant integration is not available.")

            // Original Groovy code (non-functional):
            // Ant.getBuilder(getDefaultClasspath(this.dir)).execute {
            //     this.plugins.each { Object plugin ->
            //         ant(antfile: new File(this.dir, 'integrator.xml'),
            //             target: 'install', useNativeBaseDir: true) {
            //             property(name: 'plugin.file', value: plugin.toString())
            //         }
            //     }
            // }
        }
    }
}
