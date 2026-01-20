# DITA-OT Gradle Plugin

[![CI/CD](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml/badge.svg)](https://github.com/jyjeanne/dita-ot-gradle/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.jyjeanne.dita-ot-gradle)](https://plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle)

A modern [Gradle] plugin for publishing DITA documents with [DITA Open Toolkit].

---

## Highlights (v2.4.0)

| Feature | Description |
|---------|-------------|
| **DitaOtDownloadTask** | Built-in DITA-OT download with retries, checksums, and caching |
| **DitaOtInstallPluginTask** | Install plugins from registry, URL, or local files |
| **Configuration Cache** | Up to **77% faster** incremental builds |
| **Improved Error Messages** | Clear, actionable errors with troubleshooting steps |
| **Cross-Platform** | Windows, macOS, Linux support |
| **Modern Architecture** | Provider API, Gradle 9.0 compatible |

**Note**: This is a continuation of the original [com.github.eerohele.dita-ot-gradle](https://github.com/eerohele/dita-ot-gradle) plugin, migrated to Kotlin with modern Gradle support.

**Author**: Jeremy Jeanne | **Original Author**: Eero Helenius

---

## Quick Start

### 1. Add the Plugin

**Groovy DSL** (`build.gradle`):
```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.4.0'
}
```

**Kotlin DSL** (`build.gradle.kts`):
```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.4.0"
}
```

### 2. Download DITA-OT (Built-in)

```groovy
// Groovy DSL
tasks.register('downloadDitaOt', com.github.jyjeanne.DitaOtDownloadTask) {
    version = '4.2.3'
}

// Kotlin DSL
val downloadDitaOt by tasks.registering(com.github.jyjeanne.DitaOtDownloadTask::class) {
    version.set("4.2.3")
}
```

### 3. Configure the Task

```groovy
dita {
    dependsOn downloadDitaOt
    ditaOt layout.buildDirectory.dir('dita-ot/dita-ot-4.2.3')
    input 'docs/guide.ditamap'
    transtype 'html5'
}
```

### 4. Run

```bash
./gradlew dita
```

Output appears in the `build` directory.

---

## Common Use Cases

This plugin supports three main workflows for DITA documentation teams:

### Use Case 1: Testing DITA-OT Plugins from the Registry

**Scenario**: You want to test an existing plugin from the [DITA-OT Plugin Registry](https://www.dita-ot.org/plugins) with different DITA-OT versions.

The [`examples/plugin-test`](examples/plugin-test) project provides a complete solution:

```bash
cd examples/plugin-test

# Test with defaults (DITA-OT 4.2.3, org.lwdita plugin)
./gradlew test

# Test with specific DITA-OT version
./gradlew -PditaOtVersion=4.1.0 test

# Test different plugin and transtype
./gradlew -PpluginId=org.dita.normalize -Ptranstype=normalize test
```

**Project Structure:**
```
examples/plugin-test/
├── build.gradle           # Automated download, install, transform
├── dita/
│   ├── sample.ditamap
│   └── topics/            # Sample DITA content
└── README.md
```

**Key Tasks:**

| Task | Description |
|------|-------------|
| `downloadDitaOt` | Downloads DITA-OT from GitHub releases |
| `installPlugin` | Installs plugin from official registry |
| `ditaTransform` | Runs the transformation |
| `verifyOutput` | Validates output files exist |
| `test` | Full workflow: download → install → transform → verify |

**Sample `build.gradle` (using built-in tasks):**

```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.4.0'
}

def ditaOtVersion = project.findProperty('ditaOtVersion') ?: '4.2.3'
def pluginId = project.findProperty('pluginId') ?: 'org.lwdita'
def selectedTranstype = project.findProperty('transtype') ?: 'markdown'

// Built-in download task - no external plugins needed!
tasks.register('downloadDitaOt', com.github.jyjeanne.DitaOtDownloadTask) {
    version = ditaOtVersion
    destinationDir = layout.buildDirectory.dir('dita-ot')
    retries = 3  // Automatic retry on failure
}

// Built-in plugin installation task
tasks.register('installPlugin', com.github.jyjeanne.DitaOtInstallPluginTask) {
    dependsOn downloadDitaOt
    ditaOtDir = layout.buildDirectory.dir("dita-ot/dita-ot-${ditaOtVersion}")
    plugins = [pluginId]
    retries = 2
}
```

---

### Use Case 2: Developing a Custom DITA-OT Plugin

**Scenario**: You're developing a new DITA-OT plugin and need a fast iteration workflow with automatic testing.

The [`examples/custom-plugin-dev`](examples/custom-plugin-dev) project provides a complete plugin development environment:

```bash
cd examples/custom-plugin-dev

# Run development build (download DITA-OT, install plugin, transform)
./gradlew dev

# Auto-rebuild when plugin source changes
./gradlew dev --continuous
```

**Project Structure:**
```
my-dita-plugin/
├── build.gradle
├── src/
│   └── my-plugin/
│       ├── plugin.xml
│       ├── xsl/
│       └── ...
└── test-content/
    ├── test.ditamap
    └── topics/
```

**Sample `build.gradle` (using built-in tasks):**

```groovy
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.4.0'
}

def ditaOtVersion = '4.2.3'
def ditaOtDir = layout.buildDirectory.dir("dita-ot/dita-ot-${ditaOtVersion}")
def myPluginDir = file('src/my-plugin')

// ============================================================================
// Download DITA-OT (built-in - no external plugins needed!)
// ============================================================================

tasks.register('downloadDitaOt', com.github.jyjeanne.DitaOtDownloadTask) {
    version = ditaOtVersion
    destinationDir = layout.buildDirectory.dir('dita-ot')
    retries = 3
}

// ============================================================================
// Install Your Custom Plugin (built-in)
// ============================================================================

tasks.register('installMyPlugin', com.github.jyjeanne.DitaOtInstallPluginTask) {
    description = 'Install custom plugin into DITA-OT'
    group = 'Plugin Development'

    dependsOn downloadDitaOt

    ditaOtDir = layout.buildDirectory.dir("dita-ot/dita-ot-${ditaOtVersion}")
    plugins = [myPluginDir.absolutePath]  // Local plugin path
    force = true  // Reinstall to pick up changes
}

// ============================================================================
// Test Your Plugin
// ============================================================================

tasks.register('testMyPlugin', com.github.jyjeanne.DitaOtTask) {
    description = 'Test custom plugin transformation'
    group = 'Plugin Development'

    dependsOn installMyPlugin

    ditaOt ditaOtDir
    input file('test-content/test.ditamap')
    output file('build/test-output')
    transtype 'my-custom-transtype'  // Your plugin's transtype

    properties {
        property(name: 'processing-mode', value: 'strict')
    }
}

// ============================================================================
// Development Workflow
// ============================================================================

task dev {
    description = 'Quick development cycle: install plugin and test'
    group = 'Plugin Development'
    dependsOn testMyPlugin
}

// Run with: ./gradlew dev --continuous
// This will automatically rebuild when plugin source changes!
```

**Development Commands:**

```bash
# One-time setup
./gradlew extractDitaOt

# Development cycle (runs when files change)
./gradlew dev --continuous

# Full clean rebuild
./gradlew clean dev

# Check plugin installation
./gradlew installMyPlugin --info
```

---

### Use Case 3: CI/CD Documentation Publishing

**Scenario**: Automate DITA documentation builds in your CI/CD pipeline with reproducible results.

The [`examples/ci-cd-publishing`](examples/ci-cd-publishing) project provides a complete CI/CD setup:

```bash
cd examples/ci-cd-publishing

# Generate all documentation formats (HTML and PDF)
./gradlew generateDocs

# Test with specific DITA-OT version
./gradlew generateDocs -PditaOtVersion=4.1.0
```

**Project Structure:**
```
docs-project/
├── build.gradle.kts
├── gradle.properties
├── docs/
│   ├── guide.ditamap
│   ├── release.ditaval
│   └── topics/
└── .github/
    └── workflows/
        └── docs.yml
```

**Sample `build.gradle.kts` (using built-in tasks):**

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.4.0"
}

val ditaOtVersion: String by project  // From gradle.properties
val ditaOtDir = layout.buildDirectory.dir("dita-ot/dita-ot-$ditaOtVersion")

// Download DITA-OT (built-in - no external plugins needed!)
val downloadDitaOt by tasks.registering(com.github.jyjeanne.DitaOtDownloadTask::class) {
    version.set(ditaOtVersion)
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))
    retries.set(3)  // Automatic retry on failure
}

