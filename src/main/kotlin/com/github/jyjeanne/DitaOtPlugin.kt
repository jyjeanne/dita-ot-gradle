package com.github.jyjeanne

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for publishing DITA documents using DITA Open Toolkit.
 *
 * This plugin registers two tasks:
 * - `ditaOt`: Deprecated setup task for installing DITA-OT plugins
 * - `dita`: Main task for publishing DITA documents
 *
 * Usage:
 * ```
 * plugins {
 *     id("com.github.eerohele.dita-ot-gradle")
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

        // Register the deprecated ditaOt setup task
        @Suppress("DEPRECATION")
        project.tasks.register(DITA_OT, DitaOtSetupTask::class.java) { task ->
            task.group = "Documentation"
            task.description = "Set up DITA Open Toolkit (DEPRECATED)"
        }

        // Register the main dita task
        project.tasks.register(DITA, DitaOtTask::class.java) { task ->
            task.group = "Documentation"
            task.description = "Publishes DITA documentation with DITA Open Toolkit."
        }
    }

    companion object {
        const val DITA = "dita"
        const val DITA_OT = "ditaOt"
    }
}
