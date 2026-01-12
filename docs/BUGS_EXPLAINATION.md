# DITA-OT Gradle Plugin - Implicit Task Dependencies Bug

**Plugin**: `io.github.jyjeanne.dita-ot-gradle`
**Version**: 2.3.1
**Date**: January 5, 2026
**Status**: Under Investigation

---

## Bug Summary

When running multiple `DitaOtTask` instances (e.g., `html` and `pdf`) in the DITA-OT distribution build, Gradle reports errors about implicit dependencies between tasks that should be independent.

### Symptoms

- Tasks run fine **independently** (`./gradlew html` or `./gradlew pdf`)
- Tasks run fine from the **standalone docs repository**
- Errors occur when running from the **DITA-OT distribution build** with multiple tasks
- Gradle infers implicit dependencies between `html` and `pdf` tasks

### Error Context

```
> Task :docs:html
> Task :docs:pdf FAILED

FAILURE: Build failed with exception.
* What went wrong:
Gradle detected a problem with the following location: '<dita-ot>/doc'.

Reason: Task ':docs:pdf' uses this output of task ':docs:html' without declaring an explicit or implicit dependency.
```

---

## Root Cause Analysis

### The Problem: `@InputDirectory` on `ditaOtDir`

In `DitaOtTask.kt`, the DITA-OT installation directory is declared as:

```kotlin
@get:InputDirectory
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val ditaOtDir: DirectoryProperty
```

This annotation causes Gradle to:

1. **Track the entire directory** as task input for up-to-date checks
2. **Monitor all files** within the directory for changes
3. **Infer implicit dependencies** between tasks sharing the same input directory

### Why It Manifests in Distribution Builds

In a typical DITA-OT distribution structure:

```
dita-ot-4.x/
├── bin/
├── config/
├── doc/              ← OUTPUT directory (inside ditaOtDir!)
├── docsrc/           ← Input source files
├── lib/
├── plugins/
└── build.xml
```

When multiple tasks are configured:

```gradle
task html(type: DitaOtTask) {
    ditaOt file(ditaHome)      // Points to dita-ot-4.x/
    output "${ditaHome}/doc"   // Writes INSIDE ditaOtDir
}

task pdf(type: DitaOtTask) {
    ditaOt file(ditaHome)      // Same ditaOtDir as html
    output "${ditaHome}/doc"   // Same output location
}
```

**Gradle's perspective:**
- Both tasks declare `ditaOtDir` as input (due to `@InputDirectory`)
- Task `html` writes to `doc/` which is inside `ditaOtDir`
- Task `pdf` also uses `ditaOtDir` as input
- Gradle infers: "pdf depends on html because html modifies pdf's input"

### Why This is Incorrect

The DITA-OT installation directory is a **tool**, not a **data input**:

| Concept | Example | Should be Task Input? |
|---------|---------|----------------------|
| Tool/Runtime | DITA-OT, JDK, Node.js | No (`@Internal`) |
| Data Input | `.ditamap`, `.dita`, `.ditaval` | Yes (`@InputFiles`) |

You don't declare `JAVA_HOME` as a task input when compiling Java code - the same principle applies here.

### Additional Issue: `getInputFileTree()` Method

The `getInputFileTree()` method compounds the problem:

```kotlin
@InputFiles
@PathSensitive(PathSensitivity.RELATIVE)
fun getInputFileTree(): FileCollection {
    // In dev mode, includes entire ditaOtDir
    if (devMode.get()) {
        // Adds ditaOtDir excluding only temp files
    }
    // Also adds parent directories of input files
}
```

This can inadvertently include output directories or shared paths as inputs.

---

## Solution Options

### Solution 1: Change `ditaOtDir` to `@Internal` (Recommended)

**Minimal change, maximum impact.**

```kotlin
// BEFORE (problematic):
@get:InputDirectory
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val ditaOtDir: DirectoryProperty

// AFTER (fixed):
@get:Internal
abstract val ditaOtDir: DirectoryProperty
```

**Pros:**
- Simple one-line change
- Fixes the implicit dependency issue immediately
- DITA-OT directory is correctly treated as a tool, not input data

**Cons:**
- Loses up-to-date checking when DITA-OT itself changes
- Tasks won't re-run automatically if plugins are modified

**Mitigation:** Users who need up-to-date checks on DITA-OT changes can use `devMode(true)` or manually invalidate the build.

---

### Solution 2: Selective Input Tracking

Track only specific subdirectories/files that matter for transformation.

```kotlin
@get:Internal
abstract val ditaOtDir: DirectoryProperty

@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
val ditaOtPlugins: FileCollection
    get() = objectFactory.fileCollection().from(
        ditaOtDir.map { it.dir("plugins") }
    )

@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
val ditaOtConfig: FileCollection
    get() = objectFactory.fileCollection().from(
        ditaOtDir.map { it.file("config/configuration.properties") },
        ditaOtDir.map { it.file("config/messages.xml") }
    )
```

**Pros:**
- Maintains useful up-to-date checking
- Only tracks files that actually affect transformation
- Avoids output directory conflicts

**Cons:**
- More complex implementation
- Need to identify all relevant input files

---

### Solution 3: Exclude Output Directory from Input Tracking

If keeping `@InputDirectory`, explicitly exclude the output path.

```kotlin
@get:InputDirectory
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val ditaOtDir: DirectoryProperty

@InputFiles
@PathSensitive(PathSensitivity.RELATIVE)
fun getFilteredDitaOtDir(): FileTree {
    return ditaOtDir.asFileTree.matching {
        exclude("**/doc/**")      // Exclude common output dirs
        exclude("**/out/**")
        exclude("**/build/**")
        exclude("**/.gradle/**")
        exclude("**/temp/**")
    }
}
```

