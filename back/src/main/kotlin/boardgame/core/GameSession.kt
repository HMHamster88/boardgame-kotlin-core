package boardgame.core

import boardgame.api.BotAction
import boardgame.api.BotGameContext
import boardgame.api.FullGameData
import boardgame.api.Game
import boardgame.api.GameBackService
import boardgame.api.GameContext
import boardgame.api.GameMessageResponse
import boardgame.api.GameStatePayload
import boardgame.api.GameStatus
import boardgame.api.HostServices
import boardgame.api.MessageTypes
import boardgame.api.NotifyGameMessage
import boardgame.api.ObjectSyncMessage
import boardgame.api.Peer
import boardgame.api.Player
import boardgame.api.SyncPart
import boardgame.api.User
import boardgame.api.decodeFromJson
import boardgame.api.decodeTree
import boardgame.api.diffJson
import boardgame.api.encodeToJson
import boardgame.api.encodeTree
import boardgame.api.jsonEquals
import boardgame.api.nowIso
import boardgame.api.obj
import boardgame.api.str
import boardgame.api.withParts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class GameSession(
    initialGame: Game,
    private val host: HostServices,
    private val service: GameBackService,
    private val userProvider: (String) -> User?,
    private val scope: CoroutineScope
) {
    var game: Game = initialGame
        private set

    var settings: JsonObject = loadSettings()
        private set

    var state: GameStatePayload? = loadState()
        private set

    private val peers = mutableMapOf<String, Peer>()
    private val mutex = Mutex()

    private val storage get() = host.storage

    init {
        startBotLoop()
    }

    fun attach(peer: Peer) {
        scope.launch { mutex.withLock { attachLocked(peer) } }
    }

    fun detach(peer: Peer) {
        scope.launch { mutex.withLock { detachLocked(peer) } }
    }

    suspend fun handle(peer: Peer, message: JsonObject, raw: String) {
        when (message.str("type")) {
            MessageTypes.OBJECT_SYNC -> mutex.withLock { syncLocked(peer, message, raw) }
            MessageTypes.JOIN_GAME -> mutex.withLock { joinLocked(peer) }
            MessageTypes.START_GAME -> mutex.withLock { startLocked() }
            MessageTypes.GAME_ACTION -> {
                val action = message["action"] ?: return
                val responseId = message.str("id")
                val response = mutex.withLock { performActionLocked(action, peer.userId) }
                if (response != null && responseId != null) {
                    send(peer, GameMessageResponse(id = responseId, response = response))
                }
            }
            MessageTypes.ADD_BOT -> mutex.withLock { addBotLocked(message) }
            MessageTypes.UPDATE_BOT -> mutex.withLock { updateBotLocked(message) }
            MessageTypes.UPDATE_USER -> mutex.withLock { updateUserLocked(peer, message) }
            MessageTypes.KICK_PLAYER -> mutex.withLock { kickLocked(peer, message) }
            MessageTypes.CREATE_BACKUP -> mutex.withLock { backupLocked(peer) }
            MessageTypes.FORCE_UPDATE -> mutex.withLock { forceUpdateLocked(peer) }
        }
    }

    fun fullGameData(): FullGameData = FullGameData(settings = settings, game = game, gameState = state)

    suspend fun updateFullGameData(data: FullGameData) {
        mutex.withLock {
            game = data.game
            settings = data.settings as? JsonObject ?: JsonObject(emptyMap())
            state = data.gameState
            persistGame()
            persistSettings()
            persistState()
            sendFullToAll("game", gameTree())
            sendFullToAll("gameSettings", settings)
            val current = state ?: return@withLock
            sendFullToAll("gamePublicState", current.publicState)
            current.privateState.playerStates()?.forEach { element ->
                val playerId = (element as? JsonObject)?.str("playerId") ?: return@forEach
                peers[playerId]?.let { sendFull(it, "playerPrivateState:$playerId", element) }
            }
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    // ---------------------------------------------------------------- attach

    private fun attachLocked(peer: Peer) {
        // Refresh before the peer is registered: existing peers get the diff, the new peer gets the
        // updated name in its full snapshot (never a diff before the full state).
        syncPlayerProfileLocked(peer.userId)
        peers[peer.userId] = peer
        sendFull(peer, "game", gameTree())
        sendFull(peer, "gameSettings", settings)
        val current = state
        if (current != null) {
            sendFull(peer, "gamePublicState", current.publicState)
            playerPrivate(peer.userId)?.let { sendFull(peer, "playerPrivateState:${peer.userId}", it) }
        }
        setOnlineLocked(peer.userId, true)
    }

    private fun detachLocked(peer: Peer) {
        if (peers[peer.userId] !== peer) return
        peers.remove(peer.userId)
        setOnlineLocked(peer.userId, false)
    }

    private fun setOnlineLocked(userId: String, online: Boolean) {
        val index = game.players.indexOfFirst { it.userId == userId }
        if (index < 0) return
        val player = game.players[index]
        if (player.online == online) return
        game.players[index] = player.copy(online = online)
        sendToAll(ObjectSyncMessage(objectId = "game", parts = listOf(SyncPart("players.$index.online", JsonPrimitive(online)))))
    }

    // ---------------------------------------------------------------- lobby/game

    private fun joinLocked(peer: Peer) {
        if (game.players.any { it.userId == peer.userId }) return
        val user = userProvider(peer.userId) ?: User(peer.userId, "Player", "#FF0000")
        val before = gameTree()
        game.players.add(Player(userId = peer.userId, name = user.name, color = user.color, online = true, isBot = false))
        sendDiffLocked("game", before, gameTree())
        persistGame()
    }

    /** Mirrors the user document into the player entry, so profile edits show up in the game. */
    private fun updateUserLocked(peer: Peer, message: JsonObject) {
        val payload = message.obj("user") ?: return
        val userId = payload.str("id") ?: peer.userId
        if (userId != peer.userId) return
        val name = payload.str("name") ?: return
        val color = payload.str("color") ?: return
        setPlayerProfileLocked(userId, name, color)
    }

    /** Picks up a profile changed while away from the game (the request is only seen by the lobby). */
    private fun syncPlayerProfileLocked(userId: String) {
        val user = userProvider(userId) ?: return
        setPlayerProfileLocked(userId, user.name, user.color)
    }

    private fun setPlayerProfileLocked(userId: String, name: String, color: String) {
        val index = game.players.indexOfFirst { it.userId == userId }
        if (index < 0) return
        val player = game.players[index]
        if (player.name == name && player.color == color) return

        val before = gameTree()
        game.players[index] = player.copy(name = name, color = color)
        sendDiffLocked("game", before, gameTree())
        persistGame()
    }

    private fun startLocked() {
        if (state != null) return
        val before = gameTree()
        state = service.startGame(game, settings)
        sendDiffLocked("game", before, gameTree())
        val current = state ?: return
        sendFullToAll("gamePublicState", current.publicState)
        current.privateState.playerStates()?.forEach { element ->
            val playerId = (element as? JsonObject)?.str("playerId") ?: return@forEach
            peers[playerId]?.let { sendFull(it, "playerPrivateState:$playerId", element) }
        }
        persistGame()
        persistSettings()
        persistState()
    }

    private suspend fun performActionLocked(action: JsonElement, playerId: String): JsonElement? {
        val beforeGame = gameTree()
        val beforeSettings = settings
        val beforeState = state
        var lastPublic: JsonElement = beforeState?.publicState ?: JsonNull
        val context = Context(game, settings, beforeState)
        context.onNotify { peerId, message, params -> notify(peerId, message, params) }
        context.onPublishPublic { current ->
            sendDiffLocked("gamePublicState", lastPublic, current)
            lastPublic = current
        }
        val response = service.performAction(context, action, playerId)
        state = context.snapshot()
        settings = context.settingsSnapshot()

        sendDiffLocked("game", beforeGame, gameTree())
        sendDiffLocked("gameSettings", beforeSettings, settings)
        val current = state
        if (current != null) {
            sendDiffLocked("gamePublicState", lastPublic, current.publicState)
            diffPrivateLocked(beforeState, current)
        }
        persistIfChanged(beforeGame, beforeSettings, beforeState)
        return response
    }

    private fun addBotLocked(message: JsonObject) {
        val payload = message.obj("player")?.let { runCatching { decodeTree<Player>(it) }.getOrNull() } ?: return
        val before = gameTree()
        game.players.add(payload.copy(userId = UUID.randomUUID().toString(), isBot = true, online = true))
        sendDiffLocked("game", before, gameTree())
        persistGame()
    }

    private fun updateBotLocked(message: JsonObject) {
        val payload = message.obj("player")?.let { runCatching { decodeTree<Player>(it) }.getOrNull() } ?: return
        val index = game.players.indexOfFirst { it.userId == payload.userId }
        if (index < 0) return
        val before = gameTree()
        game.players[index] = payload
        sendDiffLocked("game", before, gameTree())
        persistGame()
    }

    private fun kickLocked(peer: Peer, message: JsonObject) {
        val playerId = message.str("playerId") ?: return
        val player = game.players.firstOrNull { it.userId == playerId } ?: return
        if (game.owner != peer.userId && player.userId != peer.userId) return
        val before = gameTree()
        game.players.remove(player)
        sendDiffLocked("game", before, gameTree())
        persistGame()
    }

    private fun backupLocked(peer: Peer) {
        val id = UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("id", id)
            put("gameId", game.id)
            put("game", gameTree())
            put("settings", settings)
            put("state", state?.let { encodeTree(it) } ?: JsonNull)
            put("date", nowIso())
        }
        storage.put("game_backups", id, payload.toString())
        send(peer, NotifyGameMessage(message = "Game state backup created"))
    }

    private fun forceUpdateLocked(peer: Peer) {
        val current = state ?: return
        sendFull(peer, "gamePublicState", current.publicState)
        playerPrivate(peer.userId)?.let { sendFull(peer, "playerPrivateState:${peer.userId}", it) }
    }

    private fun syncLocked(peer: Peer, message: JsonObject, raw: String) {
        val objectId = message.str("objectId") ?: return
        val parts = (message["parts"] as? JsonArray)?.mapNotNull {
            runCatching { decodeTree<SyncPart>(it) }.getOrNull()
        } ?: return

        when (objectId) {
            "game" -> game = decodeTree(gameTree().withParts(parts))
            "gameSettings" -> settings = decodeTree(settings.withParts(parts))
            else -> return
        }
        peers.values.filter { it.userId != peer.userId }.forEach { it.send(raw) }
    }

    // ---------------------------------------------------------------- sync

    private fun gameTree(): JsonElement = encodeTree(game)

    private fun playerPrivate(playerId: String): JsonElement? {
        val states = state?.privateState.playerStates() ?: return null
        return states.firstOrNull { (it as? JsonObject)?.str("playerId") == playerId }
    }

    private fun diffPrivateLocked(before: GameStatePayload?, after: GameStatePayload) {
        val oldStates = before?.privateState.playerStates()
        val newStates = after.privateState.playerStates() ?: return
        for (index in newStates.indices) {
            val newElement = newStates[index]
            val playerId = (newElement as? JsonObject)?.str("playerId") ?: continue
            val peer = peers[playerId] ?: continue
            val oldElement = oldStates?.getOrNull(index)
                ?.takeIf { (it as? JsonObject)?.str("playerId") == playerId }
            if (oldElement == null) {
                sendFull(peer, "playerPrivateState:$playerId", newElement)
            } else {
                val parts = diffJson(oldElement, newElement)
                if (parts.isNotEmpty()) {
                    send(peer, ObjectSyncMessage(objectId = "playerPrivateState:$playerId", parts = parts))
                }
            }
        }
    }

    private fun sendDiffLocked(objectId: String, before: JsonElement, after: JsonElement) {
        val parts = diffJson(before, after)
        if (parts.isEmpty()) return
        sendToAll(ObjectSyncMessage(objectId = objectId, parts = parts))
    }

    private fun sendFull(peer: Peer, objectId: String, value: JsonElement) {
        send(peer, ObjectSyncMessage(objectId = objectId, parts = listOf(SyncPart(null, value))))
    }

    private fun sendFullToAll(objectId: String, value: JsonElement) {
        sendToAll(ObjectSyncMessage(objectId = objectId, parts = listOf(SyncPart(null, value))))
    }

    private fun sendToAll(message: ObjectSyncMessage) {
        val text = encodeToJson(message)
        peers.values.forEach { it.send(text) }
    }

    private fun notify(peerId: String?, message: String, params: JsonElement?) {
        val payload = encodeToJson(NotifyGameMessage(message = message, messageParams = params))
        if (peerId != null) peers[peerId]?.send(payload) else peers.values.forEach { it.send(payload) }
    }

    private inline fun <reified T> send(peer: Peer, message: T) {
        peer.send(encodeToJson(message))
    }

    // ---------------------------------------------------------------- bots

    private fun startBotLoop() {
        scope.launch {
            while (isActive) {
                delay(100)
                if (game.status != GameStatus.STARTED) continue
                mutex.withLock {
                    var iterations = 0
                    while (iterations < 50) {
                        val current = state ?: break
                        val context = BotContext(game, settings, current.publicState, current.privateState)
                        val action: BotAction = runCatching { service.nextBotAction(context) }.getOrNull() ?: break
                        performActionLocked(action.action, action.playerId)
                        iterations++
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- storage

    private fun loadSettings(): JsonObject {
        storage.get("game_settings", game.id)?.let { stored ->
            runCatching { decodeFromJson<JsonObject>(stored) }.getOrNull()?.let { return it }
        }
        val defaults = service.getDefaultSettings()
        val withId = JsonObject(defaults + ("id" to JsonPrimitive(game.id)))
        storage.put("game_settings", game.id, encodeToJson(withId))
        return withId
    }

    private fun loadState(): GameStatePayload? =
        storage.get("game_states", game.id)?.let { runCatching { decodeFromJson<GameStatePayload>(it) }.getOrNull() }

    private fun persistIfChanged(beforeGame: JsonElement, beforeSettings: JsonObject, beforeState: GameStatePayload?) {
        if (!beforeGame.jsonEquals(gameTree())) persistGame()
        if (!beforeSettings.jsonEquals(settings)) persistSettings()
        if (state != beforeState) persistState()
    }

    private fun persistGame() {
        storage.put("games", game.id, encodeToJson(game))
    }

    private fun persistSettings() {
        storage.put("game_settings", game.id, encodeToJson(settings))
    }

    private fun persistState() {
        state?.let { storage.put("game_states", game.id, encodeToJson(it)) }
    }

    // ---------------------------------------------------------------- context

    private class Context(
        override val game: Game,
        initialSettings: JsonObject,
        initial: GameStatePayload?
    ) : GameContext {

        private val hadState = initial != null
        private var settingsValue = initialSettings
        private var publicValue: JsonElement = initial?.publicState ?: JsonNull
        private var privateValue: JsonElement? = initial?.privateState

        private var notifyHandler: ((String?, String, JsonElement?) -> Unit)? = null
        private var publishHandler: ((JsonElement) -> Unit)? = null

        fun onNotify(handler: (String?, String, JsonElement?) -> Unit) {
            notifyHandler = handler
        }

        fun onPublishPublic(handler: (JsonElement) -> Unit) {
            publishHandler = handler
        }

        override val settings: JsonObject get() = settingsValue
        override val publicState: JsonElement get() = publicValue
        override val privateState: JsonElement? get() = privateValue

        override fun updatePublicState(value: JsonElement) {
            publicValue = value
        }

        override fun updatePrivateState(value: JsonElement?) {
            privateValue = value
        }

        override fun updateSettings(value: JsonElement) {
            settingsValue = value as? JsonObject ?: JsonObject(emptyMap())
        }

        override fun publishPublicState() {
            publishHandler?.invoke(publicValue)
        }

        fun settingsSnapshot(): JsonObject = settingsValue

        override fun notify(peerId: String?, message: String, params: JsonElement?) {
            notifyHandler?.invoke(peerId, message, params)
        }

        fun snapshot(): GameStatePayload? =
            if (!hadState && publicValue is JsonNull) null
            else GameStatePayload(game.id, publicValue, privateValue)
    }

    private class BotContext(
        override val game: Game,
        override val settings: JsonObject,
        override val publicState: JsonElement,
        override val privateState: JsonElement?
    ) : BotGameContext
}

private fun JsonElement?.playerStates(): JsonArray? =
    ((this as? JsonObject)?.get("playersStates")) as? JsonArray
