# Multi-Language Documentation Example

This example demonstrates how to build documentation in multiple languages from localized DITA sources using the DITA-OT Gradle Plugin.

## Quick Start

```bash
# Build all languages in parallel
./gradlew buildAllLanguages

# Build specific language
./gradlew buildEnglish
./gradlew buildFrench
./gradlew buildGerman

# Build all languages with PDF
./gradlew release
```

## Project Structure

```
multi-language/
├── build.gradle.kts          # Kotlin DSL build configuration
├── gradle.properties         # Parallel execution settings
├── settings.gradle.kts       # Project settings
├── README.md                 # This file
├── content/
│   ├── en/                   # English source
│   │   ├── guide.ditamap
│   │   └── topics/
│   │       ├── introduction.dita
│   │       ├── getting-started.dita
│   │       └── features.dita
│   ├── fr/                   # French source
│   │   ├── guide.ditamap
│   │   └── topics/
│   │       ├── introduction.dita
│   │       ├── getting-started.dita
│   │       └── features.dita
│   └── de/                   # German source
│       ├── guide.ditamap
│       └── topics/
│           ├── introduction.dita
│           ├── getting-started.dita
│           └── features.dita
└── shared/
    └── images/               # Shared assets across languages
```

## Features

### Parallel Builds

All language builds run in parallel, significantly reducing total build time:

```bash
# Sequential: 3 languages × 30s = 90s
# Parallel:   3 languages in ~35s (70% faster!)
./gradlew buildAllLanguages
```

### Supported Languages

| Code | Language | Build Task |
|------|----------|------------|
| `en` | English | `buildEnglish` |
| `fr` | French | `buildFrench` |
| `de` | German | `buildGerman` |

### Output Structure

```
build/output/
├── en/
│   ├── html/          # English HTML
│   └── pdf/           # English PDF
├── fr/
│   ├── html/          # French HTML
│   └── pdf/           # French PDF
└── de/
    ├── html/          # German HTML
    └── pdf/           # German PDF
```

## Available Tasks

| Task | Description |
|------|-------------|
| `buildEnglish` | Build English HTML documentation |
| `buildFrench` | Build French HTML documentation |
| `buildGerman` | Build German HTML documentation |
| `buildEnglishPdf` | Build English PDF documentation |
| `buildFrenchPdf` | Build French PDF documentation |
| `buildGermanPdf` | Build German PDF documentation |
| `buildAllLanguages` | Build HTML for all languages (parallel) |
| `buildAllPdfs` | Build PDF for all languages (parallel) |
| `release` | Build all formats for all languages |
| `listLanguages` | List supported languages |
| `cleanDocs` | Clean generated documentation |
| `cleanAll` | Clean all outputs including DITA-OT |

## Adding a New Language

1. Create content directory:
   ```bash
   mkdir -p content/ja/topics
   ```

2. Copy and translate content:
   ```bash
   cp content/en/*.ditamap content/ja/
   cp content/en/topics/*.dita content/ja/topics/
   # Translate the content
   ```

3. Update `build.gradle.kts`:
   ```kotlin
   val languages = listOf("en", "fr", "de", "ja")
   val languageNames = mapOf(
       "en" to "English",
       "fr" to "French",
       "de" to "German",
       "ja" to "Japanese"
   )
   ```

4. Build:
   ```bash
   ./gradlew buildJapanese
   ```

## CI/CD Integration

Example GitHub Actions workflow for multi-language builds:

```yaml
name: Multi-Language Docs

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        language: [en, fr, de]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build ${{ matrix.language }}
        run: ./gradlew build${{ matrix.language }}
      - uses: actions/upload-artifact@v4
        with:
          name: docs-${{ matrix.language }}
          path: build/output/${{ matrix.language }}/
```

## Best Practices

1. **Shared Assets**: Put images and common resources in `shared/` to avoid duplication
2. **Consistent Structure**: Keep the same topic structure across all languages
3. **Parallel Execution**: Enable `org.gradle.parallel=true` in `gradle.properties`
4. **Configuration Cache**: Enable for faster incremental builds

## Resources

- [DITA-OT Gradle Plugin Documentation](../../README.md)
- [DITA Localization Best Practices](https://www.dita-ot.org/dev/topics/globalization-support.html)
