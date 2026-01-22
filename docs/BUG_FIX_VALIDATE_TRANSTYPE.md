# Bug Fix: DitaOtValidateTask Uses Invalid Transtype 'preprocess'

## Issue Description

**Affected File:** `src/main/kotlin/com/github/jyjeanne/DitaOtValidateTask.kt`
**Severity:** High - Validation task fails completely on DITA-OT 4.x

### Symptom

When running the `validateDita` task with DITA-OT 4.x, validation fails with:

```
[DOTA001F] 'preprocess' is not a recognized transformation type.
Supported transformation types are dita, eclipsehelp, html5, htmlhelp,
markdown, markdown_gitbook, markdown_github, pdf, pdf2, xhtml.
```

### Root Cause

The `DitaOtValidateTask` internally uses `preprocess` as the transformation type:

```kotlin
// Likely in DitaOtValidateTask.kt
transtype = "preprocess"
```

**Problem:** The `preprocess` transtype was removed or replaced in DITA-OT 3.x+. Modern DITA-OT versions no longer expose `preprocess` as a standalone transformation type.

---

## DITA-OT Version History

| DITA-OT Version | `preprocess` Transtype |
|-----------------|------------------------|
| 2.x and earlier | Available |
| 3.x | Deprecated/Internal only |
| 4.x | Removed from public transtypes |

---

## Solution Options

### Option 1: Use `dita` Transtype (Recommended)

The `dita` transtype performs preprocessing and outputs normalized DITA files, which effectively validates the content.

```kotlin
// In DitaOtValidateTask.kt
private fun buildCommand(): List<String> {
    val command = mutableListOf<String>()
    // ...
    command.add("--format")
    command.add("dita")  // Changed from "preprocess"
    // ...
}
```

**Pros:**
- Works with DITA-OT 3.x and 4.x
- Performs full preprocessing including conref resolution
- Generates normalized output (useful for debugging)

**Cons:**
- Generates output files (can be cleaned up)
- Slightly slower than preprocess-only

### Option 2: Use HTML5 with Temp-Only Mode

Use `html5` transtype but configure to stop early or use temp directory analysis.

```kotlin
command.add("--format")
command.add("html5")
command.add("-Dclean.temp=no")  // Keep temp files for inspection
```

### Option 3: Parse Preprocessing Errors from Log

Run any transtype and parse the log output for validation errors during preprocessing phase. Most validation errors occur during preprocessing regardless of output format.

---

## Recommended Fix Implementation

### Changes to DitaOtValidateTask.kt

```kotlin
/**
 * Validates DITA content by running DITA-OT preprocessing.
 * Uses 'dita' transtype for DITA-OT 3.x+ compatibility.
 */
@TaskAction
fun validate() {
    val command = mutableListOf<String>()

    // Add DITA executable
    command.add(getDitaScript().absolutePath)

    // Use 'dita' transtype for validation (compatible with DITA-OT 3.x+)
    command.add("--format")
    command.add("dita")

    command.add("--input")
    command.add(inputFile.get().asFile.absolutePath)

    // Output to temp directory (will be cleaned)
    val validateOutputDir = temporaryDir.resolve("validate-output")
    command.add("--output")
    command.add(validateOutputDir.absolutePath)

    // Enable strict processing for validation
    if (strictMode.get()) {
        command.add("--processing-mode")
        command.add("strict")
    }

    // Run validation
    val result = executeCommand(command)

    // Clean up output (we only care about errors)
    validateOutputDir.deleteRecursively()

    // Process results...
}
```

### Alternative: Add Transtype Property

Allow users to specify validation transtype:

```kotlin
@get:Input
@get:Optional
abstract val validationTranstype: Property<String>

init {
    validationTranstype.convention("dita")  // Default to 'dita' for DITA-OT 4.x compatibility
}
```

Usage in build.gradle.kts:

```kotlin
tasks.register<com.github.jyjeanne.DitaOtValidateTask>("validateDita") {
    ditaOtDir.set(file("dita-ot/dita-ot-4.2.3"))
    input("src/dita/user-guide.bookmap")
    validationTranstype.set("dita")  // or "html5" for faster validation
}
```

---

## Workaround for Users

Until the plugin is fixed, users can validate by running a standard transformation with strict mode:

```kotlin
// In build.gradle.kts
tasks.register<com.github.jyjeanne.DitaOtTask>("validateDita") {
    description = "Validate DITA content"
    group = "documentation"
    dependsOn("downloadDitaOt")
    ditaOt(file("dita-ot/dita-ot-$ditaOtVersion"))
    input("src/dita/user-guide.bookmap")
    output("build/validate-temp")
    transtype("dita")  // Outputs normalized DITA - validates without full transformation
    properties {
        property("processing-mode", "strict")
    }
}

// Clean up validation output
tasks.register<Delete>("cleanValidation") {
    delete("build/validate-temp")
}
```

---

## Testing

### Test Cases

```kotlin
@Test
fun `should validate using dita transtype on DITA-OT 4x`() {
    // Setup DITA-OT 4.x
    val task = project.tasks.create("validate", DitaOtValidateTask::class.java)
    task.ditaOtDir.set(ditaOt4Dir)
    task.input("test.ditamap")

    // Should not throw "preprocess not recognized"
    assertDoesNotThrow { task.validate() }
}

@Test
fun `should report validation errors correctly`() {
    // Create DITA file with intentional error
    val invalidDita = tempDir.resolve("invalid.dita")
    invalidDita.writeText("""
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "topic.dtd">
        <topic id="test">
          <title>Test</title>
          <body>
            <xref href="nonexistent.dita"/>
          </body>
        </topic>
    """.trimIndent())

    // Validate should fail with meaningful error
    val result = task.validate()
    assertTrue(result.hasErrors)
    assertTrue(result.errors.any { it.contains("nonexistent.dita") })
}
```

---

## References

- [DITA-OT 4.x Transformations](https://www.dita-ot.org/dev/topics/dita-command-arguments.html)
- [DITA-OT Processing Modes](https://www.dita-ot.org/dev/parameters/parameters-base.html)
- [DITA Transtype](https://www.dita-ot.org/dev/topics/output-formats.html)

---

## Changelog

| Version | Date | Description |
|---------|------|-------------|
| 2.8.1 | Jan 2026 | ✅ Fix: Use 'dita' transtype for validation (DITA-OT 4.x compatibility) |
