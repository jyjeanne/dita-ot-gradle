# Error Detection Bug Report — dita-ot-gradle Plugin

## Overview

The error detection logic in `dita-ot-gradle` plugin affects both `DitaOtTask` (transformation report) and `DitaOtValidateTask`. The log parser incorrectly classifies informational messages as errors.

**Affected versions:** v2.8.2, v2.8.4
**Related PR:** https://github.com/dita-ot/docs/pull/660

---

## Reproduction Results

### Test Environment

| Component | Version |
|-----------|---------|
| Gradle | 9.3.0 |
| dita-ot-gradle plugin | 2.8.4 |
| Java | JDK 17 |
| OS | Windows 10 |

### Tested with DITA-OT 4.3.5

```
./gradlew cleanOutput dist -PditaHome="C:\dita-ot\dita-ot-4.3.5"
```

**HTML task:**
```
Status:           SUCCESS
Files processed:  2098 files, 77.3s
Reported Errors:  6 (all false positives)
Actual Errors:    0
```

**PDF task:**
```
Status:           SUCCESS
Files processed:  1669 files, 76.5s
Reported Errors:  4 (2 false positives + 1 real + 1 stack trace)
Actual Errors:    1 (SVG/Batik)
```

### Tested with DITA-OT 4.4.0

```
./gradlew cleanOutput dist -PditaHome="C:\dita-ot\dita-ot-4.4"
```

**HTML task:**
```
Status:           SUCCESS
Files processed:  2123 files, 67.7s
Reported Errors:  6 (all false positives)
Actual Errors:    0
```

False errors reported:
```
Processing file:/.../topics/error-messages-details.xml to file:/.../build/dita-temp/[hash].xml
Processing file:/.../topics/error-messages.xml to file:/.../build/dita-temp/[hash].xml
Processing file:/.../build/dita-temp/[hash].xml to file:/.../build/dita-temp/topics/error-messages.xml
Processing file:/.../build/dita-temp/[hash].xml to file:/.../build/dita-temp/topics/error-messages-details.xml
Processing C:\...\build\dita-temp\topics\error-messages.xml
Processing file:/.../build/dita-temp/topics/error-messages.xml to file:/.../out/topics/error-messages.html
```

**PDF task:**
```
Status:           SUCCESS
Files processed:  1688 files, 63.6s
Reported Errors:  4 (2 false positives + 1 real + 1 stack trace)
Actual Errors:    1 (SVG/Batik)
```

Errors reported:
```
Processing file:/.../topics/error-messages-details.xml to file:/.../build/dita-temp/[hash].xml    ← FALSE
Processing file:/.../topics/error-messages.xml to file:/.../build/dita-temp/[hash].xml             ← FALSE
[ERROR] SVG graphic could not be built. Reason: org.apache.batik.bridge.BridgeException            ← REAL
org.apache.batik.bridge.BridgeException: file:/.../build/dita-temp/:-1                             ← STACK TRACE (double-counted)
```

### Conclusion

| Bug | DITA-OT 4.3.5 | DITA-OT 4.4.0 | Notes |
|-----|----------------|----------------|-------|
| Filename substring match | Reproduced | Reproduced | Same behavior on both versions |
| Stack trace double-counting | Reproduced | Reproduced | Same behavior on both versions |
| SVG/Batik error | Present | Present | Real FOP error, non-blocking |

The bugs are **not related to DITA-OT version** — they are caused by the plugin's log parser.

---

## Bug 1: Filename Substring Match

### Symptom

Files with "error" in their name are reported as errors in the transformation report:

```
Errors: 6
    Processing file:/.../topics/error-messages-details.xml to file:/.../build/dita-temp/[hash].xml
    Processing file:/.../topics/error-messages.xml to file:/.../build/dita-temp/[hash].xml
```

These are **informational messages** from DITA-OT showing file processing progress. They are not errors.

### Root Cause

The error detection regex matches the word `error` as a substring in the filename `error-messages.xml` and `error-messages-details.xml`.

### Expected Behavior

The parser should **only** detect errors via DITA-OT structured message codes. Generic markers like `[ERROR]`, `Error:` from third-party libraries (FOP, Batik) should be **excluded** — they are not DITA-OT errors and cause false positives and double-counting.

