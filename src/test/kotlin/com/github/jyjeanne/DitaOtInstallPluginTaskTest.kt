package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

/**
 * Tests for DitaOtInstallPluginTask.
 */
class DitaOtInstallPluginTaskTest : StringSpec({

    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(DitaOtPlugin::class.java)
    }

    "Register install plugin task" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java)

        task.get().shouldNotBeNull()
        task.get().group shouldBe "DITA-OT Setup"
        task.get().description shouldBe "Installs DITA-OT plugins"
    }

    "Force defaults to false" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java)

        task.get().force.get() shouldBe false
    }

    "Fail on error defaults to true" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java)

        task.get().failOnError.get() shouldBe true
    }

    "Plugins can be configured" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.plugins.set(listOf("org.dita.pdf2", "com.example.custom"))
        }

        val plugins = task.get().plugins.get()
        plugins.size shouldBe 2
        plugins[0] shouldBe "org.dita.pdf2"
        plugins[1] shouldBe "com.example.custom"
    }

    "DSL method plugins works with vararg" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.plugins("plugin1", "plugin2", "plugin3")
        }

        task.get().plugins.get().size shouldBe 3
    }

    "DSL method plugin adds single plugin" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.plugin("org.dita.pdf2")
            it.plugin("org.dita.html5")
        }

        val plugins = task.get().plugins.get()
        plugins.size shouldBe 2
        plugins shouldBe listOf("org.dita.pdf2", "org.dita.html5")
    }

    "DITA-OT dir can be configured" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir.set(File("/path/to/dita-ot"))
        }

        task.get().ditaOtDir.asFile.get().path shouldContain "dita-ot"
    }

    "DSL method ditaOtDir works with string" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir("build/dita-ot-4.2.3")
        }

        task.get().ditaOtDir.asFile.get().path shouldContain "dita-ot-4.2.3"
    }

    "DITA executable path is platform-aware" {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir.set(File("/path/to/dita-ot"))
        }

        val executable = task.get().ditaExecutable
        if (isWindows) {
            executable.name shouldBe "dita.bat"
        } else {
            executable.name shouldBe "dita"
        }
        executable.parent shouldContain "bin"
    }

    "Force option can be enabled" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.force.set(true)
        }

        task.get().force.get() shouldBe true
    }

    "Fail on error can be disabled" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.failOnError.set(false)
        }

        task.get().failOnError.get() shouldBe false
    }

    "Command places --force after plugin argument" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir.set(File("/path/to/dita-ot"))
            it.force.set(true)
        }

        val command = task.get().buildInstallCommand("org.dita.pdf2")

        val installIndex = command.indexOf("install")
        val pluginIndex = command.indexOf("org.dita.pdf2")
        val forceIndex = command.indexOf("--force")

        installIndex shouldBeLessThan pluginIndex
        pluginIndex shouldBeLessThan forceIndex
    }

    "Command omits --force when not set" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir.set(File("/path/to/dita-ot"))
            it.force.set(false)
        }

        val command = task.get().buildInstallCommand("org.dita.pdf2")

        command shouldNotContain "--force"
    }

    "Command handles Windows file path with --force" {
        val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
            it.ditaOtDir.set(File("/path/to/dita-ot"))
            it.force.set(true)
        }

        val command = task.get().buildInstallCommand("C:\\build\\plugin\\my-plugin.zip")

        val pluginIndex = command.indexOf("C:\\build\\plugin\\my-plugin.zip")
        val forceIndex = command.indexOf("--force")

        pluginIndex shouldBeLessThan forceIndex
    }
})
