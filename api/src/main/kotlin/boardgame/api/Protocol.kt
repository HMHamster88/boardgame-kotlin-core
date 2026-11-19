package boardgame.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import boardgame.api.codegen.GenerateTs

/** Wire protocol DTOs. Field names are part of the contract — see docs/protocol.md. */

object GameStatus {
    const val CREATED = "CREATED"
    const val STARTED = "STARTED"
    const val FINISHED = "FINISHED"
}

object Roles {
    const val ADMIN = "ADMIN"
}

@GenerateTs
@Serializable
data class User(
    val id: String,
    val name: String,
    val color: String,
    val roles: List<String> = emptyList()
)

@GenerateTs
@Serializable
data class GameType(
    val type: String,
    val localizedName: JsonElement,
    val directoryName: String = ""
)

@GenerateTs
@Serializable
data class Player(
    val userId: String,
    val name: String,
    val color: String,
    val online: Boolean,
    val isBot: Boolean
)

@GenerateTs
@Serializable
data class Game(
    val id: String,
    val owner: String,
    val name: String,
    val type: String,
    var players: MutableList<Player> = mutableListOf(),
    var status: String = GameStatus.CREATED,
    val created: String
)

@GenerateTs
@Serializable
data class GameStaticSettings(val minPlayers: Int, val maxPlayers: Int)

/** What a game module returns from startGame / performAction. */
@GenerateTs(name = "GameState")
@Serializable
data class GameStatePayload(
    val id: String,
    val publicState: JsonElement,
    val privateState: JsonElement? = null
)

@GenerateTs
@Serializable
data class FullGameData(
    val settings: JsonElement,
    val game: Game,
    val gameState: GameStatePayload? = null
)

@GenerateTs
@Serializable
data class SyncPart(val path: String? = null, val value: JsonElement)

@GenerateTs(typeLiteral = "ObjectSyncMessage")
@Serializable
data class ObjectSyncMessage(
    val type: String = "ObjectSyncMessage",
    val objectId: String,
    val parts: List<SyncPart>
)

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

@GenerateTs(typeLiteral = "AdminGameMessageResponse")
@Serializable
data class AdminGameMessageResponse(
    val type: String = "AdminGameMessageResponse",
    val id: String? = null,
    val fullGameData: FullGameData
)

@GenerateTs(typeLiteral = "UpdateFullGameDataResponse")
@Serializable
data class UpdateFullGameDataResponse(
    val type: String = "UpdateFullGameDataResponse",
    val id: String? = null,
    val error: String? = null
)

// ---------------------------------------------------------------- base models (generated for the front)

@GenerateTs
@Serializable
data class GameSettings(val id: String)

@GenerateTs
@Serializable
data class PlayerPublicState(val playerId: String, val points: Int)

@GenerateTs
@Serializable
data class PlayerPrivateState(val playerId: String)

@GenerateTs
@Serializable
data class GameStatistics(val turnCount: Int)

@GenerateTs
@Serializable
data class GamePublicState(
    val activePlayerIndex: Int = 0,
    val playersStates: List<PlayerPublicState>? = null,
    val winnersIds: List<String> = emptyList(),
    val statistics: GameStatistics = GameStatistics(0)
)

@GenerateTs
@Serializable
data class GamePrivateState(val playersStates: List<PlayerPrivateState>? = null)

// ---------------------------------------------------------------- client -> server messages

@GenerateTs
@Serializable
data class CreateGameProps(val name: String, val type: String, val owner: String)

@GenerateTs
@Serializable
data class UserPayload(val id: String, val name: String, val color: String)

@GenerateTs(typeLiteral = "HandshakeRequest")
@Serializable
data class HandshakeRequest(val type: String = "HandshakeRequest", val userId: String)

@GenerateTs(typeLiteral = "GetAllGamesRequest")
@Serializable
data class GetAllGamesRequest(val type: String = "GetAllGamesRequest")

@GenerateTs(typeLiteral = "GetGameRequest")
@Serializable
data class GetGameRequest(val type: String = "GetGameRequest")

@GenerateTs(typeLiteral = "GetGameResponse")
@Serializable
data class GetGameResponse(
    val type: String = "GetGameResponse",
    val game: Game,
    val gameSettings: JsonElement
)

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

@GenerateTs(typeLiteral = "GetAdminGameMessage")
@Serializable
data class GetAdminGameMessage(
    val type: String = "GetAdminGameMessage",
    val gameId: String,
    val id: String? = null
)

@GenerateTs(typeLiteral = "UpdateFullGameDataMessage")
@Serializable
data class UpdateFullGameDataMessage(
    val type: String = "UpdateFullGameDataMessage",
    val fullGameData: FullGameData,
    val id: String? = null
)

@GenerateTs(typeLiteral = "ForceUpdateGameData")
@Serializable
data class ForceUpdateGameData(val type: String = "ForceUpdateGameData", val id: String? = null)

/** Inbound message kinds handled by the core. */
object MessageTypes {
    const val GET_ALL_GAMES = "GetAllGamesRequest"
    const val CREATE_GAME = "CreateGameRequest"
    const val DELETE_GAME = "DeleteGameRequest"
    const val UPDATE_USER = "UpdateUserRequest"
    const val CONNECT_TO_GAME = "ConnectToGameMessage"
    const val GET_ADMIN_GAME = "GetAdminGameMessage"
    const val UPDATE_FULL_GAME_DATA = "UpdateFullGameDataMessage"

    const val JOIN_GAME = "JoinGameMessage"
    const val START_GAME = "StartGameMessage"
    const val GAME_ACTION = "GameActionMessage"
    const val ADD_BOT = "AddBotGameMessage"
    const val UPDATE_BOT = "UpdateBotGameMessage"
    const val KICK_PLAYER = "KickPlayerMessage"
    const val CREATE_BACKUP = "CrateGameBackupMessage"
    const val FORCE_UPDATE = "ForceUpdateGameData"
    const val OBJECT_SYNC = "ObjectSyncMessage"
}
