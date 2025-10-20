package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.assertions.throwables.shouldNotThrowAny
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jsoup.Jsoup
import java.io.File
import java.nio.file.Files

class DitaOtTaskSpec : StringSpec({
    val DITA = "dita"
    val ROOT_DITAMAP = "root.ditamap"
    val ROOT_DITAVAL = "root.ditaval"
    val DEFAULT_TRANSTYPE = "html5"

    lateinit var project: Project
    lateinit var testProjectDir: File
    lateinit var buildFile: File
    lateinit var settingsFile: File
    lateinit var ditaHome: String
    lateinit var examplesDir: String

    fun getInputFiles(task: Task): Set<File> {
        val method = task::class.java.getMethod("getInputFiles")
        val fileCollection = method.invoke(task) as FileCollection
        return fileCollection.files
    }

    fun getDefaultClasspath(project: Project, ditaHome: File): FileCollection {
        return Classpath.compile(project, ditaHome)
    }

    beforeTest {
        ditaHome = System.getProperty("dita.home")?.replace("\\", "/")
            ?: throw InvalidUserDataException(
                """dita.home system property not properly set.
                   To run the tests, you need a working DITA-OT installation and you
                   need to set the dita.home system property to point to that installation.""".trimIndent()
            )

        val ditaHomeFile = File(ditaHome)
        if (!ditaHomeFile.isDirectory) {
            throw InvalidUserDataException("dita.home must point to a valid directory: $ditaHome")
        }

        examplesDir = System.getProperty("examples.dir")?.replace("\\", "/")
            ?: throw InvalidUserDataException("examples.dir system property not set")

        testProjectDir = Files.createTempDirectory("gradle-test").toFile()
        buildFile = File(testProjectDir, "build.gradle")
        settingsFile = File(testProjectDir, "settings.gradle")

        project = ProjectBuilder.builder().withName("test").build()
        project.configurations.create(DITA)
    }

    afterTest {
        testProjectDir.deleteRecursively()
    }

    "Creating a task" {
        val propertiesClosure = object : groovy.lang.Closure<Any>(null) {
            override fun call(): Any {
                return "property set"
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(ROOT_DITAMAP)
            filter(ROOT_DITAVAL)
            transtype(DEFAULT_TRANSTYPE)
            properties(propertiesClosure)
        }

        task.options.input shouldBe ROOT_DITAMAP
        task.options.filter shouldBe ROOT_DITAVAL
        task.options.transtype shouldBe listOf(DEFAULT_TRANSTYPE)
        task.options.properties.shouldNotBeNull()
    }

    "Using multiple transtypes" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(ROOT_DITAMAP)
            transtype("xhtml", "pdf", "html5", "troff")
        }

        task.options.transtype shouldBe listOf("xhtml", "pdf", "html5", "troff")
    }

    "Giving single input file as String" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
        }

        getInputFiles(task).find { it.name == ROOT_DITAMAP }.shouldNotBeNull()
    }

    "Giving single input file as File" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(project.file("$examplesDir/simple/dita/root.ditamap"))
        }

        getInputFiles(task).find { it.name == ROOT_DITAMAP }.shouldNotBeNull()
    }

    "Giving single input file and multiple transtypes" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(project.file("$examplesDir/simple/dita/root.ditamap"))
            transtype("html5", "pdf")
        }

        task.getOutputDirectories().map { it.name } shouldBe listOf("html5", "pdf")
    }

    "Giving multiple input files" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
        }

        getInputFiles(task).map { it.name } shouldBe listOf("one.ditamap", "two.ditamap")
    }

    "Giving multiple input files and multiple transtypes" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
            transtype("html5", "pdf")
        }

        val outputPaths = task.getOutputDirectories().map {
            val parent = File(it.parent)
            File(parent.name, it.name).path.replace("\\", "/")
        }

        outputPaths shouldBe listOf("one/html5", "one/pdf", "two/html5", "two/pdf")
    }

    "Includes containing directories in up-to-date check" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
        }

        val inputFiles = task.getInputFileTree().flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()
        inputFiles shouldContain File("$examplesDir/multi-project/one/one.ditamap")
        inputFiles shouldContain File("$examplesDir/multi-project/two/two.ditamap")
    }

    "Getting file associated with input file" {
        val inputFile = project.file("$examplesDir/simple/dita/root.ditamap")

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(inputFile)
        }

        DitaOtTask.getAssociatedFile(inputFile, ".properties").name shouldBe "root.properties"
    }

    "Giving DITAVAL as File" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(project.file("$examplesDir/simple/dita/root.ditamap"))
            filter(project.file("$examplesDir/simple/dita/root.ditaval"))
        }

        val filterFile = task.options.filter
        when (filterFile) {
            is File -> filterFile.name shouldBe ROOT_DITAVAL
            else -> error("Filter should be a File")
        }
    }

    "Giving input file tree" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            val fileTree = project.fileTree("$examplesDir/filetree/dita")
            fileTree.include("*.ditamap")
            input(fileTree)
        }

        val inputFiles = task.getInputFileTree().flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()
        inputFiles shouldContain File("$examplesDir/filetree/dita/one.ditamap")
        inputFiles shouldContain File("$examplesDir/filetree/dita/two.ditamap")
    }

    "DITAVAL file is included in the input file tree" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
            filter("$examplesDir/simple/dita/root.ditaval")
        }

        val inputFileTree = task.getInputFileTree()
        val ditavalFile = File("$examplesDir/simple/dita/root.ditaval")
        val allFiles = inputFileTree.flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()
        allFiles shouldContain ditavalFile
    }

    "DITAVAL file is included in the input file tree when outside root map directory" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
            filter("$examplesDir/filetree/dita/two.ditaval")
        }

        val inputFileTree = task.getInputFileTree()
        val ditavalFile = File("$examplesDir/filetree/dita/two.ditaval")
        val allFiles = inputFileTree.flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()
        allFiles shouldContain ditavalFile
    }

    "Using associated DITAVAL file" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
            useAssociatedFilter(true)
        }

        val inputFile = getInputFiles(task).first()
        task.getDitavalFile(inputFile) shouldBe File("$examplesDir/simple/dita/root.ditaval")
    }

    "DITA-OT directory is included in the input file tree if devMode is enabled" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            devMode(true)
        }

        val allFiles = task.getInputFileTree().flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()

        allFiles shouldContain File(ditaHome, "build.xml")
        allFiles.contains(File(ditaHome, "lib/org.dita.dost.platform/plugin.properties")) shouldBe false
        allFiles.contains(File(ditaHome, "lib/dost-configuration.jar")) shouldBe false
    }

    "DITA-OT directory is not included in the input file tree if devMode is disabled" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            devMode(false)
        }

        val allFiles = task.getInputFileTree().flatMap {
            when (it) {
                is FileCollection -> it.files
                else -> emptySet()
            }
        }.toSet()

        allFiles.contains(File(ditaHome, "build.xml")) shouldBe false
    }

    "Single input file => single output directory" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
        }

        task.getOutputDirectories().map { it.name } shouldBe listOf("build")
    }

    "Single directory mode => single output directory" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            singleOutputDir(true)
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
        }

        task.getOutputDirectories().map { it.name } shouldBe listOf("build")
    }

    "Multiple input files => multiple input folders" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(
                project.files(
                    "$examplesDir/multi-project/one/one.ditamap",
                    "$examplesDir/multi-project/two/two.ditamap"
                )
            )
        }

        task.getOutputDirectories().map { it.name } shouldBe listOf("one", "two")
    }

    "Build fails if DITA-OT directory is not set" {
        settingsFile.writeText("rootProject.name = 'dita-test'")

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                input '$examplesDir/simple/dita/root.ditamap'
                transtype 'html5'
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita")
            .forwardOutput()
            .buildAndFail()

        result.task(":dita")?.outcome shouldBe TaskOutcome.FAILED
    }

    "Works when DITA-OT dir is defined inside the dita closure" {
        settingsFile.writeText("rootProject.name = 'dita-test'")

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                ditaOt '$ditaHome'
                input '$examplesDir/simple/dita/root.ditamap'
                transtype 'html5'
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.SUCCESS
        File(testProjectDir, "build/topic1.html").exists() shouldBe true
        shouldNotThrowAny { }
    }

    "Works when DITA-OT dir is defined inside the ditaOt closure" {
        settingsFile.writeText("rootProject.name = 'dita-test'")

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            ditaOt {
                dir '$ditaHome'
            }

            dita {
                input '$examplesDir/simple/dita/root.ditamap'
                transtype 'html5'
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.SUCCESS
        File(testProjectDir, "build/topic1.html").exists() shouldBe true
        shouldNotThrowAny { }
    }


    "Specifying no inputs skips the task" {
        settingsFile.writeText("rootProject.name = 'dita-test'")

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                ditaOt '$ditaHome'
                transtype 'html5'
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.NO_SOURCE
    }

    "Filtering with DITAVAL" {
        settingsFile.writeText("rootProject.name = 'dita-test'")

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                ditaOt '$ditaHome'
                input '$examplesDir/simple/dita/root.ditamap'
                filter '$examplesDir/simple/dita/root.ditaval'
                transtype 'html5'
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.SUCCESS

        val doc = Jsoup.parse(File(testProjectDir, "build/topic1.html"), "UTF-8")
        doc.select("p").first()?.outerHtml() shouldBe "<p class=\"p\">baz </p>"
    }

    "Relative output and temp directories resolve against project root directory" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input(ROOT_DITAMAP)
            output("out")
            temp("temp")
        }

        task.getOutputDirectories().map { it.canonicalPath } shouldBe
            listOf(File(project.rootDir, "out").canonicalPath)
        task.options.temp.canonicalPath shouldBe File(project.rootDir, "temp").canonicalPath
    }
})
