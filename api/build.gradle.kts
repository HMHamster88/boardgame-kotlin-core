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
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation(kotlin("test"))
}

/** Generates common/src/back/generated/protocol.ts from the @GenerateTs Kotlin models. */
val generateTs = tasks.register<JavaExec>("generateTs") {
    group = "codegen"
    description = "Generates the front's protocol types from the annotated Kotlin DTOs"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("boardgame.api.TsExport")
    val output = layout.projectDirectory.file("../common/src/back/generated/protocol.ts").asFile
    val classDirs = sourceSets.main.get().output.classesDirs.files
    args(listOf(output.absolutePath) + classDirs.map { it.absolutePath })
}
