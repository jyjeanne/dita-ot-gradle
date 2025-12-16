DITA-OT Gradle Plugin: Examples
===============================

This directory contains example projects demonstrating various use cases of the DITA-OT Gradle plugin, including the latest **v2.3.1** features.

## 🆕 What's New in v2.3.1

All examples have been updated to **v2.3.1** with:

- ⚡ **Full Configuration Cache Support** - Up to 77% faster incremental builds
- 🔧 **IsolatedAntBuilder Fix** - DITA_SCRIPT strategy resolves classloader issues
- 🚀 **Provider API Refactoring** - Modern Gradle architecture
- 📊 **Enhanced Logging** - Detailed build metrics and reports
- 🔧 **Type-Safe Kotlin DSL** - Improved property configuration

## Performance Benchmarks (v2.3.1)

Tested on Windows 11, Gradle 8.5, DITA-OT 3.6:

| Scenario | Time | Improvement |
|----------|------|-------------|
| Without Configuration Cache | 20.8s | baseline |
| With Cache (first run) | 22.8s | -10% (stores cache) |
| With Cache (up-to-date) | **4.8s** | **77% faster** |
| With Cache (clean build) | 22.4s | similar |

## Available Examples

Each example is provided in both **Groovy DSL** (`build.gradle`) and **Kotlin DSL** (`build.gradle.kts`) formats:

1. **simple** - Basic DITA transformation with properties
2. **configuration-cache** - ⭐ **NEW: Configuration Cache performance demo**
3. **filetree** - Process multiple files using glob patterns
4. **multi-project** - Multi-module project with shared configuration
5. **multi-task** - Multiple transformation tasks (web + pdf)
6. **classpath** - Custom classpath configuration (Saxon-PE example)
7. **download** - Download DITA-OT and install plugins automatically

## Running Examples

### Prerequisites

1. **DITA-OT Installation:**
   ```bash
   # Download DITA-OT 3.6 (or use your own installation)
   export DITA_HOME=/path/to/dita-ot-3.6
   ```

2. **Gradle:** Examples use the system Gradle or can use the wrapper script

### Individual Example Commands

Each example below shows how to run it with both **Groovy DSL** and **Kotlin DSL**.

#### 1. **simple** - Basic DITA Transformation with Properties

Demonstrates basic DITA transformation with property handling.

**Groovy DSL:**
```bash
cd simple
gradle dita -PditaHome=$DITA_HOME
```

**Kotlin DSL:**
```bash
cd simple
gradle dita -PditaHome=$DITA_HOME -b build.gradle.kts
```

**Output:** PDF document in `build/` directory

---

#### 2. **configuration-cache** - Configuration Cache Performance Demo ⭐ NEW

Demonstrates the Configuration Cache feature for significantly faster builds.

**First Run (stores cache):**
```bash
cd configuration-cache
gradle dita --configuration-cache -PditaHome=$DITA_HOME
# Output: "Configuration cache entry stored."
```

**Second Run (reuses cache - 77% faster!):**
```bash
gradle dita --configuration-cache -PditaHome=$DITA_HOME
# Output: "Reusing configuration cache."
```

**Run Benchmark Instructions:**
```bash
gradle benchmark
```

**Features Demonstrated:**
- Configuration Cache compatibility
- Multiple output formats (HTML5 + PDF)
- Type-safe Kotlin DSL properties
- Performance benchmarking

**Expected Performance:**
| Run | Time | Notes |
|-----|------|-------|
| First | ~22s | Stores configuration cache |
| Second (up-to-date) | ~5s | **77% faster** |
| Clean build (cached) | ~22s | Reuses configuration |

**Output:** HTML5 and PDF in `build/` directory

---

#### 3. **filetree** - Multiple Files with Glob Patterns

Processes multiple DITA files using glob patterns for flexible file selection.

**Groovy DSL:**
```bash
cd filetree
gradle dita -PditaHome=$DITA_HOME
```

**Kotlin DSL:**
```bash
cd filetree
gradle dita -PditaHome=$DITA_HOME -b build.gradle.kts
```

**Features Demonstrated:**
- Multiple input file handling with glob patterns
- Wildcard file selection
- Batch processing

**Output:** HTML files in `build/out/html/` directory

---

#### 4. **multi-project** - Multi-Module Project Configuration

Demonstrates shared configuration across multiple sub-projects.

**Run Parent Build (applies to all sub-modules):**
```bash
cd multi-project
gradle dita -PditaHome=$DITA_HOME
```

