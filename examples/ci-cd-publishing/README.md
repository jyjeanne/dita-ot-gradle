# CI/CD Documentation Publishing Example

This example demonstrates how to automate DITA documentation builds in CI/CD pipelines using the DITA-OT Gradle Plugin.

## Quick Start

```bash
# Generate all documentation formats
./gradlew generateDocs

# Generate HTML only
./gradlew generateHtml

# Generate PDF only
./gradlew generatePdf
```

## Project Structure

```
ci-cd-publishing/
├── build.gradle.kts            # Kotlin DSL build configuration
├── gradle.properties           # Project properties (DITA-OT version, cache settings)
├── settings.gradle.kts         # Project settings
├── README.md                   # This file
├── docs/
│   ├── guide.ditamap           # Main documentation map
│   ├── release.ditaval         # DITAVAL filter for release builds
│   └── topics/
│       ├── introduction.dita
│       ├── getting-started.dita
│       ├── configuration.dita
│       └── troubleshooting.dita
└── .github/
    └── workflows/
        └── docs.yml            # GitHub Actions workflow
```

## Features

### Multi-Format Output

Generate documentation in multiple formats:

| Format | Task | Output Directory |
|--------|------|------------------|
| HTML5 | `generateHtml` | `build/output/html/` |
| PDF | `generatePdf` | `build/output/pdf/` |
| All | `generateDocs` | Both directories |

### Configuration Cache Support

This example enables Gradle's Configuration Cache for faster builds:

```properties
# gradle.properties
org.gradle.configuration-cache=true
```

**Performance improvement**: Up to 77% faster incremental builds.

### Version Matrix Testing

Test documentation builds with multiple DITA-OT versions:

```bash
./gradlew generateDocs -PditaOtVersion=4.1.0
./gradlew generateDocs -PditaOtVersion=4.2.3
```

## GitHub Actions Integration

The included workflow (`.github/workflows/docs.yml`) provides:

1. **Matrix testing** across DITA-OT versions
2. **Artifact upload** for HTML and PDF output
3. **GitHub Pages deployment** on main branch
4. **Build status notifications**

### Workflow Triggers

- Push to `main` branch (docs/** or build files)
- Pull requests affecting documentation

### Setting Up GitHub Pages

1. Go to repository **Settings** > **Pages**
2. Set Source to **GitHub Actions**
3. The workflow will automatically deploy on push to main

## Available Tasks

| Task | Description |
|------|-------------|
| `downloadDitaOt` | Download DITA-OT from GitHub releases |
| `extractDitaOt` | Extract DITA-OT zip archive |
| `generateHtml` | Generate HTML5 documentation |
| `generatePdf` | Generate PDF documentation |
| `generateDocs` | Generate all formats |
| `verifyDocs` | Verify output was generated |
| `cleanDocs` | Clean generated documentation |
| `cleanAll` | Clean all outputs including DITA-OT |

## Customization

### Adding More Output Formats

```kotlin
val generateMarkdown by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    dependsOn(extractDitaOt)
    ditaOt(ditaOtDir)
    input("docs/guide.ditamap")
    output(layout.buildDirectory.dir("output/markdown"))
    transtype("markdown")
}
```

### Conditional Filtering

Use different DITAVAL files for different builds:

```kotlin
val generateInternal by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    filter("docs/internal.ditaval")  // Include internal content
    // ...
}

val generateExternal by tasks.registering(com.github.jyjeanne.DitaOtTask::class) {
    filter("docs/release.ditaval")   // Exclude internal content
    // ...
}
```

## Best Practices

1. **Pin DITA-OT version** in `gradle.properties` for reproducible builds
2. **Enable Configuration Cache** for faster CI builds
3. **Use DITAVAL filters** to manage content variants
4. **Upload artifacts** for review before deployment
5. **Test multiple versions** to ensure compatibility

## Resources

- [DITA-OT Gradle Plugin Documentation](../../README.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [DITA-OT User Guide](https://www.dita-ot.org/dev/)
