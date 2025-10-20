# DitaOtGradlePlugin: Groovy to Kotlin Migration Specification

## Document Information
- **Version:** 1.0
- **Date:** 2025-10-20
- **Target Version:** 1.0.0 (post-migration)
- **Current Version:** 0.7.1 (Groovy)

---

## Executive Summary

This document outlines the specification for migrating the DitaOtGradlePlugin from Groovy to Kotlin. The primary goal is to provide better type safety, improved IDE support, and seamless integration with Kotlin DSL (`build.gradle.kts`) build scripts while maintaining backward compatibility with Groovy DSL (`build.gradle`) scripts.

### Migration Objectives

1. **Convert all Groovy source code to Kotlin**
2. **Maintain API compatibility** - Existing users should not need to change their configurations
3. **Improve type safety** - Leverage Kotlin's null-safety and type system
4. **Enhance DSL experience** - Provide idiomatic Kotlin DSL for `build.gradle.kts` users
5. **Preserve all functionality** - No feature loss during migration
6. **Maintain test coverage** - Convert tests to Kotlin (using Spock or KotlinTest/Kotest)
7. **Update examples** - Provide both Groovy and Kotlin DSL examples

---

## Scope

### In Scope
- All Groovy source files in `src/main/groovy/`
- All test files in `src/test/groovy/`
- Build configuration (`build.gradle` → `build.gradle.kts`)
- Example projects (add Kotlin DSL versions)
- Documentation updates (README.md, code comments)
- Gradle wrapper compatibility verification

