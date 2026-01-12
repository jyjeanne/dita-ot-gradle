# Best Practices Guide

Proven strategies for effective use of the DITA-OT Gradle Plugin.

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Build Configuration](#build-configuration)
3. [Performance Optimization](#performance-optimization)
4. [DITA-OT Setup](#dita-ot-setup)
5. [CI/CD Integration](#cicd-integration)
6. [Error Handling](#error-handling)
7. [Maintenance](#maintenance)
8. [Team Collaboration](#team-collaboration)

---

## Project Structure

### Recommended Directory Layout

```
project/
├── build.gradle              # Build configuration
├── gradle.properties         # Gradle properties (including cache)
├── gradle/
│   └── wrapper/             # Gradle wrapper
├── src/
│   └── dita/               # DITA source files
│       ├── root.ditamap    # Main map
│       ├── topics/         # Topic files
│       ├── filters/        # DITAVAL files
│       └── resources/      # Images, CSS, etc.
├── docs/                   # Documentation
├── build/                  # Generated output (git-ignored)
│   ├── html5/              # HTML output
│   ├── pdf/                # PDF output
│   └── dita-temp/          # Temporary files
└── .gitignore              # Ignore build artifacts
```

**Rationale:**
- `src/dita/` keeps DITA content separate
- `build/` contains generated output (never committed)
- Clear separation of concerns
- Follows Gradle conventions

### Alternative: Multi-Module Structure

```
project/
├── settings.gradle         # Multi-module configuration
├── common/                 # Shared DITA files
│   ├── build.gradle
│   └── src/dita/
├── product-a/             # Product-specific docs
│   ├── build.gradle
│   └── src/dita/
├── product-b/             # Product-specific docs
│   ├── build.gradle
│   └── src/dita/
└── output/                 # Aggregated output
    └── build.gradle
```

**Rationale:**
- Separates product-specific content
- Allows independent builds
- Facilitates team collaboration
- Supports CI/CD matrix testing

---

## Build Configuration

### Version Lock for Reproducibility

```gradle
// build.gradle

plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'  // Lock version
}

tasks.register('dita', DitaOtTask) {
    // Pin DITA-OT version
    ditaOt findProperty('ditaHome') ?: '/opt/dita-ot-3.6'
}
```

```properties
// gradle.properties

# Lock Gradle version (via wrapper)
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk

# Lock DITA-OT version
ditaHome=/opt/dita-ot-3.6

# Enable configuration cache for speed
org.gradle.configuration-cache=true
```

**Benefits:**
- Reproducible builds across machines
- No version surprises in CI/CD
- Team consistency

### Use Kotlin DSL for Clarity

✅ **Recommended (Kotlin DSL):**

```kotlin
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.2.1"
}

tasks.register<com.github.jyjeanne.DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-3.6"))
    input("src/dita/root.ditamap")
    transtype("html5", "pdf2")

    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
    }
}
```

**Why:**
- Better IDE support and code completion
- Type safety prevents errors
- Full configuration cache support
- More readable syntax

---

## Performance Optimization

### 1. Enable Configuration Cache

**gradle.properties:**
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache-problems=warn
```

**Performance impact:**
- First build: No change (10-50% overhead for caching)
- Subsequent builds: 10-50% faster
- CI/CD: Significant time savings

**Validation:**
```bash
# First run
gradle dita
# Building configuration cache...

# Second run
gradle dita
# Reusing configuration cache
# Much faster!
```

### 2. Optimize Java Heap Size

```properties
# gradle.properties

# For large documents
org.gradle.jvmargs=-Xmx2g -Xms512m

# For very large documents
org.gradle.jvmargs=-Xmx4g -Xms1g

# With garbage collection tuning
org.gradle.jvmargs=-Xmx2g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**Guidelines:**
- Xmx: Maximum heap (set to 50-75% of available RAM)
- Xms: Initial heap (set to 1/4 of Xmx for faster startup)
- Larger heap = faster for big documents
- Too large = slower garbage collection

### 3. Enable Parallel Processing

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4  # Adjust to CPU cores (usually 4-8)
```

**When to use:**
- Multiple input files with singleOutputDir false
- Multiple transtypes
- Large document sets

**Note:** Single file, single format won't benefit from parallelization

### 4. Use Gradle Daemon

```bash
# Daemon is enabled by default
gradle dita

# Verify daemon is running
jps | grep GradleDaemon

# Or explicitly enable
gradle --daemon dita

# Control daemon lifecycle
gradle --stop  # Stop daemon
```

**Benefits:**
- JVM stays warm between builds
- Significant startup time savings
- Especially important for CI/CD

### 5. Profile and Analyze

```bash
# Generate build profile
gradle dita --profile

# Review report
open build/reports/profile/

# Look for slowest tasks and projects
# Optimize accordingly
```

**Common bottlenecks:**
- DITA-OT initialization time
- Large document processing
- Excessive property computation
- Slow file system operations

---

## DITA-OT Setup

### Use Consistent DITA-OT Version

```bash
# Download DITA-OT 3.6 (stable)
wget https://github.com/dita-ot/dita-ot/releases/download/3.6/dita-ot-3.6.zip
unzip dita-ot-3.6.zip -d /opt/

# Set permissions
chmod -R +r /opt/dita-ot-3.6/
chmod +x /opt/dita-ot-3.6/bin/dita

# Set in gradle.properties
ditaHome=/opt/dita-ot-3.6
```

**Rationale:**
- All team members use same version
- Consistent output across machines
- Easier troubleshooting

### Use Plugin Registry for Extensions

```gradle
// build.gradle (using download example)

plugins {
    id("de.jjohannes.gradle-download-task") version "4.3.0"
}

task downloadDITA(Download) {
    src 'https://github.com/dita-ot/dita-ot/releases/download/3.6/dita-ot-3.6.zip'
    dest file('dita-ot-3.6.zip')
}

task installDITA(dependsOn: downloadDITA) {
    doLast {
        copy {
            from zipTree('dita-ot-3.6.zip')
            into 'dita-ot'
        }
    }
}

tasks.named('dita').dependsOn(installDITA)
```

**Or use examples/download:**

```bash
cd examples/download
gradle dita  # Automatically downloads DITA-OT
```

### Manage Custom Plugins

```bash
# Install plugins into DITA-OT
cd /opt/dita-ot-3.6
bin/dita install org.dita-community.pdf2-plugin

# Verify installation
bin/dita install --list

# Or use gradle-download-task
# See examples/download for automation
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/dita.yml

name: DITA Build

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Download DITA-OT
        run: |
          mkdir -p dita-ot
          curl -L https://github.com/dita-ot/dita-ot/releases/download/3.6/dita-ot-3.6.zip \
            -o dita-ot-3.6.zip
          unzip -q dita-ot-3.6.zip -d dita-ot

      - name: Build DITA
        run: ./gradlew dita -PditaHome=$PWD/dita-ot/dita-ot-3.6 --no-configuration-cache

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: dita-output
          path: build/
```

**Key points:**
- Use `--no-configuration-cache` for CI (first run always caches)
- Cache Gradle wrapper to speed up subsequent runs
- Download DITA-OT once per workflow
- Upload output as artifact for inspection

### GitLab CI Configuration

```yaml
# .gitlab-ci.yml

stages:
  - build

dita_build:
  stage: build
  image: openjdk:17

  script:
    # Download DITA-OT
    - wget -q https://github.com/dita-ot/dita-ot/releases/download/3.6/dita-ot-3.6.zip
    - unzip -q dita-ot-3.6.zip

    # Build
    - ./gradlew dita -PditaHome=$PWD/dita-ot-3.6 --no-configuration-cache

  artifacts:
    paths:
      - build/
    expire_in: 1 week

  cache:
    paths:
      - .gradle/wrapper/
```

---

## Error Handling

### Graceful Degradation

```gradle
dita {
    ditaOt findProperty('ditaHome') ?: '/opt/dita-ot-3.6'
    input 'src/dita/root.ditamap'
    transtype 'html5'

    doFirst {
        // Validate configuration before build
        if (!options.ditaOt?.exists()) {
            throw new GradleException("DITA-OT not found: ${options.ditaOt}")
        }
    }
}

// Alternative with default fallback
tasks.register('dita') {
    doFirst {
        def ditaHome = findProperty('ditaHome') ?: System.getenv('DITA_HOME')
        if (!ditaHome) {
            println 'WARNING: ditaHome not specified, using default'
            ditaHome = '/opt/dita-ot-3.6'
        }
    }
}
```

### Catch and Report Errors

```gradle
dita {
    // ...

    // Capture output and logs
    doLast {
        def logFile = file("${buildDir}/dita-build.log")
        if (logFile.exists()) {
            def warnings = logFile.text.findAll(/(?i:warning|error)/)
            if (warnings) {
                println "Build completed with warnings/errors:"
                warnings.each { println "  - $it" }
            }
        }
    }
}
```

---

## Maintenance

### Regular Updates

**Monthly:**
```bash
# Check for plugin updates
gradle wrapper --gradle-version=latest

# Test new Gradle version
./gradlew dita

# Review release notes
# https://gradle.org/releases/
```

**Quarterly:**
```bash
# Check DITA-OT updates
# https://www.dita-ot.org/download

# Update DITA-OT if new stable version available
wget https://github.com/dita-ot/dita-ot/releases/download/3.7/dita-ot-3.7.zip
# Test thoroughly before committing
```

**Annually:**
```bash
# Full version audit
gradle --version
java --version
dita --version

# Update major versions if stable
# Plan migration if breaking changes
```

### Clean Build Strategy

```bash
# Full clean when:
# 1. Dependency changes
gradle clean build

# 2. Large reorganization
gradle clean dita

# 3. Troubleshooting issues
gradle clean
gradle dita --info

# 4. Before commit/push
gradle clean dita  # Verify clean builds work
```

### Disk Space Management

```properties
# gradle.properties

# Use fast SSD for temp files
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk

# Limit artifact cleanup
org.gradle.buildOutputCleanupEnabled=true
```

```bash
# Periodic cleanup
gradle cleanBuild  # Removes build/

# Deep cleanup
gradle clean
rm -rf .gradle/  # Remove Gradle cache
gradle dita      # Rebuilds everything
```

---

## Team Collaboration

### Document Your Configuration

```gradle
// build.gradle

/**
 * DITA-OT Gradle Build Configuration
 *
 * This project builds DITA documentation for Product X.
 *
 * Configuration:
 * - Input: src/dita/root.ditamap
 * - Outputs: HTML5, PDF2
 * - Filters: Per-product DITAVAL files
 *
 * To build:
 *   gradle dita -PditaHome=/path/to/dita-ot
 *
 * For configuration cache:
 *   Set org.gradle.configuration-cache=true in gradle.properties
 *
 * See docs/ for more information.
 */

plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'
}

dita {
    ditaOt findProperty('ditaHome')
    input 'src/dita/root.ditamap'
    transtype 'html5', 'pdf2'
    // Additional config...
}
```

### Create Team Onboarding Guide

**docs/TEAM_SETUP.md:**

```markdown
# Team Setup Guide

## Prerequisites
- Java 17 or later
- Git access to this repository

## Local Setup

1. Clone repository
   \`\`\`bash
   git clone <repo-url>
   cd dita-project
   \`\`\`

2. Download and install DITA-OT 3.6
   \`\`\`bash
   # Download from https://www.dita-ot.org/download
   unzip dita-ot-3.6.zip -d ~/dita-ot/
   \`\`\`

3. Set DITA-OT path
   \`\`\`bash
   # Add to ~/.bashrc or ~/.zshrc
   export DITA_HOME=$HOME/dita-ot/dita-ot-3.6
   \`\`\`

4. Build documentation
   \`\`\`bash
   gradle dita -PditaHome=$DITA_HOME
   \`\`\`

5. View output
   \`\`\`bash
   open build/html5/index.html
   \`\`\`

## Common Tasks

- **Full rebuild**: `gradle clean dita`
- **Rebuild only HTML**: `gradle dita -Dtranstype=html5`
- **Debug build**: `gradle dita --info > debug.log`
```

### Share Configuration via gradle.properties

```properties
# gradle.properties (committed to repo)

# Shared settings all team members should use
org.gradle.configuration-cache=true
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk

# Local settings (add to .gitignore)
# ditaHome=...
```

**In .gitignore:**
```
# Local gradle.properties (not shared)
gradle.local.properties

# Generated files
build/
.gradle/
```

### Code Review Checklist

```markdown
## Build Configuration Review

- [ ] DITA-OT path is configurable (not hardcoded)
- [ ] Version numbers are locked (reproducible builds)
- [ ] Configuration cache is enabled where possible
- [ ] Error messages are helpful
- [ ] Build log is clean (no spurious warnings)
- [ ] Performance is acceptable (< 30 seconds for typical docs)
- [ ] Documentation is current
- [ ] Changes work on all platforms (Windows, macOS, Linux)
```

---

## Security Considerations

### Sensitive Information

```gradle
// ✗ DON'T: Hardcode credentials
dita {
    ditaOt '/opt/dita-ot'
    properties {
        property(name: 'api.key', value: 'my-secret-key')  // Bad!
    }
}

// ✓ DO: Use environment variables
dita {
    ditaOt '/opt/dita-ot'
    properties {
        property(name: 'api.key', value: System.getenv('API_KEY'))
    }
}
```

### Dependency Verification

```gradle
// Verify plugin from trusted source
plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.2.1'  // Use exact version
}

// Verify Gradle wrapper
// gradle-wrapper.jar should match official SHA256
// See https://gradle.org/release-checksums/
```

### Secure CI/CD Secrets

```yaml
# GitHub Actions example

jobs:
  build:
    runs-on: ubuntu-latest
    env:
      DITA_API_KEY: ${{ secrets.DITA_API_KEY }}  # From repo secrets

    steps:
      - uses: actions/checkout@v4
      # Build uses environment variable
      - run: gradle dita
```

---

## Summary Checklist

Project setup and build configuration:

- [ ] Use Kotlin DSL for type safety
- [ ] Lock all version numbers
- [ ] Enable configuration cache
- [ ] Use consistent DITA-OT version across team
- [ ] Document configuration thoroughly
- [ ] Set up CI/CD with artifact handling
- [ ] Create team onboarding guide
- [ ] Regular maintenance schedule
- [ ] Monitor performance regularly
- [ ] Keep dependencies updated
- [ ] Secure sensitive information

---

## Resources

| Topic | URL |
|-------|-----|
| **Configuration Cache** | https://docs.gradle.org/current/userguide/configuration_cache.html |
| **Gradle Performance** | https://docs.gradle.org/current/userguide/performance.html |
| **DITA-OT Best Practices** | https://www.dita-ot.org/dev/ |
| **GitHub Actions** | https://docs.github.com/en/actions |
| **GitLab CI** | https://docs.gitlab.com/ee/ci/ |

---

## Questions or Suggestions?

See related documentation:
- [Configuration Reference](CONFIGURATION_REFERENCE.md) - All options explained
- [Troubleshooting Guide](TROUBLESHOOTING.md) - Problem solving
- [Migration Guide](MIGRATION_GUIDE.md) - Upgrading from eerohele

Open an issue: https://github.com/jyjeanne/dita-ot-gradle/issues