// Generate HTML documentation
tasks.register<com.github.jyjeanne.DitaOtTask>("generateHtml") {
    dependsOn(downloadDitaOt)

    ditaOt(ditaOtDir)
    input("docs/guide.ditamap")
    output(layout.buildDirectory.dir("docs/html"))
    transtype("html5")
    filter("docs/release.ditaval")

    properties {
        "args.copycss" to "yes"
        "args.css" to "custom-theme.css"
        "nav-toc" to "partial"
    }
}

// Generate PDF documentation
tasks.register<com.github.jyjeanne.DitaOtTask>("generatePdf") {
    dependsOn(downloadDitaOt)

    ditaOt(ditaOtDir)
    input("docs/guide.ditamap")
    output(layout.buildDirectory.dir("docs/pdf"))
    transtype("pdf")
    filter("docs/release.ditaval")

    properties {
        "args.chapter.layout" to "BASIC"
        "outputFile.base" to "user-guide"
    }
}

// Generate all documentation formats
tasks.register("generateDocs") {
    dependsOn("generateHtml", "generatePdf")
    group = "Documentation"
    description = "Generate all documentation formats"
}
```

**Sample `gradle.properties`:**

```properties
ditaOtVersion=4.2.3
org.gradle.configuration-cache=true
```

**Sample GitHub Actions Workflow (`.github/workflows/docs.yml`):**

```yaml
name: Documentation Build

