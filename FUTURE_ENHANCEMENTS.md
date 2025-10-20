# DITA-OT Gradle Plugin - Future Enhancements

## Current State Assessment

### ✅ Strengths
- **Modern Kotlin codebase**: Successfully migrated from Groovy to Kotlin
- **Dual DSL support**: Works with both Groovy and Kotlin DSL build scripts
- **Incremental builds**: Proper Gradle up-to-date checking with `@InputFiles` and `@OutputDirectories`
- **Flexible configuration**: Supports multiple input files, output formats, and DITAVAL filtering
- **Test coverage**: 31 Kotest tests covering core functionality
- **CI/CD pipeline**: GitHub Actions workflow for automated testing and publishing
- **Good documentation**: README with examples, CICD documentation

### ⚠️ Areas for Improvement
- **Deprecated task**: `DitaOtSetupTask` is deprecated but still included
- **Outdated dependencies**: Some dependencies could be updated
- **Limited test coverage**: Only 3 test files, integration tests skip actual DITA transformation
- **Reflection-based Ant integration**: Complex and fragile reflection code for Groovy Ant DSL
- **Limited error handling**: Basic error messages, could be more user-friendly
- **No Gradle Configuration Cache support**: Not compatible with Gradle's configuration cache

---

## 🚀 Recommended Enhancements (Prioritized)

### **Priority 1: Critical Improvements**

#### 1.1 Update Dependencies
**Current versions:**
- Kotlin: 1.9.20 (Latest: 1.9.23+)
- commons-io: 2.8.0 (Latest: 2.15.1, released 2023)
- Kotest: 5.8.0 (Latest: 5.8.1+)
- jsoup: 1.13.1 (Latest: 1.17.2)

**Recommendation:**
```kotlin
dependencies {
    implementation("commons-io:commons-io:2.15.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("org.jsoup:jsoup:1.17.2")
}
```

**Benefits:** Security patches, bug fixes, performance improvements

#### 1.2 Remove Deprecated `DitaOtSetupTask`
**Current issue:** The task is deprecated and non-functional (plugin installation broken)

**Recommendation:**
- Remove `DitaOtSetupTask.kt` entirely in version 2.0.0
- Remove task registration from `DitaOtPlugin.kt`
- Add migration guide in README for users still using it

**Benefits:** Cleaner codebase, removes dead code

#### 1.3 Gradle Configuration Cache Support
**Current issue:** Plugin is not compatible with Gradle's configuration cache

**Recommendation:**
- Add `@CacheableTask` annotation to `DitaOtTask`
- Replace `project.file()` calls with `Provider` API
- Use `Property<File>` instead of `File?` for options
- Test with `--configuration-cache`

**Example:**
```kotlin
@CacheableTask
open class DitaOtTask : DefaultTask() {
    @get:InputDirectory
    val ditaHome: DirectoryProperty = project.objects.directoryProperty()

    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()
}
```

**Benefits:** 10-50% faster builds with configuration cache enabled

---

### **Priority 2: Enhanced User Experience**

#### 2.1 Better Error Messages
**Current issue:** Generic error messages like "DITA-OT directory not set"

**Recommendation:**
```kotlin
fun getDitaHome(): File {
    return options.ditaOt ?: throw GradleException("""
        DITA-OT directory not configured.

        Please configure it in your build script:

        dita {
            ditaOt("/path/to/dita-ot")
        }

        Or download it automatically with the download plugin:
        See: https://github.com/jyjeanne/dita-ot-gradle/tree/main/examples/download
    """.trimIndent())
}
```

**Benefits:** Faster troubleshooting for users

#### 2.2 Kotlin DSL Type-Safe Configuration
**Current issue:** Configuration uses Groovy `Closure` for properties

**Recommendation:** Add Kotlin-friendly DSL for properties:
```kotlin
fun properties(block: PropertyBuilder.() -> Unit) {
    val builder = PropertyBuilder()
    builder.block()
    options.kotlinProperties = builder.properties
}

class PropertyBuilder {
    val properties = mutableMapOf<String, String>()

    fun property(name: String, value: String) {
        properties[name] = value
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }
}
```

