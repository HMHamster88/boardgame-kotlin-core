package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Lobby traffic: handshake, game list, game creation and user updates. */

@GenerateTs
@Serializable
data class CreateGameProps(val name: String, val type: String, val owner: String)

@GenerateTs
@Serializable
data class UserPayload(val id: String, val name: String, val color: String)

// ---------------------------------------------------------------- client -> server

@GenerateTs(typeLiteral = "HandshakeRequest")
@Serializable
data class HandshakeRequest(val type: String = "HandshakeRequest", val userId: String)

@GenerateTs(typeLiteral = "GetAllGamesRequest")
@Serializable
data class GetAllGamesRequest(val type: String = "GetAllGamesRequest")

@GenerateTs(typeLiteral = "GetGameRequest")
@Serializable
data class GetGameRequest(val type: String = "GetGameRequest")

@GenerateTs(typeLiteral = "CreateGameRequest")
@Serializable
data class CreateGameRequest(
    val type: String = "CreateGameRequest",
    val props: CreateGameProps,
    val id: String? = null
)

@GenerateTs(typeLiteral = "DeleteGameRequest")
@Serializable
data class DeleteGameRequest(
    val type: String = "DeleteGameRequest",
    val gameId: String,
    val id: String? = null
)

@GenerateTs(typeLiteral = "UpdateUserRequest")
@Serializable
data class UpdateUserRequest(
    val type: String = "UpdateUserRequest",
    val user: UserPayload,
    val id: String? = null
)

// ---------------------------------------------------------------- server -> client

@GenerateTs(typeLiteral = "HandshakeResponse")
@Serializable
data class HandshakeResponse(
    val type: String = "HandshakeResponse",
    val user: User,
    val gameTypes: List<GameType>
)

@GenerateTs(typeLiteral = "AllGamesResponse")
@Serializable
data class AllGamesResponse(
    val type: String = "AllGamesResponse",
    val games: List<Game>
)

@GenerateTs(typeLiteral = "GameCreatedMessage")
@Serializable
data class GameCreatedMessage(
    val type: String = "GameCreatedMessage",
    val game: Game
)

@GenerateTs(typeLiteral = "GameDeletedMessage")
@Serializable
data class GameDeletedMessage(
    val type: String = "GameDeletedMessage",
    val gameId: String
)

@GenerateTs(typeLiteral = "GetGameResponse")
@Serializable
data class GetGameResponse(
    val type: String = "GetGameResponse",
    val game: Game,
    val gameSettings: JsonElement
)
