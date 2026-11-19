plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

/** Latest reachable git tag without the leading `v`; null when the repository has no tags. */
fun latestGitTag(): String? = runCatching {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val tag = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) tag.removePrefix("v").takeIf { it.isNotBlank() } else null
}.getOrNull()

/**
 * -PcoreVersion=… > RELEASE_VERSION > latest git tag > default. Tag the release (and fetch tags)
 * before building so the artifact name matches the GitHub release.
 */
val projectVersion = providers.gradleProperty("coreVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .getOrElse(latestGitTag() ?: "0.1.0")

allprojects {
    group = "com.hmhamster"
    version = projectVersion
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

/** Release build: front + dual-format plugin jar (with classes.dex), zipped for a GitHub release. */
tasks.register("releaseBuild") {
    group = "release"
    description = "Builds back/build/dist/boardgame-core-<version>.zip (front, jar and classes.dex)"
    dependsOn(":back:releaseZip")
}
