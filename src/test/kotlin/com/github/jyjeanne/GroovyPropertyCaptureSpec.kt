package com.github.jyjeanne

import groovy.lang.Closure
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for GroovyPropertyCapture - ensures Groovy closure properties are correctly captured.
 *
 * This test prevents regression of the v2.3.0 bug where properties { } closure syntax
 * was silently ignored when using DITA_SCRIPT execution strategy.
 */
class GroovyPropertyCaptureSpec : StringSpec({

    "Captures single property from Groovy closure" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                (delegate as GroovyPropertyCapture).property(
                    mapOf("name" to "args.css", "value" to "custom.css")
                )
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 1
        captured shouldContainKey "args.css"
        captured["args.css"] shouldBe "custom.css"
    }

    "Captures multiple properties from Groovy closure" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.copycss", "value" to "yes"))
                capture.property(mapOf("name" to "args.css", "value" to "dita-ot-doc.css"))
                capture.property(mapOf("name" to "args.csspath", "value" to "css"))
                capture.property(mapOf("name" to "args.cssroot", "value" to "/resources/"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 4
        captured["args.copycss"] shouldBe "yes"
        captured["args.css"] shouldBe "dita-ot-doc.css"
        captured["args.csspath"] shouldBe "css"
        captured["args.cssroot"] shouldBe "/resources/"
    }

    "Captures HTML5-specific properties" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.copycss", "value" to "yes"))
                capture.property(mapOf("name" to "args.css", "value" to "custom-theme.css"))
                capture.property(mapOf("name" to "args.gen.task.lbl", "value" to "YES"))
                capture.property(mapOf("name" to "args.hdr", "value" to "header.xml"))
                capture.property(mapOf("name" to "args.rellinks", "value" to "noparent"))
                capture.property(mapOf("name" to "html5.toc.generate", "value" to "no"))
                capture.property(mapOf("name" to "nav-toc", "value" to "partial"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 7
        captured["args.copycss"] shouldBe "yes"
        captured["args.css"] shouldBe "custom-theme.css"
        captured["args.gen.task.lbl"] shouldBe "YES"
        captured["args.hdr"] shouldBe "header.xml"
        captured["args.rellinks"] shouldBe "noparent"
        captured["html5.toc.generate"] shouldBe "no"
        captured["nav-toc"] shouldBe "partial"
    }

    "Captures PDF-specific properties" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.chapter.layout", "value" to "BASIC"))
                capture.property(mapOf("name" to "args.gen.task.lbl", "value" to "YES"))
                capture.property(mapOf("name" to "include.rellinks", "value" to "#default external"))
                capture.property(mapOf("name" to "outputFile.base", "value" to "userguide"))
                capture.property(mapOf("name" to "theme", "value" to "custom-theme.yaml"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 5
        captured["args.chapter.layout"] shouldBe "BASIC"
        captured["outputFile.base"] shouldBe "userguide"
        captured["theme"] shouldBe "custom-theme.yaml"
    }

    "Returns empty map for null closure" {
        val captured = GroovyPropertyCapture.captureFromClosure(null)

        captured shouldHaveSize 0
    }

    "Returns empty map for empty closure" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                // Empty closure - no properties
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 0
    }

    "Ignores properties with missing name" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("value" to "orphan-value"))  // Missing 'name'
                capture.property(mapOf("name" to "valid.prop", "value" to "valid-value"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 1
        captured["valid.prop"] shouldBe "valid-value"
    }

    "Ignores properties with missing value" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "orphan-name"))  // Missing 'value'
                capture.property(mapOf("name" to "valid.prop", "value" to "valid-value"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 1
        captured["valid.prop"] shouldBe "valid-value"
    }

    "Handles processing-mode property (commonly used)" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "processing-mode", "value" to "strict"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 1
        captured["processing-mode"] shouldBe "strict"
    }

    "Last property value wins when duplicate names" {
        val closure = object : Closure<Unit>(null) {
            override fun call(): Unit {
                val capture = delegate as GroovyPropertyCapture
                capture.property(mapOf("name" to "args.css", "value" to "first.css"))
                capture.property(mapOf("name" to "args.css", "value" to "second.css"))
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured shouldHaveSize 1
        captured["args.css"] shouldBe "second.css"
    }
})
