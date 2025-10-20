DITA-OT Gradle Plugin: Examples
===============================

This directory contains example projects demonstrating various use cases of the DITA-OT Gradle plugin.

## Available Examples

Each example is provided in both **Groovy DSL** (`build.gradle`) and **Kotlin DSL** (`build.gradle.kts`) formats:

1. **simple** - Basic DITA transformation with properties
2. **filetree** - Process multiple files using glob patterns
3. **multi-project** - Multi-module project with shared configuration
4. **multi-task** - Multiple transformation tasks (web + pdf)
5. **classpath** - Custom classpath configuration (Saxon-PE example)
6. **download** - Download DITA-OT and install plugins automatically

## Running Examples

To run all examples with Groovy DSL:
```bash
./gradlew -PditaHome=/path/to/dita-ot
```

To run a specific example with Kotlin DSL:
```bash
cd simple
gradle dita -PditaHome=/path/to/dita-ot -b build.gradle.kts
```

## Choosing Build Script Format

- **Groovy DSL** (`build.gradle`) - Traditional Gradle syntax, more concise
- **Kotlin DSL** (`build.gradle.kts`) - Type-safe, better IDE support, modern

Both formats are functionally equivalent. Choose the one that fits your project's needs.
