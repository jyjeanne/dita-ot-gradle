# Simple Example

This example demonstrates the basic usage of the DITA-OT Gradle plugin with all the latest features from v2.2.0.

## Features Demonstrated

- Basic DITA transformation (PDF output)
- DITAVAL filtering
- Custom DITA-OT properties
- **Configuration Cache** for faster builds (v2.2.0+)
- Type-safe Kotlin DSL properties (v2.1.0+, Kotlin only)

## Running the Example

### Groovy DSL

```bash
gradle dita -PditaHome=/path/to/dita-ot
```

### Kotlin DSL

```bash
gradle dita -PditaHome=/path/to/dita-ot -b build.gradle.kts
```

## Configuration Cache (v2.2.0+)

This example includes `gradle.properties` with configuration cache enabled:

```properties
org.gradle.configuration-cache=true
```

### Performance Benefits

- **First run**: Configuration phase runs normally (~2-5 seconds)
- **Second run**: Configuration skipped entirely → **10-50% faster**
- **CI/CD**: Significant time savings on repeated builds

### Testing Configuration Cache

Run the build twice to see the performance improvement:

```bash
# First run - builds configuration cache
gradle dita -PditaHome=/path/to/dita-ot

# Second run - reuses configuration cache (much faster!)
gradle dita -PditaHome=/path/to/dita-ot
```

You should see this message on the second run:
```
Reusing configuration cache.
```

### Disabling Configuration Cache

To run without configuration cache (for testing):

```bash
gradle dita -PditaHome=/path/to/dita-ot --no-configuration-cache
```

## New Features in v2.2.0

### 1. Configuration Cache Support
- Full support for Gradle's configuration cache
- 10-50% faster builds on subsequent runs
- Especially beneficial for CI/CD pipelines

### 2. Type-Safe Kotlin DSL (v2.1.0+)

The Kotlin DSL example uses the new type-safe properties syntax:

```kotlin
properties {
    "processing-mode" to "strict"
    "args.rellinks" to "all"
}
```

### 3. Comprehensive Logging (v2.1.0+)

Build output includes detailed metrics:
- DITA-OT version
- Files processed count
- Output formats
- Total output size
- Duration in seconds

## Output

The transformed PDF will be in:
```
build/pdf/root.pdf
```