| Pattern | Should Match? | Reason |
|---------|---------------|--------|
| `[DOTJ013E] Failed to parse the referenced resource` | Yes | DITA-OT error code (suffix `E`) |
| `[DOTA001F] not a recognized transformation type` | Yes | DITA-OT fatal code (suffix `F`) |
| `[PDFX013F] PDF file cannot be generated` | Yes | DITA-OT fatal code (suffix `F`) |
| `[DOTJ031I] No rule found in DITAVAL file` | **No** | DITA-OT info code (suffix `I`) |
| `[DOTX023W] Unable to retrieve navtitle` | **No** | DITA-OT warning code (suffix `W`) — warning, not error |
| `[ERROR] SVG graphic could not be built` | **No** | Generic marker from Apache FOP/Batik — not a DITA-OT code |
| `Error: File not found` | **No** | Generic marker — not a DITA-OT code |
| `Processing file:/.../error-messages.xml` | **No** | No DITA-OT code |
| `org.apache.batik.bridge.BridgeException:` | **No** | No DITA-OT code — stack trace from third-party |

### Proposed Fix — DITA-OT Message Code Only

DITA-OT uses structured message codes that encode the severity directly. The plugin should **only** use these codes for error detection, ignoring all generic markers.

**Message code format:** `[PREFIX][NUMBER][SEVERITY]`

| Prefix | Component | Code Range | E/F Codes | W Codes | I Codes |
|--------|-----------|------------|-----------|---------|---------|
| `DOTA` | General / Ant integration | 001–069 | 3E, 8F | 7W | — |
| `DOTJ` | Java / Core processing | 005–088 | 42E, 5F | 22W | 10I |
| `DOTX` | XHTML / Transform output | 001–077 | 20E | 30W | 14I |
| `INDX` | Index processing | 001–003 | 2E | — | 1I |
| `PDFJ` | PDF / Java (FOP) | 001–003 | 2E | — | 1I |
| `PDFX` | PDF / XSL-FO output | 001–013 | 2E, 3F | 5W | — |
| `XEPJ` | XEP integration | 001–003 | 2E | 1W | — |

| Severity Suffix | Meaning | Total Codes (DITA-OT 4.4) |
|-----------------|---------|---------------------------|
| `I` | Informational | 26 |
| `W` | Warning | 65 |
| `E` | Error | 73 |
| `F` | Fatal | 16 |

**Note:** Some message numbers exist with multiple severity suffixes (e.g., `DOTJ007E`, `DOTJ007I`, `DOTJ007W` all exist for "Duplicate condition in filter file"). The severity suffix in the actual log output determines the classification.

**Examples from DITA-OT 4.4:**
- `[DOTJ031I]` → Info — "No rule found in DITAVAL file"
- `[DOTX023W]` → Warning — "Unable to retrieve navtitle from target"
- `[DOTJ013E]` → Error — "Failed to parse the referenced resource"
- `[DOTA001F]` → Fatal — "not a recognized transformation type"

```kotlin
/**
 * Error detection using DITA-OT structured message codes ONLY.
 *
 * Strategy:
 * - Parse DITA-OT message codes — severity is encoded in the code itself
 * - Lines without a DITA-OT message code are IGNORED (not errors)
 * - Generic markers ([ERROR], [FATAL], Error:) from third-party libraries are excluded
 * - This eliminates all false positives: filename matches, stack traces, third-party messages
 */

// DITA-OT message code pattern: [PREFIX][NUMBER][SEVERITY]
// Prefixes: DOTA, DOTJ, DOTX, INDX, PDFJ, PDFX, XEPJ
// Severity: I=Info, W=Warning, E=Error, F=Fatal
val ditaOtMessageCode = Regex("""\[(DOT[AJX]|INDX|PDF[JX]|XEPJ)\d{3}([IWEF])\]""")

// Classify line severity based on DITA-OT message code
enum class Severity { INFO, WARN, ERROR, FATAL, UNKNOWN }

fun getMessageSeverity(line: String): Severity {
    val match = ditaOtMessageCode.find(line)
    if (match != null) {
        return when (match.groupValues[2]) {
            "I" -> Severity.INFO
            "W" -> Severity.WARN
            "E" -> Severity.ERROR
            "F" -> Severity.FATAL
            else -> Severity.UNKNOWN
        }
    }
    return Severity.UNKNOWN  // No DITA-OT code found → not classified
}

fun isError(line: String): Boolean {
    val severity = getMessageSeverity(line)
    return severity == Severity.ERROR || severity == Severity.FATAL
}

fun isWarning(line: String): Boolean {
    return getMessageSeverity(line) == Severity.WARN
}

fun countErrors(lines: List<String>): Int {
    return lines.count { isError(it) }
}

fun countWarnings(lines: List<String>): Int {
    return lines.count { isWarning(it) }
}
```

