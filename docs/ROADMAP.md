# DITA-OT Gradle Plugin Roadmap

This document outlines planned improvements and features for the DITA-OT Gradle Plugin, organized by priority.

---

## Priority Legend

| Priority | Description | Timeline |
|----------|-------------|----------|
| **P0 - Critical** | Essential for production use | Next release |
| **P1 - High** | Significant value for most users | 1-2 releases |
| **P2 - Medium** | Improves developer experience | 3-6 months |
| **P3 - Low** | Nice to have features | Future |
| **P4 - Exploration** | Research and experimentation | TBD |

---

## ✅ v2.8.7 Maintenance Release (COMPLETED)

No change to the task DSL. This release updates the toolchain, raises the minimum Gradle version, and makes CI exercise real DITA-OT transformations.

### Minimum Gradle Raised to 8.14.4
**Status**: ✅ COMPLETED (v2.8.7)

The plugin is now built with Kotlin 2.4.21, whose Gradle plugin deprecates Gradle versions below 8.14.4. The CI build matrix moved from 8.5 / 8.10 / 9.0 to **8.14.4 / 9.0 / 9.9.1**, and the README compatibility tables now list 8.14.4 as the minimum (previously 6.5+). Users on older Gradle versions should stay on 2.8.6.

### Dependency and Toolchain Updates
**Status**: ✅ COMPLETED (v2.8.7)

| Component | Before | After |
|-----------|--------|-------|
| Kotlin | 2.1.0 | 2.4.21 (language/API level 2.2) |
| commons-io | 2.20.0 | 2.22.0 |
| Kotest | 5.9.1 | 6.2.7 |
| jsoup (tests) | 1.21.2 | 1.24.1 |
| Gradle wrapper | 8.5 | 9.9.1 (root, test-project, examples) |
| plugin-publish | 1.2.1 | 2.2.1 |
| GitHub Actions | checkout v4, setup-java v4, upload/download-artifact v4 | checkout v7, setup-java v6, upload-artifact v7, download-artifact v8 |

- `kotlinOptions` migrated to `compilerOptions`; deprecated `tasks.registering` delegates and `TaskContainer.create` replaced. The build now compiles with **no compiler or Gradle deprecation warnings**.
- `DitaOtDownloadTask` keeps the lenient `URL(String)` parsing on purpose (`URI.toURL()` would reject URLs with unencoded spaces, `[ ]`, `|` or `%` that work today).
- **Dependabot** now opens weekly PRs for GitHub Actions and Gradle dependencies (root build, test-project, examples).

### DITA-OT 4.4.2 Support and Real Integration Tests
**Status**: ✅ COMPLETED (v2.8.7)

**Problem:** The CI "integration" jobs only ran `gradlew tasks`, so they could not catch a broken transformation. The unit-test steps used `continue-on-error`, so failing tests did not fail CI.

**Solution:**
- `test-project` downloads DITA-OT with the plugin's own `DitaOtDownloadTask` (`-PditaOtVersion=<v>`), runs an HTML5 transformation, and its `verifyOutput` task fails unless `index.html` and `test-topic.html` are produced.
- CI runs it for DITA-OT 3.4 / 3.5 / 3.6 / **4.4.2** on Linux and 3.5 / 3.6 / **4.4.2** on Windows.
- Unit tests now fail CI when they fail. The root test suite can run against any DITA-OT release with `./gradlew test -PditaOtVersion=<v>`.
- Examples now default to DITA-OT 4.4.2.

**Known limitation:** on Windows, DITA-OT 4.x's `dita.bat` builds a CLASSPATH from absolute JAR paths and can exceed the 8191-character `cmd.exe` limit when the project path is long (exit code 255, "The input line is too long"). 3.x is less affected. Documented in the README troubleshooting section. **Fixed after 2.8.7:** the plugin now runs `dita.bat` through its 8.3 short path when needed (see the backlog below).

### Tests

228 tests (223 executed, 5 skipped). Verified locally on Gradle 9.9.1 against DITA-OT 3.6 and 4.4.2, and on Gradle 8.14.4 against DITA-OT 3.6.

