# DITA-OT Gradle Plugin

[![CI/CD](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml/badge.svg)](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.jyjeanne.dita-ot-gradle)](https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle)

A [Gradle] plugin for publishing DITA documents with [DITA Open Toolkit].

## Highlights (v2.3.1)

- **Full Configuration Cache Support** - Up to **77% faster** incremental builds
- **IsolatedAntBuilder Fix** - DITA_SCRIPT strategy resolves classloader issues
- **Provider API Architecture** - Modern Gradle best practices
- **Cross-Platform** - Windows, macOS, Linux support

**Note**: This is a continuation and evolution of the original [com.github.eerohele.dita-ot-gradle](https://github.com/eerohele/dita-ot-gradle) plugin, migrated to Kotlin and maintained with updated dependencies and improvements.

**Author**: Jeremy Jeanne
**Original Author**: Eero Helenius

## Installation

The plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle) and can be easily added to your project.

### Groovy DSL (build.gradle)

```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.3.1'
}
```

### Kotlin DSL (build.gradle.kts)

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}
```

## Migration Guide

### Migrating from `com.github.eerohele.dita-ot-gradle`

This plugin is a **continuation and evolution** of the original eerohele plugin. While it's **NOT a drop-in replacement**, migration is straightforward and takes only a few minutes.

#### Quick Migration (TL;DR)

1. Change plugin ID: `com.github.eerohele.dita-ot-gradle` → `io.github.jyjeanne.dita-ot-gradle`
2. Update version: `0.7.1` → `2.3.1`
3. Remove deprecated `ditaOt` setup task (if used)
4. Test your build: `gradle dita`

#### Step-by-Step Migration

##### Step 1: Update Plugin Declaration

**Groovy DSL (`build.gradle`):**
```groovy
// OLD
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

// NEW
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.3.1'
}
```

**Kotlin DSL (`build.gradle.kts`):**
```kotlin
// OLD
plugins {
    id("com.github.eerohele.dita-ot-gradle") version "0.7.1"
}

// NEW
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}
```

##### Step 2: Verify Task Configuration (No Changes Needed!)

Your existing `dita` task configuration remains **100% compatible**:

```groovy
dita {
    ditaOt '/path/to/dita-ot'
    input 'my.ditamap'
    transtype 'html5'
    filter 'my.ditaval'

    properties {
        property(name: 'processing-mode', value: 'strict')
    }
}
```

✅ **All these methods work unchanged:**
- `ditaOt()` - Set DITA-OT directory
- `input()` - Set input files
- `output()` - Set output directory
- `transtype()` - Set output formats
- `filter()` - Set DITAVAL filter
- `properties{}` - Set Ant properties
- `singleOutputDir()` - Multiple inputs to single output
- `useAssociatedFilter()` - Use associated DITAVAL

##### Step 3: Remove Deprecated Setup Task (If Applicable)

If you were using the deprecated `ditaOt` setup task for plugin installation, remove it:

```groovy
// REMOVE THIS (no longer supported)
tasks.register('installPlugins') {
    dependsOn ditaOt
}
```

**Alternative:** Install DITA-OT plugins manually before running builds:
```bash
cd /path/to/dita-ot
bin/dita install <plugin-id>
```

Or use the [download example](examples/download) which automates plugin installation.

##### Step 4: Test Your Build

Run your build to verify everything works:

```bash
gradle dita
```

You should see enhanced logging with build metrics (new in v2.1.0):
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

##### Step 5: Optional Enhancements (v2.1.0+ Features)

**Enable Configuration Cache (v2.2.0+):**

Create or update `gradle.properties`:
```properties
org.gradle.configuration-cache=true
```

**Benefits:** 10-50% faster builds on subsequent runs!

**Use Type-Safe Kotlin DSL (v2.1.0+):**

If using Kotlin DSL, upgrade to the new type-safe properties syntax:

```kotlin
// OLD (still works, but verbose)
properties(groovy.lang.Closure<Any>(this) {
    val delegate = delegate as? groovy.lang.GroovyObject
    delegate?.invokeMethod("property", mapOf("name" to "processing-mode", "value" to "strict"))
})

