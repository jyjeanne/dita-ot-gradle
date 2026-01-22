# Bug Fix: Command Path with Spaces - Cross-Platform Solution

## Issue Description

**Affected File:** `src/main/kotlin/com/github/jyjeanne/AntExecutor.kt`
**Function:** `executeViaDitaScript()`
**Severity:** High - Breaks builds on paths containing spaces

### Symptom

When the project path contains spaces, the DITA-OT transformation fails with exit code 255:

```
> Task :ditaHtml FAILED
Starting DITA-OT transformation
  [==============================] 100% - Complete
  [[                              ]] 0% - Failed at Initializing
? DITA-OT execution failed with exit code: 255
```

### Root Cause

The current implementation concatenates paths directly into command arguments using the `--option=value` format:

```kotlin
command.add("--input=${inputFile.absolutePath}")
```

When the path contains spaces, this produces:

```
--input=C:\Users\name\My Project\src\file.ditamap
```

The shell interprets the space as an argument separator, breaking the command into:
- `--input=C:\Users\name\My`
- `Project\src\file.ditamap`

This occurs on **all platforms** (Windows, macOS, Linux) when paths contain spaces.

---

## Cross-Platform Solution

### Recommended Fix: Use Separate Arguments

Instead of `--option=value`, pass the option and value as separate list items. `ProcessBuilder` handles each list item as a distinct argument on all operating systems.

### Code Changes

**Location:** `AntExecutor.kt`, function `executeViaDitaScript()` (approximately lines 57-90)

#### Before (Current Code)

```kotlin
val command = mutableListOf<String>()

// Add script
command.add(ditaScript.absolutePath)

// Add required properties (in DITA-OT format)
command.add("--input=${inputFile.absolutePath}")
command.add("--format=$transtype")
command.add("--output=${outputDir.absolutePath}")

// Add temp directory
if (tempDir.absolutePath.isNotEmpty()) {
    command.add("--temp=${tempDir.absolutePath}")
}

// Add filter if specified
if (filterFile != null && filterFile.exists()) {
    command.add("--filter=${filterFile.absolutePath}")
}

// Add verbose flag
command.add("--verbose")

// Add custom properties
properties.forEach { (key, value) ->
    command.add("-D${key}=${value}")
}
```

#### After (Fixed Code)

```kotlin
val command = mutableListOf<String>()

// Add script
command.add(ditaScript.absolutePath)

// Add required properties using separate arguments for cross-platform compatibility
// This ensures paths with spaces are handled correctly on Windows, macOS, and Linux
command.add("--input")
command.add(inputFile.absolutePath)

command.add("--format")
command.add(transtype)

command.add("--output")
command.add(outputDir.absolutePath)

// Add temp directory
if (tempDir.absolutePath.isNotEmpty()) {
    command.add("--temp")
    command.add(tempDir.absolutePath)
}

// Add filter if specified
if (filterFile != null && filterFile.exists()) {
    command.add("--filter")
    command.add(filterFile.absolutePath)
}

// Add verbose flag
command.add("--verbose")

// Add custom properties (ANT -D properties don't typically have path issues)
properties.forEach { (key, value) ->
    // For properties that might contain paths, consider separate handling
    command.add("-D${key}=${value}")
}
```

---

## Why This Solution Works

### ProcessBuilder Argument Handling

When using `ProcessBuilder` with a `List<String>`:

| Approach | Command List | Shell Interpretation |
|----------|--------------|----------------------|
| Combined | `["--input=/path with spaces/file"]` | Breaks at space |
| Separate | `["--input", "/path with spaces/file"]` | Each item = 1 argument |

### Cross-Platform Compatibility

| Platform | Combined `--opt=val` | Separate `--opt` `val` |
|----------|---------------------|------------------------|
| Windows (cmd.exe) | Fails with spaces | Works |
| Windows (PowerShell) | Fails with spaces | Works |
| macOS (zsh/bash) | Fails with spaces | Works |
| Linux (bash) | Fails with spaces | Works |

---

## Testing the Fix

### Test Cases

1. **Path with spaces:**
   ```
   C:\Users\John Doe\My Projects\dita-demo\
   /home/user/my projects/dita-demo/
   ```

2. **Path with special characters:**
   ```
   C:\Users\José\Projets\dita-demo\
   /home/user/project-v1.0 (beta)/
   ```

3. **Standard path (regression test):**
   ```
   C:\projects\dita-demo\
   /home/user/dita-demo/
   ```

### Manual Verification

Run with debug output to verify the command structure:

```bash
./gradlew ditaHtml --debug 2>&1 | grep "Command:"
```

**Before fix:**
```
Command: /path/to/dita.bat --input=/path with spaces/file.ditamap --format=html5
```

**After fix:**
```
Command: [/path/to/dita.bat, --input, /path with spaces/file.ditamap, --format, html5]
```

---

## Alternative Solutions (Not Recommended)

### Option A: Quote Paths

```kotlin
command.add("--input=\"${inputFile.absolutePath}\"")
```

**Drawback:** Quoting behavior differs between shells. May work on some platforms but fail on others.

### Option B: Escape Spaces

```kotlin
command.add("--input=${inputFile.absolutePath.replace(" ", "\\ ")}")
```

**Drawback:** Escape characters differ between platforms (`\` on Unix, varies on Windows).

---

## Additional Recommendations

### 1. Update Logging

When logging the command, show it as a proper list to aid debugging:

```kotlin
logger.debug("Command: ${command.joinToString(" ") { "\"$it\"" }}")
// Or better:
logger.debug("Command arguments: $command")
```

### 2. Add Integration Test

```kotlin
@Test
fun `should handle paths with spaces`() {
    val projectDir = tempDir.resolve("path with spaces").apply { mkdirs() }
    // ... test DITA transformation in this directory
}
```

### 3. Document in README

Add a note that paths with spaces are supported (after the fix).

---

## References

- [Java ProcessBuilder Documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ProcessBuilder.html)
- [DITA-OT Command Line Syntax](https://www.dita-ot.org/dev/parameters/dita-command-arguments.html)

---

## Changelog

| Version | Date | Description |
|---------|------|-------------|
| 2.8.1 | Jan 2026 | ✅ Fix: Cross-platform support for paths with spaces |
