package com.github.jyjeanne

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for ProgressReporter.
 *
 * Note: These tests focus on the enum values and stage detection logic,
 * as the actual progress reporting requires a real Gradle Logger.
 */
class ProgressReporterTest : StringSpec({

    "ProgressStyle enum has correct values" {
        ProgressReporter.ProgressStyle.DETAILED.name shouldBe "DETAILED"
        ProgressReporter.ProgressStyle.SIMPLE.name shouldBe "SIMPLE"
        ProgressReporter.ProgressStyle.MINIMAL.name shouldBe "MINIMAL"
        ProgressReporter.ProgressStyle.QUIET.name shouldBe "QUIET"
    }

    "ProgressStyle values count is 4" {
        ProgressReporter.ProgressStyle.values().size shouldBe 4
    }

    "Stage enum has correct display names" {
        ProgressReporter.Stage.INIT.displayName shouldBe "Initializing"
        ProgressReporter.Stage.PREPROCESS.displayName shouldBe "Preprocessing"
        ProgressReporter.Stage.TRANSFORM.displayName shouldBe "Transforming"
        ProgressReporter.Stage.COMPLETE.displayName shouldBe "Complete"
    }

    "Stage enum has correct progress percentages" {
        ProgressReporter.Stage.INIT.progressPercent shouldBe 0
        ProgressReporter.Stage.PREPROCESS.progressPercent shouldBe 10
        ProgressReporter.Stage.TRANSFORM.progressPercent shouldBe 70
        ProgressReporter.Stage.COMPLETE.progressPercent shouldBe 100
    }

    "Stage PREPROCESS_CONREF progress is 40" {
        ProgressReporter.Stage.PREPROCESS_CONREF.progressPercent shouldBe 40
    }

    "Stage PREPROCESS_KEYREF progress is 30" {
        ProgressReporter.Stage.PREPROCESS_KEYREF.progressPercent shouldBe 30
    }

    "Stage FINALIZE progress is 95" {
        ProgressReporter.Stage.FINALIZE.progressPercent shouldBe 95
    }

    "Stage.fromLogMessage detects preprocess stage" {
        ProgressReporter.Stage.fromLogMessage("[preprocess] INFO: Starting") shouldBe ProgressReporter.Stage.PREPROCESS
    }

    "Stage.fromLogMessage detects transform stage" {
        ProgressReporter.Stage.fromLogMessage("[transform] INFO: Transforming topics") shouldBe ProgressReporter.Stage.TRANSFORM
    }

    "Stage.fromLogMessage detects build successful" {
        ProgressReporter.Stage.fromLogMessage("BUILD SUCCESSFUL") shouldBe ProgressReporter.Stage.COMPLETE
    }

    "Stage.fromLogMessage detects build finished" {
        ProgressReporter.Stage.fromLogMessage("Build finished successfully") shouldBe ProgressReporter.Stage.COMPLETE
    }

    "Stage.fromLogMessage detects conref stage" {
        ProgressReporter.Stage.fromLogMessage("[conref] INFO: Resolving content references") shouldBe ProgressReporter.Stage.PREPROCESS_CONREF
    }

    "Stage.fromLogMessage detects keyref stage" {
        ProgressReporter.Stage.fromLogMessage("[keyref] INFO: Resolving key references") shouldBe ProgressReporter.Stage.PREPROCESS_KEYREF
    }

    "Stage.fromLogMessage detects coderef stage" {
        ProgressReporter.Stage.fromLogMessage("[coderef] INFO: Resolving code references") shouldBe ProgressReporter.Stage.PREPROCESS_CODEREF
    }

    "Stage.fromLogMessage detects mapref stage" {
        ProgressReporter.Stage.fromLogMessage("[mapref] INFO: Resolving map references") shouldBe ProgressReporter.Stage.PREPROCESS_MAPREF
    }

    "Stage.fromLogMessage detects branch-filter stage" {
        ProgressReporter.Stage.fromLogMessage("[branch-filter] INFO: Applying filters") shouldBe ProgressReporter.Stage.PREPROCESS_BRANCH_FILTER
    }

    "Stage.fromLogMessage detects chunk stage" {
        ProgressReporter.Stage.fromLogMessage("[chunk] INFO: Chunking topics") shouldBe ProgressReporter.Stage.PREPROCESS_CHUNK
    }

    "Stage.fromLogMessage detects copy-files stage" {
        ProgressReporter.Stage.fromLogMessage("[copy-files] INFO: Copying files") shouldBe ProgressReporter.Stage.PREPROCESS_COPY_FILES
    }

    "Stage.fromLogMessage detects move-meta stage" {
        ProgressReporter.Stage.fromLogMessage("[move-meta] INFO: Moving metadata") shouldBe ProgressReporter.Stage.PREPROCESS_MOVE_META
    }

    "Stage.fromLogMessage detects debug-filter stage" {
        ProgressReporter.Stage.fromLogMessage("[debug-filter] INFO: Filtering debug") shouldBe ProgressReporter.Stage.PREPROCESS_DEBUG_FILTER
    }

    "Stage.fromLogMessage detects CSS copy stage" {
        ProgressReporter.Stage.fromLogMessage("Copying CSS files") shouldBe ProgressReporter.Stage.CSS_COPY
        ProgressReporter.Stage.fromLogMessage("[copy-css] INFO: Copying stylesheets") shouldBe ProgressReporter.Stage.CSS_COPY
        ProgressReporter.Stage.fromLogMessage("copy style.css to output") shouldBe ProgressReporter.Stage.CSS_COPY
    }

    "Stage.fromLogMessage does not match css in unrelated contexts" {
        // "css" alone should not match - needs to be in a copy context
        ProgressReporter.Stage.fromLogMessage("processing access control") shouldBe null
    }

    "Stage.fromLogMessage detects init stage" {
        ProgressReporter.Stage.fromLogMessage("[init] INFO: Initializing") shouldBe ProgressReporter.Stage.INIT
    }

    "Stage.fromLogMessage returns null for unknown messages" {
        ProgressReporter.Stage.fromLogMessage("Random log message") shouldBe null
    }

    "Stage.fromLogMessage returns null for empty message" {
        ProgressReporter.Stage.fromLogMessage("") shouldBe null
    }

    "Stage.fromLogMessage is case insensitive" {
        ProgressReporter.Stage.fromLogMessage("PREPROCESS started") shouldBe ProgressReporter.Stage.PREPROCESS
        ProgressReporter.Stage.fromLogMessage("PreProcess started") shouldBe ProgressReporter.Stage.PREPROCESS
    }

    "Stage values are in correct order" {
        val stages = ProgressReporter.Stage.values()

        // Verify INIT comes before PREPROCESS
        stages.indexOf(ProgressReporter.Stage.INIT) shouldBe 0

        // Verify PREPROCESS comes before TRANSFORM
        (stages.indexOf(ProgressReporter.Stage.PREPROCESS) < stages.indexOf(ProgressReporter.Stage.TRANSFORM)) shouldBe true

        // Verify COMPLETE is last
        stages.last() shouldBe ProgressReporter.Stage.COMPLETE
    }

    "Stage progress percentages increase monotonically" {
        val stages = ProgressReporter.Stage.values()
        var lastPercent = -1

        stages.forEach { stage ->
            (stage.progressPercent >= lastPercent) shouldBe true
            lastPercent = stage.progressPercent
        }
    }

    "All stages have progress percentages between 0 and 100" {
        ProgressReporter.Stage.values().forEach { stage ->
            (stage.progressPercent >= 0) shouldBe true
            (stage.progressPercent <= 100) shouldBe true
        }
    }

    "INIT stage has 0 percent progress" {
        ProgressReporter.Stage.INIT.progressPercent shouldBe 0
    }

    "COMPLETE stage has 100 percent progress" {
        ProgressReporter.Stage.COMPLETE.progressPercent shouldBe 100
    }

    "ProgressStyle valueOf works correctly" {
        ProgressReporter.ProgressStyle.valueOf("DETAILED") shouldBe ProgressReporter.ProgressStyle.DETAILED
        ProgressReporter.ProgressStyle.valueOf("SIMPLE") shouldBe ProgressReporter.ProgressStyle.SIMPLE
        ProgressReporter.ProgressStyle.valueOf("MINIMAL") shouldBe ProgressReporter.ProgressStyle.MINIMAL
        ProgressReporter.ProgressStyle.valueOf("QUIET") shouldBe ProgressReporter.ProgressStyle.QUIET
    }

    "Stage count is 20" {
        ProgressReporter.Stage.values().size shouldBe 20
    }
})
