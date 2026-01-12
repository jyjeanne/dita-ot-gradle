# DITA-OT Gradle Plugin v2.3.1
## Presentation Plan (20 minutes)

---

## Slide Structure Overview

| Section | Duration | Slides |
|---------|----------|--------|
| 1. Introduction & Speaker | 2 min | 1-3 |
| 2. The Problem | 3 min | 4-6 |
| 3. The Solution | 5 min | 7-12 |
| 4. Live Demo | 5 min | 13-15 |
| 5. Why Choose This Plugin | 3 min | 16-18 |
| 6. Q&A | 2 min | 19-20 |

---

## SECTION 1: Introduction (2 min)

### Slide 1: Title Slide
```
DITA-OT Gradle Plugin v2.3.1
Modern Documentation Build Automation

[Your Name]
Software Developer
[Date]
```

**Speaker Notes:**
- Welcome everyone
- Today I'll present a Gradle plugin that simplifies DITA documentation builds

---

### Slide 2: About Me
```
About the Developer

[Your Photo]

- Software Developer specializing in build automation
- Kotlin & Gradle enthusiast
- Open Source contributor
- Focus: Developer Experience & CI/CD optimization

GitHub: github.com/jyjeanne
```

**Speaker Notes:**
- Brief personal introduction
- Experience with build tools and documentation systems
- Passion for improving developer workflows

---

### Slide 3: Agenda
```
What We'll Cover Today

1. The Problem with DITA builds
2. Introducing dita-ot-gradle plugin
3. Key Features & Architecture
4. Live Demo
5. Migration Guide
6. Q&A
```

---

## SECTION 2: The Problem (3 min)

### Slide 4: What is DITA?
```
DITA (Darwin Information Typing Architecture)

- XML-based documentation standard
- Topic-based authoring
- Content reuse & single-sourcing
- Multiple output formats (PDF, HTML, EPUB...)

Used by: Microsoft, IBM, SAP, Adobe, Boeing...
```

**Speaker Notes:**
- DITA is the industry standard for technical documentation
- Allows creating content once, publishing many formats
- Problem: Building DITA requires DITA-OT (Open Toolkit)

---

### Slide 5: The Build Challenge
```
Traditional DITA Build Process

  Developer
      |
      v
  [Write DITA XML] --> [Run ANT script manually]
      |                        |
      v                        v
  [Download DITA-OT]    [Configure classpath]
      |                        |
      v                        v
  [Set environment]     [Handle platform differences]
      |
      v
  [Debug failures] <-- Common issues:
                       - ClassLoader errors
                       - Missing dependencies
                       - Path problems
```

**Speaker Notes:**
- Manual process is error-prone
- Different commands for Windows/Linux/Mac
- Hard to integrate with CI/CD pipelines

---

### Slide 6: Why Existing Solutions Fail
```
Problems with Existing Approaches

+-----------------------------+---------------------------+
| Approach                    | Issues                    |
+-----------------------------+---------------------------+
| Manual ANT execution        | Not reproducible          |
| Shell scripts               | Platform-specific         |
| Old Gradle plugins          | Broken on Gradle 8+       |
|                             | ClassLoader errors        |
|                             | No Configuration Cache    |
+-----------------------------+---------------------------+

The IsolatedAntBuilder Problem:
  Error: "taskdef class org.dita.dost.ant.InitializeProjectTask
          cannot be found"
```

**Speaker Notes:**
- Old plugin (eerohele/dita-ot-gradle) abandoned since 2020
- Gradle's IsolatedAntBuilder has ClassLoader restrictions
- No modern plugin supports Configuration Cache

---

## SECTION 3: The Solution (5 min)

### Slide 7: Introducing dita-ot-gradle v2.3.1
```
io.github.jyjeanne.dita-ot-gradle

A Modern Gradle Plugin for DITA-OT

  +------------------------+
  |    Your build.gradle   |
  |------------------------|
  | plugins {              |
  |   id 'io.github....'   |
  | }                      |
  |                        |
  | dita {                 |
  |   ditaOt '/path/...'   |
  |   input 'guide.ditamap'|
  |   transtype 'html5'    |
  | }                      |
  +------------------------+
           |
           v
    [DITA-OT Gradle Plugin]
           |
           v
    +------+------+
    |  PDF  | HTML |
    +------+------+
```

**Speaker Notes:**
- Simple, declarative configuration
- Works out of the box
- Supports both Groovy and Kotlin DSL

---

