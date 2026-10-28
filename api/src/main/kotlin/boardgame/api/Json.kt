package boardgame.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

val apiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

inline fun <reified T> encodeToJson(value: T): String = apiJson.encodeToString(value)

inline fun <reified T> encodeTree(value: T): JsonElement = apiJson.encodeToJsonElement(value)

inline fun <reified T> decodeFromJson(text: String): T = apiJson.decodeFromString(text)

inline fun <reified T> decodeTree(element: JsonElement): T = apiJson.decodeFromJsonElement(element)

/** Path grammar shared with the front: `a.b[2].c` and `a.2.c` are equivalent. */
fun splitPath(path: String): List<String> = path.split(Regex("[.\\[\\]]")).filter { it.isNotEmpty() }

fun JsonElement.atPath(path: String): JsonElement? {
    var current: JsonElement = this
    for (key in splitPath(path)) {
        current = when (current) {
            is JsonObject -> current[key] ?: return null
            is JsonArray -> current.getOrNull(key.toIntOrNull() ?: return null) ?: return null
            else -> return null
        }
    }
    return current
}

/** Immutable patch application, mirroring the front's setValueByPath. */
fun JsonElement.withPart(path: String?, value: JsonElement): JsonElement =
    if (path == null) value else setAt(this, splitPath(path), value)

fun JsonElement.withParts(parts: List<SyncPart>): JsonElement =
    parts.fold(this) { acc, part -> acc.withPart(part.path, part.value) }

private fun setAt(node: JsonElement, keys: List<String>, value: JsonElement): JsonElement {
    if (keys.isEmpty()) return value
    val head = keys.first()
    val rest = keys.drop(1)
    return when (node) {
        is JsonObject -> JsonObject(node + (head to setAt(node[head] ?: JsonObject(emptyMap()), rest, value)))
        is JsonArray -> {
            val index = head.toIntOrNull() ?: return node
            val items = node.toMutableList()
            while (items.size <= index) items.add(JsonNull)
            items[index] = setAt(items[index], rest, value)
            JsonArray(items)
        }
        else -> JsonObject(mapOf(head to setAt(JsonObject(emptyMap()), rest, value)))
    }
}

fun JsonElement.jsonEquals(other: JsonElement): Boolean = when {
    this is JsonNull || other is JsonNull -> this is JsonNull && other is JsonNull
    this is JsonPrimitive && other is JsonPrimitive -> {
        if (isString != other.isString) false
        else if (isString) this.content == other.content
        else {
            val left = content.toDoubleOrNull()
            val right = other.content.toDoubleOrNull()
            if (left != null && right != null) left == right else this.content == other.content
        }
    }
    this is JsonObject && other is JsonObject ->
        keys == other.keys && keys.all { this[it]!!.jsonEquals(other[it]!!) }
    this is JsonArray && other is JsonArray ->
        size == other.size && indices.all { this[it].jsonEquals(other[it]) }
    else -> false
}

/**
 * Property-level diff in the shape the front consumes. Arrays are replaced as a whole when their
 * length changes because the front's setValueByPath turns a missing index into an object.
 */
fun diffJson(old: JsonElement, new: JsonElement, path: String = ""): List<SyncPart> {
    if (old.jsonEquals(new)) return emptyList()

    if (old is JsonObject && new is JsonObject) {
        // A removed key cannot be represented as a null patch (the front would keep the key), so
        // replace the whole object in that case.
        if (old.keys.any { it !in new }) return listOf(SyncPart(path.ifEmpty { null }, new))

        val parts = mutableListOf<SyncPart>()
        for (key in (old.keys + new.keys).sorted()) {
            val oldValue = old[key]
            val newValue = new[key]
            val childPath = if (path.isEmpty()) key else "$path.$key"
            when {
                newValue == null -> parts += SyncPart(childPath, JsonNull)
                oldValue == null -> parts += SyncPart(childPath, newValue)
                else -> parts += diffJson(oldValue, newValue, childPath)
            }
        }
        return cap(parts, new, path)
    }

    if (old is JsonArray && new is JsonArray) {
        if (old.size != new.size) return listOf(SyncPart(path.ifEmpty { null }, new))
        val parts = mutableListOf<SyncPart>()
        for (index in new.indices) {
            val childPath = if (path.isEmpty()) "$index" else "$path.$index"
            parts += diffJson(old[index], new[index], childPath)
        }
        return cap(parts, new, path)
    }

    return listOf(SyncPart(path.ifEmpty { null }, new))
}

private fun cap(parts: List<SyncPart>, new: JsonElement, path: String): List<SyncPart> =
    if (parts.size > 100) listOf(SyncPart(path.ifEmpty { null }, new)) else parts

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

@Suppress("unused")
fun JsonElement.asPrimitive(): JsonPrimitive = jsonPrimitive
