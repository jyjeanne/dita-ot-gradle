package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    // The relevant lines of DITA-OT 4.x and 3.x bin\dita.bat
    val ditaBat4 = listOf(
        "set DITA_HOME=%~dp0..",
        """if "!DITA_CMD_SUBCOMMAND!" == "install" goto checkJava""",
        """if "!DITA_CMD_SUBCOMMAND!" == "uninstall" goto checkJava""",
        """call "%DITA_HOME%\config\env.bat""""
    )
    val ditaBat3 = listOf("set DITA_HOME=%~dp0..", """call "%DITA_HOME%\config\env.bat"""")

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

    /** Creates <temp>/<nested>/dita-ot-4.4.2/{bin/dita.bat, config/env.bat}; returns dita.bat. */
    fun fakeDitaOt(root: File, nested: String, ditaBatLines: List<String>, envBatLines: List<String>): File {
        val home = File(File(root, nested), "dita-ot-4.4.2")
        File(home, "bin").mkdirs()
        File(home, "config").mkdirs()
        File(home, "config/env.bat").writeText(envBatLines.joinToString("\r\n"))
        return File(home, "bin/dita.bat").apply { writeText(ditaBatLines.joinToString("\r\n")) }
    }

    // ============================================================================
    // Estimate
    // ============================================================================

    "estimate exceeds the cmd.exe limit for a DITA-OT 4.x install under a long path" {
        // 104 characters: the layout that failed with "La ligne entrée est trop longue"
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

    "estimate counts the inherited CLASSPATH (env.bat appends to it) and quotes around arguments with spaces" {
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
    // Which env.bat lines run
    // ============================================================================

    "DITA-OT 4.x dita.bat skips env.bat for install and uninstall only" {
        WindowsCommandLine.skipsEnvBat(ditaBat4, "install") shouldBe true
        WindowsCommandLine.skipsEnvBat(ditaBat4, "uninstall") shouldBe true
        WindowsCommandLine.skipsEnvBat(ditaBat4, "plugins") shouldBe false
    }

    "DITA-OT 3.x dita.bat runs env.bat for install too" {
        WindowsCommandLine.skipsEnvBat(ditaBat3, "install") shouldBe false
    }

    "env.bat is counted for install with DITA-OT 3.x but not with 4.x" {
        val root = Files.createTempDirectory("dita-ot-env").toFile()
        try {
            val script4 = fakeDitaOt(File(root, "v4"), "x", ditaBat4, envBat)
            val script3 = fakeDitaOt(File(root, "v3"), "x", ditaBat3, envBat)
            val install = listOf("install", "org.dita.pdf2")
            val transform = listOf("--input", "a.ditamap", "--format", "html5")

            WindowsCommandLine.envBatLinesFor(script4, install) shouldBe emptyList()
            WindowsCommandLine.envBatLinesFor(script4, transform).size shouldBe envBat.size
            WindowsCommandLine.envBatLinesFor(script3, install).size shouldBe envBat.size
        } finally {
            root.deleteRecursively()
        }
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
        val both = mapOf("JAVACMD" to "C:\\j\\java.exe", "JAVA_HOME" to "C:\\jdk")
        WindowsCommandLine.javaCommand { both[it] } shouldBe "C:\\j\\java.exe"
        WindowsCommandLine.javaCommand { mapOf("JAVA_HOME" to "C:\\jdk\\")[it] } shouldBe "C:\\jdk\\bin\\java.exe"
        WindowsCommandLine.javaCommand { null } shouldBe "java.exe"
    }

    // ============================================================================
    // resolveDitaScript
    // ============================================================================

    "resolveDitaScript keeps the absolute script when the command line fits" {
        // A relative path under the working directory. (Not relativeTo() on a temp file: on CI
        // the temp folder is on C: and the checkout on D:, and paths across drives can't be relative.)
        val root = File("build/tmp/windows-command-line-test-${System.nanoTime()}")
        try {
            val relative = fakeDitaOt(root, "x", ditaBat4, envBat.take(2))
            relative.isAbsolute shouldBe false

            WindowsCommandLine.resolveDitaScript(relative, listOf("--format", "html5"), logger).use { resolved ->
                resolved.file.isAbsolute shouldBe true
                resolved.file.canonicalPath shouldBe relative.absoluteFile.canonicalPath
            }
        } finally {
            root.deleteRecursively()
        }
    }

    "resolveDitaScript never changes the Unix dita script, other than making it absolute" {
        val script = File(File(File("opt"), "bin"), "dita")
        WindowsCommandLine.resolveDitaScript(script, emptyList(), logger).use { resolved ->
            resolved.file shouldBe script.absoluteFile
        }
    }

    "resolveDitaScript runs an over-limit dita.bat through a shorter path to the same file".config(
        enabled = Platform.isWindows
    ) {
        val root = Files.createTempDirectory("dita-ot-long").toFile()
        try {
            val script = fakeDitaOt(root, LONG_FOLDER, ditaBat4, envBat)
            val arguments = listOf("--input", "a.ditamap", "--format", "html5")
            WindowsCommandLine.estimate(script, arguments, envBat) shouldBeGreaterThan
                WindowsCommandLine.CMD_EXE_MAX_LENGTH

            // 8.3 short path where available, otherwise a junction: both must lead to dita.bat
            WindowsCommandLine.resolveDitaScript(script, arguments, logger).use { resolved ->
                resolved.path shouldNotBe script.absolutePath
                resolved.path.length shouldBeLessThan script.absolutePath.length
                resolved.file.toPath().toRealPath() shouldBe script.toPath().toRealPath()
                WindowsCommandLine.estimate(resolved.file, arguments, envBat) shouldBeLessThan
                    WindowsCommandLine.CMD_EXE_MAX_LENGTH
            }
        } finally {
            root.deleteRecursively()
        }
    }

    "the warning lists why each shortening attempt failed" {
        val message = WindowsCommandLine.CMD_EXE_MAX_LENGTH.let { limit ->
            Messages.windowsCommandLineTooLong(9000, limit, "C:\\dita-ot", listOf("first reason", "second reason"))
        }

        message.lines().filter { it.startsWith("  - ") } shouldBe listOf("  - first reason", "  - second reason")
    }

    // ============================================================================
    // shortPathOf
    // ============================================================================

    "shortPathOf points to the same directory, or reports that no 8.3 name exists".config(
        enabled = Platform.isWindows
    ) {
        val parent = Files.createTempDirectory("short-path-test").toFile()
        try {
            val longDir = File(parent, "a directory name that is much longer than eight characters").apply { mkdirs() }

            when (val shortPath = WindowsCommandLine.shortPathOf(longDir, logger)) {
                is WindowsCommandLine.ShortPath.Found -> {
                    shortPath.dir.path.length shouldBeLessThan longDir.absolutePath.length
                    shortPath.dir.canonicalPath shouldBe longDir.canonicalPath
                }
                else -> shortPath shouldBe WindowsCommandLine.ShortPath.Unavailable
            }
        } finally {
            parent.deleteRecursively()
        }
    }

    "shortPathOf refuses UNC paths, where cmd.exe would fall back to C:\\Windows".config(
        enabled = Platform.isWindows
    ) {
        val shortPath = WindowsCommandLine.shortPathOf(File("\\\\server\\share\\dita-ot\\bin"), logger)

        (shortPath is WindowsCommandLine.ShortPath.Failed) shouldBe true
    }

    // ============================================================================
    // Junction fallback (8.3 names disabled)
    // ============================================================================

    "removing a junction deletes only the link, never the DITA-OT files behind it".config(
        enabled = Platform.isWindows
    ) {
        val target = Files.createTempDirectory("junction-target").toFile()
        val base = Files.createTempDirectory("junction-base").toFile()
        try {
            val marker = File(target, "plugins/org.example/plugin.xml").apply { parentFile.mkdirs(); writeText("<plugin/>") }

            val created = WindowsCommandLine.createJunction(target, base, logger)
            (created is WindowsCommandLine.Junction.Created) shouldBe true
            val link = (created as WindowsCommandLine.Junction.Created).link
            WindowsCommandLine.isJunction(link.toPath()) shouldBe true
            File(link, "plugins/org.example/plugin.xml").readText() shouldBe "<plugin/>"

            WindowsCommandLine.removeJunction(link, logger)

            link.exists() shouldBe false
            marker.readText() shouldBe "<plugin/>"
        } finally {
            base.listFiles()?.forEach { WindowsCommandLine.removeJunction(it, logger) }
            base.delete()
            target.deleteRecursively()
        }
    }

    "removeJunction leaves real folders alone" {
        val folder = Files.createTempDirectory("not-a-junction").toFile()
        try {
            File(folder, "keep.txt").writeText("keep")

            WindowsCommandLine.removeJunction(folder, logger)

            File(folder, "keep.txt").readText() shouldBe "keep"
        } finally {
            folder.deleteRecursively()
        }
    }

    "without 8.3 names, an over-limit dita.bat runs through a junction that is removed on close".config(
        enabled = Platform.isWindows
    ) {
        val root = Files.createTempDirectory("dita-ot-junction").toFile()
        val base = Files.createTempDirectory("junction-base").toFile()
        try {
            // A dita.bat that prints the folder it was invoked from (%~dp0)
            val script = fakeDitaOt(root, LONG_FOLDER, listOf("@echo off", "echo %~dp0"), envBat)
            val arguments = listOf("--input", "a.ditamap", "--format", "html5")

            val resolved = WindowsCommandLine.resolveDitaScript(
                script, arguments, logger, useShortNames = false, junctionBaseDir = base
            )
            val link = resolved.file.parentFile.parentFile
            try {
                link.parentFile.canonicalPath shouldBe base.canonicalPath
                link.name.startsWith(WindowsCommandLine.JUNCTION_PREFIX) shouldBe true
                resolved.file.toPath().toRealPath() shouldBe script.toPath().toRealPath()
                WindowsCommandLine.estimate(resolved.file, arguments, envBat) shouldBeLessThan
                    WindowsCommandLine.CMD_EXE_MAX_LENGTH

                // dita.bat derives DITA_HOME from %~dp0, which must be the junction path
                val process = ProcessBuilder("cmd.exe", "/d", "/c", resolved.path).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                process.waitFor()
                output.lowercase() shouldBe (link.path + "\\bin\\").lowercase()
            } finally {
                resolved.close()
            }

            link.exists() shouldBe false
            script.isFile shouldBe true
            File(script.parentFile.parentFile, "config/env.bat").isFile shouldBe true
        } finally {
            base.listFiles()?.forEach { WindowsCommandLine.removeJunction(it, logger) }
            base.delete()
            root.deleteRecursively()
        }
    }

    "the stale-junction sweep removes only old junctions".config(enabled = Platform.isWindows) {
        val target = Files.createTempDirectory("junction-target").toFile()
        val base = Files.createTempDirectory("junction-base").toFile()
        try {
            File(target, "keep.txt").writeText("keep")
            val link = (WindowsCommandLine.createJunction(target, base, logger) as WindowsCommandLine.Junction.Created).link
            val realFolder = File(base, WindowsCommandLine.JUNCTION_PREFIX + "realfolder").apply { mkdirs() }

            // Not stale yet
            WindowsCommandLine.sweepStaleJunctions(base, System.currentTimeMillis(), logger)
            link.exists() shouldBe true

            // Two days later
            val later = System.currentTimeMillis() + 2 * WindowsCommandLine.STALE_JUNCTION_AGE_MILLIS
            WindowsCommandLine.sweepStaleJunctions(base, later, logger)

            link.exists() shouldBe false
            realFolder.isDirectory shouldBe true
            File(target, "keep.txt").readText() shouldBe "keep"
        } finally {
            base.listFiles()?.forEach { WindowsCommandLine.removeJunction(it, logger) }
            base.deleteRecursively()
            target.deleteRecursively()
        }
    }
})

/** A folder name that pushes a DITA-OT 4.x command line over the cmd.exe limit. */
private const val LONG_FOLDER = "a folder name long enough to push the dita bat command line over the cmd limit"