### Slide 8: Key Features
```
What Makes This Plugin Special

1. Configuration Cache Support (77% faster builds)
2. DITA_SCRIPT Strategy (fixes ClassLoader issues)
3. Provider API Architecture (modern Gradle)
4. Cross-Platform (Windows, macOS, Linux)
5. Both DSLs (Groovy & Kotlin)
6. Multiple Transtypes (pdf, html5, epub...)
7. DITAVAL Filtering
8. Custom Properties
```

**Speaker Notes:**
- Each feature solves a real problem
- Configuration Cache = huge time savings in CI/CD
- DITA_SCRIPT = reliable execution on all platforms

---

### Slide 9: Performance Benchmarks
```
Configuration Cache Performance

+---------------------------+--------+-------------+
| Scenario                  | Time   | Improvement |
+---------------------------+--------+-------------+
| Without Cache             | 20.8s  | baseline    |
| With Cache (first run)    | 22.8s  | stores cache|
| With Cache (up-to-date)   | 4.8s   | 77% faster  |
| With Cache (clean build)  | 22.4s  | reuses conf |
+---------------------------+--------+-------------+

  In CI/CD pipelines:
  - 100 builds/day = 26 minutes saved daily
  - 500 builds/day = 2+ hours saved daily
```

**Speaker Notes:**
- Real benchmarks from Windows 11, Gradle 8.5, DITA-OT 3.6
- Configuration Cache stores task graph
- Huge impact on CI/CD pipelines

---

### Slide 10: Architecture Overview
```
Plugin Architecture

  +------------------+
  |  DitaOtPlugin    | <-- Entry point, registers task
  +------------------+
           |
           v
  +------------------+
  |   DitaOtTask     | <-- Main task, Provider API
  +------------------+
           |
     +-----+-----+
     |           |
     v           v
+----------+ +-------------+
| Options  | | AntExecutor |
+----------+ +-------------+
     |              |
     v              v
+-----------+ +-------------+
| Classpath | | DITA Script |
+-----------+ +-------------+
```

**Speaker Notes:**
- Clean separation of concerns
- DitaOtTask is the heart - uses Provider API
- AntExecutor handles multiple execution strategies

---

### Slide 11: Code Example - Basic
```kotlin
// build.gradle.kts

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}

tasks.named<DitaOtTask>("dita") {
    ditaOt(file("/opt/dita-ot-4.0"))
    input("docs/guide.ditamap")
    transtype("html5", "pdf")
    filter("docs/release.ditaval")
}

// Run: ./gradlew dita --configuration-cache
```

**Speaker Notes:**
- Clean, declarative syntax
- Multiple output formats in one task
- DITAVAL filtering for conditional content

---

### Slide 12: Code Example - Advanced
```kotlin
// Custom properties for HTML output

tasks.named<DitaOtTask>("dita") {
    ditaOt(file(ditaHome))
    input("userguide.ditamap")
    transtype("html5")

    // Method 1: Direct API (recommended)
    ditaProperties.put("args.copycss", "yes")
    ditaProperties.put("args.css", "custom-theme.css")
    ditaProperties.put("nav-toc", "partial")

    // Method 2: Closure (backward compatible)
    properties {
        property("processing-mode", "strict")
    }
}
```

**Speaker Notes:**
- Two ways to set properties
- Direct API is configuration cache friendly
- Closure syntax for backward compatibility

---

## SECTION 4: Live Demo (5 min)

### Slide 13: Demo Setup
```
Live Demo

What we'll do:
1. Create a new Gradle project
2. Add the plugin
3. Configure DITA transformation
4. Run with Configuration Cache
5. See the performance difference

Prerequisites:
- Gradle 8.5+
- DITA-OT 3.6+
- Java 17+
```

**Speaker Notes:**
- Walk through creating a simple project
- Show both Groovy and Kotlin DSL options

---

### Slide 14: Demo Commands
```bash
# Initialize project
mkdir dita-demo && cd dita-demo
gradle init --type basic

# Add plugin to build.gradle.kts
# (show editor)

# First run - stores configuration
./gradlew dita --configuration-cache -PditaHome=/path/to/dita-ot

# Second run - uses cache (FAST!)
./gradlew dita --configuration-cache -PditaHome=/path/to/dita-ot

# Clean and rebuild
./gradlew clean dita --configuration-cache
```

**Speaker Notes:**
- Show the speed difference between first and second run
- Demonstrate output files generated

---

### Slide 15: Demo Output
```
Terminal Output:

> Task :dita
Starting DITA-OT transformation...

Input files: 1 file(s)
Output formats: html5, pdf
ANT execution strategy: DITA_SCRIPT

Processing: guide.ditamap
  -> Generating html5 output...
  [OK] Successfully generated html5 output
  -> Generating pdf output...
  [OK] Successfully generated pdf output

Status:           SUCCESS
Duration:         12.34s

BUILD SUCCESSFUL in 15s

Configuration cache entry reused.  <-- KEY MESSAGE
```

