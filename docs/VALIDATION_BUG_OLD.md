# DitaOtValidateTask Bug Report

## Status: FIXED in v2.8.3

## Issue

The `DitaOtValidateTask` in `dita-ot-gradle` plugin v2.8.2 reports false positive errors during DITA validation.

**Related PR:** https://github.com/dita-ot/docs/pull/660#discussion_r2729465235

## Symptoms

```
Execution failed for task ':validateDita'.
> DITA validation failed: 55 error(s), 878 warning(s)
```

The task fails even though the native `dita validate` subcommand reports no errors on the same content.

## Root Cause Analysis

The error detection logic in `DitaOtValidateTask` is too broad. It incorrectly classifies **informational messages** as errors.

### False Positives (incorrectly treated as errors)

| Message Pattern | Actual Meaning | Current Classification |
|-----------------|----------------|------------------------|
| `Processing file:/path/to/input.xml to file:/path/to/output.xml` | DITA-OT is processing a file | ERROR (wrong) |
| `Writing file:/path/to/output.xml` | DITA-OT is writing output | ERROR (wrong) |

### Example Output

```
? ERROR [/path/to/topics/error-messages.xml]: Processing file:/path/to/topics/error-messages.xml to file:/temp/output.xml
? ERROR [/temp/validate-123/topics/file.xml]: Writing file:/temp/validate-123/topics/file.xml
```

These are **informational messages** from DITA-OT showing processing progress, not actual errors.

### Legitimate Errors (correctly detected)

```
? ERROR [userguide-book.ditamap]: Error: File file:/path/to/missing-file.md was not found.
```

### Legitimate Warnings (correctly detected)

```
? WARN [source-files.dita]: Warning: [DOTX023W]: Unable to retrieve navtitle from target
? WARN [source-files.dita]: Warning: [DOTX027W]: Unable to retrieve linktext from target
```

## Proposed Solution

### Current Error Detection (v2.8.2)

The current regex likely matches any line containing file paths or "Processing"/"Writing" keywords.

### Fixed Error Detection (v2.8.3)

Update the error detection logic to:

1. **Only match actual error messages:**
   ```kotlin
   // Error patterns - must contain explicit error indicators
   val errorPatterns = listOf(
       Regex("""^\[ERROR\]"""),
       Regex("""Error:"""),
       Regex("""^\[FATAL\]"""),
       Regex("""Fatal:"""),
       Regex("""\[DOT[A-Z]\d+E\]""")  // DITA-OT error codes ending in 'E'
   )
   ```

2. **Exclude informational messages:**
   ```kotlin
   // Informational patterns - should be ignored or logged as INFO
   val infoPatterns = listOf(
       Regex("""^Processing file:"""),
       Regex("""^Writing file:"""),
       Regex("""Processing .+ to file:""")
   )
   ```

3. **Filter logic:**
   ```kotlin
   fun classifyMessage(line: String): MessageType {
       // First check if it's informational (exclude from errors)
       if (infoPatterns.any { it.containsMatchIn(line) }) {
           return MessageType.INFO
       }

       // Then check for actual errors
       if (errorPatterns.any { it.containsMatchIn(line) }) {
           return MessageType.ERROR
       }

       // Check for warnings
       if (warningPatterns.any { it.containsMatchIn(line) }) {
           return MessageType.WARNING
       }

       return MessageType.INFO
   }
   ```

## Implementation Details

### File to Modify

`src/main/kotlin/com/github/jyjeanne/DitaOtValidateTask.kt`

### Key Changes

1. Add `infoPatterns` list to identify informational messages
2. Update `classifyMessage()` or equivalent function to filter out info messages
3. Only count lines matching `errorPatterns` as actual errors
4. Add unit tests for edge cases

### Test Cases

```kotlin
@Test
fun `should not treat Processing file as error`() {
    val line = "Processing file:/path/input.xml to file:/path/output.xml"
    assertThat(classifyMessage(line)).isEqualTo(MessageType.INFO)
}

@Test
fun `should not treat Writing file as error`() {
    val line = "Writing file:/path/output.xml"
    assertThat(classifyMessage(line)).isEqualTo(MessageType.INFO)
}

@Test
fun `should detect actual Error message`() {
    val line = "Error: File file:/path/missing.md was not found."
    assertThat(classifyMessage(line)).isEqualTo(MessageType.ERROR)
}

@Test
fun `should detect DITA-OT error code`() {
    val line = "[DOTX001E]: Some error message"
    assertThat(classifyMessage(line)).isEqualTo(MessageType.ERROR)
}
```

## Fix Applied (v2.8.3)

Added `PROGRESS_PATTERN` to filter out informational progress messages:

```kotlin
/**
 * Pattern for DITA-OT progress messages.
 * These are informational messages about file processing, not errors.
 * Examples:
 * - "Processing file:/path/input.xml to file:/path/output.xml"
 * - "Writing file:/path/output.xml"
 * - "Loading file:/path/input.xml"
 * - "Transforming file:/path/input.xml"
 * - "Copying file:/path/file.ext to /path/dest/"
 */
private val PROGRESS_PATTERN = Pattern.compile(
    "^\\s*(?:Processing|Writing|Loading|Transforming|Copying)\\s+file:|" +
    "Processing\\s+.+\\s+to\\s+file:|" +
    "Copying\\s+.+\\s+to\\s+",
    Pattern.CASE_INSENSITIVE
)
```

The fix filters these messages in:
1. `parseOutputLine()` - skips progress messages before error classification
2. Fallback error extraction - excludes progress messages from error line collection

### DITA-OT Message Classification Reference

Based on analysis of [DITA-OT source code](https://github.com/dita-ot/dita-ot):

**Message ID Format:** `[DOT{prefix}{number}{severity}]`
- Prefixes: DOTA (Ant/core), DOTJ (Java), DOTX (XSLT)
- Severity: F (Fatal), E (Error), W (Warning), I (Info)

**INFO Messages (NOT errors):**
- DOTJ007I, DOTJ018I, DOTJ029I, DOTJ030I, DOTJ031I, DOTJ045I, DOTJ047I, DOTJ048I
- DOTX003I, DOTX004I, DOTX018I, DOTX029I, DOTX042I, DOTX043I, DOTX062I, DOTX072I, DOTX073I

## References

- PR Discussion: https://github.com/dita-ot/docs/pull/660#discussion_r2729465235
- Plugin Repository: https://github.com/jyjeanne/dita-ot-gradle
- DITA-OT Error Messages: https://www.dita-ot.org/dev/topics/error-messages.html
- DITA-OT Source Code: https://github.com/dita-ot/dita-ot
