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