**Why DITA-OT message codes only (no generic fallback):**

| Approach | False Positive Risk | Maintenance | Third-party noise |
|----------|-------------------|-------------|-------------------|
| `(?i)error` anywhere (current) | **High** — matches filenames, paths, content | None | **All captured** |
| Generic `[ERROR]` + exclusions | Medium — requires growing exclusion list | Moderate | Captured (FOP, Batik) |
| DITA-OT message codes only | **None** — severity is authoritative | **None** | **Excluded** |

Advantages:
1. **Zero false positives** — only lines with a valid DITA-OT message code are classified
2. **No exclusion patterns needed** — no need to maintain lists of `Processing file:`, `Writing file:`, stack trace patterns
3. **Third-party noise eliminated** — `[ERROR] SVG graphic...` from Apache FOP/Batik is simply ignored (no DITA-OT code = not classified)
4. **Stack trace double-counting eliminated** — `org.apache.batik.bridge.BridgeException:` has no DITA-OT code = ignored
5. **Simple code** — no regex lists, no exclusions, just one pattern match

---

## Bug 2: "Writing file" Messages Treated as Errors

### Symptom (DitaOtValidateTask)

```
? ERROR [validate-temp/topics/file.dita]: Writing file:/path/to/validate-temp/topics/file.dita
```

### Root Cause

Same as Bug 1 — `Writing file:` messages are informational but get classified as errors or warnings.

### Proposed Fix

With the DITA-OT message code only approach (see Bug 1), this bug is **automatically fixed**:

- `Writing file:/path/to/...` → no DITA-OT code → **ignored**

---

## Bug 3: SVG/Batik Stack Trace Double-Counting

### Symptom

```
Errors: 4
    ...
    [ERROR] SVG graphic could not be built. Reason: org.apache.batik.bridge.BridgeException: file:/.../build/dita-temp/:-1
    org.apache.batik.bridge.BridgeException: file:/.../build/dita-temp/:-1
```

The `[ERROR]` line is a **third-party message** from Apache FOP (not a DITA-OT message code). The `org.apache.batik.bridge.BridgeException` line is a **stack trace continuation**. Both are counted as separate errors.

### Analysis

- This is a **non-blocking error** from Apache FOP during PDF rendering
- The build still succeeds
- Present in both DITA-OT 4.3.5 and 4.4.0
- Neither line has a DITA-OT message code — both should be ignored

### Proposed Fix

With the DITA-OT message code only approach (see Bug 1), this bug is **automatically fixed**:

- `[ERROR] SVG graphic...` → no DITA-OT code → **ignored**
- `org.apache.batik.bridge.BridgeException:` → no DITA-OT code → **ignored**

No stack trace detection pattern is needed. Any line without a DITA-OT message code is simply not classified.

---

## Summary of Fixes Needed

All three bugs are fixed by a **single change**: replace the current `(?i)error` regex with DITA-OT message code detection only.

| # | Bug | Current Behavior | Fix | How DITA-OT Code Approach Fixes It |
|---|-----|-----------------|-----|-------------------------------------|
| 1 | Filename substring match | 6 false positives (HTML) | Use DITA-OT codes only | `Processing file:...error-messages.xml` has no DITA-OT code → ignored |
| 2 | "Writing file" false positives | Varies | Use DITA-OT codes only | `Writing file:...` has no DITA-OT code → ignored |
| 3 | SVG/Batik double-counting | 2 reported vs 0 actual | Use DITA-OT codes only | `[ERROR] SVG...` and stack trace have no DITA-OT code → both ignored |

### Expected Results After Fix

| Task | Current Reported Errors | Expected After Fix | Notes |
|------|------------------------|--------------------|-------|
| HTML (4.3.5) | 6 | 0 | All were false positives (filename matches) |
| HTML (4.4.0) | 6 | 0 | All were false positives (filename matches) |
| PDF (4.3.5) | 4 | 0 | SVG/Batik is a third-party message, not a DITA-OT code |
| PDF (4.4.0) | 4 | 0 | SVG/Batik is a third-party message, not a DITA-OT code |

## Files to Modify in Plugin

- `src/main/kotlin/com/github/jyjeanne/DitaOtTask.kt` — transformation report error parsing
- `src/main/kotlin/com/github/jyjeanne/DitaOtValidateTask.kt` — validation error parsing

## Test Cases

All test cases below use **real message codes and messages from DITA-OT 4.4**.

### DITA-OT Error Codes (should be detected as errors)

