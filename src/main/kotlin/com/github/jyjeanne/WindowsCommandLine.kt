package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Keeps DITA-OT's `dita.bat` under the cmd.exe command-line length limit on Windows.
 *
 * `dita.bat` sets `DITA_HOME=%~dp0..`, and `config\env.bat` appends one absolute path per
 * plugin JAR to `CLASSPATH`. The final `java` command line therefore grows with
 * (number of JARs x length of the DITA-OT path). DITA-OT 4.x adds about 47 JARs, so under a
 * directory of roughly 100 characters the line passes cmd.exe's 8191-character limit and
 * `dita.bat` fails with exit code 255 ("The input line is too long").
 *
 * `%~dp0` is the path the script was *invoked* with, so running `dita.bat` through its 8.3
 * short path (for example `C:\Users\me\DOCUME~1\...`) shortens every CLASSPATH entry.
 * This is only done when the estimated command line would exceed the limit, so builds that
 * work today run exactly as before.
 *
 * @since 2.8.8
 */
internal object WindowsCommandLine {

    /** Maximum length of a command line that cmd.exe can process. */
    const val CMD_EXE_MAX_LENGTH = 8191

    /**
     * Characters of dita.bat's `java` line that the estimate does not compute from inputs
     * (`-Djava.awt.headless=true`, `-classpath "...\lib\ant-launcher.jar;...\config"`,
     * `org.apache.tools.ant.launch.Launcher`, `-buildfile`, `-main ...`), with headroom.
     */
    internal const val FIXED_OVERHEAD = 400

    /** `%DITA_HOME%` references in dita.bat's `java` line outside `CLASSPATH`. */
    internal const val DITA_HOME_REFERENCES = 5

    private val CLASSPATH_LINE =
        Regex("""^\s*set\s+"CLASSPATH=%CLASSPATH%(.*)"\s*$""", RegexOption.IGNORE_CASE)

    private const val DITA_HOME_TOKEN = "%DITA_HOME%"

    /**
     * Returns the script to execute: [script] itself, or the same file addressed through its
     * 8.3 short path when the long path would push dita.bat's command line over the limit.
     *
     * On non-Windows platforms, or for anything other than a `.bat` file, [script] is returned
     * unchanged.
     *
     * @param script the `dita.bat` file
     * @param arguments the arguments passed after the script
     */
    fun resolveDitaScript(script: File, arguments: List<String>, logger: Logger): File {
        if (!Platform.isWindows || !script.name.endsWith(".bat", ignoreCase = true)) {
            return script
        }

        val envBat = envBatLines(script)
        val estimate = estimate(script, arguments, envBat)
        if (estimate <= CMD_EXE_MAX_LENGTH) {
            logger.debug("dita.bat command line estimate: $estimate characters (limit $CMD_EXE_MAX_LENGTH)")
            return script
        }

        val shortDir = shortPathOf(script.parentFile, logger)
        if (shortDir == null) {
            logger.warn(
                Messages.windowsCommandLineTooLong(
                    estimate, CMD_EXE_MAX_LENGTH, ditaHomeOf(script).absolutePath, shortPathUsed = false
                )
            )
            return script
        }

        val shortScript = File(shortDir, script.name)
        val shortEstimate = estimate(shortScript, arguments, envBat)
        logger.info(
            "dita.bat command line would be about $estimate characters (cmd.exe limit: " +
                "$CMD_EXE_MAX_LENGTH); running it through its short path instead " +
                "($shortEstimate characters): ${shortScript.path}"
        )
        if (shortEstimate > CMD_EXE_MAX_LENGTH) {
            logger.warn(
                Messages.windowsCommandLineTooLong(
                    shortEstimate, CMD_EXE_MAX_LENGTH, ditaHomeOf(script).absolutePath, shortPathUsed = true
                )
            )
        }
        return shortScript
    }

