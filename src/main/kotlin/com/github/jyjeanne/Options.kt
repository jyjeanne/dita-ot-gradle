package com.github.jyjeanne

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import java.io.File

class Options {
    companion object {
        const val DEFAULT_TRANSTYPE = "html5"

        // ANT execution strategy options
        enum class AntExecutionStrategy {
            /** Use Gradle's IsolatedAntBuilder (default, has known issues) */
            ISOLATED_BUILDER,

            /** Execute via DITA-OT script (dita or dita.bat) */
            DITA_SCRIPT,

            /** Execute via custom Java classloader */
            CUSTOM_CLASSLOADER,

            /** Execute via Gradle exec task wrapper */
            GRADLE_EXEC,

            /** Experimental: Use Groovy's Ant binding */
            GROOVY_ANT_BINDING
        }

        private fun getDefaultTempDir(): File {
            val tmpdir = System.getProperty("java.io.tmpdir")
            return File("$tmpdir/dita-ot", System.currentTimeMillis().toString())
        }
    }

    var devMode: Boolean = false
    var singleOutputDir: Boolean = false
    var useAssociatedFilter: Boolean = false

    var ditaOt: File? = null
    var classpath: FileCollection? = null
    var input: Any? = null
    var filter: Any? = null
    var output: File? = null
    var temp: File = getDefaultTempDir()

    // Groovy Closure-based properties (for backward compatibility)
    var properties: Closure<*>? = null

    // Kotlin DSL-based properties
    var kotlinProperties: Map<String, String>? = null

    var transtype: List<String> = listOf(DEFAULT_TRANSTYPE)

    // Experimental: ANT execution strategy (default is ISOLATED_BUILDER for backward compatibility)
    var antExecutionStrategy: AntExecutionStrategy = AntExecutionStrategy.ISOLATED_BUILDER
}
