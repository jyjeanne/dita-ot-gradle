# DITA-OT Gradle Plugin Demo Project

A demonstration project showcasing the [DITA-OT Gradle Plugin](https://github.com/jyjeanne/dita-ot-gradle) v2.8.1 for building DITA documentation with Gradle.

## Overview

This project demonstrates a modern docs-as-code workflow using:

- **Oxygen XML Editor** for DITA authoring and validation
- **Gradle 9** with the DITA-OT Gradle Plugin for build automation
- **DITA Open Toolkit 4.2.3** for documentation transformation
- **Git** for version control

## Prerequisites

- Java JDK 17 or higher
- Gradle 9+ (or use the included Gradle Wrapper)
- Oxygen XML Editor (recommended for DITA authoring)

## Quick Start

1. **Run the complete publication workflow**:
   ```bash
   ./gradlew publishWorkflow
   ```

   This executes the full workflow:
   - Download DITA-OT 4.2.3
   - Install plugins (Bootstrap)
   - Validate DITA content
   - Check internal and external links
   - Publish HTML5, PDF, and Bootstrap
   - Verify output files

2. **View outputs** in `build/docs/`

## Available Gradle Tasks

### Core Tasks

| Task | Description |
|------|-------------|
| `downloadDitaOt` | Download DITA-OT 4.2.3 from GitHub releases |
| `installPlugins` | Install DITA-OT plugins from registry (Bootstrap) |
| `validateDita` | Validate DITA content for errors |
| `checkLinks` | Check internal and external links |

### Output Tasks

| Task | Description | Output |
|------|-------------|--------|
| `ditaHtml` | Build HTML5 output | `build/docs/html5/` |
| `ditaPdf` | Build PDF output | `build/docs/pdf/` |
| `ditaMarkdown` | Build GitHub Markdown | `build/docs/markdown/` |
| `ditaXhtml` | Build XHTML output | `build/docs/xhtml/` |
| `ditaBootstrap` | Build Bootstrap 5 HTML | `build/docs/bootstrap/` |
| `ditaAll` | Build all formats | All directories |

### Workflow Tasks

| Task | Description |
|------|-------------|
| `publishWorkflow` | Complete publication workflow (recommended) |
| `publishDocs` | Publish HTML5, PDF, and Bootstrap |
| `verifyOutput` | Verify generated files exist and are not empty |
| `buildDocs` | Build default documentation (HTML5) |

### Continuous Build Mode

Watch for changes and rebuild automatically:

```bash
./gradlew ditaHtml --continuous
```

## Publication Workflow

The `publishWorkflow` task executes a complete CI/CD-ready documentation pipeline:

```
┌─────────────────────────────────────────────────────────────┐
│                    publishWorkflow                          │
├─────────────────────────────────────────────────────────────┤
│  1. downloadDitaOt    │ Download DITA-OT 4.2.3              │
│  2. installPlugins    │ Install Bootstrap plugin            │
│  3. validateDita      │ Validate DITA content               │
│  4. checkLinks        │ Check internal/external links       │
│  5. publishDocs       │ Generate HTML5 + PDF + Bootstrap    │
│  6. verifyOutput      │ Verify files exist and not empty    │
└─────────────────────────────────────────────────────────────┘
```

See `docs/publish-workflow.puml` for the full UML activity diagram.

## Project Structure

```
dita-gradle-demo/
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts       # Gradle settings
├── gradlew / gradlew.bat     # Gradle Wrapper (v9.0)
├── gradle/
│   └── wrapper/              # Gradle Wrapper files
│
├── src/
│   └── dita/
│       ├── user-guide.bookmap    # Main DITA bookmap
│       ├── topics/               # DITA topics
│       │   ├── intro.dita
│       │   ├── install.dita
│       │   ├── quick-start.dita
│       │   ├── configuration.dita
│       │   └── tasks-reference.dita
│       └── ditaval/              # Conditional filtering
│           └── pdf.ditaval
│
├── dita-ot/                  # Downloaded DITA-OT (gitignored)
│   └── dita-ot-4.2.3/        # DITA-OT installation
│
├── build/                    # Generated outputs (gitignored)
│   └── docs/
│       ├── html5/            # Standard HTML5
│       ├── pdf/              # PDF document
│       ├── markdown/         # GitHub Markdown
│       ├── xhtml/            # Legacy XHTML
│       └── bootstrap/        # Bootstrap 5 HTML
│
├── docs/                     # Documentation diagrams
│   ├── publish-workflow.puml
│   └── README.md
│
├── .gitignore
├── .gitattributes
└── README.md
```

## Oxygen XML Editor Integration

This project works seamlessly with Oxygen XML Editor:

1. **Open the project** - Open the project folder in Oxygen XML Editor
2. **Configure DITA-OT** - Point Oxygen to `dita-ot/dita-ot-4.2.3` after running `./gradlew downloadDitaOt`
3. **Edit DITA content** - Use Oxygen's DITA authoring features
4. **Build with Gradle** - Use the terminal or Oxygen's external tools to run Gradle tasks

### Setting Up External Tools in Oxygen

You can configure Oxygen to run Gradle tasks:

1. Go to **Tools > External Tools > Configure**
2. Add new tools for common tasks:
   - **Name**: Publish Workflow
   - **Command**: `${pd}/gradlew.bat` (Windows) or `${pd}/gradlew` (macOS/Linux)
   - **Arguments**: `publishWorkflow`
   - **Working directory**: `${pd}`

## Configuration

### Plugin Version

This project uses DITA-OT Gradle Plugin v2.8.1:

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.1"
}
```

### Enabling Configuration Cache

For faster incremental builds, add to `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

### Customizing Output

Edit `build.gradle.kts` to modify:
- Input DITA map location
- Output directory
- DITA-OT parameters
- Conditional filtering (DITAVAL)
- Installed plugins

### Adding More Plugins

To install additional DITA-OT plugins:

```kotlin
tasks.register<com.github.jyjeanne.DitaOtInstallPluginTask>("installPlugins") {
    dependsOn("downloadDitaOt")
    ditaOtDir.set(file("dita-ot/dita-ot-4.2.3"))
    plugins.set(listOf(
        "net.infotexture.dita-bootstrap",  // Bootstrap 5 HTML
        "org.lwdita",                       // Lightweight DITA
        "fox.jason.extend.css"              // Extended CSS
    ))
}
```

## Resources

- [DITA-OT Gradle Plugin](https://github.com/jyjeanne/dita-ot-gradle) - Plugin source and documentation
- [DITA Open Toolkit](https://www.dita-ot.org/) - Official DITA-OT website
- [Oxygen XML Editor](https://www.oxygenxml.com/) - Professional DITA authoring tool
- [DITA Bootstrap Plugin](https://infotexture.github.io/dita-bootstrap/) - Bootstrap HTML theme

## License

This demo project is provided as-is for demonstration purposes.