**Run Specific Sub-Project:**
```bash
cd multi-project/one
gradle dita -PditaHome=$DITA_HOME
```

Or use the root gradle wrapper:
```bash
cd multi-project
gradle :one:dita -PditaHome=$DITA_HOME
gradle :two:dita -PditaHome=$DITA_HOME
```

**Features Demonstrated:**
- Plugin applied to multiple sub-projects
- Shared DITA-OT configuration
- Per-project customization

**Output:**
- `multi-project/one/build/out/` - XHTML output
- `multi-project/two/build/out/` - XHTML output

---

#### 5. **multi-task** - Multiple Transformation Tasks

Demonstrates running multiple DITA transformations in the same project (web and PDF).

**Run All Transformation Tasks:**
```bash
cd multi-task
gradle dita -PditaHome=$DITA_HOME
```

**Run Specific Transformation:**
```bash
cd multi-task
gradle ditaWeb -PditaHome=$DITA_HOME    # HTML5 output
gradle ditaPdf -PditaHome=$DITA_HOME    # PDF output
gradle ditaBoth -PditaHome=$DITA_HOME   # Both formats
```

**With Kotlin DSL:**
```bash
cd multi-task
gradle dita -PditaHome=$DITA_HOME -b build.gradle.kts
```

**Features Demonstrated:**
- Multiple transformation tasks in one project
- Different output formats per task
- Custom task names
- Incremental builds

**Output:**
- `multi-task/build/out-html5/` - HTML5 web output
- `multi-task/build/out-pdf/` - PDF output

---

#### 6. **classpath** - Custom Classpath Configuration

Demonstrates custom classpath setup (advanced feature using Saxon-PE as example).

**Groovy DSL:**
```bash
cd classpath
gradle dita -PditaHome=$DITA_HOME
```

**Kotlin DSL:**
```bash
cd classpath
gradle dita -PditaHome=$DITA_HOME -b build.gradle.kts
```

**Features Demonstrated:**
- Custom JAR file inclusion
- Classpath augmentation
- XSLT processor customization
- Advanced configuration patterns

**Output:** PDF with custom processing in `build/out/` directory

**Note:** This example may require external dependencies. See `build.gradle` comments for details.

---

#### 7. **download** - Automatic DITA-OT Download and Plugin Installation

Demonstrates automatic download and setup of DITA-OT without manual installation.

**Groovy DSL:**
```bash
cd download
gradle dita
```

**Kotlin DSL:**
```bash
cd download
gradle dita -b build.gradle.kts
```

**Features Demonstrated:**
- Automatic DITA-OT download
- Plugin auto-installation
- No manual setup required
- CI/CD ready

**Output:** HTML5 output in `build/out/html5/` directory

**Note:** This example downloads DITA-OT (~500MB), so first run takes longer.

---

### Running Examples from Parent Directory

To run all examples from the main project root:

```bash
# Set up the path to DITA-OT
export DITA_HOME=/path/to/dita-ot-3.6

# Run all examples at once
cd examples
gradle -PditaHome=$DITA_HOME
```

Or run a specific example's default task:
```bash
gradle -p simple dita -PditaHome=$DITA_HOME
gradle -p filetree dita -PditaHome=$DITA_HOME
gradle -p multi-project dita -PditaHome=$DITA_HOME
gradle -p multi-task dita -PditaHome=$DITA_HOME
gradle -p classpath dita -PditaHome=$DITA_HOME
gradle -p download dita
```

## Configuration Cache Benefits (v2.2.2+)

Enable configuration cache for significantly faster builds:

**In `gradle.properties`:**
```properties
org.gradle.configuration-cache=true
```

**Or via command line:**
```bash
gradle dita --configuration-cache
```

**Performance Impact:**
- First run: Normal configuration phase
- Second run: **Configuration skipped** → 10-50% faster
- CI/CD: Major time savings on repeated builds

## Choosing Build Script Format

- **Groovy DSL** (`build.gradle`) - Traditional Gradle syntax, more concise
- **Kotlin DSL** (`build.gradle.kts`) - Type-safe, better IDE support, modern
  - ⭐ **Recommended for v2.2.2+** - Best configuration cache compatibility
  - ⭐ Includes new type-safe properties syntax (v2.1.0+)

Both formats are functionally equivalent. Choose the one that fits your project's needs.

## New Features Highlights

### Type-Safe Kotlin DSL Properties (v2.1.0+)

