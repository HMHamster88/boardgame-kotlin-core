package boardgame.api

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Game state envelope and the base models a game module extends.
 *
 * `publicState` / `settings` stay untyped on the wire: each game module sends its own state shape.
 */

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
