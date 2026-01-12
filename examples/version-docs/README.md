# Product Version Documentation Example

This example demonstrates how to generate documentation for multiple product versions from a single source using DITA conditional processing (profiling).

## Quick Start

```bash
# Build all versions in parallel
./gradlew buildAllVersions

# Build specific version
./gradlew buildV1      # Build v1.x (LTS)
./gradlew buildV2      # Build v2.x
./gradlew buildV3      # Build v3.x (Latest)

# Build only the latest version
./gradlew buildLatest
```

## Project Structure

```
version-docs/
├── build.gradle.kts          # Kotlin DSL build configuration
├── gradle.properties         # Parallel execution settings
├── settings.gradle.kts       # Project settings
├── README.md                 # This file
├── content/
│   ├── guide.ditamap         # Main map (references all topics)
│   └── topics/
│       ├── introduction.dita     # All versions
│       ├── installation.dita     # All versions (with conditional sections)
│       ├── configuration.dita    # All versions (with conditional sections)
│       ├── features.dita         # All versions
│       ├── docker-support.dita   # v2+ only
│       ├── kubernetes-support.dita # v3 only
│       └── migration.dita        # v2+ only
├── filters/
│   ├── v1.ditaval            # Filter for v1.x content
│   ├── v2.ditaval            # Filter for v2.x content
│   └── v3.ditaval            # Filter for v3.x content
└── branding/
    ├── v1/css/               # v1-specific styling
    ├── v2/css/               # v2-specific styling
    └── v3/css/               # v3-specific styling
```

## How It Works

### DITA Profiling

Content is marked with `product` attributes to indicate version applicability:

```xml
<!-- Content for all versions (no attribute) -->
<p>This appears in all versions.</p>

<!-- Content for v2 and later -->
<section product="v2-v3">
  <title>Docker Support</title>
  <p>Available in v2.0 and later.</p>
</section>

<!-- Content only for v3 -->
<section product="v3-only">
  <title>Kubernetes Support</title>
  <p>New in v3.0!</p>
</section>

<!-- Deprecated content (v1 only) -->
<section product="v1" status="deprecated">
  <title>Legacy Feature</title>
  <p>This is deprecated in v2.0.</p>
</section>
```

### DITAVAL Filters

Each version has a DITAVAL filter that includes/excludes content:

**v1.ditaval** - Includes v1 content, excludes v2-only and v3-only
**v2.ditaval** - Includes v1+v2 content, excludes deprecated and v3-only
**v3.ditaval** - Includes all current content, excludes deprecated

### Profiling Attributes Used

| Attribute | Value | Meaning |
|-----------|-------|---------|
| `product` | `v1` | Applies to v1.x |
| `product` | `v2` | Applies to v2.x |
| `product` | `v3` | Applies to v3.x |
| `product` | `v2-only` | Only v2.x (not v3) |
| `product` | `v3-only` | Only v3.x |
| `product` | `v2-v3` | v2.x and v3.x |
| `status` | `deprecated` | Deprecated (excluded in newer versions) |

## Supported Versions

| Version | Status | Build Task | Filter |
|---------|--------|------------|--------|
| v1.x | LTS | `buildV1` | `filters/v1.ditaval` |
| v2.x | Stable | `buildV2` | `filters/v2.ditaval` |
| v3.x | Latest | `buildV3` | `filters/v3.ditaval` |

## Output Structure

```
build/output/
├── v1/
│   ├── html/          # v1.x HTML documentation
│   └── pdf/           # v1.x PDF documentation
├── v2/
│   ├── html/          # v2.x HTML documentation
│   └── pdf/           # v2.x PDF documentation
└── v3/
    ├── html/          # v3.x HTML documentation
    └── pdf/           # v3.x PDF documentation
```

## Available Tasks

| Task | Description |
|------|-------------|
| `buildV1` | Build v1.x HTML documentation |
| `buildV2` | Build v2.x HTML documentation |
| `buildV3` | Build v3.x HTML documentation |
| `buildV1Pdf` | Build v1.x PDF documentation |
| `buildV2Pdf` | Build v2.x PDF documentation |
| `buildV3Pdf` | Build v3.x PDF documentation |
| `buildAllVersions` | Build HTML for all versions (parallel) |
| `buildAllPdfs` | Build PDF for all versions (parallel) |
| `buildLatest` | Build only the latest version |
| `release` | Build all formats for all versions |
| `listVersions` | List supported versions |
| `cleanDocs` | Clean generated documentation |
| `cleanAll` | Clean all outputs including DITA-OT |

## Adding a New Version

1. Create new DITAVAL filter:
   ```bash
   cp filters/v3.ditaval filters/v4.ditaval
   # Edit v4.ditaval to include v4 content
   ```

2. Update `build.gradle.kts`:
   ```kotlin
   val productVersions = listOf("v1", "v2", "v3", "v4")
   val latestVersion = "v4"
   val versionLabels = mapOf(
       // ...existing...
       "v4" to "4.x (Latest)"
   )
   ```

3. Update existing content with `v4` profiling where needed

4. Build:
   ```bash
   ./gradlew buildV4
   ```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Version Documentation

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        version: [v1, v2, v3]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build ${{ matrix.version }}
        run: ./gradlew build${{ matrix.version }}
      - uses: actions/upload-artifact@v4
        with:
          name: docs-${{ matrix.version }}
          path: build/output/${{ matrix.version }}/
```

## Best Practices

1. **Use meaningful profiling values**: `v2-only` is clearer than `v2`
2. **Keep common content unmarked**: Only profile version-specific content
3. **Document deprecated content**: Mark with `status="deprecated"` before removal
4. **Test all versions**: Build all versions in CI to catch profiling errors
5. **Review filter files**: Ensure each DITAVAL correctly includes/excludes content

## Resources

- [DITA-OT Gradle Plugin Documentation](../../README.md)
- [DITA Conditional Processing](https://www.dita-ot.org/dev/topics/condproc.html)
- [DITAVAL Reference](https://www.dita-ot.org/dev/topics/dita-ditaval-file.html)