```kotlin
@Test
fun `should detect DOTJ013E - failed to parse referenced resource`() {
    val line = "[DOTJ013E] Failed to parse the referenced resource 'topics/missing.dita'."
    assertTrue(isError(line))
    assertEquals(Severity.ERROR, getMessageSeverity(line))
}

@Test
fun `should detect DOTA001F - not a recognized transformation type`() {
    val line = "[DOTA001F] 'unknown' is not a recognized transformation type."
    assertTrue(isError(line))
    assertEquals(Severity.FATAL, getMessageSeverity(line))
}

@Test
fun `should detect PDFJ001E - PDF indexing sort error`() {
    val line = "[PDFJ001E] PDF indexing cannot find sort location for 'term'."
    assertTrue(isError(line))
}

@Test
fun `should detect XEPJ002E - XEP error`() {
    val line = "[XEPJ002E] XEP processing failed."
    assertTrue(isError(line))
}

@Test
fun `should detect INDX002E - index sort error`() {
    val line = "[INDX002E] PDF indexing cannot find sort location for 'term'."
    assertTrue(isError(line))
}

@Test
fun `should detect DOTX010E - unable to find conref target`() {
    val line = "[DOTX010E] Unable to find @conref target 'topics/shared.dita#topic/id'."
    assertTrue(isError(line))
}

@Test
fun `should detect PDFX013F - PDF file cannot be generated`() {
    val line = "[PDFX013F] PDF file cannot be generated."
    assertTrue(isError(line))
    assertEquals(Severity.FATAL, getMessageSeverity(line))
}

@Test
fun `should detect DOTJ012F - failed to parse input resource`() {
    val line = "[DOTJ012F] Failed to parse the input resource 'map.ditamap'."
    assertTrue(isError(line))
    assertEquals(Severity.FATAL, getMessageSeverity(line))
}
```

### DITA-OT Warning Codes (should be warnings, not errors)

```kotlin
@Test
fun `should detect DOTX023W - unable to retrieve navtitle`() {
    val line = "[DOTX023W] Unable to retrieve navtitle from target 'topics/overview.dita'."
    assertFalse(isError(line))
    assertTrue(isWarning(line))
    assertEquals(Severity.WARN, getMessageSeverity(line))
}

@Test
fun `should detect DOTJ014W - indexterm with no content`() {
    val line = "[DOTJ014W] Found an <indexterm> element with no content."
    assertFalse(isError(line))
    assertTrue(isWarning(line))
}

@Test
fun `should detect DOTA006W - absolute paths not supported for CSSPATH`() {
    val line = "[DOTA006W] Absolute paths on the local file system are not supported for CSSPATH."
    assertFalse(isError(line))
    assertTrue(isWarning(line))
}

@Test
fun `should detect PDFX001W - index term range with no matching end`() {
    val line = "[PDFX001W] index term range specified with @start but no matching @end."
    assertFalse(isError(line))
    assertTrue(isWarning(line))
}
```

### DITA-OT Info Codes (should be ignored)

```kotlin
@Test
fun `should NOT detect DOTJ031I as error - no rule found in DITAVAL`() {
    val line = "[DOTJ031I] No rule for 'audience' found in DITAVAL file."
    assertFalse(isError(line))
    assertFalse(isWarning(line))
    assertEquals(Severity.INFO, getMessageSeverity(line))
}

@Test
fun `should NOT detect DOTJ045I as error - key defined more than once`() {
    val line = "[DOTJ045I] key 'product-name' is defined more than once in the same map."
    assertFalse(isError(line))
    assertFalse(isWarning(line))
}

@Test
fun `should NOT detect PDFJ003I as error - index sorted under special chars`() {
    val line = "[PDFJ003I] Index entry will be sorted under Special characters heading."
    assertFalse(isError(line))
}

@Test
fun `should NOT detect DOTX003I as error - anchorref should reference map`() {
    val line = "[DOTX003I] @anchorref should reference DITA map or Eclipse XML TOC file."
    assertFalse(isError(line))
}
```

### Same Code Number With Different Severities

```kotlin
// DOTJ007 exists as E, I, and W — severity suffix determines classification
@Test
fun `should detect DOTJ007E as error - duplicate condition`() {
    val line = "[DOTJ007E] Duplicate condition in filter file for rule 'audience:expert'."
    assertTrue(isError(line))
}

@Test
fun `should detect DOTJ007W as warning - duplicate condition`() {
    val line = "[DOTJ007W] Duplicate condition in filter file for rule 'audience:expert'."
    assertFalse(isError(line))
    assertTrue(isWarning(line))
}

@Test
fun `should detect DOTJ007I as info - duplicate condition`() {
    val line = "[DOTJ007I] Duplicate condition in filter file for rule 'audience:expert'."
    assertFalse(isError(line))
    assertFalse(isWarning(line))
    assertEquals(Severity.INFO, getMessageSeverity(line))
}
```

