package com.github.jyjeanne

import java.io.File

/**
 * Platform detection utilities.
 *
 * Provides consistent platform detection across all plugin tasks.
 *
 * @since 2.4.0
 */
object Platform {

    /**
     * Whether the current platform is Windows.
     */
    val isWindows: Boolean by lazy {
        System.getProperty("os.name").lowercase().contains("windows")
    }

    /**
     * Whether the current platform is macOS.
     */
    val isMacOS: Boolean by lazy {
        System.getProperty("os.name").lowercase().contains("mac")
    }

    /**
     * Whether the current platform is Linux.
     */
    val isLinux: Boolean by lazy {
        System.getProperty("os.name").lowercase().contains("linux")
    }

    /**
     * Whether the current platform is Unix-like (Linux or macOS).
     */
    val isUnix: Boolean by lazy {
        !isWindows
    }

    /**
     * Get the DITA executable for the given DITA-OT installation.
     *
     * @param ditaHome DITA-OT installation directory
     * @return Path to the DITA executable (dita.bat on Windows, dita on Unix)
     */
    fun getDitaExecutable(ditaHome: File): File {
        return if (isWindows) {
            File(ditaHome, "bin/dita.bat")
        } else {
            File(ditaHome, "bin/dita")
        }
    }

    /**
     * Get the script extension for the current platform.
     *
     * @return ".bat" on Windows, ".sh" on Unix
     */
    val scriptExtension: String
        get() = if (isWindows) ".bat" else ".sh"

    /**
     * Get the executable extension for the current platform.
     *
     * @return ".exe" on Windows, empty string on Unix
     */
    val executableExtension: String
        get() = if (isWindows) ".exe" else ""

    /**
     * Get the path separator for the current platform.
     *
     * @return ";" on Windows, ":" on Unix
     */
    val pathSeparator: String
        get() = File.pathSeparator

    /**
     * Get the line separator for the current platform.
     *
     * @return "\r\n" on Windows, "\n" on Unix
     */
    val lineSeparator: String
        get() = System.lineSeparator()
}
