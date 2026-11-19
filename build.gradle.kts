plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

allprojects {
    group = "com.hmhamster"
    version = "0.1.0"
}

/**
 * Development build: regenerates the front protocol types, builds the Vue front and builds the core
 * plugin jar, which the launcher's DEV watcher picks up automatically.
 */
val devFront = tasks.register<Exec>("devFront") {
    group = "development"
    description = "Builds the common TypeScript package and the Vue front into back/public (needs npm)"
    dependsOn(":api:generateTs")
    workingDir = projectDir
    val npm = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        listOf("cmd", "/c", "npm")
    } else {
        listOf("npm")
    }
    commandLine(npm + listOf("run", "build-all"))
}

tasks.register("devBuild") {
    group = "development"
    description = "Regenerates the protocol types, builds the front and the core plugin jar"
    dependsOn(":api:generateTs", devFront, ":back:jar")
}
