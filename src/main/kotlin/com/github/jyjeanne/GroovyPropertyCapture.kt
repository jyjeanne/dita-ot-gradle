package com.github.jyjeanne

import groovy.lang.Closure

/**
 * Captures properties from a Groovy closure that uses ANT-style property syntax.
 *
 * This class acts as a delegate for Groovy closures that call:
 * ```groovy
 * property name: 'key', value: 'value'
 * property(name: 'key', value: 'value')
 * ```
 *
 * The captured properties can then be used with the DITA_SCRIPT execution strategy.
 */
class GroovyPropertyCapture {
    private val capturedProperties = mutableMapOf<String, String>()

    /**
     * Capture a property call with named arguments (ANT style).
     * Handles: property name: 'key', value: 'value'
     */
    fun property(args: Map<String, Any?>) {
        val name = args["name"]?.toString()
        val value = args["value"]?.toString()
        if (name != null && value != null) {
            capturedProperties[name] = value
        }
    }

    /**
     * Get all captured properties.
     */
    fun getCapturedProperties(): Map<String, String> = capturedProperties.toMap()

    companion object {
        /**
         * Execute a Groovy closure and capture all property definitions.
         *
         * @param closure The Groovy closure that defines properties
         * @return A map of captured property name-value pairs
         */
        @JvmStatic
        fun captureFromClosure(closure: Closure<*>?): Map<String, String> {
            if (closure == null) return emptyMap()

            val capture = GroovyPropertyCapture()
            closure.delegate = capture
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            try {
                closure.call()
            } catch (e: Exception) {
                // Log but don't fail - some closures might have side effects we can't handle
            }
            return capture.getCapturedProperties()
        }
    }
}