**Usage:**
```kotlin
dita {
    properties {
        "processing-mode" to "strict"
        "args.cssroot" to "$projectDir/css"
    }
}
```

**Benefits:** Type-safe, IDE autocomplete, no Groovy dependency in Kotlin DSL

#### 2.3 Validation and Helpful Warnings
**Recommendation:**
```kotlin
init {
    // Validate DITA-OT version
    doFirst {
        val version = detectDitaOtVersion()
        if (version < "3.0") {
            logger.warn("DITA-OT version $version detected. Version 3.0+ recommended.")
        }
    }

    // Validate input files exist
    doFirst {
        getInputFiles().files.forEach { file ->
            if (!file.exists()) {
                throw GradleException("Input file not found: ${file.absolutePath}")
            }
        }
    }
}
```

**Benefits:** Catch configuration errors early

---

### **Priority 3: New Features**

#### 3.1 Support for DITA-OT 4.x
**Current:** Plugin uses DITA-OT 3.6 for testing

**Recommendation:**
- Test with DITA-OT 4.2 (latest stable)
- Support new DITA 2.0 features
- Update documentation with 4.x compatibility

#### 3.2 Plugin Installation Support
**Current:** Deprecated `DitaOtSetupTask` doesn't work

**Recommendation:** Add working plugin installation:
```kotlin
dita {
    ditaOt("/path/to/dita-ot")

    plugins {
        install("org.lwdita")
        install("org.dita.pdf2.fop")
        install("file:///path/to/custom-plugin.zip")
    }
}
```

**Implementation:**
```kotlin
fun installPlugins() {
    val pluginInstaller = DitaOtPluginInstaller(getDitaHome())
    options.pluginsToInstall.forEach { plugin ->
        pluginInstaller.install(plugin)
    }
}
```

**Benefits:** Automated plugin management, reproducible builds

#### 3.3 Parallel Transformation Support
**Current:** Multiple transtypes run sequentially

**Recommendation:**
```kotlin
@get:Internal
val parallel: Property<Boolean> = project.objects.property(Boolean::class.java)
    .convention(false)

fun render() {
    if (parallel.get()) {
        // Use Gradle worker API for parallel execution
        val workQueue = workerExecutor.noIsolation()
        options.transtype.forEach { transtype ->
            workQueue.submit(DitaTransformationWorker::class.java) { params ->
                params.inputFile.set(inputFile)
                params.transtype.set(transtype)
                params.outputDir.set(outputDir)
            }
        }
    } else {
        // Sequential execution (current behavior)
        options.transtype.forEach { /* ... */ }
    }
}
```

**Benefits:** Faster multi-format builds (HTML5 + PDF simultaneously)

#### 3.4 Watch Mode for Development
**Recommendation:**
```kotlin
tasks.register("ditaWatch") {
    group = "Documentation"
    description = "Continuously rebuild DITA documentation on file changes"

    doLast {
        // Use Gradle's continuous build (--continuous)
        // Or implement custom file watcher
    }
}
```

**Usage:**
```bash
./gradlew ditaWatch --continuous
```

**Benefits:** Live documentation updates during authoring

#### 3.5 HTML Preview Server
**Recommendation:**
```kotlin
tasks.register<JavaExec>("ditaServe") {
    group = "Documentation"
    description = "Start local server to preview HTML output"

    classpath = configurations["runtimeOnly"]
    mainClass.set("SimpleHTTPServer")
    args = listOf(ditaTask.getOutputDirectory().absolutePath, "8080")
}
```

**Benefits:** Preview documentation without deploying

#### 3.6 Build Report and Metrics
**Recommendation:**
```kotlin
doLast {
    val report = BuildReport(
        inputFiles = getInputFiles().files.size,
        transtypes = options.transtype,
        duration = System.currentTimeMillis() - startTime,
        outputSize = calculateOutputSize()
    )

    logger.lifecycle("""
        DITA-OT Build Report:
        - Input files: ${report.inputFiles}
        - Formats: ${report.transtypes.joinToString()}
        - Duration: ${report.duration}ms
        - Output size: ${report.outputSize}
    """.trimIndent())
}
```

**Benefits:** Build insights, performance tracking

---

