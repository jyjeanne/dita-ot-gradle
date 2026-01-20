package com.github.jyjeanne

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for publishing DITA documents using DITA Open Toolkit.
 *
 * This plugin provides:
 * - `dita` task for transforming DITA documents
 * - `DitaOtDownloadTask` for automatic DITA-OT download
 * - `DitaOtInstallPluginTask` for installing DITA-OT plugins
 * - `DitaOtValidateTask` for validating DITA content
 * - `DitaLinkCheckTask` for checking broken links
 *
 * **Quick Start (Kotlin DSL):**
 * ```kotlin
 * plugins {
 *     id("io.github.jyjeanne.dita-ot-gradle")
 * }
 *
 * // Download DITA-OT automatically
 * val downloadDitaOt by tasks.registering(DitaOtDownloadTask::class) {
 *     version.set("4.2.3")
 * }
 *
 * // Configure transformation
 * dita {
 *     ditaOt(downloadDitaOt.flatMap { it.ditaOtHome })
 *     input("my.ditamap")
 *     transtype("html5")
 * }
 * ```
 *
 * **Quick Start (Groovy DSL):**
 * ```groovy
 * plugins {
 *     id 'io.github.jyjeanne.dita-ot-gradle'
 * }
 *
 * tasks.register('downloadDitaOt', DitaOtDownloadTask) {
 *     version = '4.2.3'
 * }
 *
 * dita {
 *     ditaOt tasks.downloadDitaOt.flatMap { it.ditaOtHome }
 *     input 'my.ditamap'
 *     transtype 'html5'
 * }
 * ```
 *
 * @since 1.0.0
 */
class DitaOtPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Apply the base plugin to get standard lifecycle tasks (clean, assemble, check, build)
        project.plugins.apply("base")

        // Register the main dita task
        project.tasks.register(DITA, DitaOtTask::class.java) { task ->
            task.group = GROUP_DOCUMENTATION
            task.description = "Publishes DITA documentation with DITA Open Toolkit."
        }

        // Log plugin application
        project.logger.info("DITA-OT Gradle Plugin applied")
        project.logger.info("  Available tasks:")
        project.logger.info("    - dita: Transform DITA documents")
        project.logger.info("  Available task types:")
        project.logger.info("    - DitaOtTask: Main transformation task")
        project.logger.info("    - DitaOtDownloadTask: Download DITA-OT automatically")
        project.logger.info("    - DitaOtInstallPluginTask: Install DITA-OT plugins")
        project.logger.info("    - DitaOtValidateTask: Validate DITA content")
        project.logger.info("    - DitaLinkCheckTask: Check for broken links")
    }

    companion object {
        /** Task name for the main DITA transformation task */
        const val DITA = "dita"

        /** Task group for documentation tasks */
        const val GROUP_DOCUMENTATION = "Documentation"

        /** Task group for setup tasks */
        const val GROUP_SETUP = "DITA-OT Setup"

        /** Plugin ID */
        const val PLUGIN_ID = "io.github.jyjeanne.dita-ot-gradle"
    }
}
