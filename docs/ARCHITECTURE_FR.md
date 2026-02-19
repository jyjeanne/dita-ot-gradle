# Architecture du Plugin DITA-OT Gradle

Guide complet pour comprendre l'architecture du projet destiné aux développeurs débutants.

---

## Table des Matières

1. [Introduction](#introduction)
2. [Structure du Projet](#structure-du-projet)
3. [Concepts Fondamentaux](#concepts-fondamentaux)
4. [Les Classes Kotlin Expliquées](#les-classes-kotlin-expliquées)
5. [Flux d'Exécution](#flux-dexécution)
6. [Tests](#tests)
7. [Glossaire](#glossaire)

---

## Introduction

### Qu'est-ce que ce plugin ?

Ce plugin Gradle permet de transformer des documents **DITA** (Darwin Information Typing Architecture) en différents formats de sortie comme PDF, HTML5, EPUB, etc.

**DITA** est un standard XML pour la documentation technique utilisé par des entreprises comme Microsoft, IBM, et Adobe.

### Pourquoi Kotlin ?

Le plugin est écrit en **Kotlin** car :
- Kotlin est le langage recommandé par Gradle depuis 2019
- Syntaxe plus concise que Java
- Excellente interopérabilité avec Java et Groovy
- Support natif des DSL (Domain Specific Languages)

---

## Structure du Projet

```
dita-ot-gradle/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/github/jyjeanne/
│   │           ├── DitaOtPlugin.kt            # Point d'entrée du plugin
│   │           ├── DitaOtTask.kt              # Tâche principale de transformation
│   │           ├── DitaOtDownloadTask.kt      # Téléchargement et installation DITA-OT
│   │           ├── DitaOtInstallPluginTask.kt # Installation de plugins
│   │           ├── DitaOtValidateTask.kt      # Validation du contenu DITA
│   │           ├── DitaLinkCheckTask.kt       # Vérification des liens
│   │           ├── ProgressReporter.kt        # Reporting visuel de progression
│   │           ├── Platform.kt                # Utilitaires multi-plateformes
│   │           ├── Options.kt                 # Configuration des options
│   │           ├── AntExecutor.kt             # Exécution de processus DITA-OT
│   │           ├── Classpath.kt               # Gestion du classpath
│   │           ├── PropertyBuilder.kt         # DSL pour les propriétés
│   │           ├── GroovyPropertyCapture.kt   # Capture des closures Groovy
│   │           ├── FileExtensions.kt          # Constantes d'extensions
│   │           ├── GlobPatterns.kt            # Patterns de fichiers
│   │           ├── Messages.kt                # Messages d'erreur
│   │           └── Properties.kt              # Constantes de propriétés
│   └── test/
│       └── kotlin/
│           └── com/github/jyjeanne/
│               ├── DitaOtTaskSpec.kt              # Tests DitaOtTask
│               ├── DitaOtPluginTest.kt            # Tests enregistrement plugin
│               ├── DitaOtInstallPluginTaskTest.kt # Tests installation plugins
│               ├── DitaOtValidateTaskTest.kt      # Tests validation
│               ├── DitaLinkCheckTaskTest.kt       # Tests vérification liens
│               ├── AntExecutorTest.kt             # Tests AntExecutor
│               ├── ProgressReporterTest.kt        # Tests reporting progression
│               └── GroovyPropertyCaptureSpec.kt   # Tests capture Groovy
├── examples/                                # Exemples d'utilisation
├── docs/                                    # Documentation
├── build.gradle.kts                         # Configuration du build
└── README.md
```

---

## Concepts Fondamentaux

### 1. Plugin Gradle

Un **plugin Gradle** est une extension qui ajoute des fonctionnalités à Gradle. Il peut :
- Ajouter des **tâches** (tasks)
- Définir des **configurations**
- Étendre le DSL de Gradle

```kotlin
// Comment un plugin est utilisé dans build.gradle.kts
plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.5"
}
```

### 2. Tâche Gradle (Task)

Une **tâche** est une unité de travail exécutable. Notre plugin crée une tâche `dita` qui transforme les documents.

```kotlin
// La tâche "dita" est automatiquement créée par le plugin
tasks.named<DitaOtTask>("dita") {
    input("guide.ditamap")
    transtype("html5")
}
```

### 3. Provider API

L'**API Provider** de Gradle permet de définir des valeurs "lazy" (paresseuses) qui sont évaluées au moment de l'exécution, pas de la configuration.

```kotlin
// Exemple : DirectoryProperty est un Provider
abstract val outputDir: DirectoryProperty

// La valeur n'est calculée que quand on appelle .get()
val dir = outputDir.get().asFile
```

### 4. Configuration Cache

Le **Configuration Cache** de Gradle stocke le graphe des tâches pour réutilisation. Cela accélère les builds suivants de **77%**.

---

## Les Classes Kotlin Expliquées

### 1. DitaOtPlugin.kt - Point d'Entrée

**Rôle :** C'est la classe principale du plugin. Elle est appelée par Gradle quand on applique le plugin.

```kotlin
package com.github.jyjeanne

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin Gradle pour publier des documents DITA.
 *
 * Cette classe implémente l'interface Plugin<Project> de Gradle.
 * La méthode apply() est appelée automatiquement quand on utilise le plugin.
 */
class DitaOtPlugin : Plugin<Project> {

    /**
     * Méthode appelée par Gradle lors de l'application du plugin.
     *
     * @param project Le projet Gradle auquel le plugin est appliqué
     */
    override fun apply(project: Project) {
        // 1. Applique le plugin "base" pour avoir les tâches standard
        project.plugins.apply("base")

        // 2. Enregistre la tâche "dita"
        project.tasks.register(DITA, DitaOtTask::class.java) { task ->
            task.group = "Documentation"  // Groupe dans ./gradlew tasks
            task.description = "Publishes DITA documentation with DITA Open Toolkit."
        }
    }

    companion object {
        const val DITA = "dita"  // Nom de la tâche
    }
}
```

**Points clés :**
- `Plugin<Project>` : Interface Gradle pour créer un plugin
- `apply()` : Méthode appelée automatiquement
- `tasks.register()` : Crée une tâche de façon "lazy"

---

### 2. DitaOtTask.kt - Tâche Principale

**Rôle :** C'est le cœur du plugin. Cette classe définit la tâche qui transforme les documents DITA.

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
 * Tâche Gradle pour exécuter des transformations DITA-OT.
 *
 * Annotations importantes :
 * - @CacheableTask : Permet la mise en cache des résultats
 * - @Inject : Injection de dépendances Gradle
 */
@CacheableTask
abstract class DitaOtTask @Inject constructor(
    private val objectFactory: ObjectFactory,      // Pour créer des collections de fichiers
    private val projectLayout: ProjectLayout       // Pour accéder aux répertoires du projet
) : DefaultTask() {

    // =========================================================================
    // PROPRIÉTÉS D'ENTRÉE (Input Properties)
    // Ces propriétés définissent CE QUE Gradle doit transformer
    // =========================================================================

    /**
     * Répertoire d'installation de DITA-OT.
     *
     * @InputDirectory : Gradle surveille ce répertoire pour détecter les changements
     * @PathSensitive : Seul le contenu compte, pas le chemin absolu
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ditaOtDir: DirectoryProperty

    /**
     * Fichiers d'entrée DITA (ditamap, topics, etc.)
     *
     * @InputFiles : Ces fichiers sont les entrées de la transformation
     * @SkipWhenEmpty : Si vide, la tâche est ignorée (NO_SOURCE)
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * Propriétés DITA personnalisées (args.css, processing-mode, etc.)
     *
     * MapProperty<String, String> est une propriété "lazy" qui contient
     * une map clé-valeur.
     */
    @get:Input
    @get:Optional
    abstract val ditaProperties: MapProperty<String, String>

    // =========================================================================
    // PROPRIÉTÉS DE SORTIE (Output Properties)
    // Ces propriétés définissent OÙ Gradle doit générer les fichiers
    // =========================================================================

    /**
     * Répertoire de sortie pour les fichiers générés.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // =========================================================================
    // MÉTHODES DSL (Domain Specific Language)
    // Ces méthodes permettent de configurer la tâche dans build.gradle
    // =========================================================================

    /**
     * Configure le répertoire DITA-OT.
     * Peut recevoir un String, File, ou autre chose convertible en File.
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
     * Configure les fichiers d'entrée.
     */
    fun input(files: Any) {
        inputFiles.from(files)
        options.input = files
    }

    /**
     * Configure les types de sortie (html5, pdf, epub, etc.)
     */
    fun transtype(vararg t: String) {
        transtypes.set(t.toList())
        options.transtype = t.toList()
    }

    /**
     * Configure les propriétés DITA via une closure Kotlin.
     *
     * Exemple d'utilisation :
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
    // EXÉCUTION DE LA TÂCHE
    // =========================================================================

    /**
     * Méthode principale exécutée par Gradle.
     *
     * @TaskAction : Indique que c'est la méthode à exécuter
     */
    @TaskAction
    fun render() {
        // 1. Validation des entrées
        val ditaHome = resolveDitaHome()
        val inputs = inputFiles.files.filter { it.exists() }

        if (inputs.isEmpty()) {
            logger.warn("No input files found")
            return
        }

        // 2. Déterminer la stratégie d'exécution
        val strategy = antExecutionStrategy.get()

        // 3. Exécuter la transformation
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
     * Exécute via le script dita/dita.bat (stratégie par défaut).
     * Cette méthode évite les problèmes de ClassLoader de Gradle.
     */
    private fun renderViaDitaScript(
        ditaHome: File,
        inputs: Set<File>,
        types: Array<String>
    ): Boolean {
        inputs.forEach { inputFile ->
            types.forEach { transtype ->
                // Construire la map des propriétés
                val properties = mutableMapOf<String, String>()

                // Capturer les propriétés de la closure Groovy
                if (groovyProperties != null) {
                    val captured = GroovyPropertyCapture.captureFromClosure(groovyProperties)
                    properties.putAll(captured)
                }

                // Ajouter les propriétés de l'API Provider
                if (ditaProperties.isPresent) {
                    properties.putAll(ditaProperties.get())
                }

                // Exécuter via AntExecutor
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

**Points clés :**
- `@CacheableTask` : Active la mise en cache
- `@Inject constructor` : Injection de dépendances Gradle
- `abstract val` : Propriétés gérées par Gradle (Provider API)
- `@TaskAction` : Méthode exécutée par `./gradlew dita`

---

### 3. Options.kt - Configuration

**Rôle :** Stocke toutes les options de configuration de la tâche.

```kotlin
package com.github.jyjeanne

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import java.io.File

/**
 * Classe de stockage pour les options de configuration.
 *
 * Cette classe utilise des propriétés mutables (var) car elle est
 * configurée progressivement pendant la phase de configuration de Gradle.
 */
class Options {

    companion object {
        // Transtype par défaut si non spécifié
        const val DEFAULT_TRANSTYPE = "html5"

        /**
         * Stratégies d'exécution ANT disponibles.
         *
         * enum class en Kotlin est équivalent à enum en Java,
         * mais plus puissant car peut avoir des propriétés et méthodes.
         */
        enum class AntExecutionStrategy {
            /** Utilise IsolatedAntBuilder de Gradle (a des problèmes connus) */
            ISOLATED_BUILDER,

            /** Exécute via le script dita/dita.bat (RECOMMANDÉ) */
            DITA_SCRIPT,

            /** Exécute via un ClassLoader personnalisé */
            CUSTOM_CLASSLOADER,

            /** Exécute via Gradle exec */
            GRADLE_EXEC,

            /** Expérimental : Binding ANT de Groovy */
            GROOVY_ANT_BINDING
        }

        /**
         * Retourne le répertoire temporaire par défaut.
         * Utilise le répertoire temp du système avec un timestamp unique.
         */
        private fun getDefaultTempDir(): File {
            val tmpdir = System.getProperty("java.io.tmpdir")
            return File("$tmpdir/dita-ot", System.currentTimeMillis().toString())
        }
    }

    // =========================================================================
    // PROPRIÉTÉS DE CONFIGURATION
    // =========================================================================

    /** Mode développeur : inclut les fichiers DITA-OT dans les entrées */
    var devMode: Boolean = false

    /** Utiliser un seul répertoire de sortie pour tous les fichiers */
    var singleOutputDir: Boolean = false

    /** Utiliser le fichier DITAVAL associé automatiquement */
    var useAssociatedFilter: Boolean = false

    /** Répertoire d'installation DITA-OT */
    var ditaOt: File? = null

    /** Classpath personnalisé (optionnel) */
    var classpath: FileCollection? = null

    /** Fichier(s) d'entrée DITA */
    var input: Any? = null

    /** Fichier filtre DITAVAL */
    var filter: Any? = null

    /** Répertoire de sortie */
    var output: File? = null

    /** Répertoire temporaire */
    var temp: File = getDefaultTempDir()

    /** Propriétés via closure Groovy (rétrocompatibilité) */
    var properties: Closure<*>? = null

    /** Propriétés via DSL Kotlin */
    var kotlinProperties: Map<String, String>? = null

    /** Types de sortie (pdf, html5, etc.) */
    var transtype: List<String> = listOf(DEFAULT_TRANSTYPE)

    /**
     * Stratégie d'exécution ANT.
     *
     * DITA_SCRIPT est la valeur par défaut car elle évite les problèmes
     * de ClassLoader de IsolatedAntBuilder.
     */
    var antExecutionStrategy: AntExecutionStrategy = AntExecutionStrategy.DITA_SCRIPT
}
```

**Points clés :**
- `companion object` : Équivalent des membres statiques en Java
- `enum class` : Énumération Kotlin avec possibilité d'avoir des propriétés
- `var` vs `val` : `var` est mutable, `val` est immutable

---

### 4. AntExecutor.kt - Exécution

**Rôle :** Gère l'exécution de DITA-OT via différentes stratégies.

```kotlin
package com.github.jyjeanne

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Exécuteur ANT pour DITA-OT.
 *
 * object en Kotlin = Singleton (une seule instance)
 * Équivalent à une classe avec des méthodes statiques en Java.
 */
object AntExecutor {

    /**
     * Exécute DITA-OT via son script natif (dita ou dita.bat).
     *
     * C'est la stratégie RECOMMANDÉE car elle évite tous les problèmes
     * de ClassLoader de Gradle.
     *
     * @param ditaHome     Répertoire d'installation DITA-OT
     * @param inputFile    Fichier DITA d'entrée
     * @param transtype    Format de sortie (html5, pdf, etc.)
     * @param outputDir    Répertoire de sortie
     * @param tempDir      Répertoire temporaire
     * @param filterFile   Fichier DITAVAL (optionnel)
     * @param properties   Propriétés ANT supplémentaires
     * @param logger       Logger Gradle
     * @return Code de sortie (0 = succès)
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
        // 1. Détecter le système d'exploitation
        val isWindows = System.getProperty("os.name")
            .lowercase()
            .contains("win")

        // 2. Trouver le script DITA-OT
        val ditaScript = if (isWindows) {
            // Windows : chercher dita.bat
            val scriptInBin = File(ditaHome, "bin/dita.bat")
            if (scriptInBin.exists()) scriptInBin else File(ditaHome, "dita.bat")
        } else {
            // Linux/Mac : chercher dita
            val scriptInBin = File(ditaHome, "bin/dita")
            if (scriptInBin.exists()) scriptInBin else File(ditaHome, "dita")
        }

        // 3. Vérifier que le script existe
        if (!ditaScript.exists()) {
            logger.error("Script DITA non trouvé : ${ditaScript.absolutePath}")
            return -1
        }

        // 4. Construire la commande
        val command = mutableListOf<String>()
        command.add(ditaScript.absolutePath)
        command.add("--input=${inputFile.absolutePath}")
        command.add("--format=$transtype")
        command.add("--output=${outputDir.absolutePath}")
        command.add("--temp=${tempDir.absolutePath}")

        // Ajouter le filtre DITAVAL si présent
        if (filterFile != null && filterFile.exists()) {
            command.add("--filter=${filterFile.absolutePath}")
        }

        // Ajouter les propriétés personnalisées
        properties.forEach { (name, value) ->
            command.add("-D$name=$value")
        }

        // 5. Exécuter le processus
        logger.info("Exécution : ${command.joinToString(" ")}")

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(ditaHome)
        processBuilder.redirectErrorStream(true)  // Fusionner stderr dans stdout
        processBuilder.inheritIO()                 // Afficher la sortie dans la console

        // Définir les variables d'environnement
        processBuilder.environment()["DITA_HOME"] = ditaHome.absolutePath

        // Démarrer et attendre la fin
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            logger.info("✓ Transformation réussie")
        } else {
            logger.error("✗ Échec (code: $exitCode)")
        }

        return exitCode
    }
}
```

**Points clés :**
- `object` : Singleton en Kotlin
- `ProcessBuilder` : API Java pour exécuter des processus externes
- `mutableListOf` : Liste mutable (peut être modifiée)
- `?.` : Safe call operator (évite NullPointerException)

---

### 5. Classpath.kt - Gestion du Classpath

**Rôle :** Construit le classpath nécessaire pour exécuter DITA-OT.

```kotlin
package com.github.jyjeanne

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utilitaire pour construire le classpath DITA-OT.
 *
 * DITA-OT nécessite un classpath spécifique incluant :
 * - Les JARs dans lib/
 * - Les JARs des plugins
 * - Les répertoires config/ et resources/
 */
object Classpath {

    /**
     * Obtient les fichiers JAR des plugins DITA-OT.
     *
     * Parse le fichier plugins.xml pour trouver tous les JARs déclarés.
     *
     * @JvmStatic permet d'appeler cette méthode depuis Java comme
     * une méthode statique : Classpath.getPluginClasspathFiles(...)
     */
    @JvmStatic
    fun getPluginClasspathFiles(ditaHome: File?): List<File> {
        // Vérification null avec throw si null
        if (ditaHome == null) {
            throw GradleException("Répertoire DITA-OT non configuré")
        }

        // Chercher le fichier plugins.xml
        // listOf crée une liste immutable
        val plugins = listOf(
            File(ditaHome, "config/plugins.xml"),
            File(ditaHome, "resources/plugins.xml")
        ).find { it.exists() }  // find retourne le premier élément qui match
            ?: throw GradleException("Fichier plugins.xml non trouvé")

        // Parser le XML
        val archives = mutableListOf<File>()

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(plugins)

        // Parcourir les éléments <plugin>
        val pluginNodes = doc.documentElement.getElementsByTagName("plugin")
        for (i in 0 until pluginNodes.length) {
            val pluginNode = pluginNodes.item(i)

            // Obtenir l'attribut xml:base
            val xmlBase = pluginNode.attributes
                .getNamedItemNS("http://www.w3.org/XML/1998/namespace", "base")
                ?.nodeValue ?: continue  // continue si null

            val pluginDir = File(plugins.parent, xmlBase)

            // Trouver les JARs déclarés dans <feature>
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
     * Construit le classpath complet pour la compilation.
     *
     * Compatible avec le Configuration Cache grâce à l'utilisation
     * de ObjectFactory au lieu de Project.
     */
    @JvmStatic
    fun compileWithObjectFactory(
        objectFactory: ObjectFactory,
        ditaHome: File
    ): FileCollection {
        val files = getCompileClasspathFiles(ditaHome)
        // objectFactory.fileCollection() crée une collection compatible Provider API
        return objectFactory.fileCollection().from(files)
    }

    /**
     * Obtient tous les fichiers du classpath.
     */
    @JvmStatic
    fun getCompileClasspathFiles(ditaHome: File): List<File> {
        val libDir = File(ditaHome, "lib")

        // Lister les JARs (sauf ant.jar qui est fourni par Gradle)
        val libJars = libDir.listFiles { file ->
            file.isFile &&
            file.name.endsWith(".jar") &&
            file.name != "ant.jar" &&
            file.name != "ant-launcher.jar"
        }?.toList() ?: emptyList()

        // Construire le classpath final
        return mutableListOf<File>().apply {
            addAll(libJars)
            addAll(getPluginClasspathFiles(ditaHome))
            add(File(ditaHome, "config"))
            add(File(ditaHome, "resources"))
        }
    }
}
```

**Points clés :**
- `@JvmStatic` : Génère une méthode statique pour l'interop Java
- `?.` et `?:` : Opérateurs null-safe de Kotlin
- `apply {}` : Configure un objet et le retourne
- `until` : Range exclusive (0 until 5 = 0, 1, 2, 3, 4)

---

### 6. PropertyBuilder.kt - DSL pour Propriétés

**Rôle :** Permet de configurer les propriétés DITA avec une syntaxe élégante.

```kotlin
package com.github.jyjeanne

/**
 * Builder pour créer des propriétés DITA avec une syntaxe DSL.
 *
 * Exemple d'utilisation :
 *   properties {
 *       "processing-mode" to "strict"      // Syntaxe infix
 *       property("args.css", "custom.css") // Syntaxe fonction
 *   }
 */
class PropertyBuilder {
    // Map mutable pour stocker les propriétés
    private val properties = mutableMapOf<String, String>()

    /**
     * Ajouter une propriété avec notation infix.
     *
     * "infix" permet d'appeler la fonction sans parenthèses ni point :
     *   "key" to "value"   au lieu de   "key".to("value")
     *
     * Cette fonction est définie comme extension de String.
     */
    infix fun String.to(value: String) {
        properties[this] = value  // this = la String sur laquelle on appelle
    }

    /**
     * Ajouter une propriété avec appel de fonction classique.
     */
    fun property(name: String, value: String) {
        properties[name] = value
    }

    /**
     * Ajouter une propriété avec un chemin de fichier.
     */
    fun propertyLocation(name: String, location: java.io.File) {
        properties[name] = location.absolutePath
    }

    /**
     * Retourne une copie immutable des propriétés.
     *
     * toMap() crée une copie pour éviter les modifications externes.
     */
    fun build(): Map<String, String> = properties.toMap()

    fun isEmpty(): Boolean = properties.isEmpty()
    fun isNotEmpty(): Boolean = properties.isNotEmpty()
}
```

**Points clés :**
- `infix` : Permet la syntaxe `a to b` au lieu de `a.to(b)`
- Extension function : `String.to()` ajoute une méthode à String
- `this` dans une extension : Réfère à l'objet sur lequel on appelle

---

### 7. GroovyPropertyCapture.kt - Capture des Closures

**Rôle :** Extrait les propriétés d'une closure Groovy pour les utiliser avec DITA_SCRIPT.

```kotlin
package com.github.jyjeanne

import groovy.lang.Closure

/**
 * Capture les propriétés définies dans une closure Groovy.
 *
 * Problème résolu :
 * Les closures Groovy comme :
 *   properties {
 *       property name: 'args.css', value: 'custom.css'
 *   }
 *
 * Ne fonctionnaient pas avec la stratégie DITA_SCRIPT car elles
 * sont conçues pour être exécutées avec un delegate ANT.
 *
 * Cette classe agit comme un "faux" delegate qui capture les
 * appels à property() et les stocke dans une Map.
 */
class GroovyPropertyCapture {
    // Stockage des propriétés capturées
    private val capturedProperties = mutableMapOf<String, String>()

    /**
     * Intercepte les appels à property(name: '...', value: '...')
     *
     * En Groovy, property(name: 'x', value: 'y') est équivalent à
     * property(mapOf("name" to "x", "value" to "y"))
     */
    fun property(args: Map<String, Any?>) {
        val name = args["name"]?.toString()
        val value = args["value"]?.toString()

        // Seulement si les deux sont présents
        if (name != null && value != null) {
            capturedProperties[name] = value
        }
    }

    /**
     * Retourne les propriétés capturées.
     */
    fun getCapturedProperties(): Map<String, String> =
        capturedProperties.toMap()

    companion object {
        /**
         * Exécute une closure et capture toutes les propriétés.
         *
         * @JvmStatic pour pouvoir appeler depuis Java :
         *   GroovyPropertyCapture.captureFromClosure(closure)
         */
        @JvmStatic
        fun captureFromClosure(closure: Closure<*>?): Map<String, String> {
            if (closure == null) return emptyMap()

            // Créer une instance pour capturer
            val capture = GroovyPropertyCapture()

            // Configurer la closure pour utiliser notre capture comme delegate
            closure.delegate = capture
            closure.resolveStrategy = Closure.DELEGATE_FIRST

            // Exécuter la closure (les appels à property() iront vers capture)
            try {
                closure.call()
            } catch (e: Exception) {
                // Ignorer les erreurs (certaines closures peuvent avoir des effets secondaires)
            }

            return capture.getCapturedProperties()
        }
    }
}
```

**Points clés :**
- `Closure<*>` : Type Groovy pour les closures
- `delegate` : Objet sur lequel les appels non résolus sont délégués
- `DELEGATE_FIRST` : Cherche d'abord dans le delegate, puis dans le owner

---

### 8. Classes Utilitaires

#### FileExtensions.kt
```kotlin
package com.github.jyjeanne

/**
 * Constantes pour les extensions de fichiers.
 *
 * object = Singleton, les constantes sont accessibles via :
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
 * Patterns glob pour la recherche de fichiers.
 */
object GlobPatterns {
    const val ALL_FILES = "*/**"  // Tous les fichiers récursivement
}
```

#### Messages.kt
```kotlin
package com.github.jyjeanne

/**
 * Messages d'erreur et d'aide.
 *
 * Utilise les raw strings de Kotlin (triple quotes) pour
 * les messages multi-lignes avec formatage préservé.
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
    """.trimIndent()  // Supprime l'indentation commune
}
```

---

## Flux d'Exécution

### Diagramme de Séquence

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

### Phases Gradle

1. **Initialisation** : Gradle charge les scripts
2. **Configuration** : Le plugin est appliqué, les tâches sont créées
3. **Exécution** : `./gradlew dita` exécute `DitaOtTask.render()`

---

## Tests

### Structure des Tests

```kotlin
// DitaOtTaskSpec.kt utilise Kotest (framework de test Kotlin)

class DitaOtTaskSpec : StringSpec({

    // StringSpec permet des tests avec une syntaxe lisible
    "Creating a task" {
        val task = project.tasks.create("dita", DitaOtTask::class.java)
        task.input("root.ditamap")

        // shouldBe est une assertion Kotest
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

### Exécuter les Tests

```bash
# Tous les tests
./gradlew test

# Tests spécifiques
./gradlew test --tests "DitaOtTaskSpec"

# Avec rapport détaillé
./gradlew test --info
```

---

## Glossaire

| Terme | Définition |
|-------|------------|
| **DSL** | Domain Specific Language - Langage spécialisé pour un domaine |
| **Provider API** | API Gradle pour les valeurs "lazy" (évaluées à l'exécution) |
| **Configuration Cache** | Cache Gradle qui stocke le graphe des tâches |
| **Closure** | Fonction anonyme en Groovy qui capture son contexte |
| **Singleton** | Pattern où une seule instance d'une classe existe |
| **Extension Function** | Fonction ajoutée à une classe existante (Kotlin) |
| **Infix** | Notation permettant d'appeler une fonction sans parenthèses |
| **DITA-OT** | DITA Open Toolkit - Outil de transformation DITA |
| **Transtype** | Format de sortie (html5, pdf, epub, etc.) |
| **DITAVAL** | Fichier de filtrage conditionnel DITA |

---

## Pour Aller Plus Loin

### Ressources Kotlin
- [Documentation Kotlin](https://kotlinlang.org/docs/home.html)
- [Kotlin Koans](https://play.kotlinlang.org/koans/overview)

### Ressources Gradle
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Provider API](https://docs.gradle.org/current/userguide/lazy_configuration.html)
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)

### Ressources DITA
- [DITA-OT Documentation](https://www.dita-ot.org/dev/)
- [OASIS DITA Standard](https://www.oasis-open.org/committees/dita/)

---

*Document créé le 16 décembre 2025 - Mis à jour en février 2026*
*Version du plugin : 2.8.5*
