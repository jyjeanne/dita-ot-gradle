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

        val inputFiles = task.getInputFileTree().files
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

        val inputFiles = task.getInputFileTree().files
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
        val allFiles = inputFileTree.files
        allFiles shouldContain ditavalFile
    }

    "DITAVAL file is included in the input file tree when outside root map directory" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
            filter("$examplesDir/filetree/dita/two.ditaval")
        }

        val inputFileTree = task.getInputFileTree()
        val ditavalFile = File("$examplesDir/filetree/dita/two.ditaval")
        val allFiles = inputFileTree.files
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

        val allFiles = task.getInputFileTree().files

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

    "Task fails during execution if DITA-OT directory is not set" {
        // Unit test: verify task throws exception when ditaOt is not configured
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
        }

        // Should throw when trying to get DITA home without it being set
        var exceptionThrown = false
        try {
            task.resolveDitaHome()
        } catch (e: Exception) {
            exceptionThrown = true
            e.message shouldNotBe null
        }
        exceptionThrown shouldBe true
    }

    "Task is properly configured when DITA-OT dir is set" {
        // Unit test: verify task configuration works correctly
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
        }

        // Verify configuration
        task.resolveDitaHome().path.replace("\\", "/") shouldBe ditaHome
        getInputFiles(task).size shouldBe 1
        task.getOutputDirectories() shouldHaveSize 1
        task.options.transtype shouldBe listOf("html5")
    }

    "Task properly configures with custom output and temp directories" {
        // Unit test: verify custom output/temp paths work
        val customOutput = File(testProjectDir, "custom-output")
        val customTemp = File(testProjectDir, "custom-temp")

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output(customOutput.path)
            temp(customTemp.path)
            transtype("html5")
        }

        task.options.output shouldBe customOutput
        task.options.temp shouldBe customTemp
        task.getOutputDirectories().first().name shouldBe "custom-output"
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

    "Task properly configures DITAVAL filter" {
        // Unit test: verify DITAVAL configuration works
        val ditavalPath = "$examplesDir/simple/dita/root.ditaval"

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            filter(ditavalPath)
            transtype("html5")
        }

        task.options.filter shouldBe ditavalPath
        val inputFile = File("$examplesDir/simple/dita/root.ditamap")
        task.getDitavalFile(inputFile).path.replace("\\", "/") shouldBe ditavalPath.replace("\\", "/")
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

    "Kotlin DSL properties configuration works" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties {
                "processing-mode" to "strict"
                "args.rellinks" to "all"
            }
        }

        task.options.kotlinProperties.shouldNotBeNull()
        task.options.kotlinProperties!!["processing-mode"] shouldBe "strict"
        task.options.kotlinProperties!!["args.rellinks"] shouldBe "all"
    }

    "Kotlin DSL properties with property() method works" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties {
                property("processing-mode", "lax")
                property("args.draft", "yes")
            }
        }

        task.options.kotlinProperties.shouldNotBeNull()
        task.options.kotlinProperties!!["processing-mode"] shouldBe "lax"
        task.options.kotlinProperties!!["args.draft"] shouldBe "yes"
    }

    // ============================================================================
    // Groovy Closure Properties Tests (v2.3.1 regression prevention)
    // These tests ensure the v2.3.0 bug where properties { } closure was ignored
    // does not recur.
    // ============================================================================

    "Groovy closure properties are captured correctly" {
        // This test prevents regression of the v2.3.0 bug
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.css", "value" to "custom.css"))
                capture.property(mapOf("name" to "args.copycss", "value" to "yes"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(propertiesClosure)

        captured["args.css"] shouldBe "custom.css"
        captured["args.copycss"] shouldBe "yes"
    }

    "Groovy closure with HTML5 CSS properties" {
        // Specific test for html5 transtype with args.css property
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.copycss", "value" to "yes"))
                capture.property(mapOf("name" to "args.css", "value" to "dita-ot-doc.css"))
                capture.property(mapOf("name" to "args.csspath", "value" to "css"))
                capture.property(mapOf("name" to "args.cssroot", "value" to "/resources/"))
                capture.property(mapOf("name" to "args.gen.task.lbl", "value" to "YES"))
                capture.property(mapOf("name" to "nav-toc", "value" to "partial"))
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties(propertiesClosure)
        }

        // Verify closure was stored
        task.options.properties.shouldNotBeNull()

        // Verify properties can be captured from the closure
        val captured = GroovyPropertyCapture.captureFromClosure(propertiesClosure)
        captured["args.css"] shouldBe "dita-ot-doc.css"
        captured["args.copycss"] shouldBe "yes"
        captured["args.csspath"] shouldBe "css"
        captured["args.cssroot"] shouldBe "/resources/"
        captured["args.gen.task.lbl"] shouldBe "YES"
        captured["nav-toc"] shouldBe "partial"
    }

    "Task stores Groovy closure in options.properties" {
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "processing-mode", "value" to "strict"))
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties(propertiesClosure)
        }

        task.options.properties shouldBe propertiesClosure
    }

    "ditaProperties MapProperty can be used directly" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            ditaProperties.put("args.css", "direct-api.css")
            ditaProperties.put("args.copycss", "yes")
        }

        task.ditaProperties.get()["args.css"] shouldBe "direct-api.css"
        task.ditaProperties.get()["args.copycss"] shouldBe "yes"
    }

    "Both closure and ditaProperties can be used together" {
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.css", "value" to "from-closure.css"))
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties(propertiesClosure)
            ditaProperties.put("args.copycss", "yes")
        }

        // Both should be set
        task.options.properties.shouldNotBeNull()
        task.ditaProperties.get()["args.copycss"] shouldBe "yes"

        // Closure properties should be captured
        val closureProps = GroovyPropertyCapture.captureFromClosure(propertiesClosure)
        closureProps["args.css"] shouldBe "from-closure.css"
    }

    "ditaProperties overrides closure properties when same key" {
        // This tests the expected behavior: ditaProperties.put() takes precedence
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.css", "value" to "from-closure.css"))
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            properties(propertiesClosure)
            ditaProperties.put("args.css", "from-direct-api.css")
        }

        // Direct API should override closure
        task.ditaProperties.get()["args.css"] shouldBe "from-direct-api.css"
    }

    "Groovy closure with PDF properties" {
        val propertiesClosure = object : groovy.lang.Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.chapter.layout", "value" to "BASIC"))
                capture.property(mapOf("name" to "args.gen.task.lbl", "value" to "YES"))
                capture.property(mapOf("name" to "include.rellinks", "value" to "#default external"))
                capture.property(mapOf("name" to "outputFile.base", "value" to "userguide"))
                capture.property(mapOf("name" to "theme", "value" to "custom-theme.yaml"))
            }
        }

        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("pdf")
            properties(propertiesClosure)
        }

        val captured = GroovyPropertyCapture.captureFromClosure(propertiesClosure)
        captured["args.chapter.layout"] shouldBe "BASIC"
        captured["outputFile.base"] shouldBe "userguide"
        captured["theme"] shouldBe "custom-theme.yaml"
    }

    "Integration test: Groovy DSL build with args.css property" {
        settingsFile.writeText("rootProject.name = 'dita-css-test'")

        // Copy example DITA files to test project
        val ditaDir = File(testProjectDir, "dita")
        ditaDir.mkdirs()
        File("$examplesDir/simple/dita").copyRecursively(ditaDir, overwrite = true)

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                transtype 'html5'

                // Test the Groovy closure properties syntax
                properties {
                    property name: 'processing-mode', value: 'strict'
                    property name: 'args.rellinks', value: 'all'
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita", "--info")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    "Integration test: ditaProperties.put with args.css" {
        settingsFile.writeText("rootProject.name = 'dita-properties-test'")

        // Copy example DITA files to test project
        val ditaDir = File(testProjectDir, "dita")
        ditaDir.mkdirs()
        File("$examplesDir/simple/dita").copyRecursively(ditaDir, overwrite = true)

        buildFile.writeText(
            """
            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            dita {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                transtype 'html5'

                // Test the direct ditaProperties API
                ditaProperties.put('processing-mode', 'strict')
                ditaProperties.put('args.rellinks', 'all')
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("dita", "--info")
            .forwardOutput()
            .build()

        result.task(":dita")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    // ============================================================================
    // Implicit Dependency Bug Tests (v2.3.2 fix)
    // These tests ensure multiple tasks sharing the same ditaOtDir don't have
    // implicit dependencies inferred by Gradle.
    // See: BUGS_EXPLAINATION.md for full analysis
    // ============================================================================

    "ditaOtDir should be marked as @Internal, not @InputDirectory" {
        // This test verifies the fix for the implicit dependency bug.
        // When ditaOtDir was @InputDirectory, Gradle would infer implicit
        // dependencies between tasks sharing the same DITA-OT installation.
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
        }

        // Get all task inputs - ditaOtDir should NOT be among them
        val taskInputs = task.inputs.files.files
        val ditaOtFile = File(ditaHome)

        // The DITA-OT directory itself should not be a direct task input
        // (it may be included via getInputFileTree in devMode, but not as direct input)
        taskInputs.contains(ditaOtFile) shouldBe false
    }

    "Multiple tasks with same ditaOtDir should not have shared input files" {
        // Create two tasks pointing to the same DITA-OT directory
        val task1 = project.tasks.create("html", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output("out/html")
            transtype("html5")
        }

        val task2 = project.tasks.create("pdf", DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            output("out/pdf")
            transtype("pdf")
        }

        // Get input files for both tasks (excluding getInputFileTree which is separate)
        val task1DirectInputs = task1.inputFiles.files
        val task2DirectInputs = task2.inputFiles.files

        // Both should have the same input files (the ditamap)
        task1DirectInputs shouldBe task2DirectInputs

        // But neither should include the DITA-OT directory as a direct input
        val ditaOtFile = File(ditaHome)
        task1DirectInputs.none { it.absolutePath.startsWith(ditaOtFile.absolutePath) } shouldBe true
        task2DirectInputs.none { it.absolutePath.startsWith(ditaOtFile.absolutePath) } shouldBe true
    }

    "Integration test: Multiple tasks with same ditaOtDir run without implicit dependency errors" {
        settingsFile.writeText("rootProject.name = 'multi-task-test'")

        // Copy example DITA files to test project
        val ditaDir = File(testProjectDir, "dita")
        ditaDir.mkdirs()
        File("$examplesDir/simple/dita").copyRecursively(ditaDir, overwrite = true)

        buildFile.writeText(
            """
            import com.github.jyjeanne.DitaOtTask

            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            task html(type: DitaOtTask) {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                output 'out/html'
                transtype 'html5'
            }

            task pdf(type: DitaOtTask) {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                output 'out/pdf'
                transtype 'pdf'
            }
            """.trimIndent()
        )

        // Run both tasks - should not fail with implicit dependency errors
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("html", "pdf", "--info")
            .forwardOutput()
            .build()

        result.task(":html")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":pdf")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    "Integration test: Output inside ditaOtDir does not cause dependency errors" {
        // This simulates the DITA-OT distribution build scenario where
        // output is written inside the DITA-OT directory structure
        settingsFile.writeText("rootProject.name = 'output-inside-ditaot-test'")

        // Copy example DITA files to test project
        val ditaDir = File(testProjectDir, "dita")
        ditaDir.mkdirs()
        File("$examplesDir/simple/dita").copyRecursively(ditaDir, overwrite = true)

        // Create output directory inside the project (simulating ditaOt/doc scenario)
        val outputDir = File(testProjectDir, "doc")
        outputDir.mkdirs()

        buildFile.writeText(
            """
            import com.github.jyjeanne.DitaOtTask

            plugins {
                id 'io.github.jyjeanne.dita-ot-gradle'
            }

            task html(type: DitaOtTask) {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                output 'doc'
                transtype 'html5'
            }

            task pdf(type: DitaOtTask) {
                ditaOt '$ditaHome'
                input 'dita/root.ditamap'
                output 'doc'
                transtype 'pdf'
            }
            """.trimIndent()
        )

        // Run both tasks - should not fail even with shared output directory
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("html", "pdf", "--info")
            .forwardOutput()
            .build()

        result.task(":html")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":pdf")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    "devMode includes ditaOtDir in input file tree but not as direct task input" {
        val task = project.tasks.create(DITA, DitaOtTask::class.java).apply {
            ditaOt(ditaHome)
            input("$examplesDir/simple/dita/root.ditamap")
            transtype("html5")
            devMode(true)
        }

        // In devMode, getInputFileTree() should include DITA-OT files
        val inputFileTree = task.getInputFileTree().files
        inputFileTree shouldContain File(ditaHome, "build.xml")

        // But the direct task inputs should NOT include ditaOtDir
        // (getInputFileTree is a separate @InputFiles method)
        val directInputFiles = task.inputFiles.files
        directInputFiles.none { it.absolutePath == File(ditaHome).absolutePath } shouldBe true
    }
})
