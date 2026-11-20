package boardgame.core

import boardgame.api.ChatMessage
import boardgame.api.ChatMessageKind
import boardgame.api.Storage
import boardgame.api.User
import boardgame.api.decodeFromJson
import boardgame.api.encodeToJson
import boardgame.api.nowIso
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ChatPage(
    val messages: List<ChatMessage>,
    val hasMore: Boolean
)

class ChatException(val code: String, override val message: String) : RuntimeException(message)

@Serializable
private data class ChatRoomState(
    val roomId: String = "",
    val nextSeq: Long = 1,
    val messages: List<ChatMessage> = emptyList()
)

/**
 * Persistent, per-room chat rooms.
 *
 * A room is stored as one JSON document (`chat/<roomId>.json`) with the newest [maxMessages]
 * messages. Room membership and broadcast are handled by [CoreImpl], which also owns the peers.
 */
class ChatService(
    private val storage: Storage,
    private val userProvider: (String) -> User?,
    private val clock: () -> String = { nowIso() },
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val maxMessages: Int = 200,
    private val maxTextLength: Int = 2_000,
    private val historyLimit: Int = 50,
    private val maxHistoryLimit: Int = 100,
    private val rateWindowMs: Long = 5_000,
    private val rateMaxMessages: Int = 5
) {

    private val roomLocks = ConcurrentHashMap<String, Mutex>()
    private val recentPosts = ConcurrentHashMap<String, ArrayDeque<Long>>()

    suspend fun post(
        roomId: String,
        userId: String,
        rawText: String,
        clientMessageId: String?
    ): ChatMessage {
        val text = normalize(rawText)
        checkRate(userId)
        val lock = roomLocks.computeIfAbsent(roomId) { Mutex() }
        return lock.withLock {
            val state = load(roomId)
            val user = userProvider(userId) ?: User(userId, "Player", "#FF0000")
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                seq = state.nextSeq,
                userId = userId,
                userName = user.name,
                userColor = user.color,
                text = text,
                createdAt = clock(),
                kind = ChatMessageKind.USER,
                clientMessageId = clientMessageId
            )
            val messages = (state.messages + message).takeLast(maxMessages)
            storage.put(
                "chat",
                roomId,
                encodeToJson(ChatRoomState(roomId = roomId, nextSeq = state.nextSeq + 1, messages = messages))
            )
            message
        }
    }

    suspend fun history(roomId: String, beforeSeq: Long?, limit: Int?): ChatPage {
        val take = (limit ?: historyLimit).coerceIn(1, maxHistoryLimit)
        val lock = roomLocks.computeIfAbsent(roomId) { Mutex() }
        return lock.withLock {
            val state = load(roomId)
            val eligible = if (beforeSeq == null) {
                state.messages
            } else {
                state.messages.filter { it.seq < beforeSeq }
            }
            val page = eligible.takeLast(take)
            ChatPage(page, eligible.size > page.size)
        }
    }

    fun deleteRoom(roomId: String) {
        storage.delete("chat", roomId)
        roomLocks.remove(roomId)
    }

    fun forgetUser(userId: String) {
        recentPosts.remove(userId)
    }

    private fun normalize(raw: String): String {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (text.isEmpty()) throw ChatException("EMPTY_MESSAGE", "Message is empty")
        if (text.length > maxTextLength) {
            throw ChatException("TEXT_TOO_LONG", "Message must be at most $maxTextLength characters")
        }
        if (text.any { it.isISOControl() && it != '\n' && it != '\t' }) {
            throw ChatException("INVALID_TEXT", "Message contains invalid characters")
        }
        return text
    }

    private fun checkRate(userId: String) {
        val now = nowMillis()
        val posts = recentPosts.computeIfAbsent(userId) { ArrayDeque() }
        synchronized(posts) {
            while (posts.isNotEmpty() && now - posts.first() >= rateWindowMs) posts.removeFirst()
            if (posts.size >= rateMaxMessages) {
                throw ChatException("RATE_LIMITED", "Too many messages, slow down")
            }
            posts.addLast(now)
        }
    }

    private fun load(roomId: String): ChatRoomState {
        val stored = storage.get("chat", roomId)
            ?.let { runCatching { decodeFromJson<ChatRoomState>(it) }.getOrNull() }
        if (stored == null || stored.roomId != roomId) return ChatRoomState(roomId)
        val nextSeq = maxOf(stored.nextSeq, (stored.messages.maxOfOrNull { it.seq } ?: 0) + 1)
        return stored.copy(nextSeq = nextSeq)
    }
}