### **Priority 4: Code Quality**

#### 4.1 Replace Reflection-Based Ant Integration
**Current issue:** Complex reflection code in `DitaOtTask.render()` (lines 216-263)

**Recommendation:**
```kotlin
// Option A: Direct Ant API usage
private fun executeAnt(ditaHome: File, properties: Map<String, String>) {
    val ant = project.ant
    ant.invokeMethod("ant", mapOf(
        "antfile" to File(ditaHome, "build.xml")
    ))
    // Set properties directly
}

// Option B: Use Gradle's AntBuilder properly
private fun executeAnt(ditaHome: File, inputFile: File, outputDir: File) {
    project.ant.withGroovyBuilder {
        "ant"("antfile" to File(ditaHome, "build.xml")) {
            "property"("name" to "args.input", "location" to inputFile.path)
            "property"("name" to "output.dir", "location" to outputDir.path)
        }
    }
}
```

**Benefits:** More maintainable, less fragile

#### 4.2 Improve Test Coverage
**Current:** 31 tests, but integration tests skip actual transformation

**Recommendation:**
- Add unit tests for each method in `DitaOtTask`
- Add integration tests with real DITA-OT transformation
- Mock DITA-OT for faster tests
- Target 80%+ code coverage

**New tests needed:**
```kotlin
class DitaOtTaskTest : StringSpec({
    "getOutputDirectory returns correct path for single input" { }
    "getOutputDirectory returns correct path for multiple inputs" { }
    "getDitavalFile uses filter when specified" { }
    "getDitavalFile uses associated file when useAssociatedFilter is true" { }
    "getInputFileTree excludes build directory" { }
    "getInputFileTree includes DITAVAL file" { }
    "render throws exception when DITA-OT not found" { }
    "render creates output directory" { }
    "parallel rendering works correctly" { }
})
```

#### 4.3 Add Logging Levels
**Recommendation:**
```kotlin
fun render() {
    logger.info("Starting DITA-OT transformation")
    logger.debug("Input files: ${getInputFiles().files}")
    logger.debug("Transtypes: ${options.transtype}")
    logger.debug("Output directory: ${options.output}")

    // ... transformation ...

    logger.lifecycle("DITA transformation completed successfully")
}
```

**Benefits:** Better debugging, user feedback

#### 4.4 Add Gradle Build Scan Support
**Recommendation:**
```kotlin
tasks.register("publishBuildScan") {
    doLast {
        buildScan {
            tag("dita-ot")
            value("dita-version", detectDitaOtVersion())
            value("transtypes", options.transtype.joinToString())
        }
    }
}
```

**Benefits:** Performance insights with Gradle Enterprise

---

### **Priority 5: Documentation**

#### 5.1 API Documentation (KDoc)
**Current:** Limited KDoc comments

**Recommendation:**
- Add comprehensive KDoc for all public APIs
- Generate API docs with Dokka
- Publish to GitHub Pages

```kotlin
/**
 * Main task for transforming DITA documents with DITA Open Toolkit.
 *
 * This task supports:
 * - Multiple input files (DITA maps)
 * - Multiple output formats (transtypes)
 * - DITAVAL filtering
 * - Incremental builds
 * - Continuous mode
 *
 * @property ditaOt Path to DITA-OT installation directory
 * @property input Input DITA map file(s)
 * @property transtype Output format(s) to generate
 * @property filter DITAVAL filter file
 * @property output Output directory (default: build/)
 *
 * @sample samples.DitaOtTaskSamples.basicUsage
 * @sample samples.DitaOtTaskSamples.multipleFormats
 */
open class DitaOtTask : DefaultTask() { }
```

#### 5.2 Migration Guide
**Recommendation:** Create `MIGRATION.md` for:
- Groovy → Kotlin DSL migration
- Version 0.x → 1.x changes
- Breaking changes in 2.0

#### 5.3 Troubleshooting Guide
**Recommendation:** Create `TROUBLESHOOTING.md` with:
- Common errors and solutions
- Performance tuning
- Debugging techniques

#### 5.4 Video Tutorials
**Recommendation:**
- Quick start video (5 min)
- Advanced configuration video (10 min)
- CI/CD integration demo (5 min)