### Open Issues (Backlog from the v2.8.7 Code Review)

| Area | Issue | Priority |
|------|-------|----------|
| Windows / DITA-OT 4.x | `dita.bat` exceeds the command-line limit on long paths. **Fixed for the next release:** the plugin estimates the command-line length and runs `dita.bat` through its 8.3 short path when needed (transformation and validation). Still open when 8.3 names are disabled on the volume (a warning is logged). | ✅ P1 |
| `DitaOtDownloadTask` | Relative `Location` headers on redirects are not resolved against the current URL, so such downloads fail on every retry. | P2 |
| `DitaOtDownloadTask` | Sends `Accept-Encoding: gzip` but never decompresses; a gzipping server leaves a broken archive in the cache. | P2 |
| Build | Gradle 8.14.x embeds Kotlin stdlib 2.0.21 while the plugin compiles against API level 2.2; nothing yet checks that no newer stdlib API is used. | P2 |
| Build | `kotlin-reflect` is a runtime dependency but unused. | P3 |
| Build | Root `extractDitaOt` ignores a failed rename and re-extracts the DITA-OT zip on every test run. | P3 |
| Repository | No `.gitattributes` (line endings flip between contributors); example wrapper jars/scripts were not regenerated; root `gradlew` is not executable in git. | P3 |

---

## ✅ v2.8.6 Bug Fixes (COMPLETED)

### DitaOtInstallPluginTask - DITA-OT 4.4 Compatibility
**Status**: ✅ COMPLETED (v2.8.6)

Fixed `--force` flag placement breaking plugin installation on DITA-OT 4.4+ (picocli argument parsing).

**Problem:** The `--force` flag was placed before the positional plugin argument (`dita install --force <plugin>`), which worked with DITA-OT ≤4.3 but fails with DITA-OT 4.4's picocli parser that requires positional arguments first.

**Solution:** Reordered command arguments to `dita install <plugin> --force`. Extracted `buildInstallCommand()` as `internal fun` for testability.

**Also fixed:** Troubleshooting message used deprecated `--install` syntax; updated to `install` (subcommand format).

**Files Fixed:**
- `DitaOtInstallPluginTask.kt` — Command argument ordering, buildInstallCommand extraction, troubleshooting message

### ProgressReporter - Duplicate Progress Lines Fix
**Status**: ✅ COMPLETED (v2.8.6)

Fixed duplicate 100% completion lines appearing in task logs.

**Problem:** Three separate code paths could print completion lines:
1. `processLine()` detecting "build successful" → `updateProgress(Stage.COMPLETE)`
2. `processOutput()` explicitly calling `updateProgress(Stage.COMPLETE)`
3. `printSummary()` printing the final progress bar with duration

This produced 2-3 duplicate lines in non-terminal mode (CI/Gradle logs). Also, `printSummary()` wrapped `buildProgressBar()` output (which already includes `[...]`) with extra brackets, producing `[[...]]`.

