package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable

/** Chat rooms and wire messages. Chat is implemented by the core; game modules stay unaware. */

object ChatRoomIds {
    const val LOBBY = "lobby"

    fun game(gameId: String): String = "game_$gameId"

    fun gameId(roomId: String): String? =
        if (roomId.startsWith("game_")) roomId.removePrefix("game_").takeIf { it.isNotBlank() } else null
}

@GenerateTs(asUnion = true)
@Serializable
enum class ChatMessageKind {
    USER,
    SYSTEM
}

@GenerateTs
@Serializable
data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val seq: Long = 0,
    val userId: String = "",
    val userName: String = "",
    val userColor: String = "",
    val text: String = "",
    val createdAt: String = "",
    val kind: ChatMessageKind = ChatMessageKind.USER,
    val clientMessageId: String? = null,
    val deleted: Boolean = false
)

// ---------------------------------------------------------------- client -> server

@GenerateTs(typeLiteral = "ChatSendRequest")
@Serializable
data class ChatSendRequest(
    val type: String = "ChatSendRequest",
    val id: String? = null,
    val roomId: String = "",
    val text: String = "",
    val clientMessageId: String? = null
)

@GenerateTs(typeLiteral = "ChatHistoryRequest")
@Serializable
data class ChatHistoryRequest(
    val type: String = "ChatHistoryRequest",
    val id: String? = null,
    val roomId: String = "",
    val beforeSeq: Long? = null,
    val limit: Int? = null
)

// ---------------------------------------------------------------- server -> client

@GenerateTs(typeLiteral = "ChatMessageReceived")
@Serializable
data class ChatMessageReceived(
    val type: String = "ChatMessageReceived",
    val message: ChatMessage
)

@GenerateTs(typeLiteral = "ChatHistoryResponse")
@Serializable
data class ChatHistoryResponse(
    val type: String = "ChatHistoryResponse",
    val id: String? = null,
    val roomId: String = "",
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val hasMore: Boolean = false
)

@GenerateTs(typeLiteral = "ChatErrorResponse")
@Serializable
data class ChatErrorResponse(
    val type: String = "ChatErrorResponse",
    val id: String? = null,
    val code: String = "",
    val message: String = ""
)
