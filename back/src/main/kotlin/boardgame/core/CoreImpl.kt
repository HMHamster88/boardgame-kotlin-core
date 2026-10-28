package boardgame.core

import boardgame.api.AllGamesResponse
import boardgame.api.AdminGameMessageResponse
import boardgame.api.Game
import boardgame.api.GameBackModule
import boardgame.api.GameBackService
import boardgame.api.GameCreatedMessage
import boardgame.api.GameCore
import boardgame.api.GameDeletedMessage
import boardgame.api.GameModuleInfo
import boardgame.api.GameType
import boardgame.api.HandshakeResponse
import boardgame.api.HostServices
import boardgame.api.MessageTypes
import boardgame.api.Peer
import boardgame.api.Roles
import boardgame.api.User
import boardgame.api.UpdateFullGameDataResponse
import boardgame.api.apiJson
import boardgame.api.arr
import boardgame.api.decodeFromJson
import boardgame.api.decodeTree
import boardgame.api.encodeToJson
import boardgame.api.nowIso
import boardgame.api.obj
import boardgame.api.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

class CoreImpl(private val host: HostServices) : GameCore {

    private val peers = ConcurrentHashMap<String, Peer>()
    private val peerGames = ConcurrentHashMap<String, String>()
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val services = ConcurrentHashMap<String, GameBackService>()
    private val modules = ConcurrentHashMap<String, GameModuleInfo>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val storage get() = host.storage

    init {
        loadInstalledModules()
    }

    override fun attach(peer: Peer) {
        peers[peer.userId] = peer
        val user = loadOrCreateUser(peer.userId)
        peer.onMessage { text -> scope.launch { handle(peer, text) } }
        peer.onClose { detach(peer) }
        peer.send(encodeToJson(HandshakeResponse(user = user, gameTypes = gameTypes())))
    }

    override fun detach(peer: Peer) {
        // A reconnect registers a new Peer instance for the same user; the old close handler must
        // not remove the new one.
        if (peers[peer.userId] !== peer) return
        peers.remove(peer.userId)
        peerGames.remove(peer.userId)?.let { gameId -> sessions[gameId]?.detach(peer) }
    }

    override fun gameModules(): List<GameModuleInfo> = modules.values.sortedBy { it.type }

    override suspend fun shutdown() {
        sessions.values.forEach { it.shutdown() }
        scope.cancel()
    }

    // ---------------------------------------------------------------- messages

