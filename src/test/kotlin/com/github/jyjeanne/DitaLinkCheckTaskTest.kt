package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

/**
 * Tests for DitaLinkCheckTask.
 */
class DitaLinkCheckTaskTest : StringSpec({

    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(DitaOtPlugin::class.java)
    }

    "Register checkLinks task" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().shouldNotBeNull()
        task.get().group shouldBe "Verification"
        task.get().description shouldBe "Checks for broken links in DITA content"
    }

    "Default checkExternal is false" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().checkExternal.get() shouldBe false
    }

    "Default failOnBroken is true" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().failOnBroken.get() shouldBe true
    }

    "Default recursive is true" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().recursive.get() shouldBe true
    }

    "Default quiet is false" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().quiet.get() shouldBe false
    }

    "Default connectTimeout is 5000ms" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().connectTimeout.get() shouldBe 5000
    }

    "Default readTimeout is 10000ms" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().readTimeout.get() shouldBe 10000
    }

    "checkExternal can be enabled" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.checkExternal.set(true)
        }

        task.get().checkExternal.get() shouldBe true
    }

    "failOnBroken can be disabled" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.failOnBroken.set(false)
        }

        task.get().failOnBroken.get() shouldBe false
    }

    "recursive can be disabled" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.recursive.set(false)
        }

        task.get().recursive.get() shouldBe false
    }

    "quiet can be enabled" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.quiet.set(true)
        }

        task.get().quiet.get() shouldBe true
    }

    "connectTimeout can be configured" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.connectTimeout.set(10000)
        }

        task.get().connectTimeout.get() shouldBe 10000
    }

    "readTimeout can be configured" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.readTimeout.set(20000)
        }

        task.get().readTimeout.get() shouldBe 20000
    }

    "DSL method input adds files" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.input("docs/guide.ditamap")
        }

        task.get().inputFiles.files.size shouldBe 1
    }

    "Multiple input files can be added" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.input("docs/guide.ditamap")
            it.input("docs/reference.ditamap")
        }

        task.get().inputFiles.files.size shouldBe 2
    }

    "Input files collection starts empty" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().inputFiles.files.size shouldBe 0
    }

    "Task name constant is correct" {
        DitaLinkCheckTask.TASK_NAME shouldBe "checkLinks"
    }

    "excludeUrl adds patterns" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java) {
            it.excludeUrl("localhost")
            it.excludeUrl("example.com")
        }

        task.get().excludeUrlPatterns.get().size shouldBe 2
        task.get().excludeUrlPatterns.get().contains("localhost") shouldBe true
        task.get().excludeUrlPatterns.get().contains("example.com") shouldBe true
    }

    "excludeUrlPatterns default is empty list" {
        val task = project.tasks.register("checkLinks", DitaLinkCheckTask::class.java)

        task.get().excludeUrlPatterns.get().size shouldBe 0
    }

    "LinkType enum has correct values" {
        DitaLinkCheckTask.LinkType.HREF.name shouldBe "HREF"
        DitaLinkCheckTask.LinkType.CONREF.name shouldBe "CONREF"
        DitaLinkCheckTask.LinkType.CONKEYREF.name shouldBe "CONKEYREF"
        DitaLinkCheckTask.LinkType.KEYREF.name shouldBe "KEYREF"
        DitaLinkCheckTask.LinkType.IMAGE.name shouldBe "IMAGE"
    }

    "LinkInfo data class works correctly" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "other.dita",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = 42
        )

        linkInfo.sourceFile.name shouldBe "test.dita"
        linkInfo.target shouldBe "other.dita"
        linkInfo.type shouldBe DitaLinkCheckTask.LinkType.HREF
        linkInfo.elementName shouldBe "xref"
        linkInfo.lineNumber shouldBe 42
    }

    "LinkInfo lineNumber can be null" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "other.dita",
            type = DitaLinkCheckTask.LinkType.CONREF,
            elementName = "p",
            lineNumber = null
        )

        linkInfo.lineNumber shouldBe null
    }

    "BrokenLink data class works correctly" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "missing.dita",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = 10
        )
        val brokenLink = DitaLinkCheckTask.BrokenLink(
            link = linkInfo,
            reason = "File not found"
        )

        brokenLink.link shouldBe linkInfo
        brokenLink.reason shouldBe "File not found"
    }

    "BrokenLink with HTTP error" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "https://example.com/missing",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = null
        )
        val brokenLink = DitaLinkCheckTask.BrokenLink(
            link = linkInfo,
            reason = "HTTP 404"
        )

        brokenLink.reason shouldBe "HTTP 404"
    }

    "LinkType IMAGE for image elements" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "images/diagram.png",
            type = DitaLinkCheckTask.LinkType.IMAGE,
            elementName = "image",
            lineNumber = 25
        )

        linkInfo.type shouldBe DitaLinkCheckTask.LinkType.IMAGE
        linkInfo.elementName shouldBe "image"
    }

    "LinkType KEYREF for keyref attributes" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "product-name",
            type = DitaLinkCheckTask.LinkType.KEYREF,
            elementName = "keyword",
            lineNumber = 15
        )

        linkInfo.type shouldBe DitaLinkCheckTask.LinkType.KEYREF
        linkInfo.elementName shouldBe "keyword"
    }

    "LinkInfo isExternalScope defaults to false" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "other.dita",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = null
        )

        linkInfo.isExternalScope shouldBe false
    }

    "LinkInfo isExternalScope can be set to true" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "https://example.com",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = null,
            isExternalScope = true
        )

        linkInfo.isExternalScope shouldBe true
    }

    "LinkInfo with scope external and non-URL target" {
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "external-doc.pdf",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = 30,
            isExternalScope = true
        )

        linkInfo.target shouldBe "external-doc.pdf"
        linkInfo.isExternalScope shouldBe true
    }

    // ============================================================================
    // Peer Link Tests (v2.8.1 bug fix)
    // Tests for proper handling of scope="peer" links
    // ============================================================================

    "LinkInfo isPeerScope defaults to false" {
        // By default, links should not be marked as peer
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("test.dita"),
            target = "other.dita",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = null
        )

        linkInfo.isPeerScope shouldBe false
    }

    "LinkInfo isPeerScope can be set to true" {
        // Peer links should be skipped during checking
        // Example: <topicref href="api/index.html" format="html" scope="peer">
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("userguide.ditamap"),
            target = "api/index.html",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "topicref",
            lineNumber = 21,
            isPeerScope = true
        )

        linkInfo.isPeerScope shouldBe true
        linkInfo.target shouldBe "api/index.html"
    }

    "Peer links should be identified separately from external links" {
        // scope="peer" is different from scope="external"
        // - peer: Part of same information set but not in this build
        // - external: Outside the documentation (websites, etc.)
        val peerLink = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("guide.ditamap"),
            target = "api/index.html",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "topicref",
            lineNumber = null,
            isExternalScope = false,
            isPeerScope = true
        )

        val externalLink = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("guide.ditamap"),
            target = "https://example.com/docs",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "xref",
            lineNumber = null,
            isExternalScope = true,
            isPeerScope = false
        )

        peerLink.isPeerScope shouldBe true
        peerLink.isExternalScope shouldBe false

        externalLink.isPeerScope shouldBe false
        externalLink.isExternalScope shouldBe true
    }

    "LinkInfo with API docs peer reference" {
        // Real-world example from dita-ot/docs feedback:
        // <topicref href="api/index.html" format="html" scope="peer">
        // This link points to API docs generated separately, not in this build
        val linkInfo = DitaLinkCheckTask.LinkInfo(
            sourceFile = File("userguide.ditamap"),
            target = "api/index.html",
            type = DitaLinkCheckTask.LinkType.HREF,
            elementName = "topicref",
            lineNumber = 21,
            isExternalScope = false,
            isPeerScope = true
        )

        linkInfo.target shouldBe "api/index.html"
        linkInfo.isPeerScope shouldBe true
        linkInfo.isExternalScope shouldBe false
        linkInfo.elementName shouldBe "topicref"
    }
})
