package com.github.jyjeanne

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import java.io.File

class Options {
    companion object {
        const val DEFAULT_TRANSTYPE = "html5"

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

    var properties: Closure<*>? = null
    var transtype: List<String> = listOf(DEFAULT_TRANSTYPE)
}