    private suspend fun handle(peer: Peer, text: String) {
        val message = runCatching { apiJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val type = message.str("type") ?: return

        when (type) {
            MessageTypes.GET_ALL_GAMES -> send(peer, AllGamesResponse(games = allGames()))
            MessageTypes.CREATE_GAME -> createGame(peer, message)
            MessageTypes.DELETE_GAME -> deleteGame(message)
            MessageTypes.UPDATE_USER -> updateUser(peer, message)
            MessageTypes.CONNECT_TO_GAME -> connectToGame(peer, message)
            MessageTypes.GET_ADMIN_GAME -> adminGame(peer, message)
            MessageTypes.UPDATE_FULL_GAME_DATA -> updateFullGameData(peer, message)
        }

        // A session sees every message from its peers too, like the TS implementation.
        peerGames[peer.userId]?.let { gameId -> sessions[gameId]?.handle(peer, message, text) }
    }

    private fun createGame(peer: Peer, message: JsonObject) {
        val props = message.obj("props") ?: return
        val type = props.str("type") ?: return
        val name = props.str("name") ?: "Game"
        val game = Game(
            id = UUID.randomUUID().toString(),
            owner = peer.userId,
            name = name,
            type = type,
            created = nowIso()
        )
        storage.put("games", game.id, encodeToJson(game))
        broadcast(GameCreatedMessage(game = game))
    }

    private fun deleteGame(message: JsonObject) {
        val gameId = message.str("gameId") ?: return
        storage.delete("games", gameId)
        storage.delete("game_settings", gameId)
        storage.delete("game_states", gameId)
        sessions.remove(gameId)?.shutdown()
        broadcast(GameDeletedMessage(gameId = gameId))
    }

    private fun updateUser(peer: Peer, message: JsonObject) {
        val payload = message.obj("user") ?: return
        val id = payload.str("id") ?: peer.userId
        val name = payload.str("name") ?: return
        val color = payload.str("color") ?: return
        val existing = storage.get("users", id)?.let { decodeFromJson<User>(it) }
        storage.put("users", id, encodeToJson(User(id, name, color, existing?.roles ?: emptyList())))
    }

    private fun connectToGame(peer: Peer, message: JsonObject) {
        val gameId = message.str("gameId") ?: return
        val session = sessionFor(gameId) ?: return
        val previous = peerGames.put(peer.userId, gameId)
        if (previous != null && previous != gameId) sessions[previous]?.detach(peer)
        session.attach(peer)
    }

    private fun adminGame(peer: Peer, message: JsonObject) {
        val gameId = message.str("gameId") ?: return
        val session = sessionFor(gameId) ?: return
        send(peer, AdminGameMessageResponse(id = message.str("id"), fullGameData = session.fullGameData()))
    }

    private fun updateFullGameData(peer: Peer, message: JsonObject) {
        val id = message.str("id")
        val data = message.obj("fullGameData")?.let { runCatching { decodeTree<boardgame.api.FullGameData>(it) }.getOrNull() }
        if (data == null) {
            send(peer, UpdateFullGameDataResponse(id = id, error = "invalid payload"))
            return
        }
        val user = loadOrCreateUser(peer.userId)
        if (Roles.ADMIN !in user.roles) {
            send(peer, UpdateFullGameDataResponse(id = id, error = "missing ADMIN role"))
            return
        }
        val session = sessionFor(data.game.id)
        if (session == null) {
            send(peer, UpdateFullGameDataResponse(id = id, error = "unknown game"))
            return
        }
        scope.launch { session.updateFullGameData(data) }
        send(peer, UpdateFullGameDataResponse(id = id, error = null))
    }

    // ---------------------------------------------------------------- sessions

    private fun sessionFor(gameId: String): GameSession? {
        sessions[gameId]?.let { return it }
        val game = storage.get("games", gameId)?.let { runCatching { decodeFromJson<Game>(it) }.getOrNull() } ?: return null
        val service = services[game.type] ?: return null
        return sessions.computeIfAbsent(gameId) {
            GameSession(
                initialGame = game,
                host = host,
                service = service,
                userProvider = { id ->
                    storage.get("users", id)?.let { runCatching { decodeFromJson<User>(it) }.getOrNull() }
                },
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            )
        }
    }

    private fun allGames(): List<Game> =
        storage.getAll("games").mapNotNull { runCatching { decodeFromJson<Game>(it) }.getOrNull() }

    private fun loadOrCreateUser(id: String): User {
        storage.get("users", id)?.let { return decodeFromJson(it) }
        val user = User(id = id, name = "Player", color = "#FF0000")
        storage.put("users", id, encodeToJson(user))
        return user
    }

    private fun gameTypes(): List<GameType> = modules.values.sortedBy { it.type }.map { module ->
        GameType(
            type = module.type,
            localizedName = services[module.type]?.localizedName ?: JsonObject(emptyMap()),
            directoryName = module.directory
        )
    }

    // ---------------------------------------------------------------- modules

    private fun loadInstalledModules() {
        val gamesDir = File(host.platform.dataDir, "games-modules")
        if (!gamesDir.isDirectory) return
        gamesDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { moduleDir ->
            val backDir = File(moduleDir, "back")
            val jar = backDir.listFiles()?.firstOrNull { it.extension == "jar" } ?: return@forEach
            runCatching {
                val module = host.loadPlugin(jar, GameBackModule::class.java)
                val service = module.createService()
                val info = GameModuleInfo(
                    type = service.type,
                    version = service.version,
                    apiVersion = 1,
                    directory = moduleDir.name,
                    homepage = service.homepage
                )
                services[service.type] = service
                modules[service.type] = info
                log("Loaded module ${info.type} ${info.version} from ${moduleDir.name}")
            }.onFailure { log("Failed to load module ${moduleDir.name}: ${it.message}") }
        }
    }

    override suspend fun installGameModule(homepage: String): GameModuleInfo {
        val releaseUrl = homepage.trimEnd('/')
            .replace("github.com/", "api.github.com/repos/") + "/releases/latest"
        val release = host.http.getText(releaseUrl)?.let { runCatching { apiJson.parseToJsonElement(it).jsonObject }.getOrNull() }
            ?: error("failed to fetch latest release for $homepage")
        val tag = release.str("tag_name") ?: error("release has no tag_name")
        val assets = release.arr("assets") ?: error("release has no assets")
        val asset = assets.firstOrNull()?.jsonObject ?: error("release has no assets")
        val downloadUrl = asset.str("browser_download_url") ?: error("asset has no download url")
        val bytes = host.http.getBytes(downloadUrl) ?: error("failed to download $downloadUrl")

        val directory = downloadUrl.substringAfterLast('/').substringBeforeLast('.').replace('.', '_')
        val gamesDir = File(host.platform.dataDir, "games-modules").apply { mkdirs() }
        val target = File(gamesDir, directory)
        val temp = File(gamesDir, "$directory.tmp")
        if (temp.exists()) temp.deleteRecursively()
        temp.mkdirs()
        unzip(bytes, temp)
        if (target.exists()) target.deleteRecursively()
        temp.renameTo(target)

        val jar = File(target, "back").listFiles()?.firstOrNull { it.extension == "jar" }
            ?: error("installed module has no back/*.jar")
        val module = host.loadPlugin(jar, GameBackModule::class.java)
        val service = module.createService()
        services[service.type] = service
        val info = GameModuleInfo(service.type, service.version, 1, directory, homepage)
        modules[service.type] = info

        storage.put(
            "game_modules",
            info.type,
            buildJsonObject {
                put("id", info.type)
                put("type", info.type)
                put("directory", info.directory)
                put("version", info.version)
                put("homepage", info.homepage)
                put("tag", tag)
            }.toString()
        )
        log("Installed module ${info.type} ${info.version}")
        return info
    }

    private fun unzip(bytes: ByteArray, target: File) {
        val root = target.canonicalFile
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val file = File(root, entry.name)
                if (!file.canonicalPath.startsWith(root.path + File.separator)) continue
                if (entry.isDirectory) file.mkdirs() else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private inline fun <reified T> send(peer: Peer, value: T) {
        peer.send(encodeToJson(value))
    }

    private inline fun <reified T> broadcast(value: T) {
        val text = encodeToJson(value)
        peers.values.forEach { it.send(text) }
    }

    private fun log(message: String) {
        host.console?.println("[core] $message") ?: println("[core] $message")
    }
}
