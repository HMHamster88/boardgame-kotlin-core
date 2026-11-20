package boardgame.core

import boardgame.api.Storage
import boardgame.api.User
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChatServiceTest {

    private class MemoryStorage : Storage {
        private val data = mutableMapOf<String, MutableMap<String, String>>()

        override fun get(collection: String, id: String): String? = data[collection]?.get(id)

        override fun put(collection: String, id: String, json: String) {
            data.getOrPut(collection) { mutableMapOf() }[id] = json
        }

        override fun delete(collection: String, id: String) {
            data[collection]?.remove(id)
        }

        override fun getAll(collection: String): List<String> = data[collection]?.values?.toList().orEmpty()

        override fun findByField(collection: String, field: String, value: String): String? =
            getAll(collection).firstOrNull { it.contains("\"$field\":\"$value\"") }
    }

    private fun service(
        storage: MemoryStorage,
        maxMessages: Int = 200,
        rateMaxMessages: Int = 100,
        clockValue: () -> String = { "2026-11-20T00:00:00.000Z" }
    ) = ChatService(
        storage = storage,
        userProvider = { id -> User(id, "User $id", "#ff0000") },
        clock = clockValue,
        nowMillis = { 1_000L },
        maxMessages = maxMessages,
        rateMaxMessages = rateMaxMessages
    )

    @Test
    fun postsOrderedMessagesAndTrimsTheRoom() = runBlocking {
        val storage = MemoryStorage()
        var tick = 0
        val chat = service(storage, maxMessages = 2, clockValue = { "t${tick++}" })

        chat.post("lobby", "u1", " first ", null)
        chat.post("lobby", "u1", "second", null)
        chat.post("lobby", "u1", "third", null)

        val page = chat.history("lobby", null, 10)
        assertEquals(listOf("second", "third"), page.messages.map { it.text })
        assertEquals(listOf(2L, 3L), page.messages.map { it.seq })
        assertTrue(!page.hasMore)
    }

    @Test
    fun historyPagesOlderMessages() = runBlocking {
        val storage = MemoryStorage()
        val chat = service(storage)

        repeat(4) { index -> chat.post("lobby", "u1", "m$index", null) }

        val page = chat.history("lobby", beforeSeq = 4, limit = 2)
        assertEquals(listOf("m1", "m2"), page.messages.map { it.text })
        assertTrue(page.hasMore)
    }

    @Test
    fun roomsAreIsolatedAndCanBeDeleted() = runBlocking {
        val storage = MemoryStorage()
        val chat = service(storage)

        chat.post("lobby", "u1", "lobby message", null)
        chat.post("game_1", "u1", "game message", null)

        assertEquals(listOf("lobby message"), chat.history("lobby", null, 10).messages.map { it.text })
        assertEquals(listOf("game message"), chat.history("game_1", null, 10).messages.map { it.text })

        chat.deleteRoom("game_1")
        assertTrue(chat.history("game_1", null, 10).messages.isEmpty())
    }

    @Test
    fun rateLimitsMessages() = runBlocking {
        val chat = service(MemoryStorage(), rateMaxMessages = 2)

        chat.post("lobby", "u1", "one", null)
        chat.post("lobby", "u1", "two", null)

        val error = assertFailsWith<ChatException> {
            chat.post("lobby", "u1", "three", null)
        }
        assertEquals("RATE_LIMITED", error.code)
    }

    @Test
    fun rejectsEmptyAndOversizedMessages() = runBlocking {
        val chat = ChatService(
            storage = MemoryStorage(),
            userProvider = { id -> User(id, "User", "#ff0000") },
            maxTextLength = 5,
            rateMaxMessages = 100
        )

        assertEquals("EMPTY_MESSAGE", assertFailsWith<ChatException> { chat.post("lobby", "u1", "  ", null) }.code)
        assertEquals("TEXT_TOO_LONG", assertFailsWith<ChatException> { chat.post("lobby", "u1", "123456", null) }.code)
    }

    @Test
    fun reloadsPersistedRoom() = runBlocking {
        val storage = MemoryStorage()
        service(storage).post("lobby", "u1", "persisted", null)

        val reloaded = service(storage).history("lobby", null, 10)

        assertEquals(listOf("persisted"), reloaded.messages.map { it.text })
    }

    @Test
    fun snapshotsTheUserProfileOnTheMessage() = runBlocking {
        val chat = service(MemoryStorage())

        val message = chat.post("lobby", "u42", "hello", "client-1")

        assertEquals("User u42", message.userName)
        assertEquals("#ff0000", message.userColor)
        assertEquals("client-1", message.clientMessageId)
    }
}