on:
  push:
    branches: [main]
    paths: ['docs/**']
  pull_request:
    paths: ['docs/**']

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        dita-version: ['4.1.0', '4.2.3']  # Test multiple versions

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build Documentation
        run: ./gradlew generateDocs -PditaOtVersion=${{ matrix.dita-version }}

      - name: Upload HTML Docs
        uses: actions/upload-artifact@v4
        with:
          name: html-docs-${{ matrix.dita-version }}
          path: build/docs/html/

      - name: Upload PDF Docs
        uses: actions/upload-artifact@v4
        with:
          name: pdf-docs-${{ matrix.dita-version }}
          path: build/docs/pdf/

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - name: Download HTML Docs
        uses: actions/download-artifact@v4
        with:
          name: html-docs-4.2.3
          path: docs-site/

      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./docs-site
```

**Benefits of CI/CD Integration:**

| Feature | Benefit |
|---------|---------|
| **Reproducible Builds** | Pinned DITA-OT versions ensure consistent output |
| **Matrix Testing** | Test across multiple DITA-OT versions |
| **Artifact Upload** | Store generated docs for review/deployment |
| **Auto-Deploy** | Publish to GitHub Pages or other hosting |
| **Configuration Cache** | 77% faster incremental builds in CI |

---

### Use Case 4: Multi-Language Documentation

**Scenario**: Your product documentation needs to be published in multiple languages from localized DITA sources.

The [`examples/multi-language`](examples/multi-language) project demonstrates parallel builds for all languages:

```bash
cd examples/multi-language

# Build all languages in parallel (English, French, German)
./gradlew buildAllLanguages

# Build specific language
./gradlew buildEnglish
./gradlew buildFrench

# Build all languages with PDF
./gradlew release
```

**Project Structure:**
```
multi-language/
├── build.gradle.kts
├── content/
│   ├── en/                   # English source
│   │   ├── guide.ditamap
│   │   └── topics/
│   ├── fr/                   # French source
│   │   └── ...
│   └── de/                   # German source
│       └── ...
└── shared/
    └── images/               # Shared across languages
```

**Key Features:**

| Feature | Benefit |
|---------|---------|
| **Parallel Execution** | Build 5 languages in time of 1 |
| **Shared Assets** | Images and resources reused across languages |
| **Organized Output** | `build/output/en/`, `build/output/fr/`, etc. |

---

### Use Case 5: Product Version Documentation

**Scenario**: Your software has multiple supported versions (v1.x LTS, v2.x, v3.x) and you need documentation for each from a single source using DITA conditional processing.

The [`examples/version-docs`](examples/version-docs) project demonstrates version-specific builds:

```bash
cd examples/version-docs

# Build all versions in parallel
./gradlew buildAllVersions

# Build specific version
./gradlew buildV1      # v1.x (LTS)
./gradlew buildV3      # v3.x (Latest)

# Build only the latest version
./gradlew buildLatest
```

**DITA Profiling Example:**
```xml
<!-- Content for all versions -->
<p>Basic installation steps...</p>

<!-- v2+ only (Docker support) -->
<section product="v2-v3">
  <title>Docker Installation</title>
  <p>New in v2.0: Docker support...</p>
</section>

<!-- v3 only (Kubernetes) -->
<section product="v3-only">
  <title>Kubernetes Deployment</title>
  <p>New in v3.0: Native K8s support...</p>
