package com.github.jyjeanne

/**
 * Builder for creating DITA-OT properties in a Kotlin DSL-friendly way.
 *
 * Example usage:
 * ```kotlin
 * dita {
 *     properties {
 *         "processing-mode" to "strict"
 *         "args.cssroot" to "$projectDir/css"
 *         property("args.rellinks", "all")
 *     }
 * }
 * ```
 */
class PropertyBuilder {
    private val properties = mutableMapOf<String, String>()

    /**
     * Add a property using infix notation.
     * Example: "processing-mode" to "strict"
     */
    infix fun String.to(value: String) {
        properties[this] = value
    }

    /**
     * Add a property using method call.
     * Example: property("processing-mode", "strict")
     */
    fun property(name: String, value: String) {
        properties[name] = value
    }

    /**
     * Add a property with a file location.
     * The file path will be converted to absolute path.
     * Example: propertyLocation("args.cssroot", File("css"))
     */
    fun propertyLocation(name: String, location: java.io.File) {
        properties[name] = location.absolutePath
    }

    /**
     * Get all properties as an immutable map.
     */
    fun build(): Map<String, String> = properties.toMap()

    /**
     * Check if any properties have been added.
     */
    fun isEmpty(): Boolean = properties.isEmpty()

    /**
     * Check if properties have been added.
     */
    fun isNotEmpty(): Boolean = properties.isNotEmpty()
}
