# DITA-OT Gradle Plugin

[![CI/CD](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml/badge.svg)](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.jyjeanne.dita-ot-gradle)](https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle)

A [Gradle] plugin for publishing DITA documents with [DITA Open Toolkit].

**Note**: This is a continuation and evolution of the original [com.github.eerohele.dita-ot-gradle](https://github.com/eerohele/dita-ot-gradle) plugin, migrated to Kotlin and maintained with updated dependencies and improvements.

**Author**: Jeremy Jeanne
**Original Author**: Eero Helenius

## Installation

The plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle) and can be easily added to your project.

### Groovy DSL (build.gradle)

```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.0'
}
```

### Kotlin DSL (build.gradle.kts)

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.0"
}
```

## Compatibility

### Migration from Original Plugin

This plugin is **NOT a drop-in replacement** for `com.github.eerohele.dita-ot-gradle`. You need to change the plugin ID, but the rest of your configuration remains compatible.

**Required Change:**
```groovy
// OLD
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

// NEW
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.0'
}
```

**What Stays the Same:**
- Task name: `dita` (unchanged)
- All configuration methods: `ditaOt()`, `input()`, `transtype()`, `filter()`, `properties{}`, etc.
- Your existing build configuration should work without changes

**Breaking Changes:**
- Plugin ID changed from `com.github.eerohele.dita-ot-gradle` to `io.github.jyjeanne.dita-ot-gradle`
- Deprecated `ditaOt` setup task removed (v2.0.0) - install DITA-OT plugins manually

### Version Compatibility

| Component | Tested Version | Supported Versions | Notes |
|-----------|----------------|-------------------|-------|
| **DITA-OT** | 3.6 | 3.0+ recommended, 2.x with warnings | Plugin auto-detects version and warns for old versions |
| **Gradle** | 8.5 | 4.x+ (4.10+ recommended) | Configuration cache requires 6.5+ |
| **Java** | 17 (build), 8 (runtime) | 8+ | Plugin compiled to Java 8 bytecode |
| **Kotlin** | 1.9.25 | N/A | Plugin dependency only |

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
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.0'
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
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.0"
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

Since version 2.2.0, this plugin fully supports Gradle's configuration cache, which can significantly speed up your builds.

### Enabling Configuration Cache

To enable configuration cache, add the `--configuration-cache` flag to your Gradle command:

```bash
gradle dita --configuration-cache
```

Or enable it globally in `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

### Performance Benefits

- **First run**: Configuration phase runs normally and is cached
- **Subsequent runs**: Configuration phase is skipped entirely, directly executing tasks
- **Expected speedup**: 10-50% faster builds, especially beneficial for CI/CD pipelines

### Compatibility Notes

- **✅ Fully supported**: All Kotlin DSL properties (recommended)
- **⚠️ Limited support**: Groovy Closure-based properties may require project state and disable caching
- **Recommendation**: Use Kotlin DSL properties for best configuration cache performance

### Example with Configuration Cache

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.0"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("my.ditamap")
    transtype("html5")

    // Type-safe properties (configuration cache compatible)
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
```

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
