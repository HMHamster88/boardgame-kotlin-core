package boardgame.api.codegen

import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Discovers `@GenerateTs` classes in compiled class directories and writes the TS file.
 * The serializer is read from the compiler-generated `Companion.serializer()` (no kotlin-reflect).
 */
object TsScanner {

    fun generate(output: File, classDirs: List<File>): Int {
        require(classDirs.isNotEmpty()) { "No class directories passed to the TypeScript generator" }

        val specs = classDirs.asSequence()
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "class" } }
            .mapNotNull { file -> loadSpec(file, classDirs) }
            .sortedBy { it.name }
            .toList()

        require(specs.isNotEmpty()) { "No @GenerateTs classes found in ${classDirs.joinToString()}" }
        output.parentFile?.mkdirs()
        output.writeText(generateTs(specs))
        return specs.size
    }

    private fun loadSpec(file: File, classDirs: List<File>): TsTypeSpec? {
        val relative = classDirs.firstNotNullOfOrNull { dir -> file.relativeToOrNull(dir) } ?: return null
        val className = relative.path
            .removeSuffix(".class")
            .replace(File.separatorChar, '.')
            .replace('/', '.')
        if (className.contains('$')) return null

        val type = runCatching { Class.forName(className) }.getOrNull() ?: return null
        val annotation = type.getAnnotation(GenerateTs::class.java) ?: return null
        val name = annotation.name.ifEmpty { type.simpleName }
        return TsTypeSpec(
            serializerOf(type),
            name,
            annotation.typeLiteral.ifEmpty { null },
            annotation.asUnion
        )
    }

    private fun serializerOf(type: Class<*>): KSerializer<*> {
        val companionField = type.getDeclaredField("Companion")
        companionField.isAccessible = true
        val companion = companionField.get(null)
        val serializer = companion.javaClass.getMethod("serializer").invoke(companion)
        return serializer as KSerializer<*>
    }
}
