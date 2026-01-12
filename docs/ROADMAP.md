# DITA-OT Gradle Plugin Roadmap

This document outlines planned improvements and features for the DITA-OT Gradle Plugin, organized by priority.

---

## Priority Legend

| Priority | Description | Timeline |
|----------|-------------|----------|
| **P0 - Critical** | Essential for production use | Next release |
| **P1 - High** | Significant value for most users | 1-2 releases |
| **P2 - Medium** | Improves developer experience | 3-6 months |
| **P3 - Low** | Nice to have features | Future |
| **P4 - Exploration** | Research and experimentation | TBD |

---

## P0 - Critical Priority

### 1. Built-in DITA-OT Download Task
**Status**: Planned
**Impact**: High
**Effort**: Medium

Currently, users must use a separate plugin (`de.undercouch.download`) to download DITA-OT. A built-in task would simplify setup.

**Proposed API:**
```kotlin
ditaOt {
    version = "4.2.3"
    installDir = layout.buildDirectory.dir("dita-ot")
}

// Auto-creates downloadDitaOt and extractDitaOt tasks
```

**Benefits:**
- Single plugin dependency
- Automatic version management
- Simplified example projects
- Better caching of downloaded artifacts

---

### 2. DITA-OT Plugin Installation Task
**Status**: Planned
**Impact**: High
**Effort**: Medium

Built-in support for installing DITA-OT plugins from the registry.

**Proposed API:**
```kotlin
ditaOt {
    plugins {
        install("org.lwdita")
        install("com.example.custom-plugin", file("path/to/plugin"))
    }
}
```

**Benefits:**
- No need for `Exec` tasks
- Automatic dependency on extract task
- Up-to-date checking for installed plugins

---

### 3. Improved Error Messages and Diagnostics
**Status**: Planned
**Impact**: High
**Effort**: Low

Enhance error messages with actionable suggestions.

**Current:**
```
DITA-OT transformation failed (exit code: 1)
```

**Proposed:**
```
DITA-OT transformation failed (exit code: 1)

Possible causes:
  - Invalid DITA content in: src/topics/chapter1.dita (line 45)
  - Missing referenced topic: concepts/overview.dita

Suggestions:
  - Run with --info for detailed DITA-OT output
  - Validate your DITA content: ./gradlew ditaValidate
  - Check DITA-OT logs: build/dita-ot-logs/
```

---

## P1 - High Priority

### 4. DITA Validation Task
**Status**: Planned
**Impact**: High
**Effort**: Medium

Validate DITA content without running full transformation.

**Proposed API:**
```kotlin
tasks.register<DitaValidateTask>("validateDita") {
    input("docs/guide.ditamap")
    strictMode = true  // Fail on warnings
}
```

**Benefits:**
- Fast feedback during development
- CI/CD integration for content validation
- Catch errors before expensive transformations

---

### 5. Incremental Build Support
**Status**: Research
**Impact**: Very High
**Effort**: High

Only rebuild changed topics and their dependents.

**Current behavior:** Full rebuild on any change
**Proposed:** Track topic dependencies, rebuild only affected outputs

**Benefits:**
- Dramatically faster builds for large documentation sets
- Better developer experience with `--continuous` mode
- Reduced CI/CD build times

**Challenges:**
- DITA-OT doesn't natively support incremental builds
- Need to parse DITA maps for dependency tracking

---

### 6. Link Checker Task
**Status**: Planned
**Impact**: Medium
**Effort**: Medium

Check for broken links in DITA content and generated output.

**Proposed API:**
```kotlin
tasks.register<DitaLinkCheckTask>("checkLinks") {
    input("docs/guide.ditamap")
    checkExternal = true  // Also check external URLs
    failOnBroken = true
}
```

**Output:**
```
Link Check Results:
  ✓ 145 internal links OK
  ✓ 23 external links OK
  ✗ 2 broken links found:
    - topics/install.dita:34 -> concepts/missing.dita (file not found)
    - topics/api.dita:56 -> https://old-api.example.com (404)
```

---

### 7. Gradle Wrapper in Examples
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Add Gradle wrapper to all example projects for standalone execution.

**Affected examples:**
- `custom-plugin-dev/`
- `ci-cd-publishing/`
- `multi-language/`
- `version-docs/`
- `plugin-test/`

---

## P2 - Medium Priority

### 8. Watch Mode Improvements
**Status**: Planned
**Impact**: Medium
**Effort**: Medium

Enhanced continuous build experience.

**Features:**
- Browser auto-refresh for HTML output
- Build notifications (system tray)
- Selective rebuilds based on changed files

**Proposed API:**
```kotlin
tasks.named<DitaOtTask>("dita") {
    watch {
        autoRefresh = true
        notifyOnComplete = true
    }
}
```

---

### 9. Multi-Format Single Task
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Generate multiple output formats in a single task.

**Current:** Requires multiple tasks
```kotlin
tasks.register<DitaOtTask>("html") { transtype("html5") }
tasks.register<DitaOtTask>("pdf") { transtype("pdf") }
```

**Proposed:**
```kotlin
tasks.register<DitaOtTask>("docs") {
    transtype("html5", "pdf", "markdown")
    output(layout.buildDirectory.dir("docs"))
    // Creates: docs/html5/, docs/pdf/, docs/markdown/
}
```