**Pros:**
- Keeps most up-to-date functionality
- Prevents output directory conflicts

**Cons:**
- Hardcoded exclusions may not cover all cases
- Users might use custom output directories

---

### Solution 4: Use `@NormalizeLineEndings` and Path Normalization

Ensure paths are normalized to prevent false matches.

```kotlin
@get:Internal
abstract val ditaOtDir: DirectoryProperty

// Normalized path for comparison
private val normalizedDitaOtPath: Provider<String>
    get() = ditaOtDir.map { it.asFile.canonicalPath }

// Check if output is inside ditaOtDir
private fun isOutputInsideDitaOt(): Boolean {
    val ditaPath = normalizedDitaOtPath.get()
    val outPath = outputDir.get().asFile.canonicalPath
    return outPath.startsWith(ditaPath)
}
```

---

### Solution 5: Add `inputTracking` Configuration Option

Give users control over input tracking behavior.

```kotlin
enum class InputTrackingMode {
    NONE,           // @Internal - no tracking
    MINIMAL,        // Only track config and plugins
    FULL            // Track entire ditaOtDir (current behavior)
}

@get:Input
@get:Optional
abstract val inputTrackingMode: Property<InputTrackingMode>

init {
    inputTrackingMode.convention(InputTrackingMode.MINIMAL)
}
```

**Usage:**
```gradle
task html(type: DitaOtTask) {
    inputTrackingMode = InputTrackingMode.NONE  // Disable for distribution builds
}
```

---

## Recommended Implementation

### Phase 1: Quick Fix (v2.3.2)

Change `ditaOtDir` to `@Internal`:

```kotlin
// In DitaOtTask.kt

/**
 * The DITA-OT installation directory.
 * Marked as @Internal because it's a tool, not input data.
 * This prevents implicit dependency inference when multiple tasks
 * share the same DITA-OT installation.
 */
@get:Internal
abstract val ditaOtDir: DirectoryProperty
```

### Phase 2: Enhanced Solution (v2.4.0)

Add selective input tracking with user configuration:

```kotlin
/**
 * Controls how DITA-OT directory contents are tracked for up-to-date checks.
 * - NONE: No tracking (safest for distribution builds)
 * - MINIMAL: Track only plugins and config (recommended)
 * - FULL: Track entire directory (legacy behavior, may cause issues)
 */
@get:Input
@get:Optional
abstract val ditaOtInputTracking: Property<String>

init {
    ditaOtInputTracking.convention("MINIMAL")
}

@InputFiles
@PathSensitive(PathSensitivity.RELATIVE)
@Optional
fun getTrackedDitaOtFiles(): FileCollection? {
    return when (ditaOtInputTracking.get()) {
        "NONE" -> null
        "MINIMAL" -> objectFactory.fileCollection().from(
            ditaOtDir.map { it.dir("plugins") },
            ditaOtDir.map { it.dir("config") }
        )
        "FULL" -> ditaOtDir.asFileTree.matching {
            exclude("**/doc/**", "**/out/**", "**/build/**", "**/temp/**")
        }
        else -> null
    }
}
```

---

## Test Cases to Add

```kotlin
class ImplicitDependencySpec : Specification {

    def "multiple tasks with same ditaOtDir should not have implicit dependencies"() {
        given:
        buildFile << """
            task html(type: DitaOtTask) {
                ditaOt file('${ditaOtDir}')
                input file('test.ditamap')
                output file('out/html')
                transtype 'html5'
            }

            task pdf(type: DitaOtTask) {
                ditaOt file('${ditaOtDir}')
                input file('test.ditamap')
                output file('out/pdf')
                transtype 'pdf'
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments('html', 'pdf', '--parallel')
            .build()

        then:
        result.task(':html').outcome == SUCCESS
        result.task(':pdf').outcome == SUCCESS
        // No implicit dependency errors
    }

    def "output inside ditaOtDir should not cause dependency errors"() {
        given:
        buildFile << """
            task html(type: DitaOtTask) {
                ditaOt file('${ditaOtDir}')
                input file('docsrc/test.ditamap')
                output file('${ditaOtDir}/doc')  // Output inside ditaOtDir
                transtype 'html5'
            }

            task pdf(type: DitaOtTask) {
                ditaOt file('${ditaOtDir}')
                input file('docsrc/test.ditamap')
                output file('${ditaOtDir}/doc')
                transtype 'pdf'
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments('html', 'pdf')
            .build()

        then:
        noExceptionThrown()
    }
}
```

---

## Workaround for Consumers (Until Fix is Released)

Users can add this to their `build.gradle` as a temporary workaround:

```gradle
tasks.withType(DitaOtTask).configureEach {
    // Disable up-to-date checks to avoid implicit dependency errors
    outputs.upToDateWhen { false }
}
```

Or for specific tasks:

```gradle
task html(type: DitaOtTask) {
    // ... configuration ...

    // Workaround: disable caching
    outputs.upToDateWhen { false }
}
```

---

## References

- [Gradle Incremental Build](https://docs.gradle.org/current/userguide/incremental_build.html)
- [Gradle Task Inputs and Outputs](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:task_inputs_outputs)
- [Implicit Dependencies](https://docs.gradle.org/current/userguide/validation_problems.html#implicit_dependency)
- [dita-ot-gradle Repository](https://github.com/jyjeanne/dita-ot-gradle)

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| 2026-01-05 | Analysis | Initial bug report and solution analysis |
