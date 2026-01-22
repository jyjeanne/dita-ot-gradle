/**
 * Configuration Cache Example
 *
 * This example demonstrates the DITA-OT Gradle plugin's compatibility with
 * Gradle's Configuration Cache feature, which significantly improves build
 * performance by caching the task graph between builds.
 *
 * Configuration Cache Benefits:
 * - First run: ~20-30 seconds (calculates and stores task graph)
 * - Subsequent runs: ~2-5 seconds (reuses cached configuration)
 * - Up to 80-90% faster configuration phase
 *
 * Requirements:
 * - Gradle 8.1+ (Configuration Cache stable)
 * - DITA-OT 3.0+ (for DITA_SCRIPT strategy)
 * - Plugin version 2.8.1+
 *
 * Usage:
 *   # First run (stores configuration cache)
 *   ./gradlew dita --configuration-cache -PditaHome=/path/to/dita-ot
 *
 *   # Subsequent runs (reuses cache - much faster!)
 *   ./gradlew dita --configuration-cache -PditaHome=/path/to/dita-ot
 *
 *   # Clean and rebuild
 *   ./gradlew clean dita --configuration-cache -PditaHome=/path/to/dita-ot
 */

plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.1"
}

defaultTasks("dita")

tasks.named<com.github.jyjeanne.DitaOtTask>("dita") {
    // DITA-OT installation directory
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required. Use -PditaHome=/path/to/dita-ot"))

    // Input DITA map
    input("dita/guide.ditamap")

    // Output formats - generate both HTML5 and PDF
    transtype("html5", "pdf")

    // DITA-OT properties using Kotlin DSL (configuration cache compatible)
    properties {
        "processing-mode" to "strict"
        "args.rellinks" to "all"
        "nav-toc" to "partial"
    }

    // Optional: Enable dev mode to include DITA-OT changes in up-to-date checks
    // devMode(true)
}

// Performance comparison task
tasks.register("benchmark") {
    group = "Documentation"
    description = "Run a simple benchmark comparing configuration cache performance"

    doLast {
        println("""
            |
            |Configuration Cache Benchmark Instructions:
            |============================================
            |
            |1. First, clean any existing cache:
            |   ./gradlew --stop
            |   rm -rf .gradle/configuration-cache
            |
            |2. Run WITHOUT configuration cache (baseline):
            |   time ./gradlew clean dita --no-configuration-cache -PditaHome=/path/to/dita-ot
            |
            |3. Run WITH configuration cache (first run - stores cache):
            |   time ./gradlew clean dita --configuration-cache -PditaHome=/path/to/dita-ot
            |
            |4. Run WITH configuration cache (second run - reuses cache):
            |   time ./gradlew dita --configuration-cache -PditaHome=/path/to/dita-ot
            |
            |Expected Results:
            |- Without cache: ~25-35 seconds total
            |- With cache (first run): ~25-35 seconds (similar, stores cache)
            |- With cache (reuse): ~15-20 seconds (configuration phase ~0.5s vs ~5s)
            |
            |The real benefit is in the configuration phase:
            |- Without cache: Configuration phase takes 3-8 seconds
            |- With cache: Configuration phase takes <1 second
            |
        """.trimMargin())
    }
}