### Out of Scope
- Changing plugin functionality or features
- Major version upgrades of dependencies (unless required for Kotlin)
- Performance optimization (unless it's a side effect of migration)
- New features unrelated to migration

---

## Technical Requirements

### Language and Build Tool Versions

| Component | Current (Groovy) | Target (Kotlin) |
|-----------|------------------|-----------------|
| Source Language | Groovy 2.5+ | Kotlin 1.9+ |
| Build Script | Groovy DSL | Kotlin DSL |
| Gradle | 4.x+ | 8.x+ |
| Java Compatibility | 1.8+ | 1.8+ (sourceCompatibility) |
| JVM Target | 1.8 | 1.8 (jvmTarget) |

### Dependencies Changes

#### Current Runtime Dependencies
```groovy
dependencies {
    implementation localGroovy()
    implementation 'commons-io:commons-io:2.8.0'
}
```

#### Target Runtime Dependencies
```kotlin
dependencies {
    implementation(kotlin("stdlib"))
    implementation("commons-io:commons-io:2.8.0")
}
```

#### Test Framework Decision
**Option A:** Convert Spock to Kotest (recommended)
- Native Kotlin testing framework
- Excellent DSL and assertions
- Better IDE support for Kotlin

**Option B:** Keep Spock
- Less migration work
- Requires Groovy dependency for tests
- Mixed language testing

**Recommendation:** Use Kotest for idiomatic Kotlin testing

---

## File-by-File Migration Plan

### Phase 1: Core Classes (Foundation)

#### 1.1 Constants and Utilities (No Dependencies)

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `Properties.groovy` | `Properties.kt` | Low | Simple constants → `object` with `const val` |
| `FileExtensions.groovy` | `FileExtensions.kt` | Low | Constants → `object` with `const val` |
| `GlobPatterns.groovy` | `GlobPatterns.kt` | Low | Constants → `object` with `const val` |
| `Messages.groovy` | `Messages.kt` | Low | Static properties → `object` with lazy initialization |

**Migration Strategy:**
- Convert Groovy classes to Kotlin `object` declarations
- Use `const val` for compile-time constants
- Use `lazy` delegation for runtime-initialized values

#### 1.2 Data Classes

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `Options.groovy` | `Options.kt` | Medium | Convert to Kotlin data class or regular class |

**Migration Strategy:**
- Convert to Kotlin class with mutable properties
- Add proper null-safety annotations (`?` for nullable)
- Use Kotlin property syntax (no getters/setters needed)
- Maintain default values for backward compatibility

#### 1.3 Business Logic

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `Classpath.groovy` | `Classpath.kt` | Medium | Static utility methods → `object` with functions |

**Migration Strategy:**
- Convert to Kotlin `object` with functions
- Use Kotlin's `File` API improvements
- Replace Groovy's `XmlSlurper` with Kotlin XML parsing
- Handle null-safety in XML parsing

### Phase 2: Gradle Integration (Depends on Phase 1)

#### 2.1 Tasks

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `DitaOtSetupTask.groovy` | `DitaOtSetupTask.kt` | Medium | Keep `@Deprecated`, minimal changes |
| `DitaOtTask.groovy` | `DitaOtTask.kt` | High | Core task, complex Gradle API usage |

**Migration Strategy for DitaOtTask:**
- Extend `DefaultTask` (same as Groovy)
- Convert Groovy properties to Kotlin properties
- Use Gradle's `Property<T>` and `ConfigurableFileCollection` types
- Maintain annotation-based task configuration (`@InputFiles`, `@OutputDirectories`, etc.)
- Convert Groovy closures to Kotlin lambdas/function types
- Use Kotlin's `apply` and `let` for fluent configuration
- Handle Ant integration (may need Java interop)

#### 2.2 Plugin Entry Point

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `DitaOtPlugin.groovy` | `DitaOtPlugin.kt` | Medium | Implements `Plugin<Project>` interface |

**Migration Strategy:**
- Implement `Plugin<Project>` in Kotlin
- Use Kotlin's extension functions for cleaner Gradle API usage
- Maintain task registration logic
- Use type-safe task registration: `project.tasks.register<DitaOtTask>("dita")`

### Phase 3: Testing

| Groovy File | Kotlin File | Complexity | Notes |
|-------------|-------------|------------|-------|
| `DitaOtPluginSpec.groovy` | `DitaOtPluginTest.kt` | High | Convert Spock to Kotest |
| `DitaOtTaskSpec.groovy` | `DitaOtTaskTest.kt` | High | 30+ test cases, complex scenarios |

**Migration Strategy:**
- Replace Spock specs with Kotest specs
- Use `StringSpec`, `FunSpec`, or `BehaviorSpec` style
- Maintain all 30+ test cases
- Use Kotest matchers for assertions
- Preserve test semantics and coverage

### Phase 4: Build Configuration

| Current File | Target File | Complexity | Notes |
|--------------|-------------|------------|-------|
| `build.gradle` | `build.gradle.kts` | Medium | Convert to Kotlin DSL |
| `settings.gradle` | `settings.gradle.kts` | Low | Simple conversion |
| `gradle.properties` | `gradle.properties` | None | No changes needed |

---

## API Compatibility Strategy

### Groovy DSL Configuration (Existing Users)

Users currently configure the plugin like this:

```groovy
// build.gradle
plugins {
    id 'com.github.eerohele.dita-ot-gradle' version '0.7.1'
}

dita {
    ditaOt '/path/to/dita-ot'
    input 'my.ditamap'
    transtype 'html5', 'pdf'
    filter 'my.ditaval'
    output 'build/docs'

    properties {
        property(name: 'args.rellinks', value: 'all')
    }
}
```

### Kotlin DSL Configuration (New Capability)

After migration, users should be able to write:

```kotlin
// build.gradle.kts
plugins {
    id("com.github.eerohele.dita-ot-gradle") version "1.0.0"
}

dita {
    ditaOt.set("/path/to/dita-ot")
    input.set("my.ditamap")
    transtype.set(listOf("html5", "pdf"))
    filter.set("my.ditaval")
    output.set("build/docs")

    properties {
        property("args.rellinks", "all")
    }
}
```

### Implementation Approach

**Key Principles:**
1. **Use Gradle's `Property<T>` API** - Provides lazy configuration and type safety
2. **Provide setter methods** - Maintain compatibility with Groovy DSL
3. **Support Kotlin DSL idioms** - Use `.set()` for Kotlin users
4. **Extension functions** - Enhance DSL experience for Kotlin

**Example Implementation Pattern:**

```kotlin
// Kotlin implementation
abstract class DitaOtTask : DefaultTask() {
    // Gradle Property API (lazy, type-safe)
    @get:Input
    abstract val ditaOtHome: Property<String>

    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val transtypes: ListProperty<String>

    // Convenience methods for Groovy DSL compatibility
    fun ditaOt(path: Any) {
        ditaOtHome.set(project.file(path).absolutePath)
    }

    fun input(file: Any) {
        when (file) {
            is String -> inputFiles.from(project.file(file))
            is File -> inputFiles.from(file)
            is FileCollection -> inputFiles.from(file)
            else -> inputFiles.from(file)
        }
    }

    fun transtype(vararg types: String) {
        transtypes.set(types.toList())
    }

    // Kotlin DSL-friendly approach
    fun transtype(types: List<String>) {
        transtypes.set(types)
    }
}
```

---

## Detailed Migration Steps

### Step 1: Project Setup and Configuration ✓

**Objective:** Set up Kotlin support in the Gradle project

**Tasks:**
1. Add Kotlin Gradle plugin to `buildSrc` or `build.gradle`
2. Add Kotlin standard library dependency
3. Configure Kotlin compilation options (JVM target, API version)
4. Create `src/main/kotlin` directory structure
5. Update `.gitignore` for Kotlin build artifacts

**Acceptance Criteria:**
- Kotlin compilation works in Gradle
- Can compile basic Kotlin classes
- Existing Groovy code still compiles

**Estimated Effort:** 2 hours

---

### Step 2: Convert Constant Classes ✓

**Objective:** Migrate simple constant/utility classes

**Files to Convert:**
1. `Properties.groovy` → `Properties.kt`
2. `FileExtensions.groovy` → `FileExtensions.kt`
3. `GlobPatterns.groovy` → `GlobPatterns.kt`
4. `Messages.groovy` → `Messages.kt`

**Example Conversion:**

**Before (Groovy):**
```groovy
// Properties.groovy
class Properties {
    static final String ARGS_INPUT = 'args.input'
    static final String TRANSTYPE = 'transtype'
}
```

**After (Kotlin):**
```kotlin
// Properties.kt
object Properties {
    const val ARGS_INPUT = "args.input"
    const val TRANSTYPE = "transtype"
}
```

**Acceptance Criteria:**
- All constants accessible from Kotlin code
- All constants accessible from existing Groovy code (interop)
- No compilation errors

**Estimated Effort:** 3 hours

---

### Step 3: Convert Options Data Class ✓

**Objective:** Migrate the configuration container class

**Files to Convert:**
1. `Options.groovy` → `Options.kt`

**Key Decisions:**
- Use regular class (not `data class`) for mutability
- Use nullable types where appropriate
- Maintain default values

**Example Conversion:**

**Before (Groovy):**
```groovy
class Options {
    Boolean devMode = false
    Boolean singleOutputDir = false
    File ditaOt
    Object input
    File output
    List<String> transtype = ['html5']
}
```

**After (Kotlin):**
```kotlin
class Options {
    var devMode: Boolean = false
    var singleOutputDir: Boolean = false
    var useAssociatedFilter: Boolean = false

    var ditaOt: File? = null
    var input: Any? = null
    var filter: Any? = null
    var output: File? = null
    var temp: File? = null

    var classpath: FileCollection? = null
    var properties: ((AntBuilder) -> Unit)? = null
    var transtypes: List<String> = listOf("html5")
}
```

**Acceptance Criteria:**
- All properties accessible and mutable
- Default values preserved
- Null-safety properly defined
- Works with both Groovy and Kotlin code

**Estimated Effort:** 2 hours

---

### Step 4: Convert Classpath Utility ✓

**Objective:** Migrate classpath resolution logic

**Files to Convert:**
1. `Classpath.groovy` → `Classpath.kt`

**Key Challenges:**
- XML parsing (Groovy's `XmlSlurper` → Kotlin XML or Java DOM)
- File tree operations
- Gradle `FileCollection` API

**Example Conversion:**

**Before (Groovy):**
```groovy
static FileCollection pluginClasspath(Project project, File ditaHome) {
    def config = new File(ditaHome, 'config/plugins.xml')
    def plugins = new XmlSlurper().parse(config)
    // ... parsing logic
}
```

**After (Kotlin):**
```kotlin
object Classpath {
    fun pluginClasspath(project: Project, ditaHome: File): FileCollection {
        val config = File(ditaHome, "config/plugins.xml")
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(config)
        // ... parsing logic with DOM
    }
}
```

**Alternative:** Use `XmlSlurper` from Groovy library (requires Groovy dependency)

**Acceptance Criteria:**
- Classpath resolution works identically to Groovy version
- All JAR files discovered correctly
- Compatible with existing tests

**Estimated Effort:** 4 hours

---

### Step 5: Convert DitaOtSetupTask (Deprecated) ✓

**Objective:** Migrate deprecated setup task with minimal changes

**Files to Convert:**
1. `DitaOtSetupTask.groovy` → `DitaOtSetupTask.kt`

**Key Points:**
- Keep all `@Deprecated` annotations
- Maintain existing behavior
- Minimal effort (will be removed in future)

**Acceptance Criteria:**
- Task compiles and works
- Deprecation warnings still shown
- Backward compatibility maintained

**Estimated Effort:** 2 hours

---

### Step 6: Convert DitaOtTask (Core Task) ✓

**Objective:** Migrate the main task implementation

**Files to Convert:**
1. `DitaOtTask.groovy` → `DitaOtTask.kt`

**This is the most complex migration step.**

**Key Challenges:**
1. **Gradle Property API** - Use `Property<T>`, `ListProperty<T>`, `ConfigurableFileCollection`
2. **Closure conversion** - Groovy closures → Kotlin lambdas
3. **Dynamic typing** - `Object` parameters → specific types or `Any`
4. **Ant integration** - AntBuilder DSL in Kotlin
5. **Task annotations** - Ensure all Gradle annotations work
6. **Method overloading** - Handle varargs and multiple signatures

**Conversion Strategy:**

**A. Property Declarations:**

```kotlin
abstract class DitaOtTask : DefaultTask() {
    // Internal options (not for up-to-date checking)
    @get:Internal
    val options: Options = Options()

    // Input properties
    @get:InputDirectory
    @get:Optional
    val ditaHome: DirectoryProperty = project.objects.directoryProperty()

    @get:InputFiles
    @get:SkipWhenEmpty
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectories
    val outputDirectories: ConfigurableFileCollection = project.objects.fileCollection()
}
```

**B. Configuration Methods:**

```kotlin
// Support both Groovy and Kotlin DSL
fun ditaOt(path: Any) {
    options.ditaOt = project.file(path)
}

fun input(input: Any) {
    options.input = input
}

fun transtype(vararg types: String) {
    options.transtypes = types.toList()
}

fun properties(configure: AntBuilder.() -> Unit) {
    options.properties = configure
}
```

**C. Task Execution:**

```kotlin
@TaskAction
fun render() {
    val ditaHome = options.ditaOt
        ?: throw GradleException(Messages.ditaHomeError)

    val classpath = options.classpath ?: getDefaultClasspath()
    val ant = antBuilder(classpath)

    for (inputFile in getInputFiles()) {
        for (transtype in options.transtypes) {
            val outputDir = getOutputDirectory(inputFile, transtype)

            ant.withGroovyBuilder {
                "ant"("antfile" to File(ditaHome, "build.xml").absolutePath) {
                    "property"("name" to Properties.ARGS_INPUT,
                              "location" to inputFile.absolutePath)
                    "property"("name" to Properties.OUTPUT_DIR,
                              "location" to outputDir.absolutePath)
                    "property"("name" to Properties.TRANSTYPE,
                              "value" to transtype)

                    // Apply user-defined properties
                    options.properties?.invoke(this as AntBuilder)
                }
            }
        }
    }
}
```

**Note:** Ant integration may require `withGroovyBuilder` extension for DSL support.

**Acceptance Criteria:**
- All configuration methods work from Groovy DSL
- All configuration methods work from Kotlin DSL
- Task executes successfully
- Incremental build detection works
- All edge cases handled (multiple files, multiple transtypes, filters, etc.)
- Existing tests pass

**Estimated Effort:** 12 hours

---

### Step 7: Convert DitaOtPlugin (Entry Point) ✓

**Objective:** Migrate the plugin class

**Files to Convert:**
1. `DitaOtPlugin.groovy` → `DitaOtPlugin.kt`

**Example Conversion:**

**Before (Groovy):**
```groovy
class DitaOtPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply('base')

        project.tasks.register('ditaOt', DitaOtSetupTask)
        project.tasks.register('dita', DitaOtTask) {
            group = 'Documentation'
            description = 'Publish DITA documents'
        }
    }
}
```

**After (Kotlin):**
```kotlin
class DitaOtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("base")

        project.tasks.register<DitaOtSetupTask>("ditaOt")

        project.tasks.register<DitaOtTask>("dita") {
            group = "Documentation"
            description = "Publish DITA documents using DITA-OT"
        }
    }
}
```

**Acceptance Criteria:**
- Plugin applies successfully
- Tasks registered with correct types
- Task group and description set
- Works with both Groovy and Kotlin build scripts

**Estimated Effort:** 3 hours

---

### Step 8: Convert Build Configuration ✓

**Objective:** Convert `build.gradle` to `build.gradle.kts`

**Files to Convert:**
1. `build.gradle` → `build.gradle.kts`
2. `settings.gradle` → `settings.gradle.kts`

**Key Changes:**
- String literals use double quotes
- Plugin syntax: `id("plugin-id") version "1.0"`
- Dependencies: `implementation("group:artifact:version")`
- Groovy DSL properties → Kotlin properties

**Example:**

**Before:**
```groovy
plugins {
    id 'groovy'
    id 'java-gradle-plugin'
}

dependencies {
    implementation localGroovy()
    implementation 'commons-io:commons-io:2.8.0'
    testImplementation 'org.spockframework:spock-core:1.3-groovy-2.5'
}

gradlePlugin {
    plugins {
        ditaOtPlugin {
            id = 'com.github.eerohele.dita-ot-gradle'
            implementationClass = 'com.github.eerohele.DitaOtPlugin'
        }
    }
}
```

**After:**
```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("commons-io:commons-io:2.8.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

gradlePlugin {
    plugins {
        create("ditaOtPlugin") {
            id = "com.github.eerohele.dita-ot-gradle"
            implementationClass = "com.github.eerohele.DitaOtPlugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

**Acceptance Criteria:**
- Build script compiles
- Plugin builds successfully
- All tasks work (build, test, publish)

**Estimated Effort:** 4 hours

---

### Step 9: Convert Tests ✓

**Objective:** Migrate test suite from Spock to Kotest

**Files to Convert:**
1. `DitaOtPluginSpec.groovy` → `DitaOtPluginTest.kt`
2. `DitaOtTaskSpec.groovy` → `DitaOtTaskTest.kt`

**Testing Framework Comparison:**

| Spock | Kotest |
|-------|--------|
| `Specification` | `StringSpec`, `FunSpec`, `BehaviorSpec` |
| `def "test name"()` | `"test name" { }` |
| `expect:` | assertions directly |
| `assert x == y` | `x shouldBe y` |
| `where:` data tables | `withData()` or parameterized tests |

**Example Conversion:**

**Before (Spock):**
```groovy
class DitaOtTaskSpec extends Specification {
    def "should create task"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        def task = project.tasks.create('dita', DitaOtTask)

        then:
        task instanceof DitaOtTask
        task.group == 'Documentation'
    }

    def "should resolve input files"() {
        given:
        def project = ProjectBuilder.builder().build()
        def task = project.tasks.create('dita', DitaOtTask)

        when:
        task.input('test.ditamap')

        then:
        task.getInputFiles().size() == 1
    }
}
```

**After (Kotest):**
```kotlin
class DitaOtTaskTest : StringSpec({
    "should create task" {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("dita", DitaOtTask::class.java)

        task shouldBeInstanceOf DitaOtTask::class
        task.group shouldBe "Documentation"
    }

    "should resolve input files" {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("dita", DitaOtTask::class.java)

        task.input("test.ditamap")

        task.getInputFiles().size shouldBe 1
    }
})
```

**Acceptance Criteria:**
- All 30+ test cases converted
- Test coverage maintained
- All tests pass
- Tests work with Gradle test task

**Estimated Effort:** 16 hours (due to 30+ test cases)

---

### Step 10: Update Examples ✓

**Objective:** Provide Kotlin DSL examples alongside Groovy examples

**Approach:**
- Keep existing Groovy examples
- Add parallel Kotlin DSL examples
- Create side-by-side comparison documentation

**Example Structure:**
```
examples/
├── simple/
│   ├── build.gradle          # Groovy DSL (existing)
│   └── build.gradle.kts      # Kotlin DSL (new)
├── filetree/
│   ├── build.gradle
│   └── build.gradle.kts
├── multi-task/
│   ├── build.gradle
│   └── build.gradle.kts
└── ...
```

**Acceptance Criteria:**
- All 6 examples have Kotlin DSL versions
- Examples work and produce same output
- Clear comments explaining differences

**Estimated Effort:** 8 hours

---

### Step 11: Update Documentation ✓

**Objective:** Update all documentation for Kotlin migration

**Files to Update:**
1. `README.md` - Add Kotlin DSL examples
2. `CHANGELOG.md` - Document migration in version 1.0.0
3. Code comments - Update to Kotlin KDoc format
4. Plugin portal description

**Key Documentation Areas:**
- Installation instructions
- Configuration examples (both DSLs)
- API reference
- Migration guide for existing users
- Known issues or differences

**Acceptance Criteria:**
- README shows both Groovy and Kotlin examples
- Migration guide helps users transition
- All code samples are accurate
- KDoc comments on public API

**Estimated Effort:** 6 hours

---

### Step 12: Remove Groovy Code ✓

**Objective:** Clean up after successful migration

**Tasks:**
1. Delete all `.groovy` files from `src/main/groovy/`
2. Delete Groovy test files
3. Remove `src/main/groovy` and `src/test/groovy` directories
4. Remove Groovy dependency (if not needed for Ant interop)
5. Update directory references in build scripts

**Acceptance Criteria:**
- No Groovy source files remain
- Project builds without Groovy (except Gradle's own Groovy)
- All tests pass
- Plugin works as expected

**Estimated Effort:** 2 hours

---

### Step 13: Integration Testing ✓

**Objective:** Comprehensive testing with real-world scenarios

**Test Scenarios:**
1. Install plugin in test project with Groovy DSL
2. Install plugin in test project with Kotlin DSL
3. Test all configuration options
4. Test multiple input files
5. Test multiple transtypes
6. Test DITAVAL filtering
7. Test custom classpath
8. Test incremental builds
9. Test with actual DITA-OT installation
10. Test all 6 example projects

**Acceptance Criteria:**
- All scenarios pass
- Output identical to Groovy version
- No regressions
- Performance comparable or better

**Estimated Effort:** 8 hours

---

### Step 14: Version and Release ✓

**Objective:** Prepare for 1.0.0 release

**Tasks:**
1. Update version number to 1.0.0
2. Update CHANGELOG.md with all changes
3. Tag release in git
4. Build and test final artifact
5. Publish to Gradle Plugin Portal
6. Update documentation with new version
7. Create GitHub release with notes

**Acceptance Criteria:**
- Version 1.0.0 published
- Documentation reflects new version
- Release notes comprehensive
- Users can install new version

**Estimated Effort:** 4 hours

---

## Risk Assessment and Mitigation

### Risk 1: Breaking API Changes
**Risk Level:** HIGH
**Impact:** Existing users' builds break
**Mitigation:**
- Maintain compatibility methods for Groovy DSL
- Extensive testing with existing configurations
- Provide migration guide
- Use semantic versioning (1.0.0 indicates potential breaking changes)

### Risk 2: Gradle API Incompatibilities
**Risk Level:** MEDIUM
**Impact:** Plugin doesn't work with certain Gradle versions
**Mitigation:**
- Test with multiple Gradle versions (7.x, 8.x)
- Use stable Gradle APIs
- Document minimum Gradle version
- Use Gradle TestKit for compatibility testing

### Risk 3: Ant Integration Issues
**Risk Level:** MEDIUM
**Impact:** DITA-OT execution fails
**Mitigation:**
- Test Ant integration extensively
- Use Gradle's `AntBuilder` (same as Groovy version)
- May need Groovy dependency for Ant DSL
- Validate with actual DITA-OT builds

### Risk 4: Performance Regression
**Risk Level:** LOW
**Impact:** Builds slower than Groovy version
**Mitigation:**
- Benchmark before and after
- Kotlin compilation is generally similar performance
- Gradle daemon mitigates startup costs

### Risk 5: Test Coverage Loss
**Risk Level:** MEDIUM
**Impact:** Bugs not caught during migration
**Mitigation:**
- Convert all 30+ tests
- Add new tests for Kotlin-specific functionality
- Use code coverage tools
- Manual testing of edge cases

### Risk 6: XML Parsing Differences
**Risk Level:** MEDIUM
**Impact:** Classpath resolution fails
**Mitigation:**
- Thoroughly test `Classpath.kt`
- Consider keeping Groovy's XmlSlurper (requires Groovy dependency)
- Alternative: Use Java DOM or Kotlin XML libraries
- Test with various DITA-OT versions

---

## Testing Strategy

### Unit Testing
- **Framework:** Kotest
- **Coverage Target:** 80%+ line coverage
- **Focus Areas:**
  - Configuration methods
  - File resolution logic
  - Output directory calculation
  - DITAVAL handling
  - Multiple inputs/transtypes

### Integration Testing
- **Framework:** Gradle TestKit
- **Test Projects:** Create sample projects with both DSLs
- **Scenarios:**
  - Simple single file build
  - Multiple files
  - Multiple transtypes
  - DITAVAL filtering
  - Custom properties
  - Custom classpath

### Compatibility Testing
- **Gradle Versions:** 7.0, 7.6, 8.0, 8.5+
- **Java Versions:** 8, 11, 17, 21
- **DITA-OT Versions:** 3.x, 4.x
- **OSes:** Windows, macOS, Linux

### Regression Testing
- Run all existing Groovy DSL examples
- Compare output with pre-migration version
- Verify no functionality lost

---

## Success Criteria

### Must Have (Required for Release)
- ✅ All source code converted to Kotlin
- ✅ All tests passing
- ✅ Backward compatibility with Groovy DSL
- ✅ Kotlin DSL support working
- ✅ All examples working
- ✅ Documentation updated
- ✅ No regressions in functionality

### Should Have (Highly Desirable)
- ✅ 80%+ test coverage
- ✅ Both Groovy and Kotlin examples
- ✅ Migration guide for users
- ✅ Performance equal or better
- ✅ Code quality improvements (type safety)

### Nice to Have (Optional)
- ✅ Enhanced Kotlin DSL features
- ✅ Additional helper functions
- ✅ Improved error messages
- ✅ Better IDE support demonstration

---

## Timeline Estimate

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| **Phase 1: Setup** | Step 1-2 | 5 hours |
| **Phase 2: Core Classes** | Step 3-4 | 6 hours |
| **Phase 3: Gradle Tasks** | Step 5-7 | 17 hours |
| **Phase 4: Build & Tests** | Step 8-9 | 20 hours |
| **Phase 5: Documentation** | Step 10-11 | 14 hours |
| **Phase 6: Finalization** | Step 12-14 | 14 hours |
| **Buffer (20%)** | Unforeseen issues | 15 hours |
| **Total** | | **~91 hours** |

**Working Days:** ~11-12 days (assuming 8 hours/day)

---

## Dependencies and Prerequisites

### Required Before Starting
1. ✅ Kotlin knowledge (intermediate level)
2. ✅ Gradle plugin development experience
3. ✅ Understanding of current codebase
4. ✅ DITA-OT installation for testing
5. ✅ Git repository access

### Tools Needed
- IntelliJ IDEA (recommended for Kotlin)
- JDK 8+ (1.8 compatibility target)
- Gradle 8.x
- DITA-OT 3.x or 4.x for testing

---

## Migration Principles

### Code Quality
1. **Type Safety First** - Leverage Kotlin's null-safety
2. **Immutability Where Possible** - Use `val` over `var`
3. **Idiomatic Kotlin** - Follow Kotlin conventions
4. **Clear Names** - Maintain or improve naming
5. **Documentation** - KDoc for all public APIs

### Backward Compatibility
1. **Groovy DSL Support** - Must continue to work
2. **Same Configuration API** - Method names unchanged
3. **Same Behavior** - Output identical to Groovy version
4. **Gradual Migration** - Users can upgrade without changes

### Kotlin DSL Enhancement
1. **Type-Safe Builders** - Leverage Kotlin's DSL capabilities
2. **Extension Functions** - Provide convenient helpers
3. **Property Delegation** - Use Gradle's Property API
4. **Null Safety** - Make API more robust

---

## Post-Migration Activities

### Version 1.0.0 Release
1. Publish to Gradle Plugin Portal
2. Update GitHub repository
3. Announce on relevant channels
4. Monitor for issues

### Version 1.1.0+ (Future)
1. Deprecate Groovy-specific methods
2. Add Kotlin-specific enhancements
3. Improve DSL further based on feedback
4. Consider removing Groovy DSL support (2.0.0?)

---

## Appendix A: Kotlin vs Groovy Quick Reference

### Variable Declarations
```groovy
// Groovy
def x = 10
String y = "hello"
```

```kotlin
// Kotlin
val x = 10
val y: String = "hello"
```

### Class Definitions
```groovy
// Groovy
class Foo {
    String bar
    Integer baz = 0
}
```

```kotlin
// Kotlin
class Foo {
    var bar: String? = null
    var baz: Int = 0
}
```

### Object (Singleton)
```groovy
// Groovy
@Singleton
class Foo {
    static final String BAR = "value"
}
```

```kotlin
// Kotlin
object Foo {
    const val BAR = "value"
}
```

### Functions
```groovy
// Groovy
def foo(String x, Integer y = 10) {
    return x * y
}
```

```kotlin
// Kotlin
fun foo(x: String, y: Int = 10): String {
    return x.repeat(y)
}
```

### Closures vs Lambdas
```groovy
// Groovy
list.each { item ->
    println item
}
```

```kotlin
// Kotlin
list.forEach { item ->
    println(item)
}
```

---

## Appendix B: Gradle API in Kotlin

### Task Registration
```groovy
// Groovy
tasks.register('myTask', MyTask) {
    prop = 'value'
}
```

```kotlin
// Kotlin
tasks.register<MyTask>("myTask") {
    prop.set("value")
}
```

### Property API
```kotlin
// Define property
abstract val myProp: Property<String>

// Set in task configuration
myProp.set("value")

// Get in task action
val value = myProp.get()
```

### File Collection
```kotlin
// Define
abstract val files: ConfigurableFileCollection

// Set
files.from(project.file("path"))

// Get
files.files.forEach { ... }
```

---

## Appendix C: References

### Documentation
- [Kotlin Language Reference](https://kotlinlang.org/docs/reference/)
- [Gradle Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Developing Gradle Plugins in Kotlin](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Kotest Documentation](https://kotest.io/)

### Migration Guides
- [Migrating build logic from Groovy to Kotlin](https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html)
- [Gradle Plugin Author's Guide](https://docs.gradle.org/current/userguide/designing_gradle_plugins.html)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-20 | Migration Team | Initial specification |

---

## Approval

This specification requires approval before implementation begins.

- [ ] Technical Lead
- [ ] Project Owner
- [ ] QA Lead

---

**End of Specification**
