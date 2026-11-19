package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Core game/lobby entities. Field names are part of the contract — see docs/protocol.md. */

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
