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
    val registry = types.associate { it.serializer.descriptor.serialName.removeSuffix("?") to it.name }
    return buildString {
        appendLine("// Generated from Kotlin models by the module's generateTs task. Do not edit.")
        appendLine("// Only model types shared with the back-end are generated; front-only helpers stay hand-written.")
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
                if (propertyName == "type" && spec.typeLiteral != null) {
                    appendLine("    $propertyName: '${spec.typeLiteral}'")
                } else {
                    // The wire omits null fields (explicitNulls = false), so nullable maps to an
                    // optional property rather than `T | null`.
                    val optional = if (element.isNullable) "?" else ""
                    appendLine("    $propertyName$optional: ${tsType(element, registry, includeNull = false)}")
                }
            }
            appendLine("}")
            appendLine()
        }
    }
}

private fun tsType(
    descriptor: SerialDescriptor,
    registry: Map<String, String>,
    includeNull: Boolean = true
): String {
    val serialName = descriptor.serialName.removeSuffix("?")
    val base = when {
        serialName == "kotlinx.serialization.json.JsonElement" -> "any"
        serialName == "kotlinx.serialization.json.JsonObject" -> "Record<string, any>"
        descriptor.kind == StructureKind.LIST -> "${tsType(descriptor.getElementDescriptor(0), registry)}[]"
        descriptor.kind == StructureKind.MAP ->
            "Record<string, ${tsType(descriptor.getElementDescriptor(1), registry)}>"
        descriptor.kind == SerialKind.ENUM ->
            descriptor.elementNames.joinToString(" | ") { "'$it'" }
        descriptor.kind == StructureKind.CLASS ->
            registry[serialName] ?: serialName.substringAfterLast('.')
        descriptor.kind == PrimitiveKind.STRING || descriptor.kind == PrimitiveKind.CHAR -> "string"
        descriptor.kind == PrimitiveKind.BOOLEAN -> "boolean"
        descriptor.kind == PrimitiveKind.INT || descriptor.kind == PrimitiveKind.LONG ||
            descriptor.kind == PrimitiveKind.SHORT || descriptor.kind == PrimitiveKind.BYTE ||
            descriptor.kind == PrimitiveKind.DOUBLE || descriptor.kind == PrimitiveKind.FLOAT -> "number"
        else -> "any"
    }
    return if (includeNull && descriptor.isNullable) "$base | null" else base
}
