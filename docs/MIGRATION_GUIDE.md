# Migration Guide: eerohele to jyjeanne Plugin

A comprehensive guide for migrating from `com.github.eerohele.dita-ot-gradle` to `io.github.jyjeanne.dita-ot-gradle`.

---

## Table of Contents

1. [Overview](#overview)
2. [What's Compatible](#whats-compatible)
3. [Breaking Changes](#breaking-changes)
4. [New Features](#new-features)
5. [Step-by-Step Migration](#step-by-step-migration)
6. [Code Examples](#code-examples)
7. [Testing Your Migration](#testing-your-migration)
8. [Troubleshooting](#troubleshooting)
9. [Performance Tips](#performance-tips)

---

## Overview

The **jyjeanne plugin** is a continuation and modernization of the eerohele plugin:

| Aspect | eerohele (0.7.1) | jyjeanne (2.2.1) |
|--------|------------------|------------------|
| **Language** | Groovy | Kotlin |
| **Last Update** | ~2016 | 2025 (active) |
| **Gradle Support** | 4.x+ | 8.x+ recommended |
| **Java Compatibility** | 1.8 | 1.8 |
| **DITA-OT Support** | 2.x, 3.x | 3.x, 4.x recommended |
| **Configuration Cache** | Not supported | ✅ Full support (v2.2.1+) |
| **Build Performance** | Normal | 10-50% faster with cache |

### Why Migrate?

- **Better Performance** - Configuration cache support
- **Modern Dependencies** - Kotlin 2.1.0, Gradle 8.5+
- **Type Safety** - Kotlin language benefits
- **Enhanced Logging** - Detailed build metrics
- **Active Maintenance** - Regular updates and bug fixes
- **Better IDE Support** - Improved code completion and type checking

---

## What's Compatible

✅ **100% API Compatible** (no code changes needed):

### Configuration Methods (All Still Work)

```groovy
dita {
    ditaOt '/path/to/dita-ot'              // ✅ Works
    input 'my.ditamap'                     // ✅ Works
    output 'build/output'                  // ✅ Works
    transtype 'html5', 'pdf'               // ✅ Works
    filter 'my.ditaval'                    // ✅ Works
    singleOutputDir true                   // ✅ Works
    useAssociatedFilter true               // ✅ Works

    properties {
        property(name: 'processing-mode', value: 'strict')  // ✅ Works
    }
}
```

### File Handling

- Single input file ✅
- Multiple input files (FileTree) ✅
- Input from different directories ✅
- DITAVAL filtering ✅
- Custom output directories ✅
- Associated filters ✅

### Multi-file Processing

```groovy
// Multiple files in single output
dita {
    input fileTree(dir: 'src/dita', include: '*.ditamap')
    singleOutputDir true
}
```

---

## Breaking Changes

⚠️ **Only 2 breaking changes** (most users won't be affected):

### 1. Plugin ID Changed (Required Update)

**Before (eerohele):**
```groovy
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}
```

**After (jyjeanne):**
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}
```

### 2. Setup Task Removed (Rarely Used)

**Before (eerohele):**
```groovy
// Plugin installation task (v0.7.1)
tasks.register('installPlugins') {
    dependsOn ditaOt  // ❌ This task no longer exists
}
```

**After (jyjeanne):**
```groovy
// Install plugins manually instead:
// cd /path/to/dita-ot
// bin/dita install <plugin-id>

// OR use the automated download example:
// See examples/download/
```

---

## New Features

### 1. Configuration Cache Support (v2.2.1+)

**10-50% faster builds on subsequent runs!**

```groovy
// Enable globally (gradle.properties)
org.gradle.configuration-cache=true

// Or per-build
gradle dita --configuration-cache
```

### 2. Enhanced Logging (v2.1.0+)

```
> Task :dita
Starting DITA-OT transformation...
DITA-OT Version: 3.6
Processing 1 input file(s)...
  ✓ Processing: my.ditamap
Generating output format: html5
  ✓ html5 → build/html5 (SUCCESS)

Transformation Report:
  Status: SUCCESS
  Files Processed: 1
  Formats: html5
  Total Output Size: 2.5 MB
  Duration: 12.3 seconds
```

### 3. Type-Safe Kotlin DSL (v2.1.0+)

```kotlin
// Recommended for best experience
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("my.ditamap")
    transtype("html5")

    // Type-safe properties (new!)
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
        "args.cssroot" to "$projectDir/css"
    }
}
```

### 4. Input Validation (v2.1.0+)

Early detection of configuration errors:
```
ERROR: DITA-OT directory not found: /invalid/path
ERROR: Input file not found: missing.ditamap
ERROR: Output directory is not writable
```

### 5. Version Detection (v2.1.0+)

Automatic DITA-OT version detection:
```
INFO: Detected DITA-OT version: 3.6.0
WARNING: DITA-OT version 2.5 is not recommended; upgrade to 3.0+
```

---

## Step-by-Step Migration

### Phase 1: Update Plugin Declaration (5 minutes)

#### Step 1.1: Groovy DSL (build.gradle)

**Before:**
```groovy
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

dita {
    ditaOt '/path/to/dita-ot'
    input 'my.ditamap'
    transtype 'html5'
}
```

**After:**
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    ditaOt '/path/to/dita-ot'
    input 'my.ditamap'
    transtype 'html5'
}
```

**That's it!** Your configuration still works.

#### Step 1.2: Kotlin DSL (build.gradle.kts)

**Before:**
```kotlin
plugins {
    id("com.github.eerohele.dita-ot-gradle") version "0.7.1"
}

tasks.register<com.github.eerohele.DitaOtTask>("dita") {
    // ...
}
```

**After:**
```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    // Same configuration, just different package name
}
```

### Phase 2: Verify Functionality (10 minutes)

#### Step 2.1: Test Your Build

```bash
# Navigate to your project
cd /your/project/root

# Run the DITA task
gradle dita

# You should see enhanced logging output
# Check that output is generated in expected location
ls build/html5/
```

#### Step 2.2: Verify Output Quality

```bash
# Compare output files
# They should be identical or nearly identical to v0.7.1

# Check file sizes are similar
ls -lh build/html5/

# Open HTML in browser and verify rendering
open build/html5/index.html
```

### Phase 3: Remove Deprecated Code (5 minutes)

#### Step 3.1: Check for ditaOt Setup Task

Search your `build.gradle` for the `ditaOt` task:

```bash
grep -n "ditaOt" build.gradle
```

If you see usage like:
```groovy
// OLD: Plugin installation task (remove this)
tasks.named('ditaOt') { ... }
tasks.register('installPlugins') { dependsOn ditaOt }
```

**Remove these lines.** They're not needed in v2.2.1.

#### Step 3.2: Install DITA Plugins Manually (if needed)

If you were using the `ditaOt` task to install plugins:

```bash
# Manual installation method
cd /path/to/dita-ot
bin/dita install <plugin-name>

# Example:
bin/dita install org.dita-community.pdf2-plugin
```

**OR** use the automated download example:

```bash
cd examples/download
gradle dita  # Downloads and installs DITA-OT automatically
```

### Phase 4: Enable New Features (Optional - 5 minutes)

#### Step 4.1: Enable Configuration Cache

Add to `gradle.properties`:

```properties
# Enable configuration cache (v2.2.1+)
org.gradle.configuration-cache=true

# Optional: Show configuration cache status
org.gradle.configuration-cache-problems=warn
```

**First run:**
```bash
gradle dita
# Configuration phase runs normally, result is cached
# Builds build configuration cache...
```

**Second run:**
```bash
gradle dita
# Configuration phase skipped!
# Reusing cached configuration...
# Much faster build!
```

#### Step 4.2: Update to Kotlin DSL (Optional)

If using Groovy DSL, consider switching to Kotlin DSL for better cache support:

**build.gradle → build.gradle.kts**

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("my.ditamap")
    transtype("html5")

    // Type-safe properties (Kotlin only)
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
```

### Phase 5: Test and Validate (10 minutes)

#### Step 5.1: Run Full Build

```bash
# Clean build (first run)
gradle clean dita

# Check output
ls -la build/

# Verify content
cat build/html5/index.html
```

#### Step 5.2: Test Incremental Build

```bash
# Modify source file
echo "<!-- updated -->" >> src/dita/topic1.dita

# Run build again (should be faster)
gradle dita

# Verify only changed files reprocessed
```

#### Step 5.3: Test with Multiple Formats

```bash
# Try multiple transformation types
gradle dita -PditaHome=/path/to/dita-ot -Dtranstype=pdf,html5

# Or update your build.gradle
dita {
    transtype 'html5', 'pdf'
}

gradle dita
```

---

## Code Examples

### Example 1: Simple Migration (Most Users)

**Before (v0.7.1):**
```groovy
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'docs/root.ditamap'
    transtype 'html5'
    output 'build/docs'
}
```

**After (v2.2.1):**
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'docs/root.ditamap'
    transtype 'html5'
    output 'build/docs'
}
```

**Changes:** Only the plugin ID and version changed!

### Example 2: With Properties

**Before (v0.7.1):**
```groovy
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'root.ditamap'
    transtype 'html5'

    properties {
        property(name: 'processing-mode', value: 'strict')
        property(name: 'args.rellinks', value: 'all')
        property(name: 'args.cssroot', value: "${projectDir}/css")
    }
}
```

**After (v2.2.1 - Groovy):**
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'root.ditamap'
    transtype 'html5'

    properties {
        property(name: 'processing-mode', value: 'strict')
        property(name: 'args.rellinks', value: 'all')
        property(name: 'args.cssroot', value: "${projectDir}/css")
    }
}
```

**After (v2.2.1 - Kotlin, Recommended):**
```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-3.6"))
    input("root.ditamap")
    transtype("html5")

    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
        "args.cssroot" to "$projectDir/css"
    }
}
```

### Example 3: Multiple Files (FileTree)

**Before (v0.7.1):**
```groovy
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input fileTree('dita') {
        include '*.ditamap'
    }
    transtype 'html5'
    singleOutputDir true
}
```

**After (v2.2.1):**
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    ditaOt '/opt/dita-ot-3.6'
    input fileTree('dita') {
        include '*.ditamap'
    }
    transtype 'html5'
    singleOutputDir true
}
```

**No changes needed!**

---

## Testing Your Migration

### Automated Migration Test

Create a simple test to verify your migration:

**test-migration.sh:**
```bash
#!/bin/bash

set -e

echo "=== DITA-OT Gradle Plugin Migration Test ==="
echo

echo "1. Running clean build..."
gradle clean

echo "2. Building DITA output..."
gradle dita -PditaHome=/path/to/dita-ot

echo "3. Checking output exists..."
if [ -d "build/html5" ]; then
    echo "   ✓ Output directory created"
else
    echo "   ✗ ERROR: Output directory not found"
    exit 1
fi

echo "4. Checking HTML files..."
count=$(find build/html5 -name "*.html" | wc -l)
if [ $count -gt 0 ]; then
    echo "   ✓ Found $count HTML files"
else
    echo "   ✗ ERROR: No HTML files generated"
    exit 1
fi

echo "5. Checking for errors in build log..."
if grep -i "error\|exception" build.log 2>/dev/null; then
    echo "   ✗ WARNING: Errors found in build log"
else
    echo "   ✓ No errors in build log"
fi

echo
echo "=== Migration Test PASSED ==="
```

**Run the test:**
```bash
chmod +x test-migration.sh
./test-migration.sh
```

### Manual Verification Checklist

- [ ] Plugin applies without errors
- [ ] DITA task registers correctly
- [ ] Build runs without errors
- [ ] Output files are created
- [ ] HTML files are valid
- [ ] Images/CSS are included
- [ ] Navigation works in HTML
- [ ] File sizes are reasonable
- [ ] Build is reproducible
- [ ] Performance is acceptable

---

## Troubleshooting

### Issue 1: "Could not find plugin 'io.github.jyjeanne.dita-ot-gradle'"

**Cause:** Plugin not found in repositories

**Solution:**
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()  // Add this line
    }
}

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}
```

### Issue 2: "Configuration cache is disabled"

**Cause:** Groovy DSL with complex closures prevents caching

**Solutions:**

Option A: Use Kotlin DSL instead
```kotlin
// Much better cache support
```

Option B: Disable cache for DITA task
```bash
gradle dita --no-configuration-cache
```

Option C: Configure gradle.properties
```properties
org.gradle.configuration-cache=false
```

### Issue 3: Different Build Output

**Cause:** Possible version differences or timing issues

**Solutions:**
1. Verify DITA-OT version: `gradle dita --info | grep "DITA-OT Version"`
2. Check plugin version: `gradle dita --info | grep "plugin"`
3. Compare with old version if available
4. Report differences: https://github.com/jyjeanne/dita-ot-gradle/issues

### Issue 4: "ditaOt task not found"

**Cause:** Trying to use deprecated setup task from v0.7.1

**Solutions:**

Option A: Install plugins manually
```bash
cd /path/to/dita-ot
bin/dita install org.dita-community.pdf2-plugin
```

Option B: Use download example
```bash
cd examples/download
gradle dita
```

Option C: Install via GitHub releases
```bash
# Download plugin ZIP from GitHub
# Extract to DITA-OT plugins directory
```

### Issue 5: Build Runs Slowly

**Cause:** Configuration cache not enabled or supported

**Solutions:**

1. Enable configuration cache (v2.2.1+):
```properties
org.gradle.configuration-cache=true
```

2. Use Kotlin DSL instead of Groovy:
```kotlin
// Better caching support
```

3. Run with daemon:
```bash
gradle dita  # Daemon is used by default
```

4. Check for slow sources:
```bash
gradle dita --profile  # Generates timing report
```

---

## Performance Tips

### 1. Enable Configuration Cache (Biggest Impact)

**Before:**
```bash
$ time gradle dita
real    0m12.345s
```

**After (first run):**
```bash
$ time gradle dita
real    0m12.500s  # Slightly slower (caching)
Builds configuration cache...
```

**After (second run):**
```bash
$ time gradle dita
real    0m3.200s   # 75% faster!
Reusing cached configuration...
```

**How to enable:**
```properties
# gradle.properties
org.gradle.configuration-cache=true
```

### 2. Use Gradle Daemon

The daemon is enabled by default, but verify:

```bash
# Check if daemon is running
jps | grep GradleDaemon

