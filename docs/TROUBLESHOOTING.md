# Troubleshooting Guide

Solutions for common issues with the DITA-OT Gradle Plugin.

---

## Table of Contents

1. [Build Failures](#build-failures)
2. [Configuration Issues](#configuration-issues)
3. [Performance Problems](#performance-problems)
4. [File and Path Issues](#file-and-path-issues)
5. [Output Issues](#output-issues)
6. [Gradle Integration Issues](#gradle-integration-issues)
7. [Platform-Specific Issues](#platform-specific-issues)
8. [DITA-OT Compatibility](#dita-ot-compatibility)
9. [Debugging Techniques](#debugging-techniques)

---

## Build Failures

### "Plugin not found" Error

**Error Message:**
```
Could not find plugin 'io.github.jyjeanne.dita-ot-gradle'
```

**Causes:**
1. Plugin not in repositories
2. Old plugin ID from eerohele
3. Gradle version too old

**Solutions:**

✅ **Solution 1: Add Gradle Plugin Portal to repositories**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()  // Add this
        mavenCentral()
    }
}

plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}
```

✅ **Solution 2: Verify plugin ID**

```groovy
// WRONG (old eerohele plugin)
plugins {
    id 'com.github.eerohele.dita-ot-gradle'
}

// CORRECT (new jyjeanne plugin)
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle'
}
```

✅ **Solution 3: Update Gradle**

```bash
# Minimum version: Gradle 4.10, recommended: 8.5+
./gradlew wrapper --gradle-version=8.10
```

**Prevention:**
- Always use correct plugin ID: `io.github.jyjeanne.dita-ot-gradle`
- Check Gradle version is 4.10+
- Include `gradlePluginPortal()` in `pluginManagement`

---

### "DITA-OT directory not found" Error

**Error Message:**
```
ERROR: DITA-OT directory not found: /path/to/dita-ot
```

**Causes:**
1. Path doesn't exist
2. Wrong path provided
3. Symlink broken
4. Permissions issue

**Solutions:**

✅ **Solution 1: Verify path exists**

```bash
# Check Linux/macOS
ls -la /opt/dita-ot-3.6/

# Check Windows
dir "C:\DITA-OT\dita-ot-3.6"

# List contents
ls -la /opt/dita-ot-3.6/bin/
```

✅ **Solution 2: Use absolute path**

```groovy
// WRONG (relative path might not work)
dita {
    ditaOt '../tools/dita-ot'
}

// CORRECT (absolute path)
dita {
    ditaOt '/opt/dita-ot-3.6'
}

// ALSO CORRECT (Gradle resolves relative to project)
dita {
    ditaOt file('tools/dita-ot').absolutePath
}
```

✅ **Solution 3: Use project property**

```groovy
// build.gradle
dita {
    ditaOt findProperty('ditaHome') ?: '/default/path'
}

// Command line
gradle dita -PditaHome=/opt/dita-ot-3.6

// Or gradle.properties
ditaHome=/opt/dita-ot-3.6
```

✅ **Solution 4: Check permissions**

```bash
# Verify you can read the directory
ls -la /opt/dita-ot-3.6/
# You should see: drwxr-xr-x or similar

# Check execute permission on bin scripts
ls -la /opt/dita-ot-3.6/bin/dita
# Should be executable (-rwxr-xr-x)

# Fix if needed
chmod +x /opt/dita-ot-3.6/bin/dita
chmod -R +r /opt/dita-ot-3.6/
```

✅ **Solution 5: Check symlink (if applicable)**

```bash
# If using symlink
ls -la /opt/dita-ot

# Verify link target exists
readlink /opt/dita-ot
# Should show the actual directory

# Fix broken symlink
rm /opt/dita-ot
ln -s /opt/dita-ot-3.6 /opt/dita-ot
```

---

### "Input file not found" Error

**Error Message:**
```
ERROR: Input file not found: docs/root.ditamap
```

**Causes:**
1. File doesn't exist
2. Wrong relative path
3. File in .gitignore (not committed)
4. Path case mismatch (Windows)

**Solutions:**

✅ **Solution 1: Verify file exists**

```bash
# Check current directory
pwd

# List files
ls -la docs/
ls -la docs/root.ditamap

# Check if file is in git
git status docs/root.ditamap

# Find the file
find . -name "*.ditamap" -type f
```

✅ **Solution 2: Use absolute path**

```groovy
// WRONG (relative might be confused)
dita {
    input 'docs/root.ditamap'
}

// CORRECT (absolute)
dita {
    input file('docs/root.ditamap').absolutePath
}

// OR use project-relative
dita {
    input "${projectDir}/docs/root.ditamap"
}
```

✅ **Solution 3: Check file is in git**

```bash
# File might not be committed
git status docs/root.ditamap

# Check .gitignore
grep "ditamap" .gitignore

# Add to git if needed
git add docs/
git commit -m "Add DITA maps"
```

✅ **Solution 4: Use FileTree for multiple files**

```groovy
// Verify FileTree finds files
dita {
    input fileTree('docs') {
        include '**/*.ditamap'
        exclude '**/temp/**'
    }
}

// Debug: list files found
doFirst {
    def files = fileTree('docs') {
        include '**/*.ditamap'
    }.files
    println "Found ${files.size()} files:"
    files.each { println "  - $it" }
}
```

---

### "ANT task execution failed" Error

**Error Message:**
```
ERROR: taskdef class org.dita.dost.ant.InitializeProjectTask cannot be found
```

**Cause:** Gradle's IsolatedAntBuilder limitation with DITA-OT

**Status:** Known issue, see [ANT_EXECUTION_STRATEGIES.md](ANT_EXECUTION_STRATEGIES.md)

**Workaround:**

```bash
# Disable configuration cache (temporary workaround)
gradle dita --no-configuration-cache
```

**Expected Fix:** v2.3.0 will implement DITA_SCRIPT strategy

---

## Configuration Issues

### Configuration Cache Disabled

**Error Message:**
```
Configuration cache is disabled due to ...
```

**Causes:**
1. Groovy DSL with complex closures
2. Accessing `project` at execution time
3. Non-serializable properties

**Solutions:**

✅ **Solution 1: Use Kotlin DSL**

```kotlin
// BETTER: Kotlin DSL has full cache support
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-3.6"))
    input("root.ditamap")
    transtype("html5")
}
```

✅ **Solution 2: Disable cache for specific task**

```bash
# Disable for single run
gradle dita --no-configuration-cache

# Disable for all DITA tasks
gradle dita --no-configuration-cache
```

✅ **Solution 3: Configure in gradle.properties**

```properties
# Disable configuration cache if having issues
org.gradle.configuration-cache=false

# Or enable with warnings shown
org.gradle.configuration-cache-problems=warn
```

---

### Property Value Not Applied

**Issue:** Configuration setting doesn't take effect

**Example:**
```groovy
dita {
    transtype 'pdf'  // This doesn't seem to work
}
```

**Causes:**
1. Using wrong method name
2. Value overridden later
3. Property not in Gradle Properties API

**Solutions:**

✅ **Solution 1: Verify method names**

```groovy
// Use exact method names
ditaOt()              // ✓ Correct
input()               // ✓ Correct
output()              // ✓ Correct
transtype()           // ✓ Correct
filter()              // ✓ Correct
properties()          // ✓ Correct
singleOutputDir()     // ✓ Correct
useAssociatedFilter() // ✓ Correct

// NOT these (incorrect names)
ditaHome()            // ✗ Wrong - use ditaOt()
inputFile()           // ✗ Wrong - use input()
outputDir()           // ✗ Wrong - use output()
transtypes()          // ✗ Wrong - use transtype()
```

✅ **Solution 2: Debug configuration**

```groovy
// Add debugging to see actual values
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'root.ditamap'
    transtype 'html5', 'pdf'
    output 'build/docs'

    doFirst {
        println "Debug DITA Configuration:"
        println "  DITA-OT: ${options.ditaOt}"
        println "  Input: ${options.input}"
        println "  Transtypes: ${options.transtypes}"
        println "  Output: ${options.output}"
    }
}
```

✅ **Solution 3: Check for task overrides**

```groovy
// First definition
tasks.register('dita', DitaOtTask) {
    transtype 'html5'
}

// Second definition (overrides first!)
tasks.named('dita') {
    transtype 'pdf'  // This overrides
}

// Use proper task configuration
tasks.named('dita') {
    transtype 'pdf'  // Correctly replaces
}
```

---

## Performance Problems

### Build is Slow

**Issue:** Build takes much longer than expected

**Benchmarking:**

```bash
# Measure build time
time gradle dita

# Or use Gradle's profiling
gradle dita --profile

# Report location
open build/reports/profile/

# Check with verbose logging
gradle dita --info
gradle dita --debug
```

**Solutions:**

✅ **Solution 1: Enable Configuration Cache**

```properties
# gradle.properties
org.gradle.configuration-cache=true

# Subsequent runs are 10-50% faster
```

**Before (first run):**
```
real    0m12.345s
```

**After (second+ run):**
```
real    0m3.456s  (72% faster!)
```

✅ **Solution 2: Use Gradle Daemon**

```bash
# Daemon is default, but verify
jps | grep GradleDaemon

# Start if not running
gradle --daemon dita

# Stop if too many instances
gradle --stop
```

✅ **Solution 3: Optimize Java Heap**

```bash
# Increase heap for large documents
export JAVA_OPTS="-Xmx2g -Xms512m"
gradle dita

# Or in gradle.properties
org.gradle.jvmargs=-Xmx2g -Xms512m
```

✅ **Solution 4: Use Parallel Processing**

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4  # Adjust to CPU cores
```

✅ **Solution 5: Check Profile Report**

```bash
gradle dita --profile
# Open build/reports/profile/
# Look for slowest tasks
```

---

## File and Path Issues

### Windows Path Problems

**Issue:** Paths not recognized on Windows

**Causes:**
1. Backslash vs forward slash confusion
2. Long path issues
3. Special characters in path

**Solutions:**

✅ **Solution 1: Use forward slashes**

```groovy
// Windows - use forward slashes
dita {
    ditaOt 'C:/DITA-OT/dita-ot-3.6'      // ✓ Works
    input 'src/dita/root.ditamap'        // ✓ Works
    output 'build/output'                // ✓ Works
}

// NOT backslashes (requires escaping)
dita {
    ditaOt 'C:\\DITA-OT\\dita-ot-3.6'    // ✓ Escapes work but ugly
}
```

✅ **Solution 2: Use File objects**

```groovy
// Let Gradle handle path conversion
dita {
    ditaOt file('C:/DITA-OT/dita-ot-3.6')  // ✓ Platform-aware
    input file('src/dita/root.ditamap')
    output file('build/output')
}

// Or absolute path resolution
dita {
    ditaOt file('/opt/dita-ot-3.6').absolutePath
}
```

✅ **Solution 3: Handle long paths**

```bash
# Windows long path support
# Enable via Group Policy or registry

# Or use 8.3 short names
C:\PROGRA~1\DITAOT~1.6\

# Or use UNC paths
\\?\C:\very\long\path\to\dita-ot
```

---

### File Encoding Issues

**Issue:** Special characters in filenames or content not handled correctly

**Causes:**
1. Wrong file encoding
2. UTF-8 vs ASCII mismatch
3. Locale settings

**Solutions:**

✅ **Solution 1: Set file encoding**

```bash
# Unix/Linux/macOS
export JAVA_OPTS=-Dfile.encoding=UTF-8
gradle dita

# Windows (PowerShell)
$env:JAVA_OPTS="-Dfile.encoding=UTF-8"
gradle dita

# Or via Gradle property
gradle dita -Dfile.encoding=UTF-8
```

✅ **Solution 2: Configure in gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

✅ **Solution 3: Verify source file encoding**

```bash
# Check file encoding
file -i src/dita/root.ditamap

# Convert to UTF-8 if needed
iconv -f ISO-8859-1 -t UTF-8 root.ditamap > root-utf8.ditamap
```

---

## Output Issues

### Output Directory Permissions

**Error Message:**
```
ERROR: Cannot write to output directory: /path/to/output
```

**Solutions:**

✅ **Solution 1: Check permissions**

```bash
# Check directory permissions
ls -la /path/to/output/
# Should show write permission: drwxrwxr-x or similar

# Fix if needed
chmod -R 755 /path/to/output/

# Or for group write
chmod -R 775 /path/to/output/
```

✅ **Solution 2: Use writable location**

```groovy
// Move to writable location
dita {
    output file("${System.getProperty('java.io.tmpdir')}/dita-output")
}

// Or use project build directory
dita {
    output file("$buildDir/dita-output")
}
```

---

### Empty or Incomplete Output

**Issue:** Output files are missing or incomplete

**Causes:**
1. Build failed silently
2. Wrong output directory
3. Incorrect file paths

**Solutions:**

✅ **Solution 1: Check build log**

```bash
# Capture full output
gradle dita > build.log 2>&1

# Check for errors
grep -i "error\|warning\|failed" build.log

# Or use verbose mode
gradle dita --info > build-info.log
```

✅ **Solution 2: Verify output location**

```bash
# Check what gradle thinks output is
gradle dita --info | grep -i "output"

# List actual files created
find build/ -type f -name "*.html"
find build/ -type f -name "*.css"
find build/ -type f -name "*.js"
```

✅ **Solution 3: Debug with temp directory**

```groovy
dita {
    // Keep temp files for inspection
    temp file("build/.dita-temp")

    properties {
        property(name: 'publish.temp', value: 'true')
    }

    doFirst {
        println "Output will be in: ${options.output}"
        println "Temp files in: ${options.temp}"
    }
}
```

---

## Gradle Integration Issues

### Task Name Conflicts

**Issue:** Task conflicts with other plugins

**Error:**
```
Duplicate task name 'dita' in plugin
```

**Solutions:**

✅ **Solution 1: Rename task**

```groovy
// Instead of default 'dita'
tasks.register('publishDita', DitaOtTask) {
    ditaOt '/opt/dita-ot-3.6'
    input 'root.ditamap'
}

// Run with custom name
gradle publishDita
```

✅ **Solution 2: Check for plugin conflicts**

```groovy
// List all tasks
gradle tasks --all

// Look for 'dita' task from other plugins
# Find duplicate sources
gradle tasks --all | grep dita
```

---

### Gradle Wrapper Issues

**Issue:** Gradle wrapper not working

**Error:**
```
ERROR: Could not locate GradleUserHomeDir...
```

**Solutions:**

✅ **Solution 1: Rebuild wrapper**

```bash
# Remove existing wrapper
rm -rf gradle/wrapper gradle gradlew gradlew.bat

# Rebuild with correct version
gradle wrapper --gradle-version=8.10

# Commit wrapper
git add gradle/wrapper
git commit -m "Update Gradle wrapper"
```

✅ **Solution 2: Use system Gradle**

```bash
# Instead of ./gradlew
gradle dita
```

✅ **Solution 3: Set proxy (if needed)**

```gradle
# gradle.properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

---

## Platform-Specific Issues

### macOS Issues

**Issue 1: Gatekeeper blocks dita script**

```bash
# Error: cannot be opened because the developer cannot be verified
# Solution: Allow execution
xattr -d com.apple.quarantine /path/to/dita-ot/bin/dita
```

**Issue 2: Permissions on extracted ZIP**

```bash
# Fix permissions after extracting
chmod -R +rx /opt/dita-ot-3.6/bin/
```

---

### Linux Issues

**Issue 1: Java not found**

```bash
# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
gradle dita

# Or add to ~/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk" >> ~/.bashrc
```

**Issue 2: UTF-8 encoding**

```bash
# Set locale
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
gradle dita
```

---

### Windows Issues

**Issue 1: Python scripts in plugins fail**

```bash
# Some DITA-OT plugins use Python
# Install Python and add to PATH

# Verify Python available
python --version
python3 --version

# Try running dita.bat directly
cd C:\DITA-OT\bin
dita.bat -i ..\sample\topics\simple.ditamap -f html5 -o output
```

**Issue 2: Long command lines exceed limit**

```groovy
// Use shorter paths
dita {
    ditaOt 'C:/DO36'  // Shorten path
    input 'root.ditamap'
}

// Or use custom temp directory on faster drive
dita {
    temp file('C:/tmp/.dita')
}
```

---

## DITA-OT Compatibility

### Outdated DITA-OT Version

**Error:**
```
WARNING: DITA-OT version 2.5 is not recommended; upgrade to 3.0+
```

**Solution:**

```bash
# Download newer DITA-OT
# https://www.dita-ot.org/download

# Or use examples/download example to automate
cd examples/download
gradle dita
```

---

### Plugin Compatibility

**Issue:** Plugin requires features not in your DITA-OT version

**Error:**
```
ERROR: Plugin org.example.plugin not compatible with DITA-OT 3.4
```

**Solutions:**

✅ **Solution 1: Upgrade DITA-OT**

```bash
# Download compatible DITA-OT version
# Update ditaOt path in build.gradle
```

✅ **Solution 2: Install plugins manually**

```bash
cd /path/to/dita-ot
bin/dita install org.example.plugin
```

✅ **Solution 3: Use download example**

```bash
cd examples/download
gradle dita  # Handles plugin installation
```

---

## Debugging Techniques

### Enable Verbose Logging

```bash
# Info level (detailed)
gradle dita --info

# Debug level (very detailed)
gradle dita --debug

# Combined with capture
gradle dita --info > dita-debug.log 2>&1
tail -f dita-debug.log
```

### Add Manual Debugging

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'root.ditamap'
    transtype 'html5'

    doFirst {
        println "=== DITA Build Debug Info ==="
        println "DITA-OT: ${options.ditaOt}"
        println "Input: ${options.input}"
        println "Output: ${options.output}"
        println "Transtype: ${options.transtypes}"
        println "Filter: ${options.filter}"
        println "Properties: ${options.properties}"
        println "Classpath entries: ${options.classpath?.size() ?: 0}"
        println "=========================="
    }
}
```

### Check File System State

```bash
# After build completes
# Check output directory
find build/ -type f | head -20

# Check output quality
# Open in browser
open build/html5/index.html

# Verify links work
# Search for broken references in browser console
```

### Profile the Build

```bash
# Generate timing profile
gradle dita --profile

# Open report
open build/reports/profile/

# Identify slow tasks
```

### Test with Minimal Configuration

```groovy
// Start simple to isolate issue
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'simple.ditamap'
    transtype 'html5'
}

// Add complexity one at a time
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'simple.ditamap'
    transtype 'html5'
    filter 'filter.ditaval'  // Add filter
}

// Identify which setting causes issue
```

---

## Getting Help

### Before Reporting Issues

1. ✅ Enable verbose logging: `gradle dita --info`
2. ✅ Try with minimal configuration
3. ✅ Check you're using latest plugin version
4. ✅ Verify DITA-OT installation works standalone
5. ✅ Search existing [GitHub issues](https://github.com/jyjeanne/dita-ot-gradle/issues)

### Reporting an Issue

Include:
- Plugin version: `id '...' version 'X.Y.Z'`
- Gradle version: `gradle --version`
- DITA-OT version: Check `dita --version`
- Java version: `java -version`
- Operating system: Windows/macOS/Linux
- Full error message and stack trace
- Minimal example to reproduce

### Useful Resources

| Resource | URL |
|----------|-----|
| **Issues** | https://github.com/jyjeanne/dita-ot-gradle/issues |
| **Examples** | https://github.com/jyjeanne/dita-ot-gradle/tree/main/examples |
| **DITA-OT Docs** | https://www.dita-ot.org/ |
| **Gradle Docs** | https://docs.gradle.org/ |

---

**Have a question not covered here?** [Open an issue](https://github.com/jyjeanne/dita-ot-gradle/issues/new)