</section>
```

**Key Features:**

| Feature | Benefit |
|---------|---------|
| **Single Source** | One topic set for all versions |
| **DITA Profiling** | Industry-standard conditional processing |
| **DITAVAL Filters** | Version-specific content inclusion/exclusion |
| **Parallel Builds** | Generate v1, v2, v3 simultaneously |

---

## Examples Directory

| Example | Description | Use Case |
|---------|-------------|----------|
| [`simple`](examples/simple) | Basic single-file transformation | Getting started |
| [`plugin-test`](examples/plugin-test) | Test plugins from DITA-OT registry | Plugin testing |
| [`custom-plugin-dev`](examples/custom-plugin-dev) | Develop and test custom plugins | Plugin development |
| [`ci-cd-publishing`](examples/ci-cd-publishing) | CI/CD documentation pipeline | Automated publishing |
| [`multi-language`](examples/multi-language) | Multi-language parallel builds | Localization |
| [`version-docs`](examples/version-docs) | Version-specific documentation | Product versioning |
| [`download`](examples/download) | Download DITA-OT automatically | CI/CD setup |
| [`filetree`](examples/filetree) | Process multiple files | Bulk processing |
| [`multi-task`](examples/multi-task) | Multiple output formats | Multi-format publishing |
| [`multi-project`](examples/multi-project) | Multi-project Gradle build | Large projects |
| [`classpath`](examples/classpath) | Custom classpath configuration | Advanced customization |
| [`configuration-cache`](examples/configuration-cache) | Configuration cache demo | Performance optimization |
| [`dita-ot-gradle-plugin-documentation`](examples/dita-ot-gradle-plugin-documentation) | Generate plugin docs from DITA | Self-documentation |

---

## Configuration Reference

### Built-in Task Types

#### DitaOtDownloadTask

Automatically downloads and extracts DITA-OT from GitHub releases. **No external plugins required!**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `version` | `String` | `4.2.3` | DITA-OT version to download |
| `destinationDir` | `Directory` | `build/dita-ot` | Where to extract DITA-OT |
| `retries` | `Int` | `3` | Number of retry attempts |
| `checksum` | `String` | - | Optional checksum (e.g., `sha256:abc123...`) |
| `connectTimeout` | `Int` | `30000` | Connection timeout (ms) |
| `readTimeout` | `Int` | `60000` | Read timeout (ms) |
| `quiet` | `Boolean` | `false` | Suppress progress output |

**Example:**
```kotlin
tasks.register<com.github.jyjeanne.DitaOtDownloadTask>("downloadDitaOt") {
    version.set("4.2.3")
    destinationDir.set(layout.buildDirectory.dir("dita-ot"))
    retries.set(3)
}
```

#### DitaOtInstallPluginTask

Installs DITA-OT plugins from registry, URL, or local files.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `ditaOtDir` | `Directory` | - | DITA-OT installation directory (required) |
| `plugins` | `List<String>` | - | Plugin IDs, URLs, or file paths (required) |
| `force` | `Boolean` | `false` | Force reinstall existing plugins |
| `failOnError` | `Boolean` | `true` | Fail build on installation error |
| `retries` | `Int` | `0` | Number of retry attempts |
| `quiet` | `Boolean` | `false` | Suppress progress output |

**Example:**
```kotlin
tasks.register<com.github.jyjeanne.DitaOtInstallPluginTask>("installPlugins") {
    dependsOn(downloadDitaOt)
    ditaOtDir.set(layout.buildDirectory.dir("dita-ot/dita-ot-4.2.3"))
    plugins.set(listOf(
        "org.dita.pdf2",                    // From registry
        "https://example.com/plugin.zip",   // From URL
        "/path/to/local/plugin.zip"         // Local file
    ))
    retries.set(2)
}
```

### DitaOtTask Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `ditaOt` | `String` or `File` | - | DITA-OT installation directory (required) |
| `input` | `String` or `File` | - | Input file or files (required) |
| `output` | `String` or `File` | `build` | Output directory |
| `transtype` | `String...` | `html5` | Output format(s) |
| `filter` | `String` or `File` | - | DITAVAL filter file |
| `singleOutputDir` | `Boolean` | `false` | Multiple inputs → single output |
| `useAssociatedFilter` | `Boolean` | `false` | Use DITAVAL with same basename |

### Passing Properties to DITA-OT

**Groovy DSL:**
```groovy
dita {
    ditaOt '/path/to/dita-ot'
    input 'guide.ditamap'
    transtype 'html5'

    properties {
        property(name: 'processing-mode', value: 'strict')
        property(name: 'args.css', value: 'custom.css')
        property(name: 'args.cssroot', value: "${projectDir}/css")
    }
}
```

**Kotlin DSL:**
```kotlin
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("guide.ditamap")
    transtype("html5")

    properties {
        "processing-mode" to "strict"
        "args.css" to "custom.css"
        "args.cssroot" to "$projectDir/css"
    }
}
```

**Direct API (recommended for Configuration Cache):**
```kotlin
tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaProperties.put("processing-mode", "strict")
    ditaProperties.put("args.css", "custom.css")
}
```

---

## Configuration Cache Support

### Performance Benchmarks

Tested on Windows 11, Gradle 8.5, DITA-OT 3.6:

| Scenario | Time | Improvement |
|----------|------|-------------|
| Without Configuration Cache | 20.8s | baseline |
| With Cache (first run) | 22.8s | -10% (stores cache) |
| **With Cache (up-to-date)** | **4.8s** | **77% faster** |
| With Cache (clean build) | 22.4s | reuses configuration |

### Enabling Configuration Cache

```bash
# Per-command
./gradlew dita --configuration-cache

