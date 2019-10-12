package razorvine.ksim65

import java.util.*

object Version {
    val version: String by lazy {
        val props = Properties()
        props.load(javaClass.getResourceAsStream("/version.properties"))
        props["version"] as String
    }

    val copyright: String by lazy {
        "KSim65 6502 cpu simulator v$version by Irmen de Jong (irmen@razorvine.net)"+"\nThis software is free and licensed under the MIT open-source license\n"
    }
}
