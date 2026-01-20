package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Gradle task for downloading and extracting DITA Open Toolkit.
 *
 * This task automatically downloads the specified version of DITA-OT
 * from GitHub releases and extracts it to a local directory.
 *
 * **Features:**
 * - Automatic download from GitHub releases
 * - Version management (default: latest stable)
 * - Caching - won't re-download if already present
 * - Checksum verification (SHA-256)
 * - Configurable retries for network failures
 * - Temp-and-move pattern for safe downloads
 * - Progress reporting with quiet mode option
 * - Configurable timeouts
 *
 * **Usage (Groovy DSL):**
 * ```groovy
 * tasks.register('downloadDitaOt', DitaOtDownloadTask) {
 *     version = '4.2.3'
 *     destinationDir = file("$buildDir/dita-ot")
 *     retries = 3
 *     checksum = 'sha256:abc123...'
 * }
 * ```
 *
 * **Usage (Kotlin DSL):**
 * ```kotlin
 * tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
 *     version.set("4.2.3")
 *     destinationDir.set(layout.buildDirectory.dir("dita-ot"))
 *     retries.set(3)
 *     checksum.set("sha256:abc123...")
 * }
 * ```
 *
 * @since 2.4.0
 */
abstract class DitaOtDownloadTask @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    companion object {
        /** Default DITA-OT version to download */
        const val DEFAULT_VERSION = "4.2.3"

        /** GitHub releases URL template */
        const val GITHUB_RELEASE_URL = "https://github.com/dita-ot/dita-ot/releases/download/%s/dita-ot-%s.zip"

        /** Task name constant */
        const val TASK_NAME = "downloadDitaOt"

        /** Default connection timeout in milliseconds */
        const val DEFAULT_CONNECT_TIMEOUT = 30_000

        /** Default read timeout in milliseconds */
        const val DEFAULT_READ_TIMEOUT = 60_000

        /** Buffer size for download */
        private const val BUFFER_SIZE = 8192

        /** Maximum redirects to follow */
        private const val MAX_REDIRECTS = 5

