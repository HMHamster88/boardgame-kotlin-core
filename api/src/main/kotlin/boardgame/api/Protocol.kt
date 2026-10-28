package boardgame.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Wire protocol DTOs. Field names are part of the contract — see docs/protocol.md. */

object GameStatus {
    const val CREATED = "CREATED"
    const val STARTED = "STARTED"
    const val FINISHED = "FINISHED"
}

object Roles {
    const val ADMIN = "ADMIN"
}

@Serializable
data class User(
    val id: String,
    val name: String,
    val color: String,
    val roles: List<String> = emptyList()
)

@Serializable
data class GameType(
    val type: String,
    val localizedName: JsonElement,
    // The typo is part of the protocol: the front reads `diectoryName`.
    @SerialName("diectoryName") val directoryName: String? = null
)

@Serializable
data class Player(
    val userId: String,
    val name: String,
    val color: String,
    val online: Boolean,
    val isBot: Boolean
)

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

@Serializable
data class GameStaticSettings(val minPlayers: Int, val maxPlayers: Int)

/** What a game module returns from startGame / performAction. */
@Serializable
data class GameStatePayload(
    val id: String,
    val publicState: JsonElement,
    val privateState: JsonElement? = null
)

@Serializable
data class FullGameData(
    val settings: JsonElement,
    val game: Game,
    val gameState: GameStatePayload? = null
)

@Serializable
data class SyncPart(val path: String? = null, val value: JsonElement)

@Serializable
data class ObjectSyncMessage(
    val type: String = "ObjectSyncMessage",
    val objectId: String,
    val parts: List<SyncPart>
)

@Serializable
data class HandshakeResponse(
    val type: String = "HandshakeResponse",
    val user: User,
    val gameTypes: List<GameType>
)

@Serializable
data class AllGamesResponse(
    val type: String = "AllGamesResponse",
    val games: List<Game>
)

@Serializable
data class GameCreatedMessage(
    val type: String = "GameCreatedMessage",
    val game: Game
)

@Serializable
data class GameDeletedMessage(
    val type: String = "GameDeletedMessage",
    val gameId: String
)

@Serializable
data class NotifyGameMessage(
    val type: String = "NotifyGameMessage",
    val message: String,
    val messageParams: JsonElement? = null
)

@Serializable
data class GameMessageResponse(
    val type: String = "GameMessageResponse",
    val id: String,
    val response: JsonElement
)

@Serializable
data class AdminGameMessageResponse(
    val type: String = "AdminGameMessageResponse",
    val id: String? = null,
    val fullGameData: FullGameData
)

@Serializable
data class UpdateFullGameDataResponse(
    val type: String = "UpdateFullGameDataResponse",
    val id: String? = null,
    val error: String? = null
)

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
