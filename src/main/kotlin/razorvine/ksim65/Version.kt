package razorvine.ksim65

import java.util.*

object Version {
    val version: String by lazy {
        val props = Properties()
        props.load(javaClass.getResourceAsStream("/version.properties"))
        props["version"] as String
    }

    val copyright: String by lazy {
        "KSim65 v$version, a 6502/65C02 cpu simulator by Irmen de Jong (irmen@razorvine.net)"
    }
}