        /** Temporary file suffix */
        private const val TEMP_SUFFIX = ".part"
    }

    // ==================== Properties ====================

    /**
     * DITA-OT version to download.
     * Defaults to [DEFAULT_VERSION].
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * Destination directory for DITA-OT installation.
     * The DITA-OT will be extracted to: destinationDir/dita-ot-{version}
     */
    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    /**
     * Custom download URL (optional).
     * If set, overrides the default GitHub releases URL.
     */
    @get:Input
    @get:org.gradle.api.tasks.Optional
    abstract val downloadUrl: Property<String>

    /**
     * Whether to skip download if DITA-OT already exists.
     * Defaults to true.
     */
    @get:Input
    abstract val skipIfExists: Property<Boolean>

    /**
     * Whether to delete existing installation before extracting.
     * Defaults to false.
     */
    @get:Input
    abstract val forceReinstall: Property<Boolean>

    /**
     * Number of retry attempts for failed downloads.
     * Defaults to 3. Set to 0 to disable retries.
     * Set to -1 for infinite retries.
     */
    @get:Input
    abstract val retries: Property<Int>

    /**
     * Expected checksum in format "algorithm:hash" (e.g., "sha256:abc123...").
     * If set, the downloaded file will be verified against this checksum.
     * Supported algorithms: md5, sha1, sha256, sha512
     */
    @get:Input
    @get:org.gradle.api.tasks.Optional
    abstract val checksum: Property<String>

    /**
     * Connection timeout in milliseconds.
     * Defaults to 30000 (30 seconds).
     */
    @get:Input
    abstract val connectTimeout: Property<Int>

    /**
     * Read timeout in milliseconds.
     * Defaults to 60000 (60 seconds).
     */
    @get:Input
    abstract val readTimeout: Property<Int>

    /**
     * Whether to suppress progress output.
     * Defaults to false.
     */
    @get:Input
    abstract val quiet: Property<Boolean>

    /**
     * Whether to use temp-and-move pattern for downloads.
     * Downloads to a .part file first, then moves on success.
     * Defaults to true.
     */
    @get:Input
    abstract val tempAndMove: Property<Boolean>

    // ==================== Internal Properties ====================

    /**
     * Cache directory for downloaded zip files.
     */
    @get:Internal
    val cacheDir: File
        get() = File(System.getProperty("user.home"), ".dita-ot/cache")

    /**
     * Get the DITA-OT home directory path after extraction.
     */
    @get:Internal
    val ditaOtHome: File
        get() = File(destinationDir.asFile.get(), "dita-ot-${version.get()}")

    // ==================== Initialization ====================

    init {
        group = "DITA-OT Setup"
        description = "Downloads and extracts DITA Open Toolkit"

        // Set defaults
        version.convention(DEFAULT_VERSION)
        destinationDir.convention(projectLayout.buildDirectory.dir("dita-ot"))
        skipIfExists.convention(true)
        forceReinstall.convention(false)
        retries.convention(3)
        connectTimeout.convention(DEFAULT_CONNECT_TIMEOUT)
        readTimeout.convention(DEFAULT_READ_TIMEOUT)
        quiet.convention(false)
        tempAndMove.convention(true)
    }

    // ==================== DSL Methods ====================

    /**
     * Set DITA-OT version (Groovy DSL friendly).
     */
    fun version(v: String) {
        version.set(v)
    }

    /**
     * Set destination directory (Groovy DSL friendly).
     */
    fun destinationDir(dir: Any) {
        when (dir) {
            is File -> destinationDir.set(dir)
            is String -> destinationDir.set(projectLayout.projectDirectory.dir(dir))
            else -> destinationDir.set(projectLayout.projectDirectory.dir(dir.toString()))
        }
    }

    /**
     * Set custom download URL (Groovy DSL friendly).
     */
    fun downloadUrl(url: String) {
        downloadUrl.set(url)
    }

    /**
     * Set number of retries (Groovy DSL friendly).
     */
    fun retries(count: Int) {
        retries.set(count)
    }

    /**
     * Set checksum (Groovy DSL friendly).
     */
    fun checksum(value: String) {
        checksum.set(value)
    }

    // ==================== Task Action ====================

    @TaskAction
    fun download() {
        val ver = version.get()
        val destDir = destinationDir.asFile.get()
        val ditaHome = ditaOtHome

        log("DITA-OT Download Task")
        log("  Version: $ver")
        log("  Destination: ${ditaHome.absolutePath}")

        // Check if already exists
        if (skipIfExists.get() && ditaHome.exists() && !forceReinstall.get()) {
            if (isValidDitaOtInstallation(ditaHome)) {
                log("  Status: DITA-OT $ver already installed (skipping)")
                return
            } else {
                logger.warn("  Existing DITA-OT installation appears invalid, re-downloading...")
            }
        }

        // Delete existing if force reinstall
        if (forceReinstall.get() && ditaHome.exists()) {
            log("  Removing existing installation...")
            ditaHome.deleteRecursively()
        }

        // Ensure cache directory exists
        cacheDir.mkdirs()

        // Download zip file
        val zipFile = File(cacheDir, "dita-ot-$ver.zip")
        if (!zipFile.exists() || forceReinstall.get()) {
            val url = downloadUrl.orNull ?: String.format(GITHUB_RELEASE_URL, ver, ver)
            log("  Downloading from: $url")
            downloadFileWithRetry(url, zipFile)
            log("  Downloaded: ${formatFileSize(zipFile.length())}")

            // Verify checksum if provided
            checksum.orNull?.let { expectedChecksum ->
                log("  Verifying checksum...")
                verifyChecksum(zipFile, expectedChecksum)
                log("  Checksum verified ✓")
            }
        } else {
            log("  Using cached download: ${zipFile.absolutePath}")

            // Verify cached file checksum if provided
            checksum.orNull?.let { expectedChecksum ->
                log("  Verifying cached file checksum...")
                verifyChecksum(zipFile, expectedChecksum)
                log("  Checksum verified ✓")
            }
        }

        // Ensure destination directory exists
        destDir.mkdirs()

        // Extract zip file
        log("  Extracting to: ${destDir.absolutePath}")
        extractZip(zipFile, destDir)

        // Validate installation
        if (!isValidDitaOtInstallation(ditaHome)) {
            throw GradleException(
                """
                DITA-OT extraction failed or produced invalid installation.

                Expected directory: ${ditaHome.absolutePath}
                Expected file: ${File(ditaHome, "build.xml").absolutePath}

                Please check:
                1. The downloaded ZIP file is not corrupted
                2. You have write permissions to the destination directory
                3. The DITA-OT version exists: https://github.com/dita-ot/dita-ot/releases
                """.trimIndent()
            )
        }

        // Set executable permissions on Unix
        setExecutablePermissions(ditaHome)

        // Detect and report version
        val detectedVersion = detectInstalledVersion(ditaHome)
        log("")
        log("═══════════════════════════════════════════════════════")
        log("DITA-OT Download Complete")
        log("═══════════════════════════════════════════════════════")
        log("Version:     $detectedVersion")
        log("Location:    ${ditaHome.absolutePath}")
        log("Executable:  ${getDitaExecutable(ditaHome).absolutePath}")
        log("═══════════════════════════════════════════════════════")
    }

    // ==================== Helper Methods ====================

    /**
     * Log message unless quiet mode is enabled.
     */
    private fun log(message: String) {
        if (!quiet.get()) {
            logger.lifecycle(message)
        }
    }

    /**
     * Download file with retry support.
     */
    private fun downloadFileWithRetry(urlString: String, destFile: File) {
        val maxRetries = retries.get()
        var lastException: Exception? = null
        var attempt = 0

        while (maxRetries < 0 || attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    // Exponential backoff: 1s, 2s, 4s, 8s, max 10s
                    val delay = minOf(1000L * (1L shl (attempt - 1).coerceAtMost(4)), 10000L)
                    log("    Retry attempt $attempt after ${delay}ms...")
                    Thread.sleep(delay)
                }

                downloadFile(urlString, destFile)
                return // Success
            } catch (e: Exception) {
                lastException = e
                attempt++

                if (maxRetries >= 0 && attempt > maxRetries) {
                    break
                }

                logger.warn("    Download failed: ${e.message}")
            }
        }

        throw GradleException(
            "Failed to download after ${if (maxRetries < 0) "multiple" else maxRetries.toString()} retries: ${lastException?.message}",
            lastException
        )
    }

    /**
     * Download a file from URL to local file.
     */
    private fun downloadFile(urlString: String, destFile: File) {
        val connections = mutableListOf<HttpURLConnection>()
        val useTempFile = tempAndMove.get()
        val targetFile = if (useTempFile) File(destFile.parent, destFile.name + TEMP_SUFFIX) else destFile

        try {
            val url = URL(urlString)
            var currentConnection = url.openConnection() as HttpURLConnection
            connections.add(currentConnection)
            currentConnection.connectTimeout = connectTimeout.get()
            currentConnection.readTimeout = readTimeout.get()
            currentConnection.instanceFollowRedirects = true
            currentConnection.setRequestProperty("Accept-Encoding", "gzip")
            currentConnection.setRequestProperty("User-Agent", "DITA-OT-Gradle-Plugin/${version.get()}")

            // Handle redirects manually for GitHub releases
            var redirectCount = 0
            while (currentConnection.responseCode in 300..399 && redirectCount < MAX_REDIRECTS) {
                val redirectUrl = currentConnection.getHeaderField("Location")
                    ?: throw GradleException(
                        """
                        HTTP redirect without Location header.
                        URL: $urlString
                        Response code: ${currentConnection.responseCode}
                        """.trimIndent()
                    )

                currentConnection = URL(redirectUrl).openConnection() as HttpURLConnection
                connections.add(currentConnection)
                currentConnection.connectTimeout = connectTimeout.get()
                currentConnection.readTimeout = readTimeout.get()
                currentConnection.setRequestProperty("Accept-Encoding", "gzip")
                currentConnection.setRequestProperty("User-Agent", "DITA-OT-Gradle-Plugin/${version.get()}")
                redirectCount++
            }

            if (currentConnection.responseCode != HttpURLConnection.HTTP_OK) {
                throw GradleException(
                    """
                    Failed to download DITA-OT: HTTP ${currentConnection.responseCode}
                    URL: $urlString

                    Possible causes:
                    1. DITA-OT version ${version.get()} does not exist
                    2. Network connectivity issues
                    3. GitHub is temporarily unavailable

                    Available versions: https://github.com/dita-ot/dita-ot/releases
                    """.trimIndent()
                )
            }

            val totalSize = currentConnection.contentLengthLong
            var downloadedSize = 0L
            var lastReportedPercent = -1

            currentConnection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead

                        // Report progress every 10%
                        if (!quiet.get() && totalSize > 0) {
                            val percent = ((downloadedSize * 100) / totalSize).toInt()
                            if (percent / 10 > lastReportedPercent / 10) {
                                logger.lifecycle("    Progress: $percent%")
                                lastReportedPercent = percent
                            }
                        }
                    }
                }
            }

            // Move temp file to final destination
            if (useTempFile) {
                if (destFile.exists()) {
                    destFile.delete()
                }
                if (!targetFile.renameTo(destFile)) {
                    // Fallback: copy and delete
                    targetFile.copyTo(destFile, overwrite = true)
                    targetFile.delete()
                }
            }
        } catch (e: GradleException) {
            // Clean up partial download (only the temp file, not existing cached file)
            targetFile.delete()
            throw e
        } catch (e: Exception) {
            // Clean up partial download (only the temp file, not existing cached file)
            targetFile.delete()
            throw GradleException("Failed to download DITA-OT: ${e.message}", e)
        } finally {
            // Close all connections (in reverse order)
            connections.asReversed().forEach { conn ->
                try {
                    conn.disconnect()
                } catch (e: Exception) {
                    logger.debug("Error closing connection: ${e.message}")
                }
            }
        }
    }

    /**
     * Verify file checksum.
     *
     * @param file File to verify
     * @param expectedChecksum Expected checksum in format "algorithm:hash"
     */
    private fun verifyChecksum(file: File, expectedChecksum: String) {
        val parts = expectedChecksum.split(":", limit = 2)
        if (parts.size != 2) {
            throw GradleException(
                """
                Invalid checksum format: $expectedChecksum
                Expected format: algorithm:hash (e.g., sha256:abc123...)
                Supported algorithms: md5, sha1, sha256, sha512
                """.trimIndent()
            )
        }

        val algorithm = parts[0].uppercase()
        val expectedHash = parts[1].lowercase()

        val algorithmName = when (algorithm) {
            "MD5" -> "MD5"
            "SHA1", "SHA-1" -> "SHA-1"
            "SHA256", "SHA-256" -> "SHA-256"
            "SHA512", "SHA-512" -> "SHA-512"
            else -> throw GradleException("Unsupported checksum algorithm: $algorithm. Supported: md5, sha1, sha256, sha512")
        }

        val digest = MessageDigest.getInstance(algorithmName)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }

        if (actualHash != expectedHash) {
            // Delete the corrupted file
            file.delete()
            throw GradleException(
                """
                Checksum verification failed for: ${file.name}

                Expected ($algorithm): $expectedHash
                Actual ($algorithm):   $actualHash

                The downloaded file may be corrupted or tampered with.
                The file has been deleted. Please try downloading again.
                """.trimIndent()
            )
        }
    }

    /**
     * Extract a ZIP file to destination directory.
     */
    private fun extractZip(zipFile: File, destDir: File) {
        try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(destDir, entry.name)

                    // Security check: prevent zip slip vulnerability
                    if (!destFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        throw GradleException("ZIP entry is outside of target dir: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            throw GradleException("Failed to extract DITA-OT: ${e.message}", e)
        }
    }

    /**
     * Check if directory contains a valid DITA-OT installation.
     */
    private fun isValidDitaOtInstallation(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val buildXml = File(dir, "build.xml")
        val binDir = File(dir, "bin")
        return buildXml.exists() && binDir.exists()
    }

    /**
     * Detect DITA-OT version from installation.
     */
    private fun detectInstalledVersion(ditaHome: File): String {
        val versionFile = File(ditaHome, "VERSION")
        return if (versionFile.exists()) {
            versionFile.readText().trim()
        } else {
            version.get()
        }
    }

    /**
     * Get DITA executable path.
     */
    private fun getDitaExecutable(ditaHome: File): File {
        return Platform.getDitaExecutable(ditaHome)
    }

    /**
     * Set executable permissions on Unix systems.
     */
    private fun setExecutablePermissions(ditaHome: File) {
        if (Platform.isUnix) {
            val ditaScript = File(ditaHome, "bin/dita")
            if (ditaScript.exists()) {
                ditaScript.setExecutable(true)
                logger.debug("Set executable permission on: ${ditaScript.absolutePath}")
            }
        }
    }

    /**
     * Format file size for display.
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