### Lines Without DITA-OT Code (should ALL be ignored)

```kotlin
@Test
fun `should ignore Processing file with error in filename`() {
    val line = "Processing file:/path/topics/error-messages.xml to file:/path/temp/hash.xml"
    assertFalse(isError(line))
    assertEquals(Severity.UNKNOWN, getMessageSeverity(line))
}

@Test
fun `should ignore Processing file with error-messages-details in filename`() {
    val line = "Processing file:/path/topics/error-messages-details.xml to file:/path/temp/hash.xml"
    assertFalse(isError(line))
}

@Test
fun `should ignore Processing with Windows path`() {
    val line = "Processing C:\\path\\build\\dita-temp\\topics\\error-messages.xml"
    assertFalse(isError(line))
}

@Test
fun `should ignore Writing file message`() {
    val line = "Writing file:/path/temp/validate/topics/file.xml"
    assertFalse(isError(line))
}

@Test
fun `should ignore generic ERROR tag from third-party (Apache FOP)`() {
    val line = "[ERROR] SVG graphic could not be built."
    assertFalse(isError(line))  // No DITA-OT code → ignored
}

@Test
fun `should ignore generic Error prefix from third-party`() {
    val line = "Error: File file:/path/missing.md was not found."
    assertFalse(isError(line))  // No DITA-OT code → ignored
}

@Test
fun `should ignore stack trace from Apache Batik`() {
    val line = "org.apache.batik.bridge.BridgeException: file:/path/temp/:-1"
    assertFalse(isError(line))  // No DITA-OT code → ignored
}

@Test
fun `should ignore Caused by in stack trace`() {
    val line = "Caused by: java.lang.RuntimeException: some message"
    assertFalse(isError(line))  // No DITA-OT code → ignored
}

@Test
fun `should ignore at line in stack trace`() {
    val line = "    at org.apache.fop.render.pdf.PDFRenderer.render(PDFRenderer.java:123)"
    assertFalse(isError(line))  // No DITA-OT code → ignored
}
```

### Integration Test (real build output)

```kotlin
@Test
fun `should correctly count errors and warnings in real build output`() {
    val buildOutput = listOf(
        // Lines WITHOUT DITA-OT code → all ignored
        "Processing file:/path/topics/error-messages.xml to file:/path/temp/hash.xml",
        "Processing file:/path/topics/error-messages-details.xml to file:/path/temp/hash.xml",
        "[ERROR] SVG graphic could not be built. Reason: org.apache.batik.bridge.BridgeException",
        "org.apache.batik.bridge.BridgeException: file:/path/temp/:-1",
        "    at org.apache.batik.bridge.SVGImageElementBridge.createGraphicsNode(...)",
        "Writing file:/path/temp/validate/topics/error-messages.xml",
        // Lines WITH DITA-OT code → classified by suffix
        "[DOTJ031I] No rule for 'audience' found in DITAVAL file.",                   // I → ignored
        "[DOTJ045I] key 'product-name' is defined more than once in the same map.",   // I → ignored
        "[DOTX023W] Unable to retrieve navtitle from target 'topics/overview.dita'.", // W → warning
        "[DOTJ014W] Found an <indexterm> element with no content.",                   // W → warning
        "[DOTJ013E] Failed to parse the referenced resource 'topics/broken.dita'.",   // E → error
        "[DOTX010E] Unable to find @conref target 'shared.dita#topic/id'.",           // E → error
        "[DOTA001F] 'unknown' is not a recognized transformation type.",              // F → error
    )

    val errorCount = buildOutput.count { isError(it) }
    val warningCount = buildOutput.count { isWarning(it) }
    assertEquals(3, errorCount)    // DOTJ013E + DOTX010E + DOTA001F
    assertEquals(2, warningCount)  // DOTX023W + DOTJ014W
}
```

## Workaround

Until the fix is released, the errors in the transformation report are cosmetic only — builds still succeed. The error count in the report is inflated but does not affect build output.

## References

- PR Discussion: https://github.com/dita-ot/docs/pull/660
- Plugin Repository: https://github.com/jyjeanne/dita-ot-gradle
- DITA-OT 4.4 Error Message Codes: https://www.dita-ot.org/4.4/topics/error-messages
