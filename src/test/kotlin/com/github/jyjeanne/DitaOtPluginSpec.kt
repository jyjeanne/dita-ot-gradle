package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder

class DitaOtPluginSpec : StringSpec({
    val DITA = "dita"
    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
    }

    "Apply plugin" {
        // expect
        project.tasks.findByName(DITA) shouldBe null

        // when
        project.plugins.apply(DitaOtPlugin::class.java)

        // then
        val task: Task? = project.tasks.findByName(DITA)
        task.shouldNotBeNull()
        task.group shouldBe "Documentation"

        project.tasks.findByName("clean").shouldNotBeNull()
    }
})
