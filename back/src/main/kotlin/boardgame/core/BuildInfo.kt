package boardgame.core

import java.util.Properties

object BuildInfo {
    val version: String by lazy {
        val properties = Properties()
        BuildInfo::class.java.getResourceAsStream("/version.properties")?.use { properties.load(it) }
        properties.getProperty("version", "0.0.0")
    }
}
