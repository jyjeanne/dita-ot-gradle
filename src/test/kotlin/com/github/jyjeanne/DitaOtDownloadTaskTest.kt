package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

/**
 * Tests for DitaOtDownloadTask.
 */
class DitaOtDownloadTaskTest : StringSpec({

    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(DitaOtPlugin::class.java)
    }

    "Register download task" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        task.get().shouldNotBeNull()
        task.get().group shouldBe "DITA-OT Setup"
        task.get().description shouldBe "Downloads and extracts DITA Open Toolkit"
    }

    "Default version is set" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        task.get().version.get() shouldBe DitaOtDownloadTask.DEFAULT_VERSION
    }

    "Default destination directory is set" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        val destDir = task.get().destinationDir.asFile.get()
        destDir.path shouldContain "build"
        destDir.path shouldContain "dita-ot"
    }

    "Version can be configured" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java) {
            it.version.set("4.1.0")
        }

        task.get().version.get() shouldBe "4.1.0"
    }

    "Custom download URL can be set" {
        val customUrl = "https://example.com/custom-dita-ot.zip"
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java) {
            it.downloadUrl.set(customUrl)
        }

        task.get().downloadUrl.get() shouldBe customUrl
    }

    "Skip if exists defaults to true" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        task.get().skipIfExists.get() shouldBe true
    }

    "Force reinstall defaults to false" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        task.get().forceReinstall.get() shouldBe false
    }

    "DitaOtHome path is correctly computed" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java) {
            it.version.set("4.2.3")
            it.destinationDir.set(File("/test/dita-ot"))
        }

        val ditaOtHome = task.get().ditaOtHome
        ditaOtHome.path shouldContain "dita-ot-4.2.3"
    }

    "Cache directory is in user home" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java)

        val cacheDir = task.get().cacheDir
        cacheDir.path shouldContain ".dita-ot"
        cacheDir.path shouldContain "cache"
    }

    "DSL method version works" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java) {
            it.version("4.0.0")
        }

        task.get().version.get() shouldBe "4.0.0"
    }

    "DSL method destinationDir works with string" {
        val task = project.tasks.register("downloadDitaOt", DitaOtDownloadTask::class.java) {
            it.destinationDir("custom/path")
        }

        task.get().destinationDir.asFile.get().path shouldContain "custom"
    }

    "GitHub release URL format is correct" {
        val expectedUrlFormat = "https://github.com/dita-ot/dita-ot/releases/download/%s/dita-ot-%s.zip"
        DitaOtDownloadTask.GITHUB_RELEASE_URL shouldBe expectedUrlFormat

        // Test URL generation
        val version = "4.2.3"
        val url = String.format(DitaOtDownloadTask.GITHUB_RELEASE_URL, version, version)
        url shouldBe "https://github.com/dita-ot/dita-ot/releases/download/4.2.3/dita-ot-4.2.3.zip"
    }
})
