plugins {
    id("io.github.jyjeanne.dita-ot-gradle") version "2.8.1"
}

import com.github.jyjeanne.DitaOtTask

tasks.register<DitaOtTask>("web") {
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))
    input("dita/root.ditamap")
    transtype("html5")
    filter("dita/a.ditaval")
}

tasks.register<DitaOtTask>("pdf") {
    ditaOt(findProperty("ditaHome") ?: error("ditaHome property required"))
    input("dita/root.ditamap")
    transtype("pdf")
    filter("dita/b.ditaval")

    properties(groovy.lang.Closure<Any>(this) {
        val delegate = delegate as? groovy.lang.GroovyObject
        delegate?.invokeMethod("property", mapOf("name" to "args.rellinks", "value" to "all"))
    })
}