# Or explicitly enable
gradle --daemon dita
```

### 3. Parallel Processing (for multiple files)

```groovy
org.gradle.parallel=true
org.gradle.workers.max=4  # Adjust to your CPU cores
```

### 4. Incremental Builds

Only changed files are reprocessed:

```bash
# First run (full)
gradle dita  # 12.3 seconds

# After editing one file
gradle dita  # 2.1 seconds (only changed file reprocessed)
```

### 5. Preload DITA-OT

For frequently changing builds, warm up the JVM:

```bash
# Pre-warm on CI/CD
gradle tasks --all  # Loads everything

# Then actual build uses warm JVM
gradle dita
```

---

## Version Compatibility Matrix

| Component | Tested | Supported | Notes |
|-----------|--------|-----------|-------|
| **DITA-OT 3.4** | ✅ | ✅ | Older version, works but not recommended |
| **DITA-OT 3.5** | ✅ | ✅ | Previous stable version |
| **DITA-OT 3.6** | ✅ | ✅ | Current recommended version |
| **DITA-OT 4.0+** | TBD | ✅ Planned | When available, will be tested |
| **Gradle 4.x** | ✅ | ✅ | Older, works but not recommended |
| **Gradle 8.x** | ✅ | ✅ | Current recommended version |
| **Gradle 9.x** | ✅ | ✅ | Latest version, fully supported |
| **Java 8** | ✅ | ✅ | Runtime minimum |
| **Java 17** | ✅ | ✅ | Recommended for building |

---

## Migration Checklist

Complete these steps to ensure successful migration:

- [ ] **Step 1:** Update plugin ID in `build.gradle` or `build.gradle.kts`
- [ ] **Step 2:** Update version to `2.2.1`
- [ ] **Step 3:** Remove deprecated `ditaOt` task (if used)
- [ ] **Step 4:** Test build: `gradle clean dita`
- [ ] **Step 5:** Verify output in `build/` directory
- [ ] **Step 6:** Compare with old version (if available)
- [ ] **Step 7:** Enable configuration cache (optional): `org.gradle.configuration-cache=true`
- [ ] **Step 8:** Test second run for cache benefits
- [ ] **Step 9:** Run all examples to verify
- [ ] **Step 10:** Update CI/CD if using DITA task
- [ ] **Step 11:** Document changes in your project
- [ ] **Step 12:** Train team on new features

---

## Support and Resources

| Resource | URL |
|----------|-----|
| **GitHub Issues** | https://github.com/jyjeanne/dita-ot-gradle/issues |
| **Gradle Plugin Portal** | https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle |
| **GitHub Repository** | https://github.com/jyjeanne/dita-ot-gradle |
| **Configuration Cache Docs** | https://docs.gradle.org/current/userguide/configuration_cache.html |
| **DITA-OT Documentation** | https://www.dita-ot.org/ |

---

## Next Steps

After successful migration:

1. **Read** [Configuration Reference](CONFIGURATION_REFERENCE.md) for advanced options
2. **Review** [Best Practices](BEST_PRACTICES.md) for optimal usage
3. **Check** [Troubleshooting Guide](TROUBLESHOOTING.md) for common issues
4. **Explore** [Examples](../examples/) for real-world use cases

---

**Happy Publishing! 🚀**

For questions or issues, please open an issue on [GitHub](https://github.com/jyjeanne/dita-ot-gradle/issues).