---

### **Priority 6: Performance**

#### 6.1 Classpath Caching
**Recommendation:**
```kotlin
private val classpathCache = mutableMapOf<File, FileCollection>()

fun getDefaultClasspath(): FileTree {
    return classpathCache.getOrPut(getDitaHome()) {
        Classpath.compile(project, getDitaHome())
    }.asFileTree
}
```

**Benefits:** Faster configuration phase

#### 6.2 Smarter Up-To-Date Checking
**Recommendation:**
```kotlin
@InputFiles
@PathSensitive(PathSensitivity.RELATIVE)
fun getInputFileTree(): Set<Any> {
    // Current implementation tracks too many files
    // Only track DITA source files, not temp files
}
```

**Benefits:** More accurate incremental builds

#### 6.3 Build Cache Support
**Recommendation:**
```kotlin
@CacheableTask
open class DitaOtTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override fun getInputFiles(): FileCollection { }
}
```

**Benefits:** Share build outputs across machines

---

## 📊 Suggested Roadmap

### Version 1.1.0 (Next Minor Release)
- ✅ Update dependencies
- ✅ Improve error messages
- ✅ Add Kotlin DSL type-safe properties
- ✅ Add validation and warnings

### Version 1.2.0
- ✅ DITA-OT 4.x support
- ✅ Plugin installation support
- ✅ Build reports and metrics

### Version 1.3.0
- ✅ Parallel transformation support
- ✅ Watch mode for development
- ✅ HTML preview server

### Version 2.0.0 (Breaking Changes)
- ✅ Remove deprecated `DitaOtSetupTask`
- ✅ Gradle Configuration Cache support
- ✅ Replace reflection-based Ant integration
- ✅ Migration to Provider API

---

## 🧪 Testing Strategy

### Unit Tests
- Test each method in isolation
- Mock external dependencies
- Fast execution (< 1 second per test)

### Integration Tests
- Test with real DITA-OT
- Test all example projects
- Test error scenarios

### Performance Tests
- Benchmark transformation times
- Test with large documents (1000+ topics)
- Memory usage profiling

### Compatibility Tests
- Test with Gradle 7.x, 8.x
- Test with JDK 8, 11, 17, 21
- Test with DITA-OT 3.x, 4.x

---

## 🔧 Development Tooling

### Recommended Tools
1. **Gradle Build Scan**: Track build performance
2. **JaCoCo**: Code coverage reporting
3. **Detekt**: Kotlin static analysis
4. **Dokka**: API documentation generation
5. **kotlinx-benchmark**: Performance benchmarking

### GitHub Actions Enhancements
```yaml
- name: Code Coverage
  run: ./gradlew jacocoTestReport

- name: Upload Coverage to Codecov
  uses: codecov/codecov-action@v3

- name: Static Analysis
  run: ./gradlew detekt

- name: Generate API Docs
  run: ./gradlew dokkaHtml
```

---

## 📝 Community Contributions

### Encourage Community
- Add `CONTRIBUTING.md`
- Create issue templates
- Add "good first issue" labels
- Respond to issues within 48 hours

### Feature Requests
- GitHub Discussions for ideas
- Vote on features
- Transparent roadmap

---

## 🎯 Success Metrics

### Track These Metrics
1. **Downloads**: Plugin Portal download count
2. **GitHub Stars**: Community interest
3. **Issues/PRs**: Community engagement
4. **Build Performance**: Avg transformation time
5. **Test Coverage**: Aim for 80%+
6. **Documentation**: Page views, time on site

---

## Summary

This plugin is already in excellent shape after the Kotlin migration. The most impactful enhancements would be:

1. **Short-term (1-2 weeks)**:
   - Update dependencies
   - Improve error messages
   - Add Kotlin DSL type-safe configuration

2. **Medium-term (1-2 months)**:
   - Gradle Configuration Cache support
   - Plugin installation support
   - Better test coverage

3. **Long-term (3-6 months)**:
   - Parallel transformation support
   - Remove deprecated code (v2.0)
   - Performance optimizations

The plugin is production-ready as-is, but these enhancements would make it even more powerful and user-friendly.
