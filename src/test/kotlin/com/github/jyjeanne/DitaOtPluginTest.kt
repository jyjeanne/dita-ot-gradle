package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Tests for DitaOtPlugin.
 *
 * This is a Kotest conversion of the original Spock test (DitaOtPluginSpec.groovy).
 * Demonstrates that Kotlin tests can test the migrated plugin.
 */
class DitaOtPluginTest : StringSpec({
    val DITA = "dita"

    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
    }

    "Apply plugin" {
        // Initially, dita task should not exist
        project.tasks.findByName(DITA) shouldBe null

        // Apply the plugin
        project.plugins.apply(DitaOtPlugin::class.java)

        // After applying, dita task should exist
        val task = project.tasks.findByName(DITA)
        task.shouldNotBeNull()
        task.group shouldBe "Documentation"

        // Base plugin should also be applied (provides 'clean' task)
        project.tasks.findByName("clean").shouldNotBeNull()
    }

    "Plugin registers correct task type" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA)
        ditaTask.shouldNotBeNull()
        (ditaTask is DitaOtTask) shouldBe true
    }

    "DitaOtTask showProgress default is true" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.showProgress.get() shouldBe true
    }

    "DitaOtTask progressStyle default is DETAILED" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.progressStyle.get() shouldBe "DETAILED"
    }

    "DitaOtTask showProgress can be disabled" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.showProgress(false)
        ditaTask.showProgress.get() shouldBe false
    }

    "DitaOtTask progressStyle can be changed to SIMPLE" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.progressStyle("SIMPLE")
        ditaTask.progressStyle.get() shouldBe "SIMPLE"
    }

    "DitaOtTask progressStyle can be changed to MINIMAL" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.progressStyle("MINIMAL")
        ditaTask.progressStyle.get() shouldBe "MINIMAL"
    }

    "DitaOtTask progressStyle can be changed to QUIET" {
        project.plugins.apply(DitaOtPlugin::class.java)

        val ditaTask = project.tasks.findByName(DITA) as DitaOtTask
        ditaTask.progressStyle("QUIET")
        ditaTask.progressStyle.get() shouldBe "QUIET"
    }
})
