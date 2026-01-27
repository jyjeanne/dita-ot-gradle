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

## ✅ v2.8.3 Bug Fixes (COMPLETED)

### validateDita Task - Progress Message False Positives Fix
**Status**: ✅ COMPLETED (v2.8.3)

Fixed false error reporting where DITA-OT progress messages ("Processing file:", "Writing file:", etc.) were incorrectly treated as errors.

**Problem:** Progress messages containing `file:` were matched by the error pattern, causing validation to fail with false positives like:
```
ERROR [/path/file.xml]: Processing file:/path/input.xml to file:/path/output.xml
ERROR [/path/file.xml]: Writing file:/path/output.xml
```

**Solution:** Added `PROGRESS_PATTERN` to filter out informational progress messages before error classification.

**Patterns Filtered:**
- `Processing file:` - file processing progress
- `Writing file:` - file output progress
- `Loading file:` - file loading progress
- `Transforming file:` - transformation progress
- `Copying ... to ...` - file copy operations

**Files Fixed:**
- `DitaOtValidateTask.kt` - Added PROGRESS_PATTERN and filtering logic

**Reference:** [VALIDATION_BUG.md](VALIDATION_BUG.md)

### New Unit Tests

Added 5 new tests for v2.8.3 (total: 208 tests):

| Test Class | New Tests | Description |
|------------|-----------|-------------|
| `DitaOtValidateTaskTest.kt` | 5 | Progress message filtering |

---

## ✅ v2.8.2 Bug Fixes and Improvements (COMPLETED)

### Bug Fixes from PR Feedback

#### validateDita Task - DITA-OT Message Classification Fix
**Status**: ✅ COMPLETED (v2.8.2)

Fixed false error reporting where informational messages (like DOTJ031I) were incorrectly treated as errors.

**Problem:** DITA-OT messages ending with `I` (Info) were matched by the error pattern, causing validation to fail on non-error messages.

**Solution:** Updated `ERROR_PATTERN` to only match `E` (Error) and `F` (Fatal) suffixes. Added `INFO_PATTERN` to explicitly skip informational messages.

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
| **validateDita Progress Message False Positives Fix** | 2.8.3 | Jan 2026 |
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