---

### 10. Kotlin DSL Extensions
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Cleaner Kotlin DSL syntax with extension functions.

**Current:**
```kotlin
tasks.register<DitaOtTask>("dita") {
    ditaOt(file("/path/to/dita-ot"))
    input("docs/guide.ditamap")
    transtype("html5")
}
```

**Proposed:**
```kotlin
dita {
    ditaOtVersion = "4.2.3"

    transform("html") {
        from("docs/guide.ditamap")
        to(layout.buildDirectory.dir("html"))
        format = "html5"
    }

    transform("pdf") {
        from("docs/guide.ditamap")
        to(layout.buildDirectory.dir("pdf"))
        format = "pdf"
    }
}
```

---

### 11. Progress Reporting
**Status**: Planned
**Impact**: Medium
**Effort**: Low

Better progress indication during long transformations.

**Current:**
```
> Task :dita
```

**Proposed:**
```
> Task :dita
  Processing: guide.ditamap
  [=====>          ] 35% - Transforming topics (12/34)
  [============>   ] 78% - Generating HTML output
  [================] 100% - Complete (45 files, 2.3 MB)
```

---

### 12. DITA-OT Version Management
**Status**: Planned
**Impact**: Medium
**Effort**: Medium

Support multiple DITA-OT versions in the same project.

**Use case:** Testing documentation with different DITA-OT versions

**Proposed API:**
```kotlin
ditaOt {
    versions {
        register("3.6") { version = "3.6" }
        register("4.2") { version = "4.2.3" }
    }
}

tasks.register<DitaOtTask>("testWith36") {
    ditaOtVersion = "3.6"
}
```

---

## P3 - Low Priority

### 13. Preview Server
**Status**: Exploration
**Impact**: Medium
**Effort**: Medium

Built-in HTTP server for previewing HTML output.

**Proposed API:**
```kotlin
tasks.register<DitaPreviewTask>("preview") {
    dependsOn("dita")
    outputDir = layout.buildDirectory.dir("html")
    port = 8080
    openBrowser = true
}
```

---

### 14. Documentation Metrics
**Status**: Exploration
**Impact**: Low
**Effort**: Low

Generate metrics about documentation.

**Output:**
```
Documentation Metrics:
  Topics: 45
  Words: 12,345
  Images: 23
  Tables: 8
  Code blocks: 34
  Reading time: ~45 minutes
```

---

### 15. Docker-based Execution
**Status**: Exploration
**Impact**: Low
**Effort**: High

Run DITA-OT in Docker container for isolation.

**Benefits:**
- No local DITA-OT installation required
- Consistent builds across environments
- Easy CI/CD integration

**Proposed API:**
```kotlin
ditaOt {
    useDocker = true
    dockerImage = "ghcr.io/dita-ot/dita-ot:4.2.3"
}
```

---

### 16. Integration with Documentation Platforms
**Status**: Exploration
**Impact**: Low
**Effort**: High

Direct publishing to platforms.

**Targets:**
- GitHub Pages
- Confluence
- ReadTheDocs
- Netlify

**Proposed API:**
```kotlin
tasks.register<DitaPublishTask>("publish") {
    dependsOn("dita")

    to {
        githubPages {
            repository = "user/repo"
            branch = "gh-pages"
        }
    }
}
```

---

## P4 - Future Exploration

### 17. Gradle Build Cache Integration
**Status**: Research
**Impact**: High
**Effort**: High

Enable Gradle's build cache for DITA transformations.

**Challenge:** DITA-OT output determinism

---

### 18. IDE Plugins
**Status**: Idea
**Impact**: Medium
**Effort**: Very High

IDE integration for IntelliJ IDEA and VS Code.

**Features:**
- Run transformations from IDE
- Preview output in IDE
- DITA content assist

---

### 19. AI-Assisted Documentation
**Status**: Idea
**Impact**: Unknown
**Effort**: Unknown

Integration with AI tools for documentation.

**Potential features:**
- Content suggestions
- Translation assistance
- Quality analysis

---

### 20. Gradle 10 Preparation
**Status**: Monitoring
**Impact**: Critical (when released)
**Effort**: Unknown

Ensure compatibility with Gradle 10 when released.

**Actions:**
- Monitor Gradle 10 development
- Test with early access versions
- Update deprecated APIs

---

## Completed Features

| Feature | Version | Date |
|---------|---------|------|
| Configuration Cache Support | 2.3.0 | Nov 2025 |
| Groovy Closure Properties Fix | 2.3.1 | Dec 2025 |
| Implicit Dependency Fix | 2.3.2 | Jan 2026 |
| Multi-language Example | 2.3.2 | Jan 2026 |
| Version Documentation Example | 2.3.2 | Jan 2026 |

---

## Contributing

We welcome contributions! If you'd like to work on any of these features:

1. Open an issue to discuss the approach
2. Fork the repository
3. Create a feature branch
4. Submit a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed guidelines.

---

## Feedback

Have a feature request not listed here? Please:

1. [Open an issue](https://github.com/jyjeanne/dita-ot-gradle/issues/new)
2. Describe the use case
3. Propose a solution (optional)

Your feedback helps prioritize the roadmap!
