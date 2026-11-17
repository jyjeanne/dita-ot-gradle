package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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

    private const val SHORT_PATH_TIMEOUT_SECONDS = 10L

    private val CLASSPATH_LINE =
        Regex("""^\s*set\s+"CLASSPATH=%CLASSPATH%(.*)"\s*$""", RegexOption.IGNORE_CASE)

    private const val DITA_HOME_TOKEN = "%DITA_HOME%"

    /** Outcome of looking up the 8.3 short form of a directory. */
    internal sealed class ShortPath {
        /** A shorter path to the same directory. */
        data class Found(val dir: File) : ShortPath()

        /** The directory has no shorter 8.3 form (8.3 names were disabled when its folders were created). */
        object Unavailable : ShortPath()

        /** The lookup itself failed; [reason] says why. */
        data class Failed(val reason: String) : ShortPath()
    }

    /** Short paths found in this JVM. Re-checked before reuse: folders can be deleted and re-created. */
    private val shortPathCache = ConcurrentHashMap<String, File>()

    private data class CachedLines(val lastModified: Long, val length: Long, val lines: List<String>)

    /** dita.bat / env.bat contents, keyed by path and invalidated when the file changes. */
    private val linesCache = ConcurrentHashMap<String, CachedLines>()

    /**
     * Returns the absolute script to execute: [script] itself, or the same file addressed
     * through its 8.3 short path when the long path would push dita.bat's command line over
     * the limit.
     *
     * On non-Windows platforms, or for anything other than a `.bat` file, the absolute
     * [script] is returned unchanged.
     *
     * @param script the `dita.bat` file
     * @param arguments the arguments passed after the script (subcommand first, if any)
     */
    fun resolveDitaScript(script: File, arguments: List<String>, logger: Logger): File {
        val absoluteScript = script.absoluteFile
        if (!Platform.isWindows || !absoluteScript.name.endsWith(".bat", ignoreCase = true)) {
            return absoluteScript
        }

        val envBat = envBatLinesFor(absoluteScript, arguments)
        val estimate = estimate(absoluteScript, arguments, envBat)
        if (estimate <= CMD_EXE_MAX_LENGTH) {
            logger.debug("dita.bat command line estimate: $estimate characters (limit $CMD_EXE_MAX_LENGTH)")
            return absoluteScript
        }

        val ditaHome = ditaHomeOf(absoluteScript).path
        val shortPath = shortPathOf(absoluteScript.parentFile, logger)
        val shortScript = (shortPath as? ShortPath.Found)?.let { File(it.dir, absoluteScript.name) }

        if (shortScript == null || !shortScript.isFile) {
            val reason = when (shortPath) {
                is ShortPath.Failed -> "and its 8.3 short path could not be determined: ${shortPath.reason}"
                is ShortPath.Found -> "and dita.bat was not found through its 8.3 short path ${shortPath.dir}"
                ShortPath.Unavailable -> "and the folder has no 8.3 short name (8.3 names are disabled " +
                    "on the volume, or were when its folders were created)"
            }
            logger.warn(Messages.windowsCommandLineTooLong(estimate, CMD_EXE_MAX_LENGTH, ditaHome, reason))
            return absoluteScript
        }

        val shortEstimate = estimate(shortScript, arguments, envBat)
        logger.info(
            "dita.bat command line would be about $estimate characters (cmd.exe limit: " +
                "$CMD_EXE_MAX_LENGTH); running it through its short path instead " +
                "($shortEstimate characters): ${shortScript.path}"
        )
        if (shortEstimate > CMD_EXE_MAX_LENGTH) {
            logger.warn(
                Messages.windowsCommandLineTooLong(
                    shortEstimate, CMD_EXE_MAX_LENGTH, ditaHome, "even through its 8.3 short path ${shortScript.parent}"
                )
            )
        }
        return shortScript
    }

    /**
     * Estimate the length of the `java` command line that dita.bat builds when it is
     * invoked as [script] with [arguments], using the current process environment.
     * `System.getenv(name)` is case-insensitive on Windows, like cmd.exe's `%NAME%`.
     */
    internal fun estimate(script: File, arguments: List<String>, envBatLines: List<String>): Int =
        estimateCommandLength(
            ditaHomeAsSeenByScript = ditaHomeAsSeenByScript(script),
            envBatLines = envBatLines,
            arguments = arguments,
            javaCommand = javaCommand(System::getenv),
            inheritedClasspath = System.getenv("CLASSPATH").orEmpty(),
            antOpts = System.getenv("ANT_OPTS").orEmpty(),
            antArgs = System.getenv("ANT_ARGS").orEmpty()
        )

    /**
     * Pure estimate of the `java` command line built by dita.bat.
     *
     * @param ditaHomeAsSeenByScript the value dita.bat gives `DITA_HOME` (`%~dp0..`)
     * @param envBatLines contents of `config\env.bat` (empty when dita.bat skips it)
     * @param arguments arguments passed to dita.bat
     * @param inheritedClasspath the `CLASSPATH` environment variable, which env.bat appends to
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
     * The `config\env.bat` lines that will run for [arguments]: none when the subcommand is
     * one that dita.bat skips env.bat for (DITA-OT 4.x does for `install`/`uninstall`;
     * DITA-OT 3.x always runs it).
     */
    internal fun envBatLinesFor(script: File, arguments: List<String>): List<String> {
        val subcommand = arguments.firstOrNull()?.takeUnless { it.startsWith("-") }
        if (subcommand != null && skipsEnvBat(readLines(script), subcommand)) {
            return emptyList()
        }
        return readLines(File(ditaHomeOf(script), "config/env.bat"))
    }

    /** Whether dita.bat jumps over `call env.bat` for [subcommand] (`== "install" goto ...`). */
    internal fun skipsEnvBat(ditaBatLines: List<String>, subcommand: String): Boolean {
        val jump = Regex("""==\s*"${Regex.escape(subcommand)}"\s+goto\b""", RegexOption.IGNORE_CASE)
        return ditaBatLines.any { jump.containsMatchIn(it) }
    }

    /**
     * `DITA_HOME` as dita.bat computes it: `%~dp0..` for a script in `bin\`
     * (DITA-OT 3.0+), or the script's own directory for older layouts.
     */
    internal fun ditaHomeAsSeenByScript(script: File): String {
        val dir = script.parentFile.path.trimEnd('\\', '/')
        return if (isInBin(script)) "$dir\\.." else dir
    }

    /**
     * The java executable dita.bat will use: `%JAVACMD%`, else `%JAVA_HOME%\bin\java.exe`,
     * else `java.exe`.
     */
    internal fun javaCommand(getenv: (String) -> String?): String {
        getenv("JAVACMD")?.takeIf { it.isNotBlank() }?.let { return it }
        getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }?.let { return it.trimEnd('\\') + "\\bin\\java.exe" }
        return "java.exe"
    }

    /**
     * The 8.3 short form of [dir]. Only a path that leads to the same directory is accepted:
     * for example, cmd.exe cannot run in a UNC path and silently falls back to `C:\Windows`.
     */
    internal fun shortPathOf(dir: File, logger: Logger): ShortPath {
        val absoluteDir = dir.absoluteFile
        if (absoluteDir.path.startsWith("\\\\")) {
            return ShortPath.Failed("cmd.exe cannot run in a UNC (network) path")
        }

        shortPathCache[absoluteDir.path]?.let { cached ->
            if (isSameDirectory(cached, absoluteDir)) return ShortPath.Found(cached)
            shortPathCache.remove(absoluteDir.path)
        }

        return try {
            // Runs in the directory itself, so the command needs no path and no quoting
            val process = ProcessBuilder("cmd.exe", "/d", "/c", "for %I in (.) do @echo %~fsI")
                .directory(absoluteDir)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()

            if (!process.waitFor(SHORT_PATH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return ShortPath.Failed("cmd.exe did not answer within $SHORT_PATH_TIMEOUT_SECONDS seconds")
            }
            // The output is one short line, far below the pipe buffer size, so the process
            // could not block on it; reading after it exited cannot hang.
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.exitValue() != 0) {
                return ShortPath.Failed("cmd.exe exited with code ${process.exitValue()}: ${output.trim()}")
            }

            val line = output.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() }
                ?: return ShortPath.Failed("cmd.exe printed no path")
            val candidate = File(line)
            when {
                !isSameDirectory(candidate, absoluteDir) ->
                    ShortPath.Failed("cmd.exe reported '$line', which is not ${absoluteDir.path}")
                line.length >= absoluteDir.path.length -> ShortPath.Unavailable
                else -> {
                    shortPathCache[absoluteDir.path] = candidate
                    ShortPath.Found(candidate)
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not determine the short path of ${absoluteDir.path}", e)
            ShortPath.Failed("could not run cmd.exe (${e.javaClass.simpleName}: ${e.message})")
        }
    }

    private fun isSameDirectory(a: File, b: File): Boolean = try {
        // canonicalPath expands 8.3 names, so both forms of one folder compare equal
        a.isDirectory && b.isDirectory && a.canonicalPath.equals(b.canonicalPath, ignoreCase = true)
    } catch (e: IOException) {
        false
    }

    private fun isInBin(script: File): Boolean =
        script.parentFile?.name.equals("bin", ignoreCase = true)

    private fun ditaHomeOf(script: File): File =
        if (isInBin(script)) script.parentFile.parentFile else script.parentFile

    private fun readLines(file: File): List<String> {
        return try {
            if (!file.isFile) return emptyList()
            val key = file.absolutePath
            val lastModified = file.lastModified()
            val length = file.length()
            linesCache[key]?.takeIf { it.lastModified == lastModified && it.length == length }?.let { return it.lines }
            file.readLines().also { linesCache[key] = CachedLines(lastModified, length, it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