// NEW (recommended)
properties {
    "processing-mode" to "strict"
    "args.rellinks" to "all"
    "args.cssroot" to "$projectDir/css"
}
```

#### Migration Checklist

Use this checklist to ensure a smooth migration:

- [ ] Updated plugin ID in `build.gradle` or `build.gradle.kts`
- [ ] Updated version to `2.3.1`
- [ ] Removed deprecated `ditaOt` setup task (if used)
- [ ] Tested build with `gradle dita`
- [ ] Verified output is generated correctly
- [ ] (Optional) Enabled configuration cache in `gradle.properties`
- [ ] (Optional) Updated to type-safe Kotlin DSL properties
- [ ] (Optional) Reviewed new logging output for insights

#### What's Compatible

✅ **100% Compatible** (no changes needed):
- Task name: `dita`
- All configuration methods
- Groovy DSL syntax
- Input/output file handling
- DITAVAL filtering
- Ant properties
- Multi-file processing
- Custom classpath configuration

#### What Changed

⚠️ **Breaking Changes:**
- **Plugin ID changed** (required): `com.github.eerohele.dita-ot-gradle` → `io.github.jyjeanne.dita-ot-gradle`
- **Setup task removed** (v2.0.0): Deprecated `ditaOt` task for plugin installation no longer available

✨ **New Features** (optional but recommended):
- **Configuration Cache** (v2.2.0): 10-50% faster builds
- **Enhanced Logging** (v2.1.0): Detailed build metrics and reports
- **Type-Safe Kotlin DSL** (v2.1.0): Better IDE support and autocomplete
- **Input Validation** (v2.1.0): Catches configuration errors early
- **Version Detection** (v2.1.0): Auto-detects DITA-OT version

#### Troubleshooting Migration

**Problem:** Build fails with "Could not find plugin"
```
Solution: Ensure you've updated the plugin ID to 'io.github.jyjeanne.dita-ot-gradle'
```

**Problem:** Task `ditaOt` not found (plugin installation)
```
Solution: This task was removed in v2.0.0. Install DITA-OT plugins manually:
  cd /path/to/dita-ot
  bin/dita install <plugin-id>

Or use the automated download example: examples/download/
```

**Problem:** Configuration cache warnings in Groovy DSL
```
Solution: This is expected. For best configuration cache support, use Kotlin DSL.
Groovy Closure properties may require project state and disable caching.

Alternatively, disable configuration cache:
  gradle dita --no-configuration-cache
```

**Problem:** Different logging output
```
Solution: This is normal. v2.1.0+ includes enhanced logging with metrics.
To see less output, use: gradle dita --quiet
To see more details, use: gradle dita --info
```

#### Migration Support

- **Examples:** See [examples/](examples/) directory for updated examples
- **Issues:** Report problems at https://github.com/jyjeanne/dita-ot-gradle/issues
- **Changelog:** Full version history in [CHANGELOG.md](CHANGELOG.md)

## Compatibility

### Version Compatibility

| Component | Tested Version | Supported Versions | Notes |
|-----------|----------------|-------------------|-------|
| **DITA-OT** | 3.6 | 3.0+ recommended, 2.x with warnings | Plugin auto-detects version and warns for old versions |
| **Gradle** | 8.5, 8.10, 9.0 | 6.5+ | Configuration cache requires 6.5+ |
| **Java** | 17 (build), 8 (runtime) | 8+ | Plugin compiled to Java 8 bytecode |
| **Kotlin** | 2.1.0 | N/A | Plugin dependency only |

**DITA-OT Version Detection:**
The plugin automatically detects your DITA-OT version and will warn if using a version older than 3.0.

**Gradle Features:**
- Configuration Cache support (v2.2.0+) requires Gradle 6.5+
- Incremental builds supported on all tested versions
- Continuous builds (`--continuous`) supported on Gradle 4.3+

## Use

### Groovy DSL (build.gradle)

In your Gradle build script (`build.gradle`), add something like this:

```gradle
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.3.1'
}

