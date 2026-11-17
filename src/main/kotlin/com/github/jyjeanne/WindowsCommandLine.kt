package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps DITA-OT's `dita.bat` under the cmd.exe command-line length limit on Windows.
 *
 * `dita.bat` sets `DITA_HOME=%~dp0..`, and `config\env.bat` appends one absolute path per
 * plugin JAR to `CLASSPATH`. The final `java` command line therefore grows with
 * (number of JARs x length of the DITA-OT path). DITA-OT 4.x adds about 47 JARs, so under a
 * directory of roughly 100 characters the line passes cmd.exe's 8191-character limit and
 * `dita.bat` fails with exit code 255 ("The input line is too long").
 *
 * `%~dp0` is the path the script was *invoked* with, so running `dita.bat` through a shorter
 * path to the same folder shortens every CLASSPATH entry. Two shorter paths are tried, only
 * when the estimated command line would exceed the limit (builds that fit run as before):
 *
 * 1. The folder's 8.3 short path (for example `C:\Users\me\DOCUME~1\...`). No side effects,
 *    but 8.3 names can be disabled per volume (common on non-system drives, never on ReFS
 *    or Dev Drives).
 * 2. A directory junction under the temp folder (`%TEMP%\dotj-1a2b3c4d`) pointing to the
 *    DITA-OT folder. Junctions need no administrator rights and work on any local NTFS
 *    volume. Each run gets its own junction, removed when DITA-OT exits; leftovers from
 *    killed builds are removed after a day.
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

    /** Name prefix of the junctions this plugin creates in the temp folder. */
    internal const val JUNCTION_PREFIX = "dotj-"

    /** Junctions older than this are leftovers from killed builds. */
    internal val STALE_JUNCTION_AGE_MILLIS = TimeUnit.DAYS.toMillis(1)

    private const val PROCESS_TIMEOUT_SECONDS = 10L
    private const val FILE_ATTRIBUTE_REPARSE_POINT = 0x400

    private val CLASSPATH_LINE =
        Regex("""^\s*set\s+"CLASSPATH=%CLASSPATH%(.*)"\s*$""", RegexOption.IGNORE_CASE)

    private const val DITA_HOME_TOKEN = "%DITA_HOME%"

    /**
     * The dita.bat to run. [close] it after DITA-OT has exited: it removes the junction
     * created for this run, if any.
     */
    class DitaScript internal constructor(val file: File, private val cleanup: () -> Unit = {}) : Closeable {
        /** Absolute path to put on the command line. */
        val path: String get() = file.absolutePath

        private val closed = AtomicBoolean(false)

        override fun close() {
            if (closed.compareAndSet(false, true)) cleanup()
        }
    }

    /** Outcome of looking up the 8.3 short form of a directory. */
    internal sealed class ShortPath {
        /** A shorter path to the same directory. */
        data class Found(val dir: File) : ShortPath()

        /** The directory has no shorter 8.3 form (8.3 names were disabled when its folders were created). */
        object Unavailable : ShortPath()

        /** The lookup itself failed; [reason] says why. */
        data class Failed(val reason: String) : ShortPath()
    }

    /** Outcome of creating a junction. */
    internal sealed class Junction {
        data class Created(val link: File) : Junction()
        data class Failed(val reason: String) : Junction()
    }

    /** Short paths found in this JVM. Re-checked before reuse: folders can be deleted and re-created. */
    private val shortPathCache = ConcurrentHashMap<String, File>()

    private data class CachedLines(val lastModified: Long, val length: Long, val lines: List<String>)

    /** dita.bat / env.bat contents, keyed by path and invalidated when the file changes. */
    private val linesCache = ConcurrentHashMap<String, CachedLines>()

    private val staleJunctionsSwept = AtomicBoolean(false)

    /**
     * Returns the script to execute: [script] itself (made absolute), or the same file
     * addressed through a shorter path when the long path would push dita.bat's command line
     * over the limit. Close the result after the DITA-OT process has exited.
     *
     * On non-Windows platforms, or for anything other than a `.bat` file, [script] is
     * returned unchanged.
     *
     * @param script the `dita.bat` file
     * @param arguments the arguments passed after the script (subcommand first, if any)
     * @param useShortNames try 8.3 short names before junctions (tests turn this off)
     * @param junctionBaseDir where junctions are created (the temp folder by default)
     */
    fun resolveDitaScript(
        script: File,
        arguments: List<String>,
        logger: Logger,
        useShortNames: Boolean = true,
        junctionBaseDir: File = File(System.getProperty("java.io.tmpdir"))
    ): DitaScript {
        val absoluteScript = script.absoluteFile
        if (!Platform.isWindows || !absoluteScript.name.endsWith(".bat", ignoreCase = true)) {
            return DitaScript(absoluteScript)
        }

        val envBat = envBatLinesFor(absoluteScript, arguments)
        val estimate = estimate(absoluteScript, arguments, envBat)
        if (estimate <= CMD_EXE_MAX_LENGTH) {
            logger.debug("dita.bat command line estimate: $estimate characters (limit $CMD_EXE_MAX_LENGTH)")
            return DitaScript(absoluteScript)
        }

        val ditaHome = ditaHomeOf(absoluteScript)
        val problems = mutableListOf<String>()
        var fallback = absoluteScript
        var fallbackEstimate = estimate

        // 1. The 8.3 short path: no side effects
        if (useShortNames) {
            when (val shortPath = shortPathOf(absoluteScript.parentFile, logger)) {
                is ShortPath.Found -> {
                    val shortScript = File(shortPath.dir, absoluteScript.name)
                    val shortEstimate = estimate(shortScript, arguments, envBat)
                    when {
                        !shortScript.isFile -> problems += "dita.bat was not found through the 8.3 short path ${shortPath.dir}"
                        shortEstimate <= CMD_EXE_MAX_LENGTH -> {
                            logSwitch(logger, estimate, shortEstimate, "its 8.3 short path", shortScript)
                            return DitaScript(shortScript)
                        }
                        else -> {
                            problems += "the 8.3 short path is still too long ($shortEstimate characters)"
                            if (shortEstimate < fallbackEstimate) {
                                fallback = shortScript
                                fallbackEstimate = shortEstimate
                            }
                        }
                    }
                }
                ShortPath.Unavailable -> problems += "the folder has no 8.3 short name (8.3 names are disabled " +
                    "on the volume, or were when its folders were created)"
                is ShortPath.Failed -> problems += "the 8.3 short path could not be determined: ${shortPath.reason}"
            }
        }

        // 2. A junction under the temp folder, removed when the caller closes the result
        when (val junction = createJunction(ditaHome, junctionBaseDir, logger)) {
            is Junction.Created -> {
                val junctionScript = File(junction.link, absoluteScript.relativeTo(ditaHome).path)
                val junctionEstimate = estimate(junctionScript, arguments, envBat)
                if (junctionScript.isFile && junctionEstimate <= CMD_EXE_MAX_LENGTH) {
                    logSwitch(logger, estimate, junctionEstimate, "a junction", junctionScript)
                    return DitaScript(junctionScript) { removeJunction(junction.link, logger) }
                }
                removeJunction(junction.link, logger)
                problems += if (junctionScript.isFile) {
                    "a junction under ${junctionBaseDir.path} is still too long ($junctionEstimate characters)"
                } else {
                    "dita.bat was not found through the junction ${junction.link}"
                }
            }
            is Junction.Failed -> problems += "no junction could be created: ${junction.reason}"
        }

        logger.warn(Messages.windowsCommandLineTooLong(fallbackEstimate, CMD_EXE_MAX_LENGTH, ditaHome.path, problems))
        return DitaScript(fallback)
    }

    private fun logSwitch(logger: Logger, estimate: Int, newEstimate: Int, via: String, newScript: File) {
        logger.info(
            "dita.bat command line would be about $estimate characters (cmd.exe limit: " +
                "$CMD_EXE_MAX_LENGTH); running it through $via instead ($newEstimate characters): ${newScript.path}"
        )
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

    // ==================== 8.3 short paths ====================

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
            val run = runCmd(listOf("for %I in (.) do @echo %~fsI"), absoluteDir)
                ?: return ShortPath.Failed("cmd.exe did not answer within $PROCESS_TIMEOUT_SECONDS seconds")
            if (run.exitCode != 0) {
                return ShortPath.Failed("cmd.exe exited with code ${run.exitCode}: ${run.output.trim()}")
            }

            val line = run.output.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() }
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

    // ==================== Junctions ====================

    /**
     * Create a uniquely named junction in [baseDir] that points to [target].
     * Junctions need no administrator rights but only work for local NTFS paths.
     */
    internal fun createJunction(target: File, baseDir: File, logger: Logger): Junction {
        val absoluteTarget = target.absoluteFile
        if (absoluteTarget.path.startsWith("\\\\")) {
            return Junction.Failed("junctions cannot point to UNC (network) paths")
        }
        if (staleJunctionsSwept.compareAndSet(false, true)) {
            sweepStaleJunctions(baseDir, System.currentTimeMillis(), logger)
        }

        val link = File(baseDir.absoluteFile, JUNCTION_PREFIX + UUID.randomUUID().toString().substring(0, 8))
        return try {
            val run = runCmd(listOf("mklink", "/J", quote(link.path), quote(absoluteTarget.path)), null)
            if (run == null || run.exitCode != 0 || !isSameDirectory(link, absoluteTarget)) {
                removeJunction(link, logger)
                val detail = run?.let { "exit code ${it.exitCode}: ${it.output.trim()}" }
                    ?: "no answer within $PROCESS_TIMEOUT_SECONDS seconds"
                return Junction.Failed("mklink /J failed ($detail)")
            }
            logger.debug("Created junction ${link.path} -> ${absoluteTarget.path}")
            Junction.Created(link)
        } catch (e: Exception) {
            logger.debug("Could not create a junction to ${absoluteTarget.path}", e)
            removeJunction(link, logger)
            Junction.Failed("could not run mklink (${e.javaClass.simpleName}: ${e.message})")
        }
    }

    /**
     * Remove a junction created by [createJunction]. Only the link itself is deleted
     * (`Files.delete` on a junction calls RemoveDirectory on the link); the DITA-OT files
     * it points to are never touched. Anything that is not a junction is left alone.
     */
    internal fun removeJunction(link: File, logger: Logger) {
        try {
            val path = link.toPath()
            if (isJunction(path)) {
                Files.delete(path)
            }
        } catch (e: Exception) {
            logger.debug("Could not remove junction ${link.path}: ${e.message}")
        }
    }

    /** Remove junctions left in [baseDir] by builds that were killed before cleaning up. */
    internal fun sweepStaleJunctions(baseDir: File, now: Long, logger: Logger) {
        val candidates = baseDir.listFiles { file -> file.name.startsWith(JUNCTION_PREFIX) } ?: return
        for (candidate in candidates) {
            try {
                val path = candidate.toPath()
                if (!isJunction(path)) continue
                val modified = Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
                    .lastModifiedTime().toMillis()
                if (now - modified > STALE_JUNCTION_AGE_MILLIS) {
                    removeJunction(candidate, logger)
                }
            } catch (e: Exception) {
                logger.debug("Could not check ${candidate.path}: ${e.message}")
            }
        }
    }

    /** A directory reparse point (junction), checked without following it. */
    internal fun isJunction(path: Path): Boolean = try {
        val dosAttributes = Files.getAttribute(path, "dos:attributes", LinkOption.NOFOLLOW_LINKS) as Int
        (dosAttributes and FILE_ATTRIBUTE_REPARSE_POINT) != 0 && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
    } catch (e: Exception) {
        false
    }

    // ==================== Helpers ====================

    private class CmdResult(val exitCode: Int, val output: String)

    /** Run `cmd.exe /d /c <command>`; `null` on timeout. Output is expected to be a few lines. */
    private fun runCmd(command: List<String>, workingDir: File?): CmdResult? {
        val process = ProcessBuilder(listOf("cmd.exe", "/d", "/c") + command)
            .apply { if (workingDir != null) directory(workingDir) }
            .redirectErrorStream(true)
            .start()
        process.outputStream.close()
        if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        // A few short lines, far below the pipe buffer size: the process could not block on
        // its output, so reading it after the exit cannot hang.
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return CmdResult(process.exitValue(), output)
    }

    private fun quote(path: String) = "\"$path\""

    /** Same folder, comparing real paths (resolves 8.3 names and junctions). */
    private fun isSameDirectory(a: File, b: File): Boolean = try {
        a.isDirectory && b.isDirectory && a.toPath().toRealPath() == b.toPath().toRealPath()
    } catch (e: Exception) {
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
