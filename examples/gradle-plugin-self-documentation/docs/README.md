# Documentation Diagrams

This folder contains PlantUML activity diagrams for the DITA publication workflow.

## Files

| File | Description |
|------|-------------|
| `publish-workflow.puml` | Detailed workflow with swimlanes (6 steps) |
| `publish-workflow-simple.puml` | Simplified workflow diagram |

## Workflow Steps

The `publishWorkflow` task executes these steps in order:

| Step | Task | Description |
|------|------|-------------|
| 1 | `downloadDitaOt` | Download DITA-OT 4.2.3 from GitHub |
| 2 | `installPlugins` | Install Bootstrap plugin from registry |
| 3 | `validateDita` | Validate DITA content (strict mode) |
| 4 | `checkLinks` | Check internal and external links |
| 5 | `publishDocs` | Generate HTML5, PDF, and Bootstrap |
| 6 | `verifyOutput` | Verify output files exist and not empty |

## Rendering the Diagrams

### Option 1: PlantUML Online Server

1. Go to [PlantUML Web Server](http://www.plantuml.com/plantuml/uml/)
2. Paste the content of the `.puml` file
3. Click "Submit" to generate the diagram
4. Download as PNG or SVG

### Option 2: VS Code Extension

1. Install the "PlantUML" extension by jebbs
2. Open the `.puml` file
3. Press `Alt+D` to preview the diagram
4. Right-click to export as PNG/SVG

### Option 3: Oxygen XML Editor

1. Open the `.puml` file in Oxygen XML
2. Use the PlantUML integration to render
3. Export as image

### Option 4: Command Line

```bash
# Using PlantUML JAR
java -jar plantuml.jar publish-workflow.puml

# Using Docker
docker run --rm -v $(pwd):/data plantuml/plantuml publish-workflow.puml
```

### Option 5: Gradle Task (if PlantUML plugin is added)

```kotlin
// Add to build.gradle.kts
plugins {
    id("io.freefair.plantuml") version "8.4"
}

plantuml {
    outputDir = file("docs/images")
}
```

Then run:
```bash
./gradlew plantUml
```

## Workflow Overview

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

## Output Formats

| Format | Directory | Entry Point |
|--------|-----------|-------------|
| HTML5 | `build/docs/html5/` | `index.html` |
| PDF | `build/docs/pdf/` | `user-guide.pdf` |
| Bootstrap | `build/docs/bootstrap/` | `toc.html` |

## Plugin Information

- **DITA-OT Gradle Plugin**: v2.8.1
- **DITA-OT Version**: 4.2.3
- **Installed Plugin**: net.infotexture.dita-bootstrap
