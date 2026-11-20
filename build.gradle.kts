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

/**
 * Publishes the release zip with the GitHub CLI. Requires an authenticated `gh` and a local tag
 * `v<version>`; the task pushes the tag and creates the release with generated notes.
 *
 * `-PghDraft=true` creates a draft (not picked up by the launcher's latest-release check).
 */
tasks.register("publishRelease") {
    group = "release"
    description = "Publishes the core release zip as a GitHub release (GitHub CLI)"
    dependsOn("releaseBuild")

    val version = project.version.toString()
    val tag = "v$version"
    val asset = layout.projectDirectory.file("back/build/dist/boardgame-core-$version.zip")
    val draft = project.hasProperty("ghDraft")

    doLast {
        fun run(command: List<String>, failure: String): String {
            val process = try {
                ProcessBuilder(command)
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start()
            } catch (error: java.io.IOException) {
                error("$failure\n${error.message}")
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() != 0) error("$failure\n$output")
            if (output.isNotEmpty()) logger.lifecycle(output)
            return output
        }

        run(
            listOf("gh", "--version"),
            "GitHub CLI (gh) is not installed or not on PATH. Install it and run `gh auth login`."
        )
        run(
            listOf("gh", "auth", "status"),
            "GitHub CLI is not authenticated. Run `gh auth login` or set GH_TOKEN."
        )
        require(asset.asFile.isFile) {
            "Release asset missing: ${asset.asFile}. Run `gradle releaseBuild` first."
        }
        val dirty = run(
            listOf("git", "status", "--porcelain", "--untracked-files=no"),
            "Failed to read the git working tree status."
        )
        require(dirty.isEmpty()) {
            "Working tree has uncommitted changes; commit them before publishing:\n$dirty"
        }
        run(
            listOf("git", "rev-parse", "-q", "--verify", "refs/tags/$tag"),
            "Missing local tag $tag. Tag the release commit first: git tag $tag"
        )
        val head = run(listOf("git", "rev-parse", "HEAD"), "Failed to read HEAD.")
        val tagged = run(listOf("git", "rev-list", "-n", "1", "refs/tags/$tag"), "Failed to read tag $tag.")
        require(head == tagged) {
            "HEAD ($head) is not the commit tagged $tag ($tagged); check out the release tag before publishing."
        }
        run(
            listOf("git", "push", "origin", "refs/tags/$tag"),
            "Failed to push tag $tag to origin."
        )

        val create = mutableListOf(
            "gh", "release", "create", tag, asset.asFile.absolutePath,
            "--title", "boardgame-core $version",
            "--generate-notes",
            "--verify-tag"
        )
        if (draft) create += "--draft" else create += "--latest"
        run(create, "gh release create failed for $tag.")
        logger.lifecycle("Published GitHub release $tag (${if (draft) "draft" else "latest"}).")
    }
}
