package com.github.jyjeanne

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object Classpath {
    @JvmStatic
    fun pluginClasspath(project: Project, ditaHome: File?): FileCollection {
        if (ditaHome == null) {
            throw GradleException("You must specify the DITA-OT directory (ditaOt.dir) before you can retrieve the plugin classpath.")
        }

        val plugins = listOf(
            File(ditaHome, "config/plugins.xml"),
            File(ditaHome, "resources/plugins.xml")
        ).find { it.exists() }
            ?: throw GradleException(
                """
                Can't find DITA-OT plugin XML file.
                Are you sure you're using a valid DITA-OT directory?
                """.trimIndent()
            )

        val archives = mutableListOf<File>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(plugins)

            val pluginNodes = doc.documentElement.getElementsByTagName("plugin")
            for (i in 0 until pluginNodes.length) {
                val pluginNode = pluginNodes.item(i)
                val xmlBase = pluginNode.attributes.getNamedItemNS(
                    "http://www.w3.org/XML/1998/namespace",
                    "base"
                )?.nodeValue ?: continue

                val pluginDir = File(plugins.parent, xmlBase)
                check(pluginDir.exists()) { "Plugin directory does not exist: $pluginDir" }

                val featureNodes = (pluginNode as org.w3c.dom.Element).getElementsByTagName("feature")
                for (j in 0 until featureNodes.length) {
                    val featureNode = featureNodes.item(j)
                    val file = featureNode.attributes.getNamedItem("file")?.nodeValue
                    if (file != null) {
                        archives.add(File(pluginDir.parent, file))
                    }
                }
            }
        } catch (e: Exception) {
            throw GradleException("Failed to parse DITA-OT plugin XML: ${plugins.absolutePath}", e)
        }

        check(archives.isNotEmpty()) { "No plugin archives found" }

        return project.files(archives)
    }

    @JvmStatic
    fun compile(project: Project, ditaHome: File): FileCollection {
        System.setProperty("logback.configurationFile", "$ditaHome/config/logback.xml")

        val fileTree = project.fileTree(ditaHome).apply {
            include(
                "config/",
                "resources/",
                "lib/**/*.jar"
            )

            exclude(
                "lib/ant-launcher.jar",
                "lib/ant.jar"
            )
        }

        return fileTree + pluginClasspath(project, ditaHome)
    }
}
