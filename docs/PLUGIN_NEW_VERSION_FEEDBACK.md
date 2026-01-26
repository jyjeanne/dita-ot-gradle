# Plugin New Version Feedback

## Source

**Pull Request:** [dita-ot/docs#660](https://github.com/dita-ot/docs/pull/660)
**Review:** [#pullrequestreview-3702297303](https://github.com/dita-ot/docs/pull/660#pullrequestreview-3702297303)
**Reviewer:** @infotexture
**Date:** January 24, 2026

---

## Positive Feedback

| Aspect | Comment |
|--------|---------|
| **Build File** | Appreciated cleaner build file without excessive comments |
| **Documentation Build** | Confirmed the documentation build works as expected |
| **Progress Indicators** | Found the new progress indicators useful for tracking transformation steps |

---

## Issues to Address

### 1. Increased Logging Verbosity

**Priority:** Medium
**Status:** FIXED (v2.8.1)

**Problem:** PDF transformation now logs numerous FOP warnings by default. While useful for debugging, this level of detail should be configurable rather than shown automatically.

**Example FOP warnings:**
```
[WARN] The contents of fo:region-body on page 1 exceed its viewport by 48187 millipoints.
[WARN] Property ID "unique_348" (found on "fo:wrapper") previously used; ID values must be unique
[WARN] table-layout="fixed" and column-width unspecified => falling back to proportional-column-width(1)
```

**Fix Applied:**
Added `showWarnings` property to control warning display (default: `false`).

```kotlin
// Suppress warnings (default behavior)
tasks.named<DitaOtTask>("dita") {
    showWarnings(false)  // Default - warnings counted but not displayed
}

// Enable warnings for debugging
tasks.named<DitaOtTask>("dita") {
    showWarnings(true)   // Show all warnings during transformation
}
```

**Behavior:**
- Warnings are always counted and shown in the summary
- When `showWarnings(false)` (default): Warnings not displayed during processing
- When `showWarnings(true)`: Warnings displayed in DETAILED mode
- Summary shows: `Warnings: N (use showWarnings(true) to display warning details)`

**Files Changed:**
- `src/main/kotlin/com/github/jyjeanne/DitaOtTask.kt` (property + DSL method)
- `src/main/kotlin/com/github/jyjeanne/ProgressReporter.kt` (warning display logic)
- `src/test/kotlin/com/github/jyjeanne/DitaOtPluginTest.kt` (3 new tests)

**Action Items:**
- [x] Make FOP warning verbosity configurable
- [x] Add option to suppress FOP warnings in normal builds
- [x] Default to quiet mode for production builds

---

### 2. validateDita Task Issues

**Priority:** High
**Status:** FIXED (v2.8.1)

**Problem:** The new validation task has issues:
- Contradicts DITA-OT's built-in `validate` command
- Reports false errors (DOTJ031I messages shouldn't be treated as errors)
- Unclear advantage over native `dita validate` subcommand

**Root Cause:**
The error detection pattern was too broad, matching ALL DITA-OT message codes instead of only actual errors.

DITA-OT message format: `DOT[component][number][severity]`
- `E` = Error (e.g., DOTJ012E)
- `F` = Fatal (e.g., DOTJ001F)
- `W` = Warning (e.g., DOTJ031W)
- `I` = Info (e.g., DOTJ031I) - **NOT an error**

**Fix Applied:**
Updated `ERROR_PATTERN` in both `DitaOtValidateTask.kt` and `ProgressReporter.kt` to:
1. Only match messages ending with `E` (Error) or `F` (Fatal)
2. Added `INFO_PATTERN` to explicitly skip informational messages
3. Info messages like DOTJ031I are now logged as debug, not errors

**Files Changed:**
- `src/main/kotlin/com/github/jyjeanne/DitaOtValidateTask.kt`
- `src/main/kotlin/com/github/jyjeanne/ProgressReporter.kt`
- `src/test/kotlin/com/github/jyjeanne/DitaOtValidateTaskTest.kt` (new tests)

**Action Items:**
- [x] Review DOTJ031I message handling - should be info, not error
- [x] Add INFO_PATTERN to skip informational messages
- [x] Add unit tests for message classification
- [ ] Compare functionality with native DITA-OT validate subcommand
- [ ] Document when to use Gradle task vs native command

---

### 3. checkLinks Task Limitations

**Priority:** Medium
**Status:** FIXED (v2.8.1)

**Problem:** The link checker doesn't recognize `scope="peer"` links properly, incorrectly flagging the API documentation reference as broken despite being explicitly marked with `scope="peer"`.

**Example from dita-ot/docs:**
```xml
<topicref href="api/index.html" format="html" scope="peer">
```
This link points to API docs generated separately, not available at build time.

**Root Cause:**
The code handled `scope="external"` but not `scope="peer"`.

DITA scope values:
- `local` (default): Resource is part of this documentation set
- `peer`: Resource is NOT in this build but part of same information set
- `external`: Resource is external (websites, etc.)

**Fix Applied:**
- Added `isPeerScope` property to `LinkInfo` data class
- Updated `scanHrefElements()` to detect `scope="peer"` attribute
- Updated `checkAllLinks()` to skip peer-scoped links
- Added peer links count to results output

**Files Changed:**
- `src/main/kotlin/com/github/jyjeanne/DitaLinkCheckTask.kt`
- `src/test/kotlin/com/github/jyjeanne/DitaLinkCheckTaskTest.kt` (4 new tests)

**New Output Format:**
```
═══════════════════════════════════════════════════════
Link Check Results
═══════════════════════════════════════════════════════
Files scanned:      290
Total links found:  2348
───────────────────────────────────────────────────────
Internal links:     489
  ✓ Valid:          489
───────────────────────────────────────────────────────
Peer links:         1
  ○ Skipped:        1 (not in build)
───────────────────────────────────────────────────────
External links:     379
  ○ Skipped:        379
───────────────────────────────────────────────────────
Status:             PASSED
═══════════════════════════════════════════════════════
```

**Action Items:**
- [x] Add support for `scope="peer"` attribute detection
- [x] Skip validation for peer-scoped links (not in current build)
- [x] Add peer links count to output
- [x] Add unit tests for peer link handling

---

### 4. Removed Functionality (htmlhelp)

**Priority:** Low
**Status:** Clarification Needed

**Problem:** A code block handling `.chm` file movement was deleted without explanation. The reviewer questioned whether this was intentional.

**Recommendation:** Treat `htmlhelp` task removal as a separate issue to avoid conflicts with upstream changes.

**Action Items:**
- [ ] Clarify if htmlhelp removal was intentional
- [ ] If needed, create separate PR for htmlhelp changes

---

## Process Recommendations

### Branch Strategy

**Recommendation:** Future pull requests should originate from dedicated feature branches rather than the fork's `develop` branch to prevent conflicts with upstream changes.

**Action Items:**
- [ ] Create feature branches for future contributions
- [ ] Follow pattern: `feature/plugin-version-X.Y.Z` or `fix/issue-description`

---

## Summary

| Category | Count | Priority | Status |
|----------|-------|----------|--------|
| Positive Feedback | 3 | - | - |
| High Priority Issues | 1 | validateDita task | **FIXED** |
| Medium Priority Issues | 2 | Logging, checkLinks | **ALL FIXED** |
| Low Priority Issues | 1 | htmlhelp removal | To Do |
| Process Improvements | 1 | Branch strategy | To Do |
| Code Review | 1 | Bug in fallback extraction | **FIXED** |

---

## Code Review (v2.8.1)

**Date:** January 25, 2026

Code reviews were performed on all bug fixes to identify potential issues and regressions.

### Code Review #1: validateDita and checkLinks Fixes

| Priority | Issue | Location | Status |
|----------|-------|----------|--------|
| HIGH | Fallback error extraction in `validateFile()` didn't exclude INFO messages | `DitaOtValidateTask.kt:420-428` | **FIXED** |

**Fix Applied:**
```kotlin
// Before: Only checked ERROR_PATTERN
val errorLines = output.lines()
    .filter { ERROR_PATTERN.matcher(it).find() }
    .take(5)

// After: Also excludes INFO messages
val errorLines = output.lines()
    .filter { line ->
        ERROR_PATTERN.matcher(line).find() &&
        !INFO_PATTERN.matcher(line).find()  // Skip info messages
    }
    .take(5)
```

### Code Review #2: showWarnings Fix

| Priority | Issue | Location | Status |
|----------|-------|----------|--------|
| MEDIUM | Warning display only worked in DETAILED mode, not SIMPLE/MINIMAL | `ProgressReporter.kt:241` | **FIXED** |

**Problem:** If user set `progressStyle("SIMPLE")` + `showWarnings(true)`, warnings were NOT displayed because the condition was:
```kotlin
// Before (buggy): Only showed warnings in DETAILED mode
if (showWarnings && style == ProgressStyle.DETAILED)
```

**Fix Applied:**
```kotlin
// After: Shows warnings in all modes except QUIET
if (showWarnings && style != ProgressStyle.QUIET)
```

### Areas Reviewed - No Issues Found

| File | Area | Status |
|------|------|--------|
| `DitaOtValidateTask.kt` | ERROR_PATTERN regex | OK |
| `DitaOtValidateTask.kt` | INFO_PATTERN regex | OK |
| `DitaOtValidateTask.kt` | parseOutputLine() | OK |
| `ProgressReporter.kt` | Message classification | OK |
| `ProgressReporter.kt` | showWarnings logic | OK (after fix) |
| `DitaLinkCheckTask.kt` | isPeerScope detection | OK |
| `DitaLinkCheckTask.kt` | checkAllLinks() peer handling | OK |
| `DitaLinkCheckTask.kt` | Results statistics | OK |
| `DitaOtTask.kt` | showWarnings property/DSL | OK |

### Test Results

All **203 tests** pass (198 executed, 5 integration tests skipped):
- `DitaOtValidateTaskTest`: 33 tests (includes message classification)
- `DitaLinkCheckTaskTest`: 33 tests (includes peer link handling)
- `DitaOtPluginTest`: 11 tests (includes showWarnings)
- `ProgressReporterTest`: 33 tests
- Other test suites: 93 tests

---

## Next Steps

1. ~~**Immediate:** Fix validateDita false error reporting (DOTJ031I)~~ **DONE**
2. ~~**Short-term:** Add peer link support to checkLinks task~~ **DONE**
3. ~~**Code Review:** Review fixes for potential bugs~~ **DONE**
4. ~~**Short-term:** Make FOP logging verbosity configurable~~ **DONE**
5. **Follow-up:** Clarify htmlhelp changes in separate PR