    /**
     * Estimate the length of the `java` command line that dita.bat builds when it is
     * invoked as [script] with [arguments], using the current process environment.
     */
    internal fun estimate(script: File, arguments: List<String>, envBatLines: List<String>): Int =
        estimateCommandLength(
            ditaHomeAsSeenByScript = ditaHomeAsSeenByScript(script),
            envBatLines = envBatLines,
            arguments = arguments,
            javaCommand = javaCommand(System.getenv()),
            inheritedClasspath = System.getenv("CLASSPATH").orEmpty(),
            antOpts = System.getenv("ANT_OPTS").orEmpty(),
            antArgs = System.getenv("ANT_ARGS").orEmpty()
        )

    /**
     * Pure estimate of the `java` command line built by dita.bat.
     *
     * @param ditaHomeAsSeenByScript the value dita.bat gives `DITA_HOME` (`%~dp0..`)
     * @param envBatLines contents of `config\env.bat`
     * @param arguments arguments passed to dita.bat
     */
    internal fun estimateCommandLength(
        ditaHomeAsSeenByScript: String,
        envBatLines: List<String>,
        arguments: List<String>,
        javaCommand: String,
        inheritedClasspath: String,
        antOpts: String,
        antArgs: String
    ): Int {
        val classpath = inheritedClasspath.length + envBatLines.sumOf { line ->
            val appended = CLASSPATH_LINE.matchEntire(line)?.groupValues?.get(1) ?: return@sumOf 0
            appended.replace(DITA_HOME_TOKEN, ditaHomeAsSeenByScript, ignoreCase = true).length
        }
        // cmd.exe keeps the quotes that Java adds around arguments containing spaces
        val argumentsLength = arguments.sumOf { arg ->
            1 + arg.length + if (arg.any { it == ' ' || it == '\t' }) 2 else 0
        }
        return javaCommand.length + 2 +
            antOpts.length + antArgs.length +
            classpath +
            DITA_HOME_REFERENCES * ditaHomeAsSeenByScript.length +
            argumentsLength +
            FIXED_OVERHEAD
    }

    /**
     * `DITA_HOME` as dita.bat computes it: `%~dp0..` for a script in `bin\`
     * (DITA-OT 3.0+), or the script's own directory for older layouts.
     */
    internal fun ditaHomeAsSeenByScript(script: File): String {
        val dir = script.parentFile.path.trimEnd('\\', '/')
        return if (script.parentFile.name.equals("bin", ignoreCase = true)) "$dir\\.." else dir
    }

    /**
     * The java executable dita.bat will use: `%JAVACMD%`, else `%JAVA_HOME%\bin\java.exe`,
     * else `java.exe`.
     */
    internal fun javaCommand(env: Map<String, String>): String {
        env["JAVACMD"]?.takeIf { it.isNotBlank() }?.let { return it }
        env["JAVA_HOME"]?.takeIf { it.isNotBlank() }?.let { return it.trimEnd('\\') + "\\bin\\java.exe" }
        return "java.exe"
    }

    private fun ditaHomeOf(script: File): File =
        if (script.parentFile.name.equals("bin", ignoreCase = true)) script.parentFile.parentFile else script.parentFile

    private fun envBatLines(script: File): List<String> {
        val envBat = File(ditaHomeOf(script), "config/env.bat")
        return try {
            if (envBat.isFile) envBat.readLines() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * The 8.3 short form of [dir], or `null` if it cannot be determined or is not shorter
     * (8.3 names can be disabled per volume).
     */
    internal fun shortPathOf(dir: File, logger: Logger): File? {
        return try {
            // Runs in [dir], so no path (and no quoting) is needed in the command itself
            val process = ProcessBuilder("cmd.exe", "/d", "/c", "for %I in (.) do @echo %~fsI")
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            val shortPath = output.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() } ?: return null
            val shortDir = File(shortPath)
            if (process.exitValue() == 0 && shortDir.isDirectory && shortPath.length < dir.absolutePath.length) {
                shortDir
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Could not determine the short path of ${dir.absolutePath}: ${e.message}")
            null
        }
    }
}
