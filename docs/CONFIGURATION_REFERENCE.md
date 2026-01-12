# Configuration Reference Guide

Complete reference for all DITA-OT Gradle Plugin configuration options.

---

## Table of Contents

1. [Configuration Methods](#configuration-methods)
2. [Input Options](#input-options)
3. [Output Options](#output-options)
4. [Transformation Options](#transformation-options)
5. [Advanced Options](#advanced-options)
6. [Properties and Parameters](#properties-and-parameters)
7. [Common Configurations](#common-configurations)
8. [Type Information](#type-information)

---

## Configuration Methods

All configuration methods can be used in the `dita` block:

```groovy
// Groovy DSL
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    // Configuration goes here
}
```

```kotlin
// Kotlin DSL
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    // Configuration goes here
}
```

---

## Input Options

### ditaOt (Required)

Specifies the location of the DITA Open Toolkit installation.

**Type:** `File` or `String`

**Required:** Yes (unless using download example)

**Default:** None

**Examples:**

```groovy
// Groovy - Absolute path
dita {
    ditaOt '/opt/dita-ot-3.6'
}

// Groovy - Relative path
dita {
    ditaOt '../tools/dita-ot'
}

// Groovy - Using project property
dita {
    ditaOt findProperty('ditaHome')
}
```

```kotlin
// Kotlin - File object
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-3.6"))
}

// Kotlin - String
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt("/opt/dita-ot-3.6")
}
```

**Command line override:**
```bash
gradle dita -PditaHome=/opt/dita-ot-3.6
```

**Notes:**
- Plugin auto-detects DITA-OT version and warns if version < 3.0
- Must contain valid DITA-OT installation with `bin/dita` or `bin/dita.bat`

---

### input

Specifies the input file(s) to be transformed.

**Type:** `String`, `File`, or `FileCollection`

**Required:** Yes

**Default:** None

**Single File Examples:**

```groovy
// Groovy - Direct filename
dita {
    input 'docs/root.ditamap'
}

// Groovy - Absolute path
dita {
    input '/absolute/path/root.ditamap'
}

// Groovy - With File object
dita {
    input file('src/dita/root.ditamap')
}
```

```kotlin
// Kotlin - String
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    input("docs/root.ditamap")
}

// Kotlin - File object
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    input(file("src/dita/root.ditamap"))
}
```

**Multiple Files Examples:**

```groovy
// Groovy - FileTree with glob pattern
dita {
    input fileTree('dita') {
        include '**/*.ditamap'
        exclude '**/temp/**'
    }
}

// Groovy - Multiple individual files
dita {
    input 'docs/doc1.ditamap'
    input 'docs/doc2.ditamap'
    input 'docs/doc3.ditamap'
}

// Groovy - FileCollection
dita {
    input files('docs/*.ditamap')
}
```

```kotlin
// Kotlin - FileTree
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    input(fileTree("dita") {
        include("**/*.ditamap")
        exclude("**/temp/**")
    })
}
```

**Notes:**
- Paths are resolved relative to project root
- File must exist at configuration time
- .ditamap is typical, but .dita maps also supported
- Relative paths converted to absolute before execution

---

## Output Options

### output

Specifies the output directory for transformed documents.

**Type:** `File` or `String`

**Required:** No

**Default:** `build/` (multiple inputs/formats) or format-specific (single input)

**Examples:**

```groovy
// Groovy - Single output directory
dita {
    output 'build/docs'
}

// Groovy - Nested directory
dita {
    output 'dist/documentation/html'
}

// Groovy - Project relative
dita {
    output "$buildDir/dita-output"
}
```

```kotlin
// Kotlin - String path
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    output("build/docs")
}

// Kotlin - File object
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    output(file("$buildDir/dita-output"))
}
```

**Default behavior without explicit output:**

```groovy
// Single input, single format
dita {
    input 'root.ditamap'
    transtype 'html5'
    // Output goes to: build/html5/
}

// Single input, multiple formats
dita {
    input 'root.ditamap'
    transtype 'html5', 'pdf'
    // Outputs go to: build/html5/ and build/pdf/
}

// Multiple inputs, multiple formats
dita {
    input fileTree('dita') { include '*.ditamap' }
    transtype 'html5', 'pdf'
    // Outputs go to: build/
    // (use singleOutputDir false for this)
}
```

**With singleOutputDir:**

```groovy
dita {
    input fileTree('dita') { include '*.ditamap' }
    transtype 'html5'
    output 'build/output'
    singleOutputDir true
    // All files output to: build/output/
}
```

**Notes:**
- Directory is created if it doesn't exist
- Parent directories created automatically
- Must be writable
- Can't use for multiple transtypes unless `singleOutputDir true`

---

## Transformation Options

### transtype

Specifies the output format(s) to generate.

**Type:** `String` (varargs) or `List<String>`

**Required:** No

**Default:** `'html5'`

**Common Transtypes:**

| Transtype | Description |
|-----------|-------------|
| `html5` | HTML5 single file or navigation |
| `pdf` | PDF (requires FOP plugin) |
| `pdf2` | PDF using DITA-OT PDF plugin (recommended) |
| `xhtml` | XHTML output |
| `eclipsehelp` | Eclipse Help plugin format |
| `markdown` | Markdown output (plugin required) |

**Examples:**

```groovy
// Groovy - Single format
dita {
    transtype 'html5'
}

// Groovy - Multiple formats (varargs)
dita {
    transtype 'html5', 'pdf', 'markdown'
}

// Groovy - List syntax
dita {
    transtype(['html5', 'pdf'])
}
```

```kotlin
// Kotlin - Single format
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    transtype("html5")
}

// Kotlin - Multiple formats (varargs)
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    transtype("html5", "pdf", "markdown")
}

// Kotlin - List syntax
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    transtype("html5", "pdf")
}
```

**Command line override:**

```bash
# Override transtypes
gradle dita -Dtranstype=html5,pdf2

# Add to defaults
gradle dita -Dtranstype=pdf2
```

**Notes:**
- Not all transtypes work by default (may require plugins)
- PDF output requires `pdf2` plugin (see examples/download)
- Multiple transtypes create separate output directories
- Use `singleOutputDir true` to combine into one directory

---

### filter (DITAVAL)

Specifies a DITAVAL (conditional text) filter file.

**Type:** `File` or `String`

**Required:** No

**Default:** None (no filtering)

**Examples:**

```groovy
// Groovy - Simple filter
dita {
    filter 'src/dita/product-a.ditaval'
}

// Groovy - Absolute path
dita {
    filter file('/absolute/path/filter.ditaval')
}

// Groovy - With input file (associated filter)
dita {
    input 'docs/product-a.ditamap'
    filter 'docs/product-a.ditaval'  // Same basename
}
```

```kotlin
// Kotlin - String path
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    filter("src/dita/product-a.ditaval")
}

// Kotlin - File object
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    filter(file("src/dita/product-a.ditaval"))
}
```

**Associated Filters (automatic matching):**

```groovy
// Enable automatic filter matching by filename
dita {
    input fileTree('docs') {
        include '*.ditamap'
    }
    useAssociatedFilter true
    // docs/product-a.ditamap → uses docs/product-a.ditaval
    // docs/product-b.ditamap → uses docs/product-b.ditaval
}
```

**Notes:**
- DITAVAL file must exist and be valid XML
- Filters are applied during transformation
- Can be used with multiple inputs
- See DITA-OT documentation for DITAVAL syntax

---

## Advanced Options

### singleOutputDir

Controls output directory behavior for multiple inputs/formats.

**Type:** `Boolean`

**Required:** No

**Default:** `false`

**Examples:**

```groovy
// Default (false): Multiple directories per format
dita {
    input fileTree('docs') { include '*.ditamap' }
    transtype 'html5', 'pdf'
    // Output structure:
    // build/html5/   (multiple input files combined)
    // build/pdf/     (multiple input files combined)
}

// With singleOutputDir true
dita {
    input fileTree('docs') { include '*.ditamap' }
    transtype 'html5'
    output 'build/output'
    singleOutputDir true
    // Output structure:
    // build/output/  (all input files in one directory)
}
```

```kotlin
// Kotlin
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    input(fileTree("docs") { include("*.ditamap") })
    transtype("html5")
    output(file("build/output"))
    singleOutputDir = true
}
```

**Notes:**
- Only used with multiple input files
- For single format only
- Useful for combining multiple DITA maps into one output

---

### useAssociatedFilter

Automatically uses DITAVAL files matching input filenames.

**Type:** `Boolean`

**Required:** No

**Default:** `false`

**Examples:**

```groovy
// File structure
// docs/
//   manual.ditamap
//   manual.ditaval      ← automatically used
//   guide.ditamap
//   guide.ditaval       ← automatically used

dita {
    input fileTree('docs') {
        include '*.ditamap'
    }
    useAssociatedFilter true
    transtype 'html5'
    // manual.ditamap filtered with manual.ditaval
    // guide.ditamap filtered with guide.ditaval
}
```

**Notes:**
- Looks for .ditaval file with same basename as input
- Silently skips if filter file doesn't exist
- Can be combined with explicit `filter()` setting
- Useful for per-product configuration

---

### temp

Specifies custom temporary directory for build artifacts.

**Type:** `File` or `String`

**Required:** No

**Default:** Gradle's temporary directory

**Examples:**

```groovy
// Custom temp directory
dita {
    temp file("build/.dita-temp")
}

// Using project property
dita {
    temp file("$buildDir/.dita-tmp")
}
```

```kotlin
// Kotlin
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    temp(file("build/.dita-temp"))
}
```

**Notes:**
- Directory is created if it doesn't exist
- Temporary files deleted after build completes
- Useful for controlling disk space or debugging
- Can speed up builds on slower disks if pointed to faster storage

---

## Properties and Parameters

### properties

Passes custom ANT properties and DITA-OT parameters to the transformation.

**Type:** `Closure` (Groovy) or lambda (Kotlin)

**Required:** No

**Default:** Empty

**Groovy Syntax (Classic):**

```groovy
dita {
    properties {
        property(name: 'processing-mode', value: 'strict')
        property(name: 'args.rellinks', value: 'all')
        property(name: 'args.cssroot', value: "${projectDir}/css")
    }
}
```

**Groovy Syntax (Kotlin DSL in Groovy):**

```groovy
dita {
    properties {
        'processing-mode' to 'strict'
        'args.rellinks' to 'all'
        'args.cssroot' to "${projectDir}/css"
    }
}
```

**Kotlin Syntax:**

```kotlin
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
        "args.cssroot" to "$projectDir/css"
    }
}
```

**Common DITA-OT Parameters:**

| Parameter | Type | Example | Notes |
|-----------|------|---------|-------|
| `processing-mode` | String | `'strict'` or `'lax'` | Error handling mode |
| `args.rellinks` | String | `'all'`, `'nofamily'` | Include relationship links |
| `args.cssroot` | Path | `'${projectDir}/css'` | CSS directory |
| `args.css` | String | `'custom.css'` | Custom CSS file |
| `args.logfile` | Path | `'${buildDir}/dita.log'` | Log file location |
| `args.xsl` | Path | `'${projectDir}/custom.xsl'` | Custom XSL |
| `publish.temp` | String | `'true'`/`'false'` | Publish temp directory |

**Advanced Examples:**

```groovy
// HTML5 with custom CSS
dita {
    transtype 'html5'
    properties {
        property(name: 'args.cssroot', value: "${projectDir}/src/css")
        property(name: 'args.css', value: 'custom.css')
        property(name: 'args.copycss', value: 'yes')
        property(name: 'processing-mode', value: 'strict')
    }
}

// PDF with custom output
dita {
    transtype 'pdf2'
    properties {
        property(name: 'args.logfile', value: "${buildDir}/dita-pdf.log")
        property(name: 'args.rellinks', value: 'all')
        property(name: 'publish.temp', value: 'true')  // Debug
    }
}

// Custom XSLT
dita {
    transtype 'html5'
    properties {
        property(name: 'args.xsl', value: "${projectDir}/custom-html5.xsl")
    }
}
```

**Notes:**
- Parameters are ANT properties and DITA-OT configuration
- Paths should be absolute (use `${projectDir}` or `File.absolutePath`)
- See [DITA-OT Parameters](http://www.dita-ot.org/dev/parameters/) for complete list
- Property names are case-sensitive
- Some parameters require plugins

---

## Custom Classpath

### classpath

Specifies custom JARs for XSLT processors or other plugins.

**Type:** `FileCollection`

**Required:** No

**Default:** Standard DITA-OT classpath

**Examples:**

```groovy
// Custom Saxon PE processor
dita {
    classpath files(
        '/opt/saxon-pe/saxon9pe.jar',
        '/opt/saxon-pe/saxon9-dom.jar'
    )
}

// Multiple JARs
dita {
    classpath fileTree('/opt/custom-libs') {
        include '*.jar'
    }
}

// Mixed sources
dita {
    classpath files(
        'libs/processor.jar',
        file('/opt/external/lib.jar')
    )
}
```

```kotlin
// Kotlin - Files collection
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    classpath = files(
        "/opt/saxon-pe/saxon9pe.jar",
        "/opt/saxon-pe/saxon9-dom.jar"
    )
}

// Kotlin - FileTree
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    classpath = fileTree("/opt/custom-libs") {
        include("*.jar")
    }
}
```

**Notes:**
- Used for custom XSLT processors (Saxon PE, etc.)
- JARs added to beginning of classpath
- See [examples/classpath](../examples/classpath) for detailed example

---

## Common Configurations

### Basic HTML5 Transformation

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'docs/root.ditamap'
    transtype 'html5'
}
```

### Single Input, Multiple Formats

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'docs/root.ditamap'
    transtype 'html5', 'pdf2'
    output 'build/docs'
}
```

### Multiple Inputs, Single Format

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input fileTree('docs') {
        include '**/*.ditamap'
    }
    transtype 'html5'
    singleOutputDir false  // Separate output per file
}
```

### Multiple Inputs with Filters

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input fileTree('docs') {
        include '**/*.ditamap'
    }
    useAssociatedFilter true
    transtype 'html5', 'pdf2'
}
```

### Custom CSS and Properties

```groovy
dita {
    ditaOt '/opt/dita-ot-3.6'
    input 'docs/root.ditamap'
    transtype 'html5'
    output 'build/html'

    properties {
        property(name: 'args.cssroot', value: "${projectDir}/css")
        property(name: 'args.css', value: 'custom.css')
        property(name: 'args.copycss', value: 'yes')
    }
}
```

### Configuration Cache Optimized

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-3.6"))
    input("docs/root.ditamap")
    transtype("html5", "pdf2")

    properties {
        "args.cssroot" to "${project.projectDir}/css"
        "processing-mode" to "strict"
    }
}
```

Enable in `gradle.properties`:
```properties
org.gradle.configuration-cache=true
```

---

## Type Information

### Gradle Property Types

When using Kotlin DSL with Gradle's Property API:

```kotlin
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    // Using Property API (type-safe)
    ditaOtHome.set("/opt/dita-ot")          // Property<String>
    inputFiles.from("docs/*.ditamap")       // ConfigurableFileCollection
    outputDirs.setFrom("build/output")      // ConfigurableFileCollection
    transtypes.set(listOf("html5", "pdf"))  // ListProperty<String>
}
```

### Nullable Types

```kotlin
tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    // Optional parameters (nullable)
    ditaOt(file("/opt/dita-ot"))     // Required
    input("docs/root.ditamap")       // Required
    transtype("html5")               // Optional (default: html5)
    output(file("build/output"))     // Optional (default: build/)
    filter(file("docs/filter.ditaval"))  // Optional
    temp(file("build/.temp"))        // Optional
}
```

---

## Configuration Methods Quick Reference

| Method | Type | Required | Default |
|--------|------|----------|---------|
| `ditaOt()` | File \| String | ✅ Yes | None |
| `input()` | String \| File \| FileCollection | ✅ Yes | None |
| `output()` | File \| String | No | `build/` |
| `transtype()` | String... \| List | No | `'html5'` |
| `filter()` | File \| String | No | None |
| `singleOutputDir()` | Boolean | No | `false` |
| `useAssociatedFilter()` | Boolean | No | `false` |
| `temp()` | File \| String | No | Gradle temp |
| `properties()` | Closure \| Lambda | No | Empty |

---

## Environment Variables

DITA-OT behavior can be influenced by environment variables:

```bash
# Set DITA-OT version
export ANT_OPTS=-Dfile.encoding=UTF-8

# Set Java heap size
export JAVA_OPTS="-Xmx2g -Xms512m"

# Custom ANT properties
export DITA_HOME=/opt/dita-ot-3.6
```

In Gradle:

```bash
# Via system property
gradle dita -Dfile.encoding=UTF-8

# Via environment variable
JAVA_OPTS="-Xmx2g" gradle dita
```

---

## Next Steps

- Review [Migration Guide](MIGRATION_GUIDE.md) for upgrading
- Check [Troubleshooting Guide](TROUBLESHOOTING.md) for common issues
- See [Best Practices](BEST_PRACTICES.md) for optimization tips
- Explore [Examples](../examples/) for real-world use cases

---

**For more help:** Open an issue on [GitHub](https://github.com/jyjeanne/dita-ot-gradle/issues)

