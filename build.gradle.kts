plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

allprojects {
    group = "com.hmhamster"
    version = "0.1.0"
}

/**
 * Development build: regenerates the front protocol types and builds the core plugin jar, which the
 * launcher's DEV watcher picks up automatically.
 */
tasks.register("devBuild") {
    group = "development"
    description = "Regenerates the front protocol types and builds the core plugin jar"
    dependsOn(":api:generateTs", ":back:jar")
}
