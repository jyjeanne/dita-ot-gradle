# DITA-OT Gradle Plugin Documentation Generator

This example project generates the plugin's own documentation from DITA source files.

## Prerequisites

- Java 17 or later
- DITA-OT 4.x (will be downloaded automatically if not installed)

## Quick Start

```bash
cd examples/dita-ot-gradle-plugin-documentation

# Download DITA-OT and generate all documentation
./gradlew downloadAndBuild

# Or if you have DITA-OT installed:
./gradlew buildDocs -PditaOtPath=/path/to/dita-ot
```

## Available Tasks

| Task | Description |
|------|-------------|
| `downloadAndBuild` | Download DITA-OT and generate all docs |
| `buildDocs` | Generate both HTML and PDF (requires DITA-OT) |
| `generateHtml` | Generate HTML documentation only |
| `generatePdf` | Generate PDF documentation only |
| `downloadDitaOt` | Download DITA-OT distribution |
| `extractDitaOt` | Extract DITA-OT distribution |
| `cleanDocs` | Clean generated documentation |

## Output

- **HTML**: `../../docs/html/` (project root: `docs/html/`)
- **PDF**: `../../docs/pdf/` (project root: `docs/pdf/`)

## DITA Source

The documentation source files are located in `../../docs/dita/`:

```
docs/dita/
├── plugin-documentation-version-2.3.2.bookmap
└── chapters/
    ├── getting-started/
    ├── configuration/
    ├── use-cases/
    ├── reference/
    └── migration/
```

## Configuration

You can override the DITA-OT path:

```bash
# Via command line property
./gradlew buildDocs -PditaOtPath=/custom/path/to/dita-ot

# Via environment variable
export DITA_OT_HOME=/custom/path/to/dita-ot
./gradlew buildDocs
```

If neither is set, the build will use the downloaded DITA-OT in `build/dita-ot-4.2.3/`.