```kotlin
properties {
    "processing-mode" to "strict"
    "args.rellinks" to "all"
}
```

### Build Reports & Metrics (v2.1.0+)

Every build now includes detailed reports:
- ✅ Transformation status
- 📁 Files processed count
- 📋 Output formats list
- 💾 Total output size
- ⏱️ Duration in seconds

### Enhanced Logging (v2.1.0+)

- Lifecycle, info, debug, and error levels
- Progress tracking for each file and format
- Professional transformation reports

---

## Version History of Examples

### v2.3.1 (November 2025)
All examples updated to **v2.3.1** featuring:
- ✅ **Full Configuration Cache Support** - Up to 77% faster incremental builds
- ✅ **IsolatedAntBuilder Fix** - DITA_SCRIPT strategy resolves classloader issues
- ✅ **Provider API Refactoring** - Modern Gradle architecture
- ✅ **New configuration-cache example** - Demonstrates performance benefits

**Tested Compatibility:**
- ✅ Gradle 8.5, 8.10, 9.0
- ✅ DITA-OT 3.4, 3.5, 3.6
- ✅ Java 8+
- ✅ Windows, macOS, Linux

### Previous Versions
- v2.2.2 - IsolatedAntBuilder workaround, Kotlin 2.1.0 compatibility
- v2.2.0 - Configuration cache support introduction
- v2.1.0 - Kotlin DSL properties, enhanced logging, build reports
- v2.0.0 - Breaking changes, Ant DSL fixes
- v1.0.0 - Kotlin migration, dual DSL support

---

## Troubleshooting

### Issue: "ditaHome property required" error

**Solution:** Ensure you pass the `ditaHome` property:
```bash
gradle dita -PditaHome=/path/to/dita-ot-3.6
```

Or set it in `gradle.properties`:
```properties
ditaHome=/path/to/dita-ot-3.6
```

### Issue: ANT ClassNotFoundException (Fixed in v2.3.1)

**Symptom:** `taskdef class org.dita.dost.ant.InitializeProjectTask cannot be found`

**Cause:** IsolatedAntBuilder classloader issue (fixed in v2.3.1)

**Solution:** Upgrade to v2.3.1 which uses DITA_SCRIPT strategy by default.

**For older versions, workaround:**
```bash
gradle dita --no-configuration-cache -PditaHome=$DITA_HOME
```

### Issue: Out of Memory during transformation

**Solution:** Increase heap size:
```bash
export GRADLE_OPTS="-Xmx2g"
gradle dita -PditaHome=$DITA_HOME
```

### Issue: Gradle wrapper permission denied (Linux/macOS)

**Solution:** Grant execute permission:
```bash
chmod +x gradlew
./gradlew dita -PditaHome=$DITA_HOME
```

---

## Performance Tips

### Configuration Cache (v2.2.2+)

Enable in `gradle.properties` for 10-50% faster builds:
```properties
org.gradle.configuration-cache=true
```

### Parallel Execution

Enable parallel project execution:
```properties
org.gradle.parallel=true
```

### Build Cache

Enable task output caching:
```properties
org.gradle.caching=true
```

### Memory Optimization

For large DITA projects:
```bash
export GRADLE_OPTS="-Xmx2g -XX:+UseG1GC"
```

---

## Documentation & Resources

- **[Main README](../README.md)** - Project overview and general usage
- **[Configuration Reference](../docs/CONFIGURATION_REFERENCE.md)** - All configuration options
- **[Troubleshooting Guide](../docs/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Migration Guide](../docs/MIGRATION_GUIDE.md)** - Upgrade from v0.7.1
- **[Best Practices](../docs/BEST_PRACTICES.md)** - Performance and CI/CD patterns

---

## Getting Help

If you encounter issues:

1. **Check Troubleshooting Guide:** See section above or [TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md)
2. **Review Example Source:** Each example includes detailed comments in `build.gradle`/`build.gradle.kts`
3. **Check Logs:** Run with `--stacktrace` or `--debug` for more details:
   ```bash
   gradle dita -PditaHome=$DITA_HOME --stacktrace
   ```
4. **Report Issues:** [GitHub Issues](https://github.com/jyjeanne/dita-ot-gradle/issues)

---

## Contributing Examples

To add new examples:

1. Create a new directory: `examples/my-example/`
2. Add both `build.gradle` and `build.gradle.kts`
3. Include a `README.md` describing the example
4. Add sample DITA files
5. Update this README with run instructions

---

**Happy Publishing! 🚀**
