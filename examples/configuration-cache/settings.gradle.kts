rootProject.name = "configuration-cache-example"

// Include the plugin from parent project for development
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.jyjeanne:dita-ot-gradle")).using(project(":"))
    }
}