# Or globally in gradle.properties
org.gradle.configuration-cache=true
```

---

## Compatibility

| Component | Tested | Supported | Notes |
|-----------|--------|-----------|-------|
| **DITA-OT** | 3.6, 4.x | 3.0+ | Auto-detects version |
| **Gradle** | 8.5, 8.10, 9.0 | 6.5+ | Configuration cache requires 6.5+ |
| **Java** | 17 (build) | 8+ | Compiled to Java 8 bytecode |

### Gradle Version Recommendations

| Gradle Version | Support Level | Recommendation |
|----------------|---------------|----------------|
| **9.0+** | Full | **Recommended** - Best performance and features |
| **8.5 - 8.14** | Full | Stable, production-ready |
| **7.0 - 8.4** | Compatible | Works, but upgrade recommended |
| **6.5 - 6.9** | Limited | Minimum for configuration cache |
| **< 6.5** | Not supported | Upgrade required |

### Benefits of Gradle 9+

Gradle 9 introduces significant improvements that enhance this plugin's performance:

| Feature | Benefit |
|---------|---------|
| **Improved Configuration Cache** | More reliable caching with fewer invalidations |
| **Faster Dependency Resolution** | Up to 25% faster build initialization |
| **Kotlin DSL Performance** | 2x faster script compilation |
| **Better Parallel Execution** | Improved task scheduling for multi-language/version builds |
| **Reduced Memory Usage** | Lower heap consumption for large documentation projects |
| **Enhanced Error Messages** | Clearer diagnostics for build failures |

**Upgrade to Gradle 9:**

```bash
# Update gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip

# Or use the wrapper task
./gradlew wrapper --gradle-version 9.0
```

**Enable Configuration Cache (Gradle 6.5+):**

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.parallel=true
```

---

## Migration from eerohele Plugin

### Quick Migration

```groovy
// OLD
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

// NEW
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.4.0'
}
```

**That's it!** Your existing `dita { }` configuration works unchanged.

### What's New

| Feature | v0.7.1 (eerohele) | v2.4.0 (jyjeanne) |
|---------|-------------------|-------------------|
| Gradle 8+ | No | Yes |
| Gradle 9+ | No | Yes |
| Configuration Cache | No | Yes (77% faster) |
| **Built-in DITA-OT Download** | No | **Yes** (DitaOtDownloadTask) |
| **Built-in Plugin Install** | No | **Yes** (DitaOtInstallPluginTask) |
| Kotlin DSL | Limited | Full support |
| Cross-platform | Partial | Full (Win/Mac/Linux) |
| Active maintenance | No (since 2020) | Yes |

See [CHANGELOG.md](docs/CHANGELOG.md) for full version history.

---

## Troubleshooting

### Common Issues

**Plugin not found:**
```
> Could not resolve plugin artifact 'io.github.jyjeanne.dita-ot-gradle'
```
**Solution:** Check your internet connection and Gradle Plugin Portal access.

**DITA-OT directory not set:**
```
> DITA-OT directory not configured.
```
**Solution:** Add `ditaOt '/path/to/dita-ot'` to your task configuration.

**Configuration cache warnings:**
```
> Configuration cache problems found
```
**Solution:** Use Kotlin DSL `properties { }` block instead of Groovy closures for full compatibility.

---

## Documentation

| Guide | Purpose |
|-------|---------|
| [Migration Guide](docs/MIGRATION_GUIDE.md) | Migrate from eerohele plugin |
| [Configuration Reference](docs/CONFIGURATION_REFERENCE.md) | All configuration options |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [Best Practices](docs/BEST_PRACTICES.md) | Performance and CI/CD tips |
| [Architecture](docs/ARCHITECTURE.md) | Plugin internals explained |
| [Roadmap](docs/ROADMAP.md) | Planned features and improvements |

---

## License

Licensed under the [Apache License 2.0].

[Apache License 2.0]: https://www.apache.org/licenses/LICENSE-2.0.html
[DITA Open Toolkit]: https://www.dita-ot.org
[Gradle]: https://gradle.org
