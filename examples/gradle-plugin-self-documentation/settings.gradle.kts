pluginManagement {
    // Include parent project to use local plugin version during development
    // Remove this block when using the published plugin from Gradle Plugin Portal
    includeBuild("../..") {
        name = "dita-ot-gradle-plugin"
    }
}

rootProject.name = "dita-gradle-demo"
