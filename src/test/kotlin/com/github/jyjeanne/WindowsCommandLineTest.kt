package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Files

/**
 * Tests for the Windows cmd.exe command-line length workaround used when running dita.bat.
 */
class WindowsCommandLineTest : StringSpec({

    val logger = Logging.getLogger(WindowsCommandLineTest::class.java)

    // Shaped like DITA-OT 4.4.2's config\env.bat: 47 plugin JARs, about 53 characters each
    val envBat = (1..47).map { i ->
        """set "CLASSPATH=%CLASSPATH%;%DITA_HOME%\plugins\org.example.plugin$i\lib\plugin-library-$i.jar""""
    }

    fun estimateFor(ditaHome: String, arguments: List<String> = emptyList(), inheritedClasspath: String = "") =
        WindowsCommandLine.estimateCommandLength(
            ditaHomeAsSeenByScript = ditaHome,
            envBatLines = envBat,
            arguments = arguments,
            javaCommand = "C:\\Java\\jdk-21\\bin\\java.exe",
            inheritedClasspath = inheritedClasspath,
            antOpts = "",
            antArgs = ""
        )

    // ============================================================================
    // Estimate
    // ============================================================================

    "estimate exceeds the cmd.exe limit for a DITA-OT 4.x install under a long path" {
        // 101 characters: the layout that failed with "La ligne entrée est trop longue"
        val longHome = "C:\\Users\\someone\\Documents\\Perso\\Projects\\dita-ot-gradle\\test-project\\build\\dita-ot\\dita-ot-4.4.2\\bin\\.."

        estimateFor(longHome) shouldBeGreaterThan WindowsCommandLine.CMD_EXE_MAX_LENGTH
    }

    "estimate stays under the limit for the same install under its 8.3 short path" {
        val shortHome = "C:\\Users\\someone\\DOCUME~1\\Perso\\Projects\\DITA-O~1\\TEST-P~1\\build\\dita-ot\\DITA-O~1.2\\bin\\.."

        estimateFor(shortHome) shouldBeLessThan WindowsCommandLine.CMD_EXE_MAX_LENGTH
    }

    "estimate grows by one DITA_HOME length per CLASSPATH entry" {
        val home = "C:\\dita"
        val longer = "C:\\dita-ot-installation"
        val perCharacter = estimateFor(longer) - estimateFor(home)

        perCharacter shouldBe (longer.length - home.length) * (envBat.size + WindowsCommandLine.DITA_HOME_REFERENCES)
    }

    "estimate ignores env.bat lines that do not append to CLASSPATH" {
        val withNoise = envBat + listOf("@echo off", "rem comment", "set \"OTHER=%DITA_HOME%\\x\"")
        val home = "C:\\dita"

        WindowsCommandLine.estimateCommandLength(home, withNoise, emptyList(), "java.exe", "", "", "") shouldBe
            WindowsCommandLine.estimateCommandLength(home, envBat, emptyList(), "java.exe", "", "", "")
    }

    "estimate counts an inherited CLASSPATH and quotes around arguments with spaces" {
        val home = "C:\\dita"
        val base = estimateFor(home)

        estimateFor(home, inheritedClasspath = "C:\\lib\\a.jar") shouldBe base + "C:\\lib\\a.jar".length
        estimateFor(home, arguments = listOf("--input", "C:\\my docs\\a.ditamap")) shouldBe
            base + (1 + "--input".length) + (1 + "C:\\my docs\\a.ditamap".length + 2)
    }

    "estimate is at least the fixed overhead without env.bat" {
        WindowsCommandLine.estimateCommandLength("C:\\d", emptyList(), emptyList(), "", "", "", "") shouldBeGreaterThan
            WindowsCommandLine.FIXED_OVERHEAD
    }

    // ============================================================================
    // DITA_HOME and java command as dita.bat computes them
    // ============================================================================

    "DITA_HOME seen by a script in bin is the bin directory plus .." {
        val script = File(File(File("dita-ot-4.4.2"), "bin"), "dita.bat")
        WindowsCommandLine.ditaHomeAsSeenByScript(script) shouldBe script.parentFile.path + "\\.."
    }

    "DITA_HOME seen by a script at the installation root is that directory" {
        val script = File(File("dita-ot-2.5"), "dita.bat")
        WindowsCommandLine.ditaHomeAsSeenByScript(script) shouldBe script.parentFile.path
    }

    "java command follows dita.bat precedence: JAVACMD, then JAVA_HOME, then java.exe" {
        WindowsCommandLine.javaCommand(mapOf("JAVACMD" to "C:\\j\\java.exe", "JAVA_HOME" to "C:\\jdk")) shouldBe "C:\\j\\java.exe"
        WindowsCommandLine.javaCommand(mapOf("JAVA_HOME" to "C:\\jdk\\")) shouldBe "C:\\jdk\\bin\\java.exe"
        WindowsCommandLine.javaCommand(emptyMap()) shouldBe "java.exe"
    }

    // ============================================================================
    // resolveDitaScript
    // ============================================================================

    "resolveDitaScript keeps the script when the command line fits" {
        val ditaHome = Files.createTempDirectory("dita-ot").toFile()
        try {
            val bin = File(ditaHome, "bin").apply { mkdirs() }
            val script = File(bin, if (Platform.isWindows) "dita.bat" else "dita").apply { writeText("") }
            File(ditaHome, "config").mkdirs()
            File(ditaHome, "config/env.bat").writeText(envBat.take(2).joinToString("\r\n"))

            WindowsCommandLine.resolveDitaScript(script, listOf("--format", "html5"), logger) shouldBe script
        } finally {
            ditaHome.deleteRecursively()
        }
    }

    "resolveDitaScript never changes the Unix dita script" {
        val script = File("/opt/dita-ot/bin/dita")
        WindowsCommandLine.resolveDitaScript(script, emptyList(), logger) shouldBe script
    }

    "shortPathOf points to the same directory, or returns null when 8.3 names are unavailable".config(
        enabled = Platform.isWindows
    ) {
        val parent = Files.createTempDirectory("short-path-test").toFile()
        try {
            val longDir = File(parent, "a directory name that is much longer than eight characters").apply { mkdirs() }

            val shortDir = WindowsCommandLine.shortPathOf(longDir, logger)

            if (shortDir != null) {
                shortDir.path.length shouldBeLessThan longDir.absolutePath.length
                shortDir.canonicalPath shouldBe longDir.canonicalPath
            }
        } finally {
            parent.deleteRecursively()
        }
    }
})
