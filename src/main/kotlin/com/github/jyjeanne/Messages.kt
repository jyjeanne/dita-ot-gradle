package com.github.jyjeanne

object Messages {
    val ditaHomeError = """
        DITA-OT directory not configured.

        Please configure it in your build.gradle:

        dita {
            ditaOt '/path/to/dita-ot'
            input 'my.ditamap'
            transtype 'html5'
        }

        Or in build.gradle.kts:

        tasks.named<DitaOtTask>("dita") {
            ditaOt(file("/path/to/dita-ot"))
            input("my.ditamap")
            transtype("html5")
        }

        To download DITA-OT automatically, see:
        https://github.com/jyjeanne/dita-ot-gradle/tree/main/examples/download
    """.trimIndent()
}
