package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.regex.Pattern
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Gradle task for checking links in DITA content.
 *
 * This task scans DITA maps and topics for broken links including:
 * - Internal file references (xref, conref, topicref)
 * - Image references
 * - External URLs (optional)
 *
 * **Usage (Groovy DSL):**
 * ```groovy
 * tasks.register('checkLinks', DitaLinkCheckTask) {
 *     input 'docs/guide.ditamap'
 *     checkExternal = true   // Also check external URLs
 *     failOnBroken = true    // Fail build on broken links
 * }
 * ```
 *
 * **Usage (Kotlin DSL):**
 * ```kotlin
 * tasks.register<DitaLinkCheckTask>("checkLinks") {
 *     input("docs/guide.ditamap")
 *     checkExternal.set(true)   // Also check external URLs
 *     failOnBroken.set(true)    // Fail build on broken links
 * }
 * ```
 *
 * **Link Types Checked:**
 * - `xref` - Cross-references to other topics or elements
 * - `conref` - Content references
 * - `topicref` - Topic references in maps
 * - `image` - Image file references
 * - `href` attributes on various elements
 *
 * @since 2.7.0
 */
abstract class DitaLinkCheckTask @Inject constructor(
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    companion object {
        /** Task name constant */
        const val TASK_NAME = "checkLinks"

        /** Default connection timeout for external URL checks (5 seconds) */
        private const val DEFAULT_CONNECT_TIMEOUT = 5000

        /** Default read timeout for external URL checks (10 seconds) */
        private const val DEFAULT_READ_TIMEOUT = 10000

        /** DITA file extensions */
        private val DITA_EXTENSIONS = setOf("dita", "ditamap", "xml")

        /** Pattern to extract fragment identifier from href */
        private val FRAGMENT_PATTERN = Pattern.compile("#(.+)$")

        /** Pattern to detect external URLs */
        private val EXTERNAL_URL_PATTERN = Pattern.compile("^(https?|ftp)://", Pattern.CASE_INSENSITIVE)

        /** Elements with href attribute to check */
        private val HREF_ELEMENTS = setOf(
            "xref", "link", "topicref", "mapref", "glossref",
            "chapter", "appendix", "part", "preface", "notices",
            "anchorref", "keydef", "data"
        )

        /** Elements with conref attribute to check */
        private val CONREF_ELEMENTS = setOf(
            "ph", "p", "li", "step", "note", "section", "table",
            "fig", "codeblock", "pre", "simpletable", "ul", "ol",
            "sl", "dl", "title", "shortdesc", "abstract", "body",
            "topic", "concept", "task", "reference"
        )
    }

    // ==================== Properties ====================

    /**
     * Input files (DITA maps or topics) to check.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * Whether to check external URLs.
     * Defaults to false (only check internal links).
     */
    @get:Input
    abstract val checkExternal: Property<Boolean>

    /**
     * Whether to fail the build on broken links.
     * Defaults to true.
     */
    @get:Input
    abstract val failOnBroken: Property<Boolean>

    /**
     * Whether to follow topic references recursively.
     * Defaults to true.
     */
    @get:Input
    abstract val recursive: Property<Boolean>

    /**
     * Whether to suppress detailed output.
     * Defaults to false.
     */
    @get:Input
    abstract val quiet: Property<Boolean>

    /**
     * Connection timeout for external URL checks (milliseconds).
     * Defaults to 5000 (5 seconds).
     */
    @get:Input
    abstract val connectTimeout: Property<Int>

    /**
     * Read timeout for external URL checks (milliseconds).
     * Defaults to 10000 (10 seconds).
     */
    @get:Input
    abstract val readTimeout: Property<Int>

    /**
     * URL patterns to exclude from external checking.
     * Useful for excluding localhost, internal domains, etc.
     */
    @get:Input
    @get:Optional
    abstract val excludeUrlPatterns: ListProperty<String>

    // ==================== Internal State ====================

    /** All files that have been scanned */
    @get:Internal
    protected val scannedFiles = mutableSetOf<File>()

    /** All links that have been found */
    @get:Internal
    protected val allLinks = mutableListOf<LinkInfo>()

    /** Broken links found */
    @get:Internal
    protected val brokenLinks = mutableListOf<BrokenLink>()

    /** Valid links count */
    @get:Internal
    protected var validInternalLinks = 0

    /** Valid external links count */
    @get:Internal
    protected var validExternalLinks = 0

    /** Skipped external links count */
    @get:Internal
    protected var skippedExternalLinks = 0

    // ==================== Initialization ====================

    init {
        group = "Verification"
        description = "Checks for broken links in DITA content"

        // Set defaults
        checkExternal.convention(false)
        failOnBroken.convention(true)
        recursive.convention(true)
        quiet.convention(false)
        connectTimeout.convention(DEFAULT_CONNECT_TIMEOUT)
        readTimeout.convention(DEFAULT_READ_TIMEOUT)
        excludeUrlPatterns.convention(listOf())
    }

    // ==================== DSL Methods ====================

    /**
     * Add input file to check (Groovy DSL friendly).
     */
    fun input(i: Any) {
        inputFiles.from(i)
    }

    /**
     * Add URL pattern to exclude from external checking.
     */
    fun excludeUrl(pattern: String) {
        excludeUrlPatterns.add(pattern)
    }

    // ==================== Task Action ====================

    @TaskAction
    fun checkLinks() {
        // Reset state
        scannedFiles.clear()
        allLinks.clear()
        brokenLinks.clear()
        validInternalLinks = 0
        validExternalLinks = 0
        skippedExternalLinks = 0

        val inputs = inputFiles.files
        val isQuiet = quiet.get()
        val shouldCheckExternal = checkExternal.get()

        if (!isQuiet) {
            logger.lifecycle("DITA Link Checker")
            logger.lifecycle("  Files to check: ${inputs.size}")
            logger.lifecycle("  Check external URLs: $shouldCheckExternal")
            logger.lifecycle("  Recursive: ${recursive.get()}")
        }

        if (inputs.isEmpty()) {
            if (!isQuiet) {
                logger.lifecycle("  No input files specified - nothing to check")
            }
            return
        }

        // Scan all input files
        inputs.forEach { inputFile ->
            if (!isQuiet) {
                logger.lifecycle("")
                logger.lifecycle("Scanning: ${inputFile.name}")
            }
            scanFile(inputFile)
        }

        // Check all collected links
        if (!isQuiet) {
            logger.lifecycle("")
            logger.lifecycle("Checking ${allLinks.size} links...")
        }

        checkAllLinks(shouldCheckExternal)

        // Print results
        printResults()

        // Fail if requested and broken links found
        if (failOnBroken.get() && brokenLinks.isNotEmpty()) {
            throw GradleException(buildErrorMessage())
        }
    }

    // ==================== Scanning Methods ====================

    /**
     * Scan a DITA file for links.
     */
    private fun scanFile(file: File) {
        if (!file.exists()) {
            logger.warn("  File not found: ${file.absolutePath}")
            return
        }

        if (file in scannedFiles) {
            return // Already scanned
        }
        scannedFiles.add(file)

        val extension = file.extension.lowercase()
        if (extension !in DITA_EXTENSIONS) {
            return
        }

        try {
            val doc = parseXmlFile(file)
            val root = doc.documentElement

            // Scan for different link types
            scanHrefElements(file, root)
            scanConrefElements(file, root)
            scanImageElements(file, root)

            // If recursive, follow topicref elements
            if (recursive.get()) {
                followTopicRefs(file, root)
            }
        } catch (e: Exception) {
            logger.warn("  Failed to parse ${file.name}: ${e.message}")
        }
    }

    /**
     * Parse an XML file into a DOM document.
     */
    private fun parseXmlFile(file: File): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    /**
     * Scan for elements with href attribute.
     */
    private fun scanHrefElements(sourceFile: File, root: Element) {
        HREF_ELEMENTS.forEach { elementName ->
            val elements = root.getElementsByTagName(elementName)
            for (i in 0 until elements.length) {
                val element = elements.item(i) as Element
                val href = element.getAttribute("href")
                if (href.isNotBlank()) {
                    // Check if scope="external" marks this as an external link
                    val scope = element.getAttribute("scope")
                    val isExternalScope = scope.equals("external", ignoreCase = true)

                    allLinks.add(LinkInfo(
                        sourceFile = sourceFile,
                        target = href,
                        type = LinkType.HREF,
                        elementName = elementName,
                        lineNumber = null, // XML parser doesn't provide line numbers by default
                        isExternalScope = isExternalScope
                    ))
                }
            }
        }
    }

    /**
     * Scan for elements with conref, conkeyref, and keyref attributes.
     */
    private fun scanConrefElements(sourceFile: File, root: Element) {
        // Scan all elements for conref attribute
        scanElementsForAttribute(sourceFile, root, "conref", LinkType.CONREF)
        scanElementsForAttribute(sourceFile, root, "conkeyref", LinkType.CONKEYREF)
        scanElementsForAttribute(sourceFile, root, "keyref", LinkType.KEYREF)
    }

    /**
     * Scan all elements for a specific attribute.
     */
    private fun scanElementsForAttribute(sourceFile: File, element: Element, attrName: String, linkType: LinkType) {
        if (element.hasAttribute(attrName)) {
            val value = element.getAttribute(attrName)
            if (value.isNotBlank()) {
                allLinks.add(LinkInfo(
                    sourceFile = sourceFile,
                    target = value,
                    type = linkType,
                    elementName = element.tagName,
                    lineNumber = null
                ))
            }
        }

        // Recurse into child elements
        val children = element.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element) {
                scanElementsForAttribute(sourceFile, child, attrName, linkType)
            }
        }
    }

    /**
     * Scan for image elements.
     */
    private fun scanImageElements(sourceFile: File, root: Element) {
        val imageElements = root.getElementsByTagName("image")
        for (i in 0 until imageElements.length) {
            val element = imageElements.item(i) as Element
            val href = element.getAttribute("href")
            if (href.isNotBlank()) {
                allLinks.add(LinkInfo(
                    sourceFile = sourceFile,
                    target = href,
                    type = LinkType.IMAGE,
                    elementName = "image",
                    lineNumber = null
                ))
            }
        }
    }

    /**
     * Follow topicref elements to scan referenced files.
     */
    private fun followTopicRefs(sourceFile: File, root: Element) {
        val topicrefs = mutableListOf<Element>()

        // Collect all topicref-like elements
        listOf("topicref", "chapter", "appendix", "part", "mapref", "keydef").forEach { tagName ->
            val elements = root.getElementsByTagName(tagName)
            for (i in 0 until elements.length) {
                topicrefs.add(elements.item(i) as Element)
            }
        }

        topicrefs.forEach { element ->
            val href = element.getAttribute("href")
            if (href.isNotBlank() && !isExternalUrl(href)) {
                val targetFile = resolveFileReference(sourceFile, href)
                if (targetFile.exists()) {
                    scanFile(targetFile)
                }
            }
        }
    }

    // ==================== Link Checking Methods ====================

    /**
     * Check all collected links.
     */
    private fun checkAllLinks(checkExternal: Boolean) {
        allLinks.forEach { link ->
            // Determine if this is an external link (URL pattern or scope="external")
            val isExternal = isExternalUrl(link.target) || link.isExternalScope

            when {
                isExternal -> {
                    if (checkExternal && !isExcludedUrl(link.target)) {
                        // Only check URLs, not scope="external" without URL
                        if (isExternalUrl(link.target)) {
                            checkExternalLink(link)
                        } else {
                            // scope="external" but not a URL - skip HTTP check
                            skippedExternalLinks++
                            logger.debug("  Skipping external scope (not URL): ${link.target}")
                        }
                    } else {
                        skippedExternalLinks++
                    }
                }
                link.type == LinkType.CONKEYREF -> {
                    // Skip conkeyref - requires key resolution which needs DITA-OT
                    logger.debug("  Skipping conkeyref: ${link.target}")
                }
                link.type == LinkType.KEYREF -> {
                    // Skip keyref - requires key resolution which needs DITA-OT
                    logger.debug("  Skipping keyref: ${link.target}")
                }
                else -> {
                    checkInternalLink(link)
                }
            }
        }
    }

    /**
     * Check an internal file link.
     */
    private fun checkInternalLink(link: LinkInfo) {
        val targetPath = link.target

        // Handle fragment-only references (#element-id)
        if (targetPath.startsWith("#")) {
            // Fragment-only reference - refers to same file
            // We would need to parse the file to verify the ID exists
            // For now, just mark as valid
            validInternalLinks++
            return
        }

        // Remove fragment from path for file check
        val pathWithoutFragment = FRAGMENT_PATTERN.matcher(targetPath).replaceAll("")

        if (pathWithoutFragment.isBlank()) {
            validInternalLinks++
            return
        }

        val targetFile = resolveFileReference(link.sourceFile, pathWithoutFragment)

        if (targetFile.exists()) {
            validInternalLinks++
            if (!quiet.get()) {
                logger.debug("  ✓ ${link.type}: ${link.target}")
            }
        } else {
            brokenLinks.add(BrokenLink(
                link = link,
                reason = "File not found: ${targetFile.absolutePath}"
            ))
            if (!quiet.get()) {
                logger.warn("  ✗ ${link.type}: ${link.target} (file not found)")
            }
        }
    }

    /**
     * Check an external URL.
     */
    private fun checkExternalLink(link: LinkInfo) {
        try {
            val url = URI(link.target).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = connectTimeout.get()
            connection.readTimeout = readTimeout.get()
            connection.instanceFollowRedirects = true

            // Set a user agent to avoid being blocked
            connection.setRequestProperty("User-Agent", "DITA-OT-Gradle-LinkChecker/2.7.0")

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode in 200..399) {
                validExternalLinks++
                if (!quiet.get()) {
                    logger.debug("  ✓ URL: ${link.target} ($responseCode)")
                }
            } else {
                brokenLinks.add(BrokenLink(
                    link = link,
                    reason = "HTTP $responseCode"
                ))
                if (!quiet.get()) {
                    logger.warn("  ✗ URL: ${link.target} (HTTP $responseCode)")
                }
            }
        } catch (e: Exception) {
            brokenLinks.add(BrokenLink(
                link = link,
                reason = e.message ?: "Connection failed"
            ))
            if (!quiet.get()) {
                logger.warn("  ✗ URL: ${link.target} (${e.message})")
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if a target is an external URL.
     */
    private fun isExternalUrl(target: String): Boolean {
        return EXTERNAL_URL_PATTERN.matcher(target).find()
    }

    /**
     * Check if a URL should be excluded from checking.
     */
    private fun isExcludedUrl(url: String): Boolean {
        val patterns = excludeUrlPatterns.getOrElse(emptyList())
        return patterns.any { pattern ->
            url.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Resolve a file reference relative to the source file.
     */
    private fun resolveFileReference(sourceFile: File, reference: String): File {
        // Handle absolute paths
        if (File(reference).isAbsolute) {
            return File(reference)
        }

        // Resolve relative to source file's directory
        return File(sourceFile.parentFile, reference).canonicalFile
    }

    // ==================== Output Methods ====================

    /**
     * Print link check results.
     */
    private fun printResults() {
        if (quiet.get() && brokenLinks.isEmpty()) {
            return
        }

        val totalInternal = validInternalLinks + brokenLinks.count { !isExternalUrl(it.link.target) }
        val totalExternal = validExternalLinks + brokenLinks.count { isExternalUrl(it.link.target) }
        val brokenInternal = brokenLinks.count { !isExternalUrl(it.link.target) }
        val brokenExternal = brokenLinks.count { isExternalUrl(it.link.target) }

        logger.lifecycle("")
        logger.lifecycle("═══════════════════════════════════════════════════════")
        logger.lifecycle("Link Check Results")
        logger.lifecycle("═══════════════════════════════════════════════════════")
        logger.lifecycle("Files scanned:      ${scannedFiles.size}")
        logger.lifecycle("Total links found:  ${allLinks.size}")
        logger.lifecycle("───────────────────────────────────────────────────────")
        logger.lifecycle("Internal links:     $totalInternal")
        logger.lifecycle("  ✓ Valid:          $validInternalLinks")
        if (brokenInternal > 0) {
            logger.lifecycle("  ✗ Broken:         $brokenInternal")
        }
        logger.lifecycle("───────────────────────────────────────────────────────")
        logger.lifecycle("External links:     ${totalExternal + skippedExternalLinks}")
        if (checkExternal.get()) {
            logger.lifecycle("  ✓ Valid:          $validExternalLinks")
            if (brokenExternal > 0) {
                logger.lifecycle("  ✗ Broken:         $brokenExternal")
            }
        }
        if (skippedExternalLinks > 0) {
            logger.lifecycle("  ○ Skipped:        $skippedExternalLinks")
        }
        logger.lifecycle("───────────────────────────────────────────────────────")

        if (brokenLinks.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("Broken Links:")
            brokenLinks.forEach { broken ->
                val location = broken.link.sourceFile.name
                logger.lifecycle("  ✗ $location -> ${broken.link.target}")
                logger.lifecycle("    Reason: ${broken.reason}")
                logger.lifecycle("    Element: <${broken.link.elementName}>")
            }
        }

        logger.lifecycle("───────────────────────────────────────────────────────")
        val status = if (brokenLinks.isEmpty()) "PASSED" else "FAILED"
        logger.lifecycle("Status:             $status")
        logger.lifecycle("═══════════════════════════════════════════════════════")
    }

    /**
     * Build error message for broken links.
     */
    private fun buildErrorMessage(): String {
        val sb = StringBuilder()
        sb.appendLine("Link check failed: ${brokenLinks.size} broken link(s) found")
        sb.appendLine()

        brokenLinks.groupBy { it.link.sourceFile }.forEach { (file, links) ->
            sb.appendLine("${file.name}:")
            links.forEach { broken ->
                sb.appendLine("  - ${broken.link.target}")
                sb.appendLine("    ${broken.reason}")
            }
            sb.appendLine()
        }

        sb.appendLine("Troubleshooting:")
        sb.appendLine("  1. Verify that referenced files exist")
        sb.appendLine("  2. Check for typos in file paths")
        sb.appendLine("  3. Ensure relative paths are correct from the source file location")
        sb.appendLine("  4. For external URLs, verify the site is accessible")

        return sb.toString()
    }

    // ==================== Data Classes ====================

    /**
     * Types of links found in DITA content.
     */
    enum class LinkType {
        HREF,       // href attribute (xref, topicref, etc.)
        CONREF,     // conref attribute
        CONKEYREF,  // conkeyref attribute (requires key resolution)
        KEYREF,     // keyref attribute (requires key resolution)
        IMAGE       // image href
    }

    /**
     * Information about a link found in DITA content.
     */
    data class LinkInfo(
        val sourceFile: File,
        val target: String,
        val type: LinkType,
        val elementName: String,
        val lineNumber: Int?,
        val isExternalScope: Boolean = false
    )

    /**
     * A broken link with reason.
     */
    data class BrokenLink(
        val link: LinkInfo,
        val reason: String
    )
}
