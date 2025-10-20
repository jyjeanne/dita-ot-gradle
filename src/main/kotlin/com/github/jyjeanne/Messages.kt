package com.github.jyjeanne

import java.util.Properties

object Messages {
    private val properties: Properties by lazy {
        Properties().apply {
            val stream = javaClass.classLoader.getResourceAsStream("messages.properties")
                ?: throw IllegalStateException("messages.properties not found")
            stream.use { load(it) }
        }
    }

    val classpathError: String
        get() = properties.getProperty("classpathError")

    val ditaHomeError: String
        get() = properties.getProperty("ditaHomeError")
}