---

## SECTION 5: Why Choose This Plugin (3 min)

### Slide 16: Comparison with Alternatives
```
Feature Comparison

+----------------------+----------+---------+----------+
| Feature              | This     | Old     | Manual   |
|                      | Plugin   | Plugin  | ANT      |
+----------------------+----------+---------+----------+
| Gradle 8+ Support    |    Yes   |   No    |   N/A    |
| Gradle 9+ Support    |    Yes   |   No    |   N/A    |
| Configuration Cache  |    Yes   |   No    |   N/A    |
| Cross-Platform       |    Yes   |  Partial|   No     |
| Kotlin DSL           |    Yes   |   No    |   N/A    |
| Active Maintenance   |    Yes   |   No    |   N/A    |
| CI/CD Ready          |    Yes   |  Partial|  Manual  |
+----------------------+----------+---------+----------+
```

**Speaker Notes:**
- Clear advantages over alternatives
- The old plugin hasn't been updated since 2020
- This is the only modern option

---

### Slide 17: Migration Guide
```
Migrating from Old Plugin

Before (old plugin):
  plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
  }

After (new plugin):
  plugins {
    id 'io.github.jyjeanne.dita-ot-gradle' version '2.3.1'
  }

Key Changes:
1. Plugin ID changed
2. Same DSL syntax (backward compatible!)
3. Add --configuration-cache for speed
4. Optional: Use ditaProperties.put() for new projects
```

**Speaker Notes:**
- Migration is straightforward
- Most builds work without changes
- Just change the plugin ID and version

---

### Slide 18: Getting Started
```
Start Using Today!

1. Add to build.gradle.kts:

   plugins {
       id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
   }

2. Configure your DITA source:

   dita {
       ditaOt(file("/path/to/dita-ot"))
       input("docs/guide.ditamap")
       transtype("html5")
   }

3. Run:

   ./gradlew dita --configuration-cache


Resources:
- GitHub: github.com/jyjeanne/dita-ot-gradle
- Examples: github.com/jyjeanne/dita-ot-gradle/tree/main/examples
- Gradle Portal: plugins.gradle.org/plugin/io.github.jyjeanne.dita-ot-gradle
```

---

## SECTION 6: Q&A (2 min)

### Slide 19: Summary
```
Key Takeaways

1. Modern DITA builds require modern tools

2. dita-ot-gradle v2.3.1 offers:
   - 77% faster incremental builds
   - Works on Gradle 8+ and 9+
   - Cross-platform reliability
   - Simple, declarative configuration

3. Migration is easy - just change the plugin ID

4. Open source and actively maintained
```

---

### Slide 20: Questions?
```
Questions & Discussion

[Your Contact Info]

GitHub:  github.com/jyjeanne/dita-ot-gradle
Twitter: @[your_handle]
Email:   [your_email]

Thank you!

Star the project: github.com/jyjeanne/dita-ot-gradle
```

---

## Presentation Tips

### Before the Presentation
- [ ] Test DITA-OT installation on demo machine
- [ ] Pre-download dependencies for offline demo
- [ ] Have backup screenshots of demo
- [ ] Test Configuration Cache works
- [ ] Prepare simple DITA files for demo

### During the Presentation
- Start with the pain point (broken builds)
- Show real performance numbers
- Live demo > slides for technical audience
- Keep code examples readable (large font)
- Leave time for questions

### Common Questions to Prepare For
1. "Does it work with DITA-OT 4.x?" - Yes
2. "Can I use it with Maven?" - No, Gradle only
3. "Is it production-ready?" - Yes, v2.3.1 is stable
4. "How does it compare to DITA-OT's Gradle plugin?" - This is more feature-rich
5. "Can I contribute?" - Yes, PRs welcome!

---

## Slide Design Suggestions

### Color Scheme
- Primary: #5C6BC0 (Gradle blue)
- Secondary: #26A69A (DITA teal)
- Background: White or light gray
- Code: Dark theme (Monokai or similar)

### Fonts
- Headings: Sans-serif (Roboto, Open Sans)
- Code: Monospace (JetBrains Mono, Fira Code)
- Body: Sans-serif

### Visual Elements
- Use diagrams for architecture
- Code snippets with syntax highlighting
- Performance charts/graphs
- Screenshots of terminal output
- GitHub activity graph (optional)
