package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jsoup.Jsoup
import java.io.File
import java.nio.file.Files

/**
 * Integration tests that actually execute DITA-OT transformations.
 *
 * NOTE: These tests are disabled by default because they require a full Gradle runtime
 * environment that's not available in ProjectBuilder. To test actual DITA-OT execution:
 * 1. Use the examples in the examples/ directory
 * 2. Run: cd examples/simple && gradle dita
 * 3. Check the build/ output directory
 *
 * These tests serve as documentation of the expected behavior but are marked as disabled.
 */
class DitaOtIntegrationTest : StringSpec({

    lateinit var project: Project
    lateinit var ditaHome: String
    lateinit var examplesDir: String
    lateinit var testOutputDir: File

    beforeTest {
        ditaHome = System.getProperty("dita.home")?.replace("\\", "/")
            ?: throw InvalidUserDataException("dita.home system property not set")

        val ditaHomeFile = File(ditaHome)
        if (!ditaHomeFile.isDirectory) {
            throw InvalidUserDataException("dita.home must point to a valid directory: $ditaHome")
        }

        examplesDir = System.getProperty("examples.dir")?.replace("\\", "/")
            ?: throw InvalidUserDataException("examples.dir system property not set")

        testOutputDir = Files.createTempDirectory("dita-integration-test").toFile()

        project = ProjectBuilder.builder()
            .withProjectDir(testOutputDir)
            .build()
    }

    afterTest {
        testOutputDir.deleteRecursively()
    }

    "Execute simple HTML5 transformation".config(enabled = false) {
        val task = project.tasks.create("dita", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output(testOutputDir.path)
            transtype("html5")
        }

        // Execute the task
        task.render()

        // Verify output was created
        val outputFile = File(testOutputDir, "topic1.html")
        outputFile.exists() shouldBe true

        // Verify content
        val doc = Jsoup.parse(outputFile, "UTF-8")
        doc.select("title").first()?.text() shouldNotBe null
    }

    "Execute transformation with DITAVAL filter".config(enabled = false) {
        val task = project.tasks.create("dita", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            filter("$examplesDir/simple/dita/root.ditaval")
            output(testOutputDir.path)
            transtype("html5")
        }

        // Execute the task
        task.render()

        // Verify output was created
        val outputFile = File(testOutputDir, "topic1.html")
        outputFile.exists() shouldBe true

        // Verify filtering was applied
        val doc = Jsoup.parse(outputFile, "UTF-8")
        val paragraph = doc.select("p").first()?.text()
        paragraph shouldNotBe null
    }

    "Execute transformation with multiple transtypes".config(enabled = false) {
        val outputDir = File(testOutputDir, "multi-format")

        val task = project.tasks.create("dita", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output(outputDir.path)
            transtype("html5", "xhtml")
        }

        // Execute the task
        task.render()

        // Verify both formats were created
        File(outputDir, "html5/topic1.html").exists() shouldBe true
        File(outputDir, "xhtml/topic1.html").exists() shouldBe true
    }

    "Execute transformation with custom properties".config(enabled = false) {
        val propertiesClosure = object : groovy.lang.Closure<Any>(null) {
            override fun call(): Any? {
                val ant = delegate as groovy.lang.GroovyObject
                ant.invokeMethod("property", mapOf(
                    "name" to "processing-mode",
                    "value" to "lax"
                ))
                return null
            }
        }

        val task = project.tasks.create("dita", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output(testOutputDir.path)
            transtype("html5")
            properties(propertiesClosure)
        }

        // Execute the task
        task.render()

        // Verify output was created
        val outputFile = File(testOutputDir, "topic1.html")
        outputFile.exists() shouldBe true
    }

    "Execute transformation with multiple input files".config(enabled = false) {
        val outputDir = File(testOutputDir, "multi-input")

        val task = project.tasks.create("dita", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
            output(outputDir.path)
            transtype("html5")
        }

        // Execute the task
        task.render()

        // Verify both outputs were created
        File(outputDir, "one/topic-one.html").exists() shouldBe true
        File(outputDir, "two/topic-two.html").exists() shouldBe true
    }
})