**Solution:**
- Removed explicit `updateProgress(Stage.COMPLETE)` call from `processOutput()`
- Added COMPLETE guard in `updateProgress()` for DETAILED and SIMPLE styles (these duplicate `printSummary()`'s progress bar format)
- MINIMAL style still shows "Complete..." (different format, no duplication)
- Fixed double bracket formatting in `printSummary()`

**Files Fixed:**
- `ProgressReporter.kt` — Duplicate progress lines, double brackets

### AntExecutor - Thread Synchronization Fix
**Status**: ✅ COMPLETED (v2.8.6)

Fixed race condition where `printSummary()` could read stale progress data.

**Problem:** The main thread called `printSummary()` before the output reader thread finished processing all DITA-OT output, potentially showing incorrect file counts or stage information.

**Solution:** Added `outputThread?.join(5000)` before `printSummary()` to ensure the reader thread completes processing.

**Files Fixed:**
- `AntExecutor.kt` — Thread join before summary

### New Unit Tests

Added 3 new tests for v2.8.6 (total: 206 tests):

| Test Class | New Tests | Description |
|------------|-----------|-------------|
| `DitaOtInstallPluginTaskTest.kt` | 3 | Command argument ordering (--force after plugin, omit --force, Windows paths) |

---

## ✅ P0 - Critical Priority (COMPLETED in v2.4.0)

### 1. ✅ Built-in DITA-OT Download Task
**Status**: ✅ COMPLETED (v2.4.0)
**Impact**: High
**Effort**: Medium

A built-in `DitaOtDownloadTask` that downloads and extracts DITA-OT from GitHub releases.

**Implemented API:**
```kotlin
tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
    version.set("4.2.3")
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))
    retries.set(3)                    // Automatic retry on failure
    checksum.set("sha256:abc123...")  // Optional verification
    connectTimeout.set(30000)         // Connection timeout (ms)
    readTimeout.set(60000)            // Read timeout (ms)
    quiet.set(false)                  // Suppress output
}
```

**Features Delivered:**
- ✅ Single plugin dependency (no `de.undercouch.download` needed)
- ✅ Automatic version management and caching
- ✅ Configurable retries with exponential backoff
- ✅ Checksum verification (MD5, SHA1, SHA256, SHA512)
- ✅ Temp-and-move pattern for safe downloads
- ✅ Configurable timeouts
- ✅ Quiet mode for CI environments

---

### 2. ✅ DITA-OT Plugin Installation Task
**Status**: ✅ COMPLETED (v2.4.0)
**Impact**: High
**Effort**: Medium

Built-in `DitaOtInstallPluginTask` for installing DITA-OT plugins.

**Implemented API:**
```kotlin
tasks.register<DitaOtInstallPluginTask>("installPlugins") {
    dependsOn(downloadDitaOt)
    ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-4.2.3"))
    plugins.set(listOf(
        "org.lwdita",                       // From registry
        "https://example.com/plugin.zip",   // From URL
        "/path/to/local/plugin.zip"         // Local file
    ))
    retries.set(2)    // Retry on failure
    force.set(false)  // Force reinstall
    quiet.set(false)  // Suppress output
}
```

**Features Delivered:**
- ✅ Install from registry, URL, or local files
- ✅ No need for `Exec` tasks
- ✅ Automatic dependency management
- ✅ Configurable retries with exponential backoff
- ✅ Force reinstall option
- ✅ Quiet mode for CI environments
- ✅ Cross-platform support (Windows, macOS, Linux)

---

### 3. ✅ Improved Error Messages and Diagnostics
**Status**: ✅ COMPLETED (v2.4.0)
**Impact**: High
**Effort**: Low

Enhanced error messages with actionable suggestions.

**Example:**
```
DITA-OT directory does not exist: /path/to/dita-ot

Please ensure DITA-OT is installed. You can use DitaOtDownloadTask:

tasks.register<DitaOtDownloadTask>("downloadDitaOt") {
    version.set("4.2.3")
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))
}
```

**Features Delivered:**
- ✅ Contextual error messages
- ✅ Troubleshooting suggestions
- ✅ Code examples in error output
- ✅ Clear actionable next steps

---

## ✅ P1 - High Priority (COMPLETED in v2.5.0)

### 4. ✅ DITA Validation Task
**Status**: ✅ COMPLETED (v2.5.0)
**Impact**: High
**Effort**: Medium

Validate DITA content without running full transformation.

**Implemented API:**
```kotlin
tasks.register<DitaOtValidateTask>("validateDita") {
    dependsOn(downloadDitaOt)
    ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-4.2.3"))
    input("docs/guide.ditamap")
    strictMode.set(true)  // Fail on warnings
    processingMode.set("strict")  // strict, lax, or skip
}
```

**Features Delivered:**
- ✅ Fast validation without full transformation
- ✅ Strict mode (fail on warnings)
- ✅ Configurable processing mode (strict, lax, skip)
- ✅ Configurable timeout
- ✅ Clear error messages with file locations
- ✅ Summary report with error/warning counts
- ✅ Automatic temp directory cleanup
- ✅ Cross-platform support (Windows, macOS, Linux)

---

## P1 - High Priority (Next Development Phase)

### 5. ✅ Link Checker Task
**Status**: ✅ COMPLETED (v2.7.0)
**Impact**: Medium
**Effort**: Medium

Check for broken links in DITA content.

**Implemented API:**
```kotlin
tasks.register<DitaLinkCheckTask>("checkLinks") {
    input("docs/guide.ditamap")
    checkExternal.set(true)   // Also check external URLs
    failOnBroken.set(true)    // Fail build on broken links
    recursive.set(true)       // Follow topic references
    excludeUrl("localhost")   // Exclude patterns
}
```

**Features Delivered:**
- ✅ Internal link checking (xref, conref, topicref, image)
- ✅ Optional external URL verification
- ✅ Recursive scanning of referenced files
- ✅ Configurable timeouts for external checks
- ✅ URL pattern exclusion
- ✅ Detailed results with file locations
- ✅ Cross-platform support (Windows, macOS, Linux)
- ✅ `scope="external"` attribute detection
- ✅ Keyref/conkeyref skipping (requires DITA-OT resolution)

---

## ✅ v2.8.4 Code Quality Improvements (COMPLETED)

### Code Review Fixes
**Status**: ✅ COMPLETED (v2.8.4)

Applied fixes from two rounds of code review on the DITA-OT message code detection system.

**Fixes Applied:**
- Fixed `extractLineNumber` regex bug — "line X" format never captured the number
- Removed dead fallback error extraction code in `validateFile()` — `parseOutputLine()` already captures all errors, so re-scanning was unreachable
- Removed unused `INFO_PATTERN` from `ProgressReporter` — declared but never referenced
- Moved `LINE_NUMBER_PATTERN` to companion object to avoid recompilation on every call
- Removed redundant `!INFO_PATTERN` guard in `ProgressReporter.processOutput`

**Files Fixed:**
- `DitaOtValidateTask.kt` — extractLineNumber, fallback cleanup, pattern optimization
- `ProgressReporter.kt` — dead code removal

---

## ✅ v2.8.3 Bug Fixes (COMPLETED)

### validateDita Task - Zero False Positives (DITA-OT Message Codes Only)
**Status**: ✅ COMPLETED (v2.8.3)

Replaced all generic error detection patterns with DITA-OT structured message code only detection, eliminating false positives entirely.

**Problem:** Generic patterns like `[ERROR]`, `Error:`, `Exception`, and filename-based matching (`file:`) caused false positives when DITA-OT output contained progress messages, informational text, or filenames resembling error patterns.

**Solution:** Rewrote error/warning detection to exclusively use DITA-OT's structured message code format: `[PREFIX][NUMBER][SEVERITY]`

**DITA-OT Message Code Format:**
- **Prefixes:** `DOTA` (Ant/core), `DOTJ` (Java), `DOTX` (XSLT), `INDX` (Index), `PDFJ` (PDF/Java), `PDFX` (PDF/XSL-FO), `XEPJ` (XEP)
- **Severity codes:** `E` = Error, `F` = Fatal, `W` = Warning, `I` = Info

**Patterns (regex):**
```kotlin
ERROR_PATTERN = "\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}[EF]\\]"
WARNING_PATTERN = "\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}W\\]"
INFO_PATTERN = "\\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\\d{3}I\\]"
```

**What changed:**
- Removed all generic patterns (`[ERROR]`, `Error:`, `Exception`, `file:` matching)
- Only lines containing valid DITA-OT message codes are now classified
- All other output lines are silently ignored — zero false positives
- Both `DitaOtValidateTask.kt` and `ProgressReporter.kt` updated

**Reference:** [VALIDATION_BUG.md](VALIDATION_BUG.md)

### New Unit Tests

Added 25+ tests for DITA-OT message code detection (total: 203 tests):

| Test Class | New Tests | Description |
|------------|-----------|-------------|
| `DitaOtValidateTaskTest.kt` | 25+ | All prefixes, severities, false positive rejection, integration |

---

## ✅ v2.8.2 Bug Fixes and Improvements (COMPLETED)

### Bug Fixes from PR Feedback

#### validateDita Task - DITA-OT Message Classification Fix
**Status**: ✅ COMPLETED (v2.8.2)

Fixed false error reporting where informational messages (like DOTJ031I) were incorrectly treated as errors.

**Problem:** DITA-OT messages ending with `I` (Info) were matched by the error pattern, causing validation to fail on non-error messages.

**Solution:** Updated `ERROR_PATTERN` to only match `E` (Error) and `F` (Fatal) suffixes. Added `INFO_PATTERN` to explicitly skip informational messages. (Further refined in v2.8.3 with DITA-OT message code only detection.)

**DITA-OT Message Format:** `DOT[component][number][severity]`
- `E` = Error (e.g., DOTJ012E)
- `F` = Fatal (e.g., DOTJ001F)
- `W` = Warning (e.g., DOTJ031W)
- `I` = Info (e.g., DOTJ031I) - **NOT an error**

**Files Fixed:**
- `DitaOtValidateTask.kt` - Error pattern and message parsing
- `ProgressReporter.kt` - Error detection during progress reporting

#### checkLinks Task - Peer Link Support
**Status**: ✅ COMPLETED (v2.8.2)

Fixed link checker incorrectly flagging `scope="peer"` links as broken.

**Problem:** The link checker only recognized `scope="external"` for skipping, but not `scope="peer"`. Peer links point to resources not available at build time (e.g., separately-generated API documentation).

**Solution:** Added `isPeerScope` detection and skip logic. Peer links are now counted separately in the results.

**DITA Scope Values:**
- `local` (default): Resource is part of this documentation set
- `peer`: Resource is NOT in this build but part of same information set
- `external`: Resource is external (websites, etc.)

**Files Fixed:**
- `DitaLinkCheckTask.kt` - Added isPeerScope property and handling

### New Features

#### Configurable Warning Display (showWarnings)
**Status**: ✅ COMPLETED (v2.8.2)

Added `showWarnings` property to control FOP warning verbosity during PDF transformation.

**Problem:** PDF transformations logged numerous FOP warnings by default, cluttering output.

**Solution:** Warnings are now suppressed by default but still counted. Users can enable display with `showWarnings(true)`.

**API:**
```kotlin
tasks.named<DitaOtTask>("dita") {
    showWarnings(false)  // Default - warnings counted but not displayed
    showWarnings(true)   // Show all warnings during transformation
}
```

**Files Changed:**
- `DitaOtTask.kt` - Added showWarnings property and DSL method
- `ProgressReporter.kt` - Updated warning display logic

### New Unit Tests

Added 6 new tests for v2.8.2 (total: 203 tests):

| Test Class | New Tests | Description |
|------------|-----------|-------------|
| `DitaOtValidateTaskTest.kt` | 2 | DITA-OT message classification (DOTJ031I) |
| `DitaLinkCheckTaskTest.kt` | 4 | Peer link handling (scope="peer") |
| `DitaOtPluginTest.kt` | 3 | showWarnings property |

---

## ✅ v2.8.1 Bug Fixes and Improvements (COMPLETED)

### Bug Fixes

#### Cross-Platform Path with Spaces Fix
**Status**: ✅ COMPLETED (v2.8.1)

Fixed command building for paths containing spaces on Windows. Changed from combined format (`--input=path`) to separate arguments (`--input path`) for ProcessBuilder compatibility.

**Files Fixed:**
- `AntExecutor.kt` - DITA-OT script execution
- `DitaOtValidateTask.kt` - Validation command building

#### DITA-OT Version Detection Fix
**Status**: ✅ COMPLETED (v2.8.1)

Fixed version detection that was returning "unknown". Now correctly reads version from multiple sources:
1. `plugins/org.dita.base/plugin.xml` (primary, DITA-OT 3.x+)
2. `lib/dost.jar` manifest (fallback)
3. Directory name pattern (last resort)

**Tested with:**
- DITA-OT 3.5.4 ✓
- DITA-OT 4.2.3 ✓
- DITA-OT 4.3.0 ✓

#### DitaOtValidateTask DITA-OT 4.x Compatibility
**Status**: ✅ COMPLETED (v2.8.1)

Changed validation transtype from deprecated `preprocess` to `dita` for DITA-OT 4.x compatibility.

### New Unit Tests

Added 20 new tests for v2.8.1 bug fixes (total: 199 tests):

| Test Class | New Tests | Description |
|------------|-----------|-------------|
| `DitaOtTaskSpec.kt` | 6 | Version detection from plugin.xml, manifest, directory name |
| `DitaOtValidateTaskTest.kt` | 2 | DITA-OT 4.x transtype compatibility |
| `AntExecutorTest.kt` | 7 | Command building with paths containing spaces |

---

### 6. Incremental Build Support
**Status**: 🔜 Next Up
**Impact**: Very High
**Effort**: High

Only rebuild changed topics and their dependents.

**Current behavior:** Full rebuild on any change
**Proposed:** Track topic dependencies, rebuild only affected outputs

**Benefits:**
- Dramatically faster builds for large documentation sets
- Better developer experience with `--continuous` mode
- Reduced CI/CD build times

**Challenges:**
- DITA-OT doesn't natively support incremental builds
- Need to parse DITA maps for dependency tracking

---

### 7. ✅ Gradle Wrapper in Examples
**Status**: ✅ COMPLETED (v2.6.0)
**Impact**: Medium
**Effort**: Low

Added Gradle wrapper to all standalone example projects for self-contained execution.

**Completed examples:**
- ✅ `simple/`
- ✅ `multi-project/`
- ✅ `configuration-cache/`
- ✅ `custom-plugin-dev/`
- ✅ `ci-cd-publishing/`
- ✅ `multi-language/`
- ✅ `version-docs/`
- ✅ `plugin-test/`
- ✅ `gradle-plugin-self-documentation/`

**Also updated:**
- All examples now use built-in `DitaOtDownloadTask` (no external `de.undercouch.download` plugin)
- All examples use `includeBuild` for local plugin development
- Fixed `output()` method calls to use String paths

---

## P1 - High Priority (Next Development Phase)

### 8. Watch Mode Improvements
**Status**: Planned
**Impact**: Medium
**Effort**: Medium

Enhanced continuous build experience.

**Features:**
- Browser auto-refresh for HTML output
- Build notifications (system tray)
- Selective rebuilds based on changed files

**Proposed API:**
```kotlin
tasks.named<DitaOtTask>("dita") {
    watch {
        autoRefresh = true
        notifyOnComplete = true
    }
}
```

---

### 9. Multi-Format Single Task
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Generate multiple output formats in a single task.

**Current:** Requires multiple tasks
```kotlin
tasks.register<DitaOtTask>("html") { transtype("html5") }
tasks.register<DitaOtTask>("pdf") { transtype("pdf") }
```

**Proposed:**
```kotlin
tasks.register<DitaOtTask>("docs") {
    transtype("html5", "pdf", "markdown")
    output(layout.buildDirectory.dir("docs"))
    // Creates: docs/html5/, docs/pdf/, docs/markdown/
}
```

---

### 10. Kotlin DSL Extensions
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Cleaner Kotlin DSL syntax with extension functions.

**Proposed:**
```kotlin
dita {
    ditaOtVersion = "4.2.3"

    transform("html") {
        from("docs/guide.ditamap")
        to(layout.buildDirectory.dir("html"))
        format = "html5"
    }

    transform("pdf") {
        from("docs/guide.ditamap")
        to(layout.buildDirectory.dir("pdf"))
        format = "pdf"
    }
}
```

---

### 11. Progress Reporting
**Status**: ✅ COMPLETED (v2.8.0)
**Impact**: Medium
**Effort**: Low

Visual progress indicators during DITA-OT transformations.

**Implemented API:**
```kotlin
tasks.named<DitaOtTask>("dita") {
    showProgress(true)            // Enable progress (default)
    progressStyle("DETAILED")     // DETAILED, SIMPLE, MINIMAL, QUIET
}
```

**Example Output:**
```
> Task :dita
  [=====>                        ] 18% - Preprocessing (12 files)
  [===============>              ] 52% - Resolving content references (24 files)
  [=========================>    ] 85% - Generating content (34 files)
  [==============================] 100% - Complete (45 files, 3.2s)
```

**Features Delivered:**
- ✅ Visual progress bar with percentage
- ✅ Stage detection from DITA-OT output (20 stages)
- ✅ File count tracking
- ✅ Configurable display styles (DETAILED, SIMPLE, MINIMAL, QUIET)
- ✅ Error and warning collection with summary
- ✅ Duration reporting
- ✅ Terminal-aware output (in-place updates when supported)

---

### 12. DITA-OT Version Management
**Status**: Planned
**Impact**: Medium
**Effort**: Medium

Support multiple DITA-OT versions in the same project.

**Use case:** Testing documentation with different DITA-OT versions

**Proposed API:**
```kotlin
ditaOt {
    versions {
        register("3.6") { version = "3.6" }
        register("4.2") { version = "4.2.3" }
    }
}

tasks.register<DitaOtTask>("testWith36") {
    ditaOtVersion = "3.6"
}
```

---

## P3 - Low Priority

### 13. Preview Server
**Status**: Exploration
**Impact**: Medium
**Effort**: Medium

Built-in HTTP server for previewing HTML output.

**Proposed API:**
```kotlin
tasks.register<DitaPreviewTask>("preview") {
    dependsOn("dita")
    outputDir = layout.buildDirectory.dir("html")
    port = 8080
    openBrowser = true
}
```

---

### 14. Documentation Metrics
**Status**: Exploration
**Impact**: Low
**Effort**: Low

Generate metrics about documentation.

**Output:**
```
Documentation Metrics:
  Topics: 45
  Words: 12,345
  Images: 23
  Tables: 8
  Code blocks: 34
  Reading time: ~45 minutes
```

---

### 15. Docker-based Execution
**Status**: Exploration
**Impact**: Low
**Effort**: High

Run DITA-OT in Docker container for isolation.

**Benefits:**
- No local DITA-OT installation required
- Consistent builds across environments
- Easy CI/CD integration

**Proposed API:**
```kotlin
ditaOt {
    useDocker = true
    dockerImage = "ghcr.io/dita-ot/dita-ot:4.2.3"
}
```

---

### 16. Integration with Documentation Platforms
**Status**: Exploration
**Impact**: Low
**Effort**: High

Direct publishing to platforms.

**Targets:**
- GitHub Pages
- Confluence
- ReadTheDocs
- Netlify

**Proposed API:**
```kotlin
tasks.register<DitaPublishTask>("publish") {
    dependsOn("dita")

    to {
        githubPages {
            repository = "user/repo"
            branch = "gh-pages"
        }
    }
}
```

---

## P4 - Future Exploration

### 17. Gradle Build Cache Integration
**Status**: Research
**Impact**: High
**Effort**: High

Enable Gradle's build cache for DITA transformations.

**Challenge:** DITA-OT output determinism

---

### 18. IDE Plugins
**Status**: Idea
**Impact**: Medium
**Effort**: Very High

IDE integration for IntelliJ IDEA and VS Code.

**Features:**
- Run transformations from IDE
- Preview output in IDE
- DITA content assist

---

### 19. AI-Assisted Documentation
**Status**: Idea
**Impact**: Unknown
**Effort**: Unknown

Integration with AI tools for documentation.

**Potential features:**
- Content suggestions
- Translation assistance
- Quality analysis

---

### 20. Gradle 10 Preparation
**Status**: Monitoring
**Impact**: Critical (when released)
**Effort**: Unknown

Ensure compatibility with Gradle 10 when released.

**Actions:**
- Monitor Gradle 10 development
- Test with early access versions
- Update deprecated APIs

---

## Completed Features

| Feature | Version | Date |
|---------|---------|------|
| **Minimum Gradle 8.14.4, Kotlin 2.4.21 toolchain** | 2.8.7 | Nov 2026 |
| **DITA-OT 4.4.2 tested with real CI transformations (Linux + Windows)** | 2.8.7 | Nov 2026 |
| **Dependabot for GitHub Actions and Gradle** | 2.8.7 | Nov 2026 |
| **Zero compiler / Gradle deprecation warnings** | 2.8.7 | Nov 2026 |
| **Configurable DITA-OT Process Timeout (`ditaProcessTimeoutMinutes`)** | 2.8.6 | Feb 2026 |
| **DitaOtInstallPluginTask DITA-OT 4.4 Compatibility** | 2.8.6 | Feb 2026 |
| **ProgressReporter Duplicate Progress Lines Fix** | 2.8.6 | Feb 2026 |
| **AntExecutor Thread Synchronization Fix** | 2.8.6 | Feb 2026 |
| **Code Review Fixes (extractLineNumber, dead code removal)** | 2.8.4 | Feb 2026 |
| **validateDita Zero False Positives (DITA-OT message codes only)** | 2.8.3 | Jan 2026 |
| **Configurable Warning Display (showWarnings)** | 2.8.2 | Jan 2026 |
| **checkLinks Peer Link Support** | 2.8.2 | Jan 2026 |
| **validateDita Message Classification Fix** | 2.8.2 | Jan 2026 |
| **Unit Tests for Bug Fixes** | 2.8.1 | Jan 2026 |
| **Cross-Platform Path with Spaces Fix** | 2.8.1 | Jan 2026 |
| **DITA-OT Version Detection Fix** | 2.8.1 | Jan 2026 |
| **DitaOtValidateTask DITA-OT 4.x Compatibility** | 2.8.1 | Jan 2026 |
| **Progress Reporting** | 2.8.0 | Jan 2026 |
| **Thread Safety Improvements** | 2.8.0 | Jan 2026 |
| **Provider<Directory> DSL Fix** | 2.8.0 | Jan 2026 |
| **Windows Path Length Documentation** | 2.8.0 | Jan 2026 |
| **DitaLinkCheckTask** | 2.7.0 | Jan 2026 |
| **Gradle Wrapper in Examples** | 2.6.0 | Jan 2026 |
| **DitaOtValidateTask** | 2.5.0 | Jan 2026 |
| **DitaOtDownloadTask** | 2.4.0 | Jan 2026 |
| **DitaOtInstallPluginTask** | 2.4.0 | Jan 2026 |
| **Improved Error Messages** | 2.4.0 | Jan 2026 |
| Configuration Cache Support | 2.3.0 | Nov 2025 |
| Groovy Closure Properties Fix | 2.3.1 | Dec 2025 |
| Implicit Dependency Fix | 2.3.2 | Jan 2026 |
| Multi-language Example | 2.3.2 | Jan 2026 |
| Version Documentation Example | 2.3.2 | Jan 2026 |

---

## Next Development Steps

Based on the roadmap, the recommended next development steps are:

### Immediate (v2.9.0)
1. **Multi-Format Single Task** - Generate multiple formats in one task
2. **Kotlin DSL Extensions** - Cleaner API

### Short-term (v2.10.0)
3. **Watch Mode Improvements** - Enhanced continuous build experience
4. **DITA-OT Version Management** - Support multiple versions

### Medium-term (v3.0.0)
5. **Incremental Build Support** - Major performance improvement
6. **DITA-OT Version Management** - Support multiple versions

---

## Contributing

We welcome contributions! If you'd like to work on any of these features:

1. Open an issue to discuss the approach
2. Fork the repository
3. Create a feature branch
4. Submit a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed guidelines.

---

## Feedback

Have a feature request not listed here? Please:

1. [Open an issue](https://github.com/jyjeanne/dita-ot-gradle/issues/new)
2. Describe the use case
3. Propose a solution (optional)

Your feedback helps prioritize the roadmap!
