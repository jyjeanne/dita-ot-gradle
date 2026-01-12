# DITA-OT Gradle Plugin Architecture

A comprehensive guide to understanding the project architecture for beginner developers.

---

## Table of Contents

1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Concepts](#core-concepts)
4. [Kotlin Classes Explained](#kotlin-classes-explained)
5. [Execution Flow](#execution-flow)
6. [Tests](#tests)
7. [Glossary](#glossary)

---

## Introduction

### What is this plugin?

This Gradle plugin transforms **DITA** (Darwin Information Typing Architecture) documents into various output formats like PDF, HTML5, EPUB, etc.

**DITA** is an XML-based documentation standard used by companies like Microsoft, IBM, and Adobe for technical documentation.

### Why Kotlin?

The plugin is written in **Kotlin** because:
- Kotlin is the recommended language by Gradle since 2019
- More concise syntax than Java
- Excellent interoperability with Java and Groovy
- Native support for DSLs (Domain Specific Languages)

---

## Project Structure

```
dita-ot-gradle/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/github/jyjeanne/
│   │           ├── DitaOtPlugin.kt      # Plugin entry point
│   │           ├── DitaOtTask.kt        # Main task (600+ lines)
│   │           ├── Options.kt           # Configuration options
│   │           ├── AntExecutor.kt       # DITA-OT execution
│   │           ├── Classpath.kt         # Classpath management
│   │           ├── PropertyBuilder.kt   # Property DSL
│   │           ├── GroovyPropertyCapture.kt  # Groovy closure capture
│   │           ├── FileExtensions.kt    # Extension constants
│   │           ├── GlobPatterns.kt      # File patterns
│   │           ├── Messages.kt          # Error messages
│   │           └── Properties.kt        # Property constants
│   └── test/
│       └── kotlin/
│           └── com/github/jyjeanne/
│               ├── DitaOtTaskSpec.kt    # Unit tests
│               └── GroovyPropertyCaptureSpec.kt
├── examples/                            # Usage examples
├── build.gradle.kts                     # Build configuration
└── README.md
```

---

## Core Concepts

### 1. Gradle Plugin

A **Gradle plugin** is an extension that adds functionality to Gradle. It can:
- Add **tasks**
- Define **configurations**
- Extend Gradle's DSL

```kotlin
// How a plugin is used in build.gradle.kts
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.3.1"
}
```

### 2. Gradle Task

A **task** is an executable unit of work. Our plugin creates a `dita` task that transforms documents.

```kotlin
// The "dita" task is automatically created by the plugin
tasks.named<DitaOtTask>("dita") {
    input("guide.ditamap")
    transtype("html5")
}
```

### 3. Provider API

Gradle's **Provider API** allows defining "lazy" values that are evaluated at execution time, not configuration time.

```kotlin
// Example: DirectoryProperty is a Provider
abstract val outputDir: DirectoryProperty

// The value is only computed when .get() is called
val dir = outputDir.get().asFile
```

### 4. Configuration Cache

Gradle's **Configuration Cache** stores the task graph for reuse. This speeds up subsequent builds by **77%**.

---

## Kotlin Classes Explained

### 1. DitaOtPlugin.kt - Entry Point

**Role:** This is the main plugin class. It's called by Gradle when the plugin is applied.

```kotlin
package com.github.jyjeanne

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for publishing DITA documents.
 *
 * This class implements Gradle's Plugin<Project> interface.
 * The apply() method is automatically called when the plugin is used.
 */
class DitaOtPlugin : Plugin<Project> {

    /**
     * Method called by Gradle when applying the plugin.
     *
     * @param project The Gradle project to which the plugin is applied
     */
    override fun apply(project: Project) {
        // 1. Apply the "base" plugin to get standard tasks
        project.plugins.apply("base")

        // 2. Register the "dita" task
        project.tasks.register(DITA, DitaOtTask::class.java) { task ->
            task.group = "Documentation"  // Group in ./gradlew tasks
            task.description = "Publishes DITA documentation with DITA Open Toolkit."
        }
    }

    companion object {
        const val DITA = "dita"  // Task name
    }
}
```

**Key points:**
- `Plugin<Project>`: Gradle interface for creating a plugin
- `apply()`: Method called automatically
- `tasks.register()`: Creates a task lazily

---

### 2. DitaOtTask.kt - Main Task

**Role:** This is the heart of the plugin. This class defines the task that transforms DITA documents.

```kotlin
package com.github.jyjeanne

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject
import java.io.File

/**
 * Gradle task for executing DITA-OT transformations.
 *
 * Important annotations:
 * - @CacheableTask: Enables result caching
 * - @Inject: Gradle dependency injection
 */
@CacheableTask
abstract class DitaOtTask @Inject constructor(
    private val objectFactory: ObjectFactory,      // For creating file collections
    private val projectLayout: ProjectLayout       // For accessing project directories
) : DefaultTask() {

    // =========================================================================
    // INPUT PROPERTIES
    // These properties define WHAT Gradle should transform
    // =========================================================================

    /**
     * DITA-OT installation directory.
     *
     * @InputDirectory: Gradle monitors this directory for changes
     * @PathSensitive: Only content matters, not the absolute path
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ditaOtDir: DirectoryProperty

    /**
     * DITA input files (ditamap, topics, etc.)
     *
     * @InputFiles: These files are transformation inputs
     * @SkipWhenEmpty: If empty, the task is skipped (NO_SOURCE)
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * Custom DITA properties (args.css, processing-mode, etc.)
     *
     * MapProperty<String, String> is a "lazy" property that contains
     * a key-value map.
     */
    @get:Input
    @get:Optional
    abstract val ditaProperties: MapProperty<String, String>

    // =========================================================================
    // OUTPUT PROPERTIES
    // These properties define WHERE Gradle should generate files
    // =========================================================================

    /**
     * Output directory for generated files.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // =========================================================================
    // DSL METHODS (Domain Specific Language)
    // These methods allow configuring the task in build.gradle
    // =========================================================================

    /**
     * Configure the DITA-OT directory.
     * Can receive a String, File, or anything convertible to File.
     */
    fun ditaOt(path: Any) {
        when (path) {
            is File -> ditaOtDir.set(path)
            is String -> ditaOtDir.set(project.file(path))
            else -> ditaOtDir.set(project.file(path))
        }
        options.ditaOt = project.file(path)
    }

    /**
     * Configure input files.
     */
    fun input(files: Any) {
        inputFiles.from(files)
        options.input = files
    }

    /**
     * Configure output types (html5, pdf, epub, etc.)
     */
    fun transtype(vararg t: String) {
        transtypes.set(t.toList())
        options.transtype = t.toList()
    }

    /**
     * Configure DITA properties via a Kotlin closure.
     *
     * Usage example:
     *   properties {
     *       "processing-mode" to "strict"
     *       property("args.css", "custom.css")
     *   }
     */
    fun properties(block: PropertyBuilder.() -> Unit) {
        val builder = PropertyBuilder()
        builder.block()
        val props = builder.build()
        ditaProperties.putAll(props)
    }

    // =========================================================================
    // TASK EXECUTION
    // =========================================================================

    /**
     * Main method executed by Gradle.
     *
     * @TaskAction: Indicates this is the method to execute
     */
    @TaskAction
    fun render() {
        // 1. Validate inputs
        val ditaHome = resolveDitaHome()
        val inputs = inputFiles.files.filter { it.exists() }

        if (inputs.isEmpty()) {
            logger.warn("No input files found")
            return
        }

        // 2. Determine execution strategy
        val strategy = antExecutionStrategy.get()

        // 3. Execute transformation
        when (Options.Companion.AntExecutionStrategy.valueOf(strategy)) {
            Options.Companion.AntExecutionStrategy.DITA_SCRIPT -> {
                renderViaDitaScript(ditaHome, inputs, transtypes.get().toTypedArray())
            }
            else -> {
                renderViaIsolatedAntBuilder(ditaHome, inputs, transtypes.get().toTypedArray())
            }
        }
    }

    /**
     * Execute via the dita/dita.bat script (default strategy).
     * This method avoids Gradle's ClassLoader issues.
     */
    private fun renderViaDitaScript(
        ditaHome: File,
        inputs: Set<File>,
        types: Array<String>
    ): Boolean {
        inputs.forEach { inputFile ->
            types.forEach { transtype ->
                // Build the properties map
                val properties = mutableMapOf<String, String>()

                // Capture properties from Groovy closure
                if (groovyProperties != null) {
                    val captured = GroovyPropertyCapture.captureFromClosure(groovyProperties)
                    properties.putAll(captured)
                }

                // Add properties from Provider API
                if (ditaProperties.isPresent) {
                    properties.putAll(ditaProperties.get())
                }

                // Execute via AntExecutor
                AntExecutor.executeViaDitaScript(
                    ditaHome = ditaHome,
                    inputFile = inputFile,
                    transtype = transtype,
                    outputDir = getOutputDirectory(inputFile, transtype),
                    tempDir = tempDir.asFile.get(),
                    filterFile = getDitavalFile(inputFile),
                    properties = properties,
                    logger = logger
                )
            }
        }
        return true
    }
}
```

**Key points:**
- `@CacheableTask`: Enables caching
- `@Inject constructor`: Gradle dependency injection
- `abstract val`: Properties managed by Gradle (Provider API)
- `@TaskAction`: Method executed by `./gradlew dita`

---

### 3. Options.kt - Configuration

**Role:** Stores all task configuration options.

```kotlin
package com.github.jyjeanne

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import java.io.File

/**
 * Storage class for configuration options.
 *
 * This class uses mutable properties (var) because it's
 * configured progressively during Gradle's configuration phase.
 */
class Options {

    companion object {
        // Default transtype if not specified
        const val DEFAULT_TRANSTYPE = "html5"

        /**
         * Available ANT execution strategies.
         *
         * enum class in Kotlin is equivalent to enum in Java,
         * but more powerful as it can have properties and methods.
         */
        enum class AntExecutionStrategy {
            /** Uses Gradle's IsolatedAntBuilder (has known issues) */
            ISOLATED_BUILDER,

            /** Executes via dita/dita.bat script (RECOMMENDED) */
            DITA_SCRIPT,

            /** Executes via custom ClassLoader */
            CUSTOM_CLASSLOADER,

            /** Executes via Gradle exec */
            GRADLE_EXEC,

            /** Experimental: Groovy ANT binding */
            GROOVY_ANT_BINDING
        }

        /**
         * Returns the default temporary directory.
         * Uses the system temp directory with a unique timestamp.
         */
        private fun getDefaultTempDir(): File {
            val tmpdir = System.getProperty("java.io.tmpdir")
            return File("$tmpdir/dita-ot", System.currentTimeMillis().toString())
        }
    }

    // =========================================================================
    // CONFIGURATION PROPERTIES
    // =========================================================================

    /** Developer mode: includes DITA-OT files in inputs */
    var devMode: Boolean = false

    /** Use a single output directory for all files */
    var singleOutputDir: Boolean = false

    /** Use the associated DITAVAL file automatically */
    var useAssociatedFilter: Boolean = false

    /** DITA-OT installation directory */
    var ditaOt: File? = null

    /** Custom classpath (optional) */
    var classpath: FileCollection? = null

    /** DITA input file(s) */
    var input: Any? = null

    /** DITAVAL filter file */
    var filter: Any? = null

    /** Output directory */
    var output: File? = null

    /** Temporary directory */
    var temp: File = getDefaultTempDir()

    /** Properties via Groovy closure (backward compatibility) */
    var properties: Closure<*>? = null

    /** Properties via Kotlin DSL */
    var kotlinProperties: Map<String, String>? = null

    /** Output types (pdf, html5, etc.) */
    var transtype: List<String> = listOf(DEFAULT_TRANSTYPE)

    /**
     * ANT execution strategy.
     *
     * DITA_SCRIPT is the default because it avoids
     * IsolatedAntBuilder ClassLoader issues.
     */
    var antExecutionStrategy: AntExecutionStrategy = AntExecutionStrategy.DITA_SCRIPT
}
```

**Key points:**
- `companion object`: Equivalent to static members in Java
- `enum class`: Kotlin enumeration that can have properties
- `var` vs `val`: `var` is mutable, `val` is immutable

---

### 4. AntExecutor.kt - Execution

**Role:** Manages DITA-OT execution via different strategies.

```kotlin
package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.File

/**
 * ANT executor for DITA-OT.
 *
 * object in Kotlin = Singleton (single instance)
 * Equivalent to a class with static methods in Java.
 */
object AntExecutor {

    /**
     * Executes DITA-OT via its native script (dita or dita.bat).
     *
     * This is the RECOMMENDED strategy because it avoids all
     * Gradle ClassLoader issues.
     *
     * @param ditaHome     DITA-OT installation directory
     * @param inputFile    DITA input file
     * @param transtype    Output format (html5, pdf, etc.)
     * @param outputDir    Output directory
     * @param tempDir      Temporary directory
     * @param filterFile   DITAVAL file (optional)
     * @param properties   Additional ANT properties
     * @param logger       Gradle logger
     * @return Exit code (0 = success)
     */
    fun executeViaDitaScript(
        ditaHome: File,
        inputFile: File,
        transtype: String,
        outputDir: File,
        tempDir: File,
        filterFile: File? = null,
        properties: Map<String, String> = emptyMap(),
        logger: Logger
    ): Int {
        // 1. Detect operating system
        val isWindows = System.getProperty("os.name")
            .lowercase()
            .contains("win")

        // 2. Find the DITA-OT script
        val ditaScript = if (isWindows) {
            // Windows: look for dita.bat
            val scriptInBin = File(ditaHome, "bin/dita.bat")
            if (scriptInBin.exists()) scriptInBin else File(ditaHome, "dita.bat")
        } else {
            // Linux/Mac: look for dita
            val scriptInBin = File(ditaHome, "bin/dita")
            if (scriptInBin.exists()) scriptInBin else File(ditaHome, "dita")
        }

        // 3. Verify script exists
        if (!ditaScript.exists()) {
            logger.error("DITA script not found: ${ditaScript.absolutePath}")
            return -1
        }

        // 4. Build the command
        val command = mutableListOf<String>()
        command.add(ditaScript.absolutePath)
        command.add("--input=${inputFile.absolutePath}")
        command.add("--format=$transtype")
        command.add("--output=${outputDir.absolutePath}")
        command.add("--temp=${tempDir.absolutePath}")

        // Add DITAVAL filter if present
        if (filterFile != null && filterFile.exists()) {
            command.add("--filter=${filterFile.absolutePath}")
        }

        // Add custom properties
        properties.forEach { (name, value) ->
            command.add("-D$name=$value")
        }

        // 5. Execute the process
        logger.info("Executing: ${command.joinToString(" ")}")

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(ditaHome)
        processBuilder.redirectErrorStream(true)  // Merge stderr into stdout
        processBuilder.inheritIO()                 // Display output in console

        // Set environment variables
        processBuilder.environment()["DITA_HOME"] = ditaHome.absolutePath

        // Start and wait for completion
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            logger.info("✓ Transformation successful")
        } else {
            logger.error("✗ Failed (code: $exitCode)")
        }

        return exitCode
    }
}
```

**Key points:**
- `object`: Singleton in Kotlin
- `ProcessBuilder`: Java API for executing external processes
- `mutableListOf`: Mutable list (can be modified)
- `?.`: Safe call operator (avoids NullPointerException)

---

### 5. Classpath.kt - Classpath Management

**Role:** Builds the classpath required to execute DITA-OT.

```kotlin
package com.github.jyjeanne

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utility for building DITA-OT classpath.
 *
 * DITA-OT requires a specific classpath including:
 * - JARs in lib/
 * - Plugin JARs
 * - config/ and resources/ directories
 */
object Classpath {

    /**
     * Gets DITA-OT plugin JAR files.
     *
     * Parses the plugins.xml file to find all declared JARs.
     *
     * @JvmStatic allows calling this method from Java as
     * a static method: Classpath.getPluginClasspathFiles(...)
     */
    @JvmStatic
    fun getPluginClasspathFiles(ditaHome: File?): List<File> {
        // Null check with throw if null
        if (ditaHome == null) {
            throw GradleException("DITA-OT directory not configured")
        }

        // Look for plugins.xml file
        // listOf creates an immutable list
        val plugins = listOf(
            File(ditaHome, "config/plugins.xml"),
            File(ditaHome, "resources/plugins.xml")
        ).find { it.exists() }  // find returns the first matching element
            ?: throw GradleException("plugins.xml file not found")

        // Parse the XML
        val archives = mutableListOf<File>()

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(plugins)

        // Iterate through <plugin> elements
        val pluginNodes = doc.documentElement.getElementsByTagName("plugin")
        for (i in 0 until pluginNodes.length) {
            val pluginNode = pluginNodes.item(i)

            // Get the xml:base attribute
            val xmlBase = pluginNode.attributes
                .getNamedItemNS("http://www.w3.org/XML/1998/namespace", "base")
                ?.nodeValue ?: continue  // continue if null

            val pluginDir = File(plugins.parent, xmlBase)

            // Find JARs declared in <feature>
            val featureNodes = (pluginNode as org.w3c.dom.Element)
                .getElementsByTagName("feature")

            for (j in 0 until featureNodes.length) {
                val file = featureNodes.item(j)
                    .attributes
                    .getNamedItem("file")
                    ?.nodeValue

                if (file != null) {
                    archives.add(File(pluginDir.parent, file))
                }
            }
        }

        return archives
    }

    /**
     * Builds the complete classpath for compilation.
     *
     * Compatible with Configuration Cache by using
     * ObjectFactory instead of Project.
     */
    @JvmStatic
    fun compileWithObjectFactory(
        objectFactory: ObjectFactory,
        ditaHome: File
    ): FileCollection {
        val files = getCompileClasspathFiles(ditaHome)
        // objectFactory.fileCollection() creates a Provider API compatible collection
        return objectFactory.fileCollection().from(files)
    }

    /**
     * Gets all classpath files.
     */
    @JvmStatic
    fun getCompileClasspathFiles(ditaHome: File): List<File> {
        val libDir = File(ditaHome, "lib")

        // List JARs (except ant.jar which is provided by Gradle)
        val libJars = libDir.listFiles { file ->
            file.isFile &&
            file.name.endsWith(".jar") &&
            file.name != "ant.jar" &&
            file.name != "ant-launcher.jar"
        }?.toList() ?: emptyList()

        // Build the final classpath
        return mutableListOf<File>().apply {
            addAll(libJars)
            addAll(getPluginClasspathFiles(ditaHome))
            add(File(ditaHome, "config"))
            add(File(ditaHome, "resources"))
        }
    }
}
```

**Key points:**
- `@JvmStatic`: Generates a static method for Java interop
- `?.` and `?:`: Kotlin null-safe operators
- `apply {}`: Configures an object and returns it
- `until`: Exclusive range (0 until 5 = 0, 1, 2, 3, 4)

---

### 6. PropertyBuilder.kt - Property DSL

**Role:** Allows configuring DITA properties with elegant syntax.

```kotlin
package com.github.jyjeanne

/**
 * Builder for creating DITA properties with DSL syntax.
 *
 * Usage example:
 *   properties {
 *       "processing-mode" to "strict"      // Infix syntax
 *       property("args.css", "custom.css") // Function syntax
 *   }
 */
class PropertyBuilder {
    // Mutable map to store properties
    private val properties = mutableMapOf<String, String>()

    /**
     * Add a property with infix notation.
     *
     * "infix" allows calling the function without parentheses or dot:
     *   "key" to "value"   instead of   "key".to("value")
     *
     * This function is defined as a String extension.
     */
    infix fun String.to(value: String) {
        properties[this] = value  // this = the String on which we call
    }

    /**
     * Add a property with classic function call.
     */
    fun property(name: String, value: String) {
        properties[name] = value
    }

    /**
     * Add a property with a file path.
     */
    fun propertyLocation(name: String, location: java.io.File) {
        properties[name] = location.absolutePath
    }

    /**
     * Returns an immutable copy of the properties.
     *
     * toMap() creates a copy to avoid external modifications.
     */
    fun build(): Map<String, String> = properties.toMap()

    fun isEmpty(): Boolean = properties.isEmpty()
    fun isNotEmpty(): Boolean = properties.isNotEmpty()
}
```

**Key points:**
- `infix`: Allows syntax `a to b` instead of `a.to(b)`
- Extension function: `String.to()` adds a method to String
- `this` in an extension: Refers to the object on which we call

---

### 7. GroovyPropertyCapture.kt - Closure Capture

**Role:** Extracts properties from a Groovy closure for use with DITA_SCRIPT.

```kotlin
package com.github.jyjeanne

import groovy.lang.Closure

/**
 * Captures properties defined in a Groovy closure.
 *
 * Problem solved:
 * Groovy closures like:
 *   properties {
 *       property name: 'args.css', value: 'custom.css'
 *   }
 *
 * Didn't work with DITA_SCRIPT strategy because they
 * are designed to be executed with an ANT delegate.
 *
 * This class acts as a "fake" delegate that captures
 * property() calls and stores them in a Map.
 */
class GroovyPropertyCapture {
    // Storage for captured properties
    private val capturedProperties = mutableMapOf<String, String>()

    /**
     * Intercepts calls to property(name: '...', value: '...')
     *
     * In Groovy, property(name: 'x', value: 'y') is equivalent to
     * property(mapOf("name" to "x", "value" to "y"))
     */
    fun property(args: Map<String, Any?>) {
        val name = args["name"]?.toString()
        val value = args["value"]?.toString()

        // Only if both are present
        if (name != null && value != null) {
            capturedProperties[name] = value
        }
    }

    /**
     * Returns the captured properties.
     */
    fun getCapturedProperties(): Map<String, String> =
        capturedProperties.toMap()

    companion object {
        /**
         * Executes a closure and captures all properties.
         *
         * @JvmStatic to be callable from Java:
         *   GroovyPropertyCapture.captureFromClosure(closure)
         */
        @JvmStatic
        fun captureFromClosure(closure: Closure<*>?): Map<String, String> {
            if (closure == null) return emptyMap()

            // Create an instance to capture
            val capture = GroovyPropertyCapture()

            // Configure the closure to use our capture as delegate
            closure.delegate = capture
            closure.resolveStrategy = Closure.DELEGATE_FIRST

            // Execute the closure (property() calls go to capture)
            try {
                closure.call()
            } catch (e: Exception) {
                // Ignore errors (some closures may have side effects)
            }

            return capture.getCapturedProperties()
        }
    }
}
```

**Key points:**
- `Closure<*>`: Groovy type for closures
- `delegate`: Object to which unresolved calls are delegated
- `DELEGATE_FIRST`: Looks first in delegate, then in owner

---

### 8. Utility Classes

#### FileExtensions.kt
```kotlin
package com.github.jyjeanne

/**
 * Constants for file extensions.
 *
 * object = Singleton, constants are accessible via:
 *   FileExtensions.PROPERTIES
 *   FileExtensions.DITAVAL
 */
object FileExtensions {
    const val PROPERTIES = ".properties"
    const val DITAVAL = ".ditaval"
}
```

#### GlobPatterns.kt
```kotlin
package com.github.jyjeanne

/**
 * Glob patterns for file searching.
 */
object GlobPatterns {
    const val ALL_FILES = "*/**"  // All files recursively
}
```

#### Messages.kt
```kotlin
package com.github.jyjeanne

/**
 * Error and help messages.
 *
 * Uses Kotlin raw strings (triple quotes) for
 * multi-line messages with preserved formatting.
 */
object Messages {
    val ditaHomeError = """
        DITA-OT directory not configured.

        Please configure it in your build.gradle:

        dita {
            ditaOt '/path/to/dita-ot'
            input 'my.ditamap'
            transtype 'html5'
        }
    """.trimIndent()  // Removes common indentation
}
```

---

## Execution Flow

### Sequence Diagram

```
┌──────────┐     ┌─────────────┐     ┌────────────┐     ┌─────────────┐
│  Gradle  │     │ DitaOtPlugin│     │ DitaOtTask │     │ AntExecutor │
└────┬─────┘     └──────┬──────┘     └─────┬──────┘     └──────┬──────┘
     │                  │                  │                   │
     │  apply plugin    │                  │                   │
     │─────────────────>│                  │                   │
     │                  │                  │                   │
     │                  │  register task   │                   │
     │                  │─────────────────>│                   │
     │                  │                  │                   │
     │  configure task  │                  │                   │
     │─────────────────────────────────────>│                   │
     │                  │                  │                   │
     │  ./gradlew dita  │                  │                   │
     │─────────────────────────────────────>│                   │
     │                  │                  │                   │
     │                  │                  │  executeViaDitaScript
     │                  │                  │──────────────────>│
     │                  │                  │                   │
     │                  │                  │   ProcessBuilder  │
     │                  │                  │   dita --input... │
     │                  │                  │<──────────────────│
     │                  │                  │                   │
     │  BUILD SUCCESS   │                  │                   │
     │<─────────────────────────────────────│                   │
```

### Gradle Phases

1. **Initialization**: Gradle loads scripts
2. **Configuration**: Plugin is applied, tasks are created
3. **Execution**: `./gradlew dita` executes `DitaOtTask.render()`

---

## Tests

### Test Structure

```kotlin
// DitaOtTaskSpec.kt uses Kotest (Kotlin test framework)

class DitaOtTaskSpec : StringSpec({

    // StringSpec allows tests with readable syntax
    "Creating a task" {
        val task = project.tasks.create("dita", DitaOtTask::class.java)
        task.input("root.ditamap")

        // shouldBe is a Kotest assertion
        task.options.input shouldBe "root.ditamap"
    }

    "Groovy closure properties are captured correctly" {
        val closure = object : Closure<Unit>(null) {
            override fun call() {
                (delegate as GroovyPropertyCapture).property(
                    mapOf("name" to "args.css", "value" to "custom.css")
                )
            }
        }

        val captured = GroovyPropertyCapture.captureFromClosure(closure)

        captured["args.css"] shouldBe "custom.css"
    }
})
```

### Running Tests

```bash
# All tests
./gradlew test

# Specific tests
./gradlew test --tests "DitaOtTaskSpec"

# With detailed report
./gradlew test --info
```

---

## Glossary

| Term | Definition |
|------|------------|
| **DSL** | Domain Specific Language - Specialized language for a domain |
| **Provider API** | Gradle API for "lazy" values (evaluated at execution) |
| **Configuration Cache** | Gradle cache that stores the task graph |
| **Closure** | Anonymous function in Groovy that captures its context |
| **Singleton** | Pattern where only one instance of a class exists |
| **Extension Function** | Function added to an existing class (Kotlin) |
| **Infix** | Notation allowing calling a function without parentheses |
| **DITA-OT** | DITA Open Toolkit - DITA transformation tool |
| **Transtype** | Output format (html5, pdf, epub, etc.) |
| **DITAVAL** | DITA conditional filtering file |

---

## Further Reading

### Kotlin Resources
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Koans](https://play.kotlinlang.org/koans/overview)

### Gradle Resources
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Provider API](https://docs.gradle.org/current/userguide/lazy_configuration.html)
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)

### DITA Resources
- [DITA-OT Documentation](https://www.dita-ot.org/dev/)
- [OASIS DITA Standard](https://www.oasis-open.org/committees/dita/)

---

*Document created December 16, 2025*
*Plugin version: 2.3.1*