// Publish my.ditamap into the HTML5 output format.
dita {
    ditaOt '/path/to/my/dita-ot/directory'
    input 'my.ditamap'
    transtype 'html5'
}
```

### Kotlin DSL (build.gradle.kts)

In your Kotlin DSL build script (`build.gradle.kts`), add something like this:

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}

// Publish my.ditamap into the HTML5 output format.
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/my/dita-ot/directory"))
    input("my.ditamap")
    transtype("html5")
}
```

Then, in the directory where you saved the file, run:

```bash
gradle dita
```

By default, the output appears in the `build` subdirectory.

## Features

- **Fast builds**: After the first build, (much) faster than running DITA-OT directly, thanks to the [Gradle Daemon].
- **Configuration Cache**: Full support for Gradle's configuration cache for faster build times (v2.2.0+).
- **Easy to configure**: Simple DSL for both Groovy and Kotlin.
- **Versatile**: Publish [multiple documents at once](https://github.com/jyjeanne/dita-ot-gradle/tree/master/examples/filetree).
- **Incremental builds**: Only build DITA documents that have changed.
- **Continuous builds**: Automatically republish your document when it changes (Gradle's `--continuous` option).
- **Comprehensive logging**: Detailed build reports with metrics and progress tracking.

## Examples

To get started, see [the simple example](https://github.com/jyjeanne/dita-ot-gradle/tree/master/examples/simple) buildfile.

For more examples, see the [`examples`](https://github.com/jyjeanne/dita-ot-gradle/tree/master/examples) directory in this repository.

You're most welcome to contribute improvements on the current set of examples or entirely new examples.

## Documentation

Comprehensive documentation for all use cases:

| Guide | Purpose |
|-------|---------|
| **[Migration Guide](docs/MIGRATION_GUIDE.md)** | Detailed instructions for migrating from eerohele plugin (v0.7.1) to jyjeanne plugin (v2.2.0) |
| **[Configuration Reference](docs/CONFIGURATION_REFERENCE.md)** | Complete reference of all configuration options with examples |
| **[Troubleshooting Guide](docs/TROUBLESHOOTING.md)** | Solutions for common issues and problems |
| **[Best Practices](docs/BEST_PRACTICES.md)** | Proven strategies for performance, CI/CD, and team collaboration |
| **[ANT Execution Strategies](docs/ANT_EXECUTION_STRATEGIES.md)** | Technical analysis of ANT execution alternatives (advanced) |
| **[Future Work Implementation](docs/FUTURE_WORK_IMPLEMENTATION.md)** | Implementation status of planned features |

### Quick Links

- **Migrating from eerohele?** → Start with [Migration Guide](docs/MIGRATION_GUIDE.md)
- **Need help with configuration?** → See [Configuration Reference](docs/CONFIGURATION_REFERENCE.md)
- **Something not working?** → Check [Troubleshooting Guide](docs/TROUBLESHOOTING.md)
- **Want to optimize?** → Read [Best Practices](docs/BEST_PRACTICES.md)
- **Looking for examples?** → Browse [`examples/`](examples/) directory

## Downloading DITA Open Toolkit

You can use the [Gradle Download Task](https://github.com/michel-kraemer/gradle-download-task) to download DITA-OT and
use the downloaded version in your build. See the [`download` example](https://github.com/jyjeanne/dita-ot-gradle/blob/master/examples/download/build.gradle) for an example.

## Options

| Type | Option | Description |
| ---- | ------ | ----------- |
| `String` or `File` | `input` | The input file. |
| `String` or `File` | `output` | The output directory. Default: `build`. |
| `String...` |	`transtype` | One or more formats to publish into. |
| `String` or `File` | `filter` | Path to DITAVAL file to use for publishing. |
| `Boolean` | `singleOutputDir` | Multiple input files ➞ single output directory. Default: `false`. |
| `Boolean` |	`useAssociatedFilter` |	For every input file, use DITAVAL file in same directory with same basename. Default: `false`. |

## Configuration Cache Support

Since version 2.3.1, this plugin **fully supports** Gradle's configuration cache with the DITA_SCRIPT execution strategy (default).

### Performance Benchmarks

Tested on Windows 11, Gradle 8.5, DITA-OT 3.6 (generating HTML5 + PDF):

| Scenario | Time | Improvement |
|----------|------|-------------|
| Without Configuration Cache | 20.8s | baseline |
| With Cache (first run) | 22.8s | -10% (stores cache) |
| **With Cache (up-to-date)** | **4.8s** | **77% faster** |
| With Cache (clean build) | 22.4s | reuses configuration |

### Enabling Configuration Cache

Add the `--configuration-cache` flag to your Gradle command:

```bash
# First run (stores cache)
gradle dita --configuration-cache
# Output: "Configuration cache entry stored."

# Second run (reuses cache - 77% faster!)
gradle dita --configuration-cache
# Output: "Reusing configuration cache."
```

Or enable it globally in `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

### How It Works

1. **First run**: Gradle calculates the task graph and stores it in cache (~22s)
2. **Subsequent runs**: Gradle skips configuration phase entirely (~5s for up-to-date checks)
3. **Clean builds**: Configuration is reused, only execution phase runs

### Compatibility Notes

| Feature | Status | Notes |
|---------|--------|-------|
| **DITA_SCRIPT strategy** | ✅ Fully supported | Default, recommended |
| **Kotlin DSL properties** | ✅ Fully supported | Use `properties { }` block |
| **Groovy DSL properties** | ⚠️ Works with warnings | Closures not serializable |
| **IsolatedAntBuilder** | ❌ Not compatible | Legacy strategy, use DITA_SCRIPT |

### Example with Configuration Cache

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("my.ditamap")
    transtype("html5", "pdf")  // Multiple formats

    // Type-safe properties (configuration cache compatible)
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
```

### Try the Configuration Cache Example

See the [configuration-cache example](examples/configuration-cache) for a complete demo with benchmark instructions.

## Passing Ant properties to DITA-OT

### Groovy DSL

To pass an Ant property to DITA-OT, use the `properties` block. For example:

```groovy
// Give DITA-OT additional parameters.
//
// For a list of the parameters DITA-OT understands, see:
// http://www.dita-ot.org/2.1/parameters/
properties {
    property(name: 'processing-mode', value: 'strict')
}
```

If your Ant properties are paths (such as `args.cssroot`), you need to use absolute paths. If the path is under the same directory as `build.gradle`, you can use the `projectDir` variable:

```groovy
properties {
    // Note the double quotes around the value; with single quotes,
    // the projectDir variable won't be expanded.
    property(name: 'args.cssroot', value: "${projectDir}/my/awesome/path")
}
```

### Kotlin DSL

To pass Ant properties in Kotlin DSL, use the type-safe `properties` block:

```kotlin
// Give DITA-OT additional parameters.
//
// For a list of the parameters DITA-OT understands, see:
// http://www.dita-ot.org/2.1/parameters/
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
```

You can also use the `property()` method:

```kotlin
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    properties {
        property("processing-mode", "strict")
    }
}
```

For paths, use the `projectDir` property:

```kotlin
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    properties {
        "args.cssroot" to "$projectDir/my/awesome/path"
    }
}
```

## License

The DITA-OT Gradle Plugin is licensed for use under the [Apache License 2.0].

[Apache License 2.0]: https://www.apache.org/licenses/LICENSE-2.0.html
[DITA Open Toolkit]: https://www.dita-ot.org
[Gradle]: https://gradle.org
[Gradle Daemon]: https://docs.gradle.org/current/userguide/gradle_daemon.html
