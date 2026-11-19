import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":api"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testImplementation(kotlin("test"))
}

/** version.properties is generated so the plugin reports the build version. */
val versionProperties = layout.buildDirectory.file("generated/version/version.properties")
val generateVersionProperties = tasks.register("generateVersionProperties") {
    val version = project.version.toString()
    inputs.property("version", version)
    outputs.file(versionProperties)
    doLast {
        versionProperties.get().asFile.apply {
            parentFile.mkdirs()
            writeText("version=$version\n")
        }
    }
}

tasks.processResources {
    dependsOn(generateVersionProperties)
    from(versionProperties)
}

// ---------------------------------------------------------------- release packaging

val isWindows = System.getProperty("os.name").lowercase().contains("win")

fun androidSdkDir(): File {
    val local = rootProject.file("local.properties")
    val configured = if (local.isFile) {
        Properties().apply { local.inputStream().use { load(it) } }.getProperty("sdk.dir")
    } else {
        System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    }
    return File(configured ?: error("Android SDK not found: add sdk.dir to local.properties or set ANDROID_HOME"))
}

val moduleJar = tasks.named<Jar>("jar")

/** classes.dex built from the plugin jar; loaded by DexClassLoader on Android. */
val dex = tasks.register("dex") {
    group = "release"
    description = "Converts the plugin jar to classes.dex with d8"
    dependsOn(moduleJar)
    val jarFile = moduleJar.flatMap { it.archiveFile }
    val dexDir = layout.buildDirectory.dir("dex")
    inputs.file(jarFile)
    outputs.dir(dexDir)

    doLast {
        val sdk = androidSdkDir()
        val d8 = File(sdk, "build-tools/35.0.0/" + if (isWindows) "d8.bat" else "d8")
        val output = dexDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        val command = mutableListOf<String>()
        if (isWindows) command += listOf("cmd", "/c", d8.absolutePath) else command += d8.absolutePath
        command += listOf(
            "--release", "--min-api", "26",
            "--lib", File(sdk, "platforms/android-35/android.jar").absolutePath,
            "--output", output.absolutePath,
            jarFile.get().asFile.absolutePath
        )
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val log = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "d8 failed:\n$log" }
        check(File(output, "classes.dex").isFile) { "d8 produced no classes.dex:\n$log" }
    }
}

/** Dual-format plugin: class files + resources + classes.dex in one jar. */
val dualJar = tasks.register<Jar>("dualJar") {
    group = "release"
    description = "Plugin jar usable by URLClassLoader and DexClassLoader"
    dependsOn(dex)
    archiveFileName.set("core.jar")
    destinationDirectory.set(layout.buildDirectory.dir("release/back"))
    from(sourceSets.main.get().output)
    from(layout.buildDirectory.dir("dex"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/** Core release asset: back/core.jar plus the built front assets, as installed by the launcher. */
val releaseZip = tasks.register<Zip>("releaseZip") {
    group = "release"
    description = "Packages the core module for a GitHub release"
    dependsOn(dualJar)
    // The asset contains front/**, so build it first (root :devFront = generateTs + npm build-all).
    dependsOn(":devFront")
    archiveFileName.set("boardgame-core-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    from(layout.buildDirectory.dir("release/back")) { into("back") }
    from(file("public")) { into("front") }
    doFirst {
        require(file("public").isDirectory) {
            "Front build missing at ${file("public")}. Run `npm run build-front` in the core repository first."
        }
    }
    doLast {
        logger.lifecycle("Release asset: ${archiveFile.get().asFile.absolutePath}")
    }
}