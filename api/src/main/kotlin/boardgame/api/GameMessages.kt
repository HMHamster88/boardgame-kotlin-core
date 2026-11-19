package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Traffic for a game room: connect, players, actions, notifications and admin operations. */

@GenerateTs(typeLiteral = "ConnectToGameMessage")
@Serializable
data class ConnectToGameMessage(
    val type: String = "ConnectToGameMessage",
    val gameId: String,
    val id: String? = null
)

@GenerateTs
@Serializable
data class GameMessage(val type: String, val id: String? = null)

@GenerateTs(typeLiteral = "GameInfoMessage")
@Serializable
data class GameInfoMessage(
    val type: String = "GameInfoMessage",
    val game: Game,
    val id: String? = null
)

@GenerateTs(typeLiteral = "ErorrGameMessage")
@Serializable
data class ErorrGameMessage(
    val type: String = "ErorrGameMessage",
    val message: String,
    val messageParams: JsonElement? = null,
    val id: String? = null
)

@GenerateTs(typeLiteral = "StartGameMessage")
@Serializable
data class StartGameMessage(val type: String = "StartGameMessage", val id: String? = null)

@GenerateTs(typeLiteral = "JoinGameMessage")
@Serializable
data class JoinGameMessage(val type: String = "JoinGameMessage", val id: String? = null)

@GenerateTs(typeLiteral = "AddBotGameMessage")
@Serializable
data class AddBotGameMessage(
    val type: String = "AddBotGameMessage",
    val player: Player,
    val id: String? = null
)

@GenerateTs(typeLiteral = "UpdateBotGameMessage")
@Serializable
data class UpdateBotGameMessage(
    val type: String = "UpdateBotGameMessage",
    val player: Player,
    val id: String? = null
)

@GenerateTs(typeLiteral = "KickPlayerMessage")
@Serializable
data class KickPlayerMessage(
    val type: String = "KickPlayerMessage",
    val playerId: String,
    val id: String? = null
)

@GenerateTs
@Serializable
data class GameAction(val type: String)

@GenerateTs(typeLiteral = "GameActionMessage")
@Serializable
data class GameActionMessage(
    val type: String = "GameActionMessage",
    val action: JsonElement,
    val id: String? = null
)

@GenerateTs(typeLiteral = "CrateGameBackupMessage")
@Serializable
data class CrateGameBackupMessage(val type: String = "CrateGameBackupMessage", val id: String? = null)

// ---------------------------------------------------------------- server -> client

@GenerateTs(typeLiteral = "NotifyGameMessage")
@Serializable
data class NotifyGameMessage(
    val type: String = "NotifyGameMessage",
    val message: String,
    val messageParams: JsonElement? = null
)

@GenerateTs(typeLiteral = "GameMessageResponse")
@Serializable
data class GameMessageResponse(
    val type: String = "GameMessageResponse",
    val id: String,
    val response: JsonElement
)

// ---------------------------------------------------------------- admin

@GenerateTs(typeLiteral = "GetAdminGameMessage")
@Serializable
data class GetAdminGameMessage(
    val type: String = "GetAdminGameMessage",
    val gameId: String,
    val id: String? = null
)

@GenerateTs(typeLiteral = "AdminGameMessageResponse")
@Serializable
data class AdminGameMessageResponse(
    val type: String = "AdminGameMessageResponse",
    val id: String? = null,
    val fullGameData: FullGameData
)

@GenerateTs(typeLiteral = "UpdateFullGameDataMessage")
@Serializable
data class UpdateFullGameDataMessage(
    val type: String = "UpdateFullGameDataMessage",
    val fullGameData: FullGameData,
    val id: String? = null
)

@GenerateTs(typeLiteral = "UpdateFullGameDataResponse")
@Serializable
data class UpdateFullGameDataResponse(
    val type: String = "UpdateFullGameDataResponse",
    val id: String? = null,
    val error: String? = null
)

@GenerateTs(typeLiteral = "ForceUpdateGameData")
@Serializable
data class ForceUpdateGameData(val type: String = "ForceUpdateGameData", val id: String? = null)
