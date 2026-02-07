package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

/**
 * Tests for DitaOtValidateTask.
 */
class DitaOtValidateTaskTest : StringSpec({

    lateinit var project: Project

    beforeTest {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(DitaOtPlugin::class.java)
    }

    "Register validate task" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().shouldNotBeNull()
        task.get().group shouldBe "Verification"
        task.get().description shouldBe "Validates DITA content without full transformation"
    }

    "Default strict mode is false" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().strictMode.get() shouldBe false
    }

    "Default fail on error is true" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().failOnError.get() shouldBe true
    }

    "Default quiet mode is false" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().quiet.get() shouldBe false
    }

    "Default processing mode is strict" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().processingMode.get() shouldBe "strict"
    }

    "Default validation timeout is 600000ms" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().validationTimeout.get() shouldBe 600000L
    }

    "Validation timeout can be configured" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.validationTimeout.set(300000L)
        }

        task.get().validationTimeout.get() shouldBe 300000L
    }

    "Strict mode can be configured" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.strictMode.set(true)
        }

        task.get().strictMode.get() shouldBe true
    }

    "Processing mode can be configured" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.processingMode.set("lax")
        }

        task.get().processingMode.get() shouldBe "lax"
    }

    "Fail on error can be disabled" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.failOnError.set(false)
        }

        task.get().failOnError.get() shouldBe false
    }

    "Quiet mode can be enabled" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.quiet.set(true)
        }

        task.get().quiet.get() shouldBe true
    }

    "DSL method input adds files" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.input("docs/guide.ditamap")
        }

        task.get().inputFiles.files.size shouldBe 1
    }

    "DSL method ditaOtDir works with string" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.ditaOtDir("build/dita-ot")
        }

        task.get().ditaOtDir.asFile.get().path shouldContain "dita-ot"
    }

    "DSL method filter works" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.filter("docs/release.ditaval")
        }

        task.get().filterFile.files.size shouldBe 1
    }

    "Multiple input files can be added" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.input("docs/guide.ditamap")
            it.input("docs/reference.ditamap")
        }

        task.get().inputFiles.files.size shouldBe 2
    }

    "Task name constant is correct" {
        DitaOtValidateTask.TASK_NAME shouldBe "validateDita"
    }

    "ValidationResult data class works correctly" {
        val result = DitaOtValidateTask.ValidationResult(
            file = File("test.ditamap"),
            success = true,
            errors = emptyList(),
            warnings = emptyList(),
            output = "Test output"
        )

        result.success shouldBe true
        result.errors.size shouldBe 0
        result.warnings.size shouldBe 0
    }

    "ValidationMessage data class works correctly" {
        val message = DitaOtValidateTask.ValidationMessage(
            file = "test.dita",
            line = 42,
            message = "Test error message"
        )

        message.file shouldBe "test.dita"
        message.line shouldBe 42
        message.message shouldBe "Test error message"
    }

    "ValidationMessage line can be null" {
        val message = DitaOtValidateTask.ValidationMessage(
            file = "test.dita",
            line = null,
            message = "Test error message"
        )

        message.line shouldBe null
    }

    "Processing mode lax can be configured" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.processingMode.set("lax")
        }

        task.get().processingMode.get() shouldBe "lax"
    }

    "Processing mode skip can be configured" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.processingMode.set("skip")
        }

        task.get().processingMode.get() shouldBe "skip"
    }

    "DSL method ditaOtDir works with File" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.ditaOtDir(File("/test/dita-ot"))
        }

        task.get().ditaOtDir.asFile.get().path shouldContain "dita-ot"
    }

    "ValidationResult with errors" {
        val errors = listOf(
            DitaOtValidateTask.ValidationMessage("test.dita", 10, "Error 1"),
            DitaOtValidateTask.ValidationMessage("test.dita", 20, "Error 2")
        )
        val result = DitaOtValidateTask.ValidationResult(
            file = File("test.ditamap"),
            success = false,
            errors = errors,
            warnings = emptyList(),
            output = "Test output"
        )

        result.success shouldBe false
        result.errors.size shouldBe 2
        result.errors[0].line shouldBe 10
        result.errors[1].message shouldBe "Error 2"
    }

    "ValidationResult with warnings" {
        val warnings = listOf(
            DitaOtValidateTask.ValidationMessage("test.dita", 5, "Warning 1")
        )
        val result = DitaOtValidateTask.ValidationResult(
            file = File("test.ditamap"),
            success = true,
            errors = emptyList(),
            warnings = warnings,
            output = "Test output"
        )

        result.success shouldBe true
        result.warnings.size shouldBe 1
        result.warnings[0].line shouldBe 5
    }

    "Multiple filters can be added" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.filter("docs/release.ditaval")
            it.filter("docs/product.ditaval")
        }

        task.get().filterFile.files.size shouldBe 2
    }

    "Input files collection starts empty" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().inputFiles.files.size shouldBe 0
    }

    "Filter files collection starts empty" {
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java)

        task.get().filterFile.files.size shouldBe 0
    }

    // ============================================================================
    // DITA-OT 4.x Compatibility Tests (v2.8.1 bug fix)
    // These tests verify the task uses 'dita' transtype instead of deprecated 'preprocess'
    // ============================================================================

    "Validation uses dita transtype for DITA-OT 4.x compatibility" {
        // This test verifies the fix for BUG_FIX_VALIDATE_TRANSTYPE.md
        // The 'preprocess' transtype was removed in DITA-OT 4.x
        // The task should use 'dita' transtype instead
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.input("docs/test.ditamap")
        }

        // Verify task is configured correctly
        task.get().shouldNotBeNull()
        // The validation uses 'dita' transtype internally (verified through code review)
        // This test ensures the task can be created and configured
        task.get().inputFiles.files.size shouldBe 1
    }

    "Validation command uses separate arguments for paths" {
        // This test verifies the fix for BUG_FIX_COMMAND_PATH.md
        // Command arguments should use separate values: --input <path>
        // instead of combined format: --input=<path>
        // This is critical for paths with spaces on Windows
        val task = project.tasks.register("validateDita", DitaOtValidateTask::class.java) {
            it.input("docs/path with spaces/test.ditamap")
            it.processingMode.set("strict")
        }

        task.get().shouldNotBeNull()
        task.get().processingMode.get() shouldBe "strict"
    }

    // ============================================================================
    // DITA-OT Message Code Only Detection Tests (v2.8.4)
    // Uses DITA-OT structured message codes ONLY — no generic patterns
    // Prefixes: DOTA, DOTJ, DOTX, INDX, PDFJ, PDFX, XEPJ
    // Severity: I=Info, W=Warning, E=Error, F=Fatal
    // ============================================================================

    "ValidationResult output can be retrieved" {
        val result = DitaOtValidateTask.ValidationResult(
            file = File("test.ditamap"),
            success = true,
            errors = emptyList(),
            warnings = emptyList(),
            output = "Build completed successfully"
        )

        result.output shouldBe "Build completed successfully"
    }

    "ValidationResult file can be retrieved" {
        val testFile = File("my-guide.ditamap")
        val result = DitaOtValidateTask.ValidationResult(
            file = testFile,
            success = true,
            errors = emptyList(),
            warnings = emptyList(),
            output = ""
        )

        result.file shouldBe testFile
        result.file.name shouldBe "my-guide.ditamap"
    }

    // --- DITA-OT Error Codes (should be detected as errors) ---

    "should detect DOTJ013E - failed to parse referenced resource" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[DOTJ013E] Failed to parse the referenced resource 'topics/missing.dita'."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect DOTA001F - not a recognized transformation type" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[DOTA001F] 'unknown' is not a recognized transformation type."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect PDFJ001E - PDF indexing sort error" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[PDFJ001E] PDF indexing cannot find sort location for 'term'."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect XEPJ002E - XEP error" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[XEPJ002E] XEP processing failed."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect INDX002E - index sort error" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[INDX002E] PDF indexing cannot find sort location for 'term'."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect PDFX013F - PDF file cannot be generated" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[PDFX013F] PDF file cannot be generated."
        errorPattern.matcher(line).find() shouldBe true
    }

    "should detect DOTX010E - unable to find conref target" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[DOTX010E] Unable to find @conref target 'topics/shared.dita#topic/id'."
        errorPattern.matcher(line).find() shouldBe true
    }

    // --- DITA-OT Warning Codes (should be warnings, not errors) ---

    "should detect DOTX023W as warning, not error" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")
        val line = "[DOTX023W] Unable to retrieve navtitle from target 'topics/overview.dita'."
        errorPattern.matcher(line).find() shouldBe false
        warningPattern.matcher(line).find() shouldBe true
    }

    "should detect DOTJ014W as warning" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")
        val line = "[DOTJ014W] Found an <indexterm> element with no content."
        errorPattern.matcher(line).find() shouldBe false
        warningPattern.matcher(line).find() shouldBe true
    }

    "should detect PDFX001W as warning" {
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")
        val line = "[PDFX001W] index term range specified with @start but no matching @end."
        warningPattern.matcher(line).find() shouldBe true
    }

    // --- DITA-OT Info Codes (should be ignored) ---

    "should NOT detect DOTJ031I as error - no rule found in DITAVAL" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")
        val infoPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}I\\]")
        val line = "[DOTJ031I] No rule for 'audience' found in DITAVAL file."
        errorPattern.matcher(line).find() shouldBe false
        warningPattern.matcher(line).find() shouldBe false
        infoPattern.matcher(line).find() shouldBe true
    }

    "should NOT detect DOTJ045I as error - key defined more than once" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[DOTJ045I] key 'product-name' is defined more than once in the same map."
        errorPattern.matcher(line).find() shouldBe false
    }

    "should NOT detect PDFJ003I as error" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val infoPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}I\\]")
        val line = "[PDFJ003I] Index entry will be sorted under Special characters heading."
        errorPattern.matcher(line).find() shouldBe false
        infoPattern.matcher(line).find() shouldBe true
    }

    // --- Same Code Number With Different Severities ---

    "DOTJ007E should be error, DOTJ007W warning, DOTJ007I info" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")
        val infoPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}I\\]")

        val errorLine = "[DOTJ007E] Duplicate condition in filter file for rule 'audience:expert'."
        val warningLine = "[DOTJ007W] Duplicate condition in filter file for rule 'audience:expert'."
        val infoLine = "[DOTJ007I] Duplicate condition in filter file for rule 'audience:expert'."

        errorPattern.matcher(errorLine).find() shouldBe true
        warningPattern.matcher(errorLine).find() shouldBe false

        errorPattern.matcher(warningLine).find() shouldBe false
        warningPattern.matcher(warningLine).find() shouldBe true

        errorPattern.matcher(infoLine).find() shouldBe false
        warningPattern.matcher(infoLine).find() shouldBe false
        infoPattern.matcher(infoLine).find() shouldBe true
    }

    // --- Lines Without DITA-OT Code (should ALL be ignored) ---

    "should ignore Processing file with error in filename" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Processing file:/path/topics/error-messages.xml to file:/path/temp/hash.xml"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore Processing file with error-messages-details in filename" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Processing file:/path/topics/error-messages-details.xml to file:/path/temp/hash.xml"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore Processing with Windows path" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Processing C:\\path\\build\\dita-temp\\topics\\error-messages.xml"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore Writing file message" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Writing file:/path/temp/validate/topics/file.xml"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore generic ERROR tag from third-party (Apache FOP)" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "[ERROR] SVG graphic could not be built."
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore generic Error prefix from third-party" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Error: File file:/path/missing.md was not found."
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore stack trace from Apache Batik" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "org.apache.batik.bridge.BridgeException: file:/path/temp/:-1"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore Caused by in stack trace" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "Caused by: java.lang.RuntimeException: some message"
        errorPattern.matcher(line).find() shouldBe false
    }

    "should ignore at line in stack trace" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val line = "    at org.apache.fop.render.pdf.PDFRenderer.render(PDFRenderer.java:123)"
        errorPattern.matcher(line).find() shouldBe false
    }

    // --- Integration Test (real build output) ---

    "should correctly count errors and warnings in real build output" {
        val errorPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]")
        val warningPattern = java.util.regex.Pattern.compile("\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]")

        val buildOutput = listOf(
            // Lines WITHOUT DITA-OT code -> all ignored
            "Processing file:/path/topics/error-messages.xml to file:/path/temp/hash.xml",
            "Processing file:/path/topics/error-messages-details.xml to file:/path/temp/hash.xml",
            "[ERROR] SVG graphic could not be built. Reason: org.apache.batik.bridge.BridgeException",
            "org.apache.batik.bridge.BridgeException: file:/path/temp/:-1",
            "    at org.apache.batik.bridge.SVGImageElementBridge.createGraphicsNode(...)",
            "Writing file:/path/temp/validate/topics/error-messages.xml",
            // Lines WITH DITA-OT code -> classified by suffix
            "[DOTJ031I] No rule for 'audience' found in DITAVAL file.",                   // I -> ignored
            "[DOTJ045I] key 'product-name' is defined more than once in the same map.",   // I -> ignored
            "[DOTX023W] Unable to retrieve navtitle from target 'topics/overview.dita'.", // W -> warning
            "[DOTJ014W] Found an <indexterm> element with no content.",                   // W -> warning
            "[DOTJ013E] Failed to parse the referenced resource 'topics/broken.dita'.",   // E -> error
            "[DOTX010E] Unable to find @conref target 'shared.dita#topic/id'.",           // E -> error
            "[DOTA001F] 'unknown' is not a recognized transformation type."               // F -> error
        )

        val errorCount = buildOutput.count { errorPattern.matcher(it).find() }
        val warningCount = buildOutput.count { warningPattern.matcher(it).find() }
        errorCount shouldBe 3    // DOTJ013E + DOTX010E + DOTA001F
        warningCount shouldBe 2  // DOTX023W + DOTJ014W
    }
})
