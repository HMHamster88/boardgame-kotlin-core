package boardgame.api

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonDiffTest {

    private fun sample() = buildJsonObject {
        put("phase", 0)
        put("points", 0)
        put("name", "game")
        put("players", buildJsonArray {
            add(buildJsonObject {
                put("playerId", "alice")
                put("online", true)
            })
        })
    }

    @Test
    fun `diff applied reproduces the new document`() {
        val old = sample()
        val new = buildJsonObject {
            put("phase", 1)
            put("points", 3)
            put("name", "game")
            put("players", buildJsonArray {
                add(buildJsonObject {
                    put("playerId", "alice")
                    put("online", false)
                })
                add(buildJsonObject {
                    put("playerId", "bob")
                    put("online", true)
                })
            })
            put("winners", buildJsonArray { add("alice") })
        }

        val parts = diffJson(old, new)
        assertEquals(new, old.withParts(parts))
        assertTrue(parts.any { it.path == "phase" })
        assertTrue(parts.any { it.path == "players" }, "array size change must replace the whole array")
    }

    @Test
    fun `nested scalar changes use dot paths`() {
        val old = sample()
        val new = buildJsonObject {
            put("phase", 0)
            put("points", 0)
            put("name", "game")
            put("players", buildJsonArray {
                add(buildJsonObject {
                    put("playerId", "alice")
                    put("online", false)
                })
            })
        }

        val parts = diffJson(old, new)
        assertEquals(listOf("players.0.online"), parts.map { it.path })
        assertEquals(new, old.withParts(parts))
    }

    @Test
    fun `equal numbers of different literals are not a change`() {
        val old = JsonPrimitive(1)
        val new = JsonPrimitive(1.0)
        assertTrue(diffJson(old, new).isEmpty())
    }

    @Test
    fun `removed keys replace the whole object`() {
        val old = buildJsonObject { put("a", 1); put("b", 2) }
        val new = buildJsonObject { put("a", 1) }
        val parts = diffJson(old, new)
        assertEquals(1, parts.size)
        assertEquals(new, old.withParts(parts))
    }

    @Test
    fun `path grammar accepts brackets and dots`() {
        val document = buildJsonObject {
            put("players", buildJsonArray {
                add(buildJsonObject { put("online", false) })
            })
        }
        assertEquals(JsonPrimitive(false), document.atPath("players[0].online"))
        assertEquals(JsonPrimitive(false), document.atPath("players.0.online"))
    }

    @Test
    fun `whole value part replaces the document`() {
        val document = buildJsonObject { put("a", 1) }
        val replacement = buildJsonObject { put("b", 2) }
        assertEquals(replacement, document.withParts(listOf(SyncPart(null, replacement))))
    }
}
