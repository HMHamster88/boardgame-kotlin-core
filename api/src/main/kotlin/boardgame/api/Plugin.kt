package boardgame.api

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

/** Services the platform host (desktop launcher / Android app) provides to plugins. */
interface HostServices {
    val platform: PlatformInfo
    val storage: Storage
    val http: HostHttp
    val console: HostConsole?

    /**
     * Loads a plugin archive (`module.jar`) with a classloader whose parent sees [service].
     * The host owns the platform-specific mechanics (URLClassLoader / DexClassLoader).
     */
    fun <T : Any> loadPlugin(file: File, service: Class<T>, parent: ClassLoader? = null): T
}

data class PlatformInfo(
    val kind: String,
    val appVersion: String,
    val dataDir: File
) {
    companion object {
        const val DESKTOP = "DESKTOP"
        const val ANDROID = "ANDROID"
    }
}

interface Storage {
    fun get(collection: String, id: String): String?
    fun put(collection: String, id: String, json: String)
    fun delete(collection: String, id: String)
    fun getAll(collection: String): List<String>
    fun findByField(collection: String, field: String, value: String): String?
}

interface HostHttp {
    suspend fun getText(url: String): String?
    suspend fun getBytes(url: String): ByteArray?

    /**
     * Progress-aware download. The default ignores [onProgress], so hosts that cannot report
     * progress keep working.
     */
    suspend fun getBytes(url: String, onProgress: (received: Long, total: Long) -> Unit): ByteArray? =
        getBytes(url)
}

interface HostConsole {
    fun println(message: String)
    suspend fun readLine(prompt: String): String?
}

/** One WebSocket client owned by the host. */
interface Peer {
    val userId: String
    fun send(text: String)
    fun onMessage(handler: (String) -> Unit)
    fun onClose(handler: () -> Unit)
}

data class GameModuleInfo(
    val type: String,
    val version: String,
    val apiVersion: Int,
    val directory: String,
    val homepage: String
)

interface GameCore {
    fun attach(peer: Peer)
    fun detach(peer: Peer)
    suspend fun installGameModule(homepage: String): GameModuleInfo
    fun gameModules(): List<GameModuleInfo>
    suspend fun shutdown()
}

/** Entry point of a downloadable core plugin (ServiceLoader service). */
interface CoreModule {
    val version: String
    fun start(host: HostServices): GameCore
}

/** Entry point of a downloadable game plugin (ServiceLoader service). */
interface GameBackModule {
    fun createService(): GameBackService
}

interface GameBackService {
    val type: String
    val localizedName: JsonObject
    val gameStaticSettings: GameStaticSettings
    val version: String
    val homepage: String

    fun getDefaultSettings(): JsonObject
    fun startGame(game: Game, settings: JsonObject): GameStatePayload
    suspend fun performAction(ctx: GameContext, action: JsonElement, playerId: String): JsonElement?

    /** Returns the next action a bot wants to take, or null when no bot wants to act. */
    suspend fun nextBotAction(ctx: BotGameContext): BotAction?
}

data class BotAction(val action: JsonElement, val playerId: String)

interface GameContext {
    val game: Game
    val settings: JsonObject
    val publicState: JsonElement
    val privateState: JsonElement?

    fun updatePublicState(value: JsonElement)
    fun updatePrivateState(value: JsonElement?)
    fun notify(peerId: String?, message: String, params: JsonElement? = null)
}

interface BotGameContext {
    val game: Game
    val settings: JsonObject
    val publicState: JsonElement
    val privateState: JsonElement?
}

inline fun <reified T> GameContext.publicStateAs(): T = apiJson.decodeFromJsonElement(publicState)

inline fun <reified T> GameContext.privateStateAs(): T =
    apiJson.decodeFromJsonElement(privateState ?: error("game has no private state"))

inline fun <reified T> GameContext.settingsAs(): T = apiJson.decodeFromJsonElement(settings)

inline fun <reified T> BotGameContext.publicStateAs(): T = apiJson.decodeFromJsonElement(publicState)

inline fun <reified T> BotGameContext.privateStateAs(): T =
    apiJson.decodeFromJsonElement(privateState ?: error("game has no private state"))

inline fun <reified T> BotGameContext.settingsAs(): T = apiJson.decodeFromJsonElement(settings)
