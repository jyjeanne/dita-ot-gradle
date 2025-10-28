DITA-OT Gradle Plugin: Examples
===============================

This directory contains example projects demonstrating various use cases of the DITA-OT Gradle plugin, including the latest **v2.2.0** features.

## 🆕 What's New in v2.2.0

All examples have been updated to **v2.2.0** with:

- ⚡ **Configuration Cache Support** - 10-50% faster builds
- 🚀 **Performance Optimizations** - Skip configuration phase on subsequent runs
- 📊 **Enhanced Logging** - Detailed build metrics and reports (v2.1.0+)
- 🔧 **Type-Safe Kotlin DSL** - Improved property configuration (v2.1.0+)

## Available Examples

Each example is provided in both **Groovy DSL** (`build.gradle`) and **Kotlin DSL** (`build.gradle.kts`) formats:

1. **simple** - Basic DITA transformation with properties ⭐ **NEW: Configuration Cache demo**
2. **filetree** - Process multiple files using glob patterns
3. **multi-project** - Multi-module project with shared configuration
4. **multi-task** - Multiple transformation tasks (web + pdf)
5. **classpath** - Custom classpath configuration (Saxon-PE example)
6. **download** - Download DITA-OT and install plugins automatically

## Running Examples

### Quick Start with Configuration Cache (v2.2.0+)

```bash
cd simple
gradle dita -PditaHome=/path/to/dita-ot
```

The `simple` example includes `gradle.properties` with configuration cache enabled. Run it twice to see the speed improvement!

### Run All Examples

To run all examples with Groovy DSL:
```bash
./gradlew -PditaHome=/path/to/dita-ot
```

To run a specific example with Kotlin DSL:
```bash
cd simple
gradle dita -PditaHome=/path/to/dita-ot -b build.gradle.kts
```

## Configuration Cache Benefits (v2.2.0+)

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
  - ⭐ **Recommended for v2.2.0+** - Best configuration cache compatibility
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
