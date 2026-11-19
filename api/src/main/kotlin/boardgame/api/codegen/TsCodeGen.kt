package boardgame.api.codegen

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames

/**
 * Emits TypeScript interfaces from kotlinx.serialization descriptors.
 *
 * Only the wire/model subset is generated; front-only types (view props, text enums) stay handwritten.
 */
data class TsTypeSpec(
    val serializer: KSerializer<*>,
    val name: String,
    /** Literal for a `type` discriminator field, e.g. "CahSendAnswersAction". */
    val typeLiteral: String? = null
)

fun generateTs(types: List<TsTypeSpec>): String {
    val registry = types.associate { it.serializer.descriptor.serialName to it.name }
    return buildString {
        appendLine("// Generated from Kotlin models by `gradle :back:generateTs`. Do not edit.")
        appendLine("// The hand-written types in ../types.ts are validated against this file.")
        appendLine()
        types.forEach { spec ->
            val descriptor = spec.serializer.descriptor
            require(descriptor.kind == StructureKind.CLASS) {
                "${descriptor.serialName} is not a class (kind=${descriptor.kind})"
            }
            appendLine("export interface ${spec.name} {")
            for (index in 0 until descriptor.elementsCount) {
                val propertyName = descriptor.getElementName(index)
                val element = descriptor.getElementDescriptor(index)
                val type = if (propertyName == "type" && spec.typeLiteral != null) {
                    "'${spec.typeLiteral}'"
                } else {
                    tsType(element, registry)
                }
                appendLine("    $propertyName: $type")
            }
            appendLine("}")
            appendLine()
        }
    }
}

private fun tsType(descriptor: SerialDescriptor, registry: Map<String, String>): String {
    val base = when {
        descriptor.serialName == "kotlinx.serialization.json.JsonElement" -> "any"
        descriptor.serialName == "kotlinx.serialization.json.JsonObject" -> "Record<string, any>"
        descriptor.kind == StructureKind.LIST -> "${tsType(descriptor.getElementDescriptor(0), registry)}[]"
        descriptor.kind == StructureKind.MAP ->
            "Record<string, ${tsType(descriptor.getElementDescriptor(1), registry)}>"
        descriptor.kind == SerialKind.ENUM ->
            descriptor.elementNames.joinToString(" | ") { "'$it'" }
        descriptor.kind == StructureKind.CLASS ->
            registry[descriptor.serialName] ?: descriptor.serialName.substringAfterLast('.')
        descriptor.kind == PrimitiveKind.STRING || descriptor.kind == PrimitiveKind.CHAR -> "string"
        descriptor.kind == PrimitiveKind.BOOLEAN -> "boolean"
        descriptor.kind == PrimitiveKind.INT || descriptor.kind == PrimitiveKind.LONG ||
            descriptor.kind == PrimitiveKind.SHORT || descriptor.kind == PrimitiveKind.BYTE ||
            descriptor.kind == PrimitiveKind.DOUBLE || descriptor.kind == PrimitiveKind.FLOAT -> "number"
        else -> "any"
    }
    return if (descriptor.isNullable) "$base | null" else base
}
