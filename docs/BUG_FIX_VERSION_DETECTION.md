# Bug Fix: DITA-OT Version Detection Returns "unknown"

## Issue Description

**Affected File:** `src/main/kotlin/com/github/jyjeanne/DitaOtTask.kt`
**Function:** `detectDitaOtVersion()`
**Severity:** Low - Cosmetic issue in transformation report

### Symptom

The DITA-OT Transformation Report always shows `unknown` for the version:

```
═══════════════════════════════════════════════════════
DITA-OT Transformation Report
═══════════════════════════════════════════════════════
Status:           SUCCESS
DITA-OT version:  unknown     ← Should show "4.2.3"
Files processed:  1
Formats:          html5
```

### Root Cause

The current implementation looks for a `VERSION` file in the DITA-OT root directory:

```kotlin
fun detectDitaOtVersion(): String {
    return try {
        val ditaHome = resolveDitaHome()
        val versionFile = File(ditaHome, "VERSION")
        if (versionFile.exists()) {
            versionFile.readText().trim()
        } else {
            "unknown"
        }
    } catch (e: Exception) {
        "unknown"
    }
}
```

**Problem:** DITA-OT does not have a `VERSION` file. The version is stored in:
- `plugins/org.dita.base/plugin.xml` (DITA-OT 3.x+)
- `lib/dost.jar` manifest (all versions)

---

## Solution

### Fixed Code

Replace the `detectDitaOtVersion()` function in `DitaOtTask.kt`:

```kotlin
/**
 * Detects the DITA-OT version from the installation directory.
 *
 * Checks multiple locations in order of preference:
 * 1. plugins/org.dita.base/plugin.xml (DITA-OT 3.x+)
 * 2. lib/dost.jar manifest Implementation-Version
 * 3. Returns "unknown" if version cannot be determined
 */
fun detectDitaOtVersion(): String {
    return try {
        val ditaHome = resolveDitaHome()

        // Method 1: Read from org.dita.base plugin.xml (DITA-OT 3.x+)
        val pluginXml = File(ditaHome, "plugins/org.dita.base/plugin.xml")
        if (pluginXml.exists()) {
            val content = pluginXml.readText()
            val versionRegex = """<plugin[^>]+version="([^"]+)"""".toRegex()
            val match = versionRegex.find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        // Method 2: Read from lib/dost.jar manifest
        val dostJar = File(ditaHome, "lib/dost.jar")
        if (dostJar.exists()) {
            java.util.jar.JarFile(dostJar).use { jar ->
                val version = jar.manifest?.mainAttributes?.getValue("Implementation-Version")
                if (!version.isNullOrBlank()) {
                    return version
                }
            }
        }

        // Method 3: Fallback - extract from directory name if it matches pattern
        val dirName = ditaHome.name
        val dirVersionRegex = """dita-ot-(\d+\.\d+(?:\.\d+)?)""".toRegex()
        val dirMatch = dirVersionRegex.find(dirName)
        if (dirMatch != null) {
            return dirMatch.groupValues[1]
        }

        "unknown"
    } catch (e: Exception) {
        logger.debug("Could not detect DITA-OT version: ${e.message}")
        "unknown"
    }
}
```

---

## Version Detection Methods Explained

### Method 1: plugin.xml (Recommended)

**File:** `plugins/org.dita.base/plugin.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<plugin id="org.dita.base" version="4.2.3">
  ...
</plugin>
```

**Pros:**
- Official version location for DITA-OT 3.x+
- Always accurate

**Cons:**
- File location may vary in older versions

### Method 2: JAR Manifest

**File:** `lib/dost.jar` → `META-INF/MANIFEST.MF`

```
Implementation-Version: 4.2.3
```

**Pros:**
- Works with all DITA-OT versions
- Standard Java mechanism

**Cons:**
- Requires JAR file access
- Slightly slower

### Method 3: Directory Name Fallback

**Pattern:** `dita-ot-X.Y.Z`

**Pros:**
- Works even if files are missing
- Fast

**Cons:**
- Relies on naming convention
- May not match actual installed version

---

## Testing

### Test Cases

```kotlin
@Test
fun `should detect version from plugin xml`() {
    // Setup: Create mock DITA-OT with plugin.xml
    val ditaHome = tempDir.resolve("dita-ot-4.2.3").apply { mkdirs() }
    val pluginDir = ditaHome.resolve("plugins/org.dita.base").apply { mkdirs() }
    pluginDir.resolve("plugin.xml").writeText("""
        <?xml version="1.0" encoding="UTF-8"?>
        <plugin id="org.dita.base" version="4.2.3">
        </plugin>
    """.trimIndent())

    // Test
    val version = task.detectDitaOtVersion()

    // Verify
    assertEquals("4.2.3", version)
}

@Test
fun `should fallback to directory name when plugin xml missing`() {
    val ditaHome = tempDir.resolve("dita-ot-4.1.0").apply { mkdirs() }

    val version = task.detectDitaOtVersion()

    assertEquals("4.1.0", version)
}

@Test
fun `should return unknown when no version info available`() {
    val ditaHome = tempDir.resolve("custom-dita").apply { mkdirs() }

    val version = task.detectDitaOtVersion()

    assertEquals("unknown", version)
}
```

### Manual Verification

After applying the fix, the report should show:

```
═══════════════════════════════════════════════════════
DITA-OT Transformation Report
═══════════════════════════════════════════════════════
Status:           SUCCESS
DITA-OT version:  4.2.3       ← Now correctly detected
Files processed:  1
Formats:          html5
Output size:      0,03 MB
Duration:         7,73s
═══════════════════════════════════════════════════════
```

---

## Additional Improvements

### 1. Add Version Caching

To avoid reading the file on every task execution:

```kotlin
private var cachedVersion: String? = null

fun detectDitaOtVersion(): String {
    cachedVersion?.let { return it }

    val version = detectDitaOtVersionInternal()
    cachedVersion = version
    return version
}
```

### 2. Add Version Validation

Warn if DITA-OT version is too old:

```kotlin
fun validateDitaOtVersion(version: String) {
    if (version != "unknown") {
        val parts = version.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.isNotEmpty() && parts[0] < 3) {
            logger.warn("DITA-OT $version is outdated. Consider upgrading to 4.x for best results.")
        }
    }
}
```

### 3. Include Version in Build Logs

```kotlin
logger.info("DITA-OT version: ${detectDitaOtVersion()}")
logger.info("DITA-OT home: ${resolveDitaHome().absolutePath}")
```

---

## DITA-OT Version File Locations Reference

| DITA-OT Version | Version Location |
|-----------------|------------------|
| 4.x | `plugins/org.dita.base/plugin.xml` |
| 3.x | `plugins/org.dita.base/plugin.xml` |
| 2.x | `lib/dost.jar` manifest |
| 1.x | `lib/dost.jar` manifest |

---

## Changelog

| Version | Date | Description |
|---------|------|-------------|
| 2.8.1 | Jan 2026 | ✅ Fix: DITA-OT version detection from plugin.xml |
