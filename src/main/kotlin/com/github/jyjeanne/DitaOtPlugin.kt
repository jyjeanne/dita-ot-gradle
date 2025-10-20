package com.github.jyjeanne

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for publishing DITA documents using DITA Open Toolkit.
 *
 * This plugin registers the `dita` task for publishing DITA documents.
 *
 * Usage:
 * ```
 * plugins {
 *     id("io.github.jyjeanne.dita-ot-gradle")
 * }
 *
 * dita {
 *     ditaOt("/path/to/dita-ot")
 *     input("my.ditamap")
 *     transtype("html5")
 * }
 * ```
 */
class DitaOtPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Apply the base plugin to get standard lifecycle tasks
        project.plugins.apply("base")

        // Register the main dita task
        project.tasks.register(DITA, DitaOtTask::class.java) { task ->
            task.group = "Documentation"
            task.description = "Publishes DITA documentation with DITA Open Toolkit."
        }
    }

    companion object {
        const val DITA = "dita"
    }
}
