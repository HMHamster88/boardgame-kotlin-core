package boardgame.api

import boardgame.api.codegen.TsTypeSpec
import boardgame.api.codegen.generateTs
import java.io.File

/**
 * Entry point for `:api:generateTs`: writes the protocol TypeScript types consumed by the front.
 */
object TsExport {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = File(args[0])
        val types = listOf(
            // base models
            TsTypeSpec(GameSettings.serializer(), "GameSettings"),
            TsTypeSpec(PlayerPublicState.serializer(), "PlayerPublicState"),
            TsTypeSpec(PlayerPrivateState.serializer(), "PlayerPrivateState"),
            TsTypeSpec(GameStatistics.serializer(), "GameStatistics"),
            TsTypeSpec(GamePublicState.serializer(), "GamePublicState"),
            TsTypeSpec(GamePrivateState.serializer(), "GamePrivateState"),
            TsTypeSpec(GameStatePayload.serializer(), "GameState"),
            TsTypeSpec(FullGameData.serializer(), "FullGameData"),

            // shared DTOs
            TsTypeSpec(User.serializer(), "User"),
            TsTypeSpec(GameType.serializer(), "GameType"),
            TsTypeSpec(Player.serializer(), "Player"),
            TsTypeSpec(Game.serializer(), "Game"),
            TsTypeSpec(GameStaticSettings.serializer(), "GameStaticSettings"),
            TsTypeSpec(CreateGameProps.serializer(), "CreateGameProps"),
            TsTypeSpec(UserPayload.serializer(), "UserPayload"),

            // sync
            TsTypeSpec(SyncPart.serializer(), "SyncPart"),
            TsTypeSpec(ObjectSyncMessage.serializer(), "ObjectSyncMessage", "ObjectSyncMessage"),

            // client -> server
            TsTypeSpec(HandshakeRequest.serializer(), "HandshakeRequest", "HandshakeRequest"),
            TsTypeSpec(GetAllGamesRequest.serializer(), "GetAllGamesRequest", "GetAllGamesRequest"),
            TsTypeSpec(GetGameRequest.serializer(), "GetGameRequest", "GetGameRequest"),
            TsTypeSpec(GetGameResponse.serializer(), "GetGameResponse", "GetGameResponse"),
            TsTypeSpec(CreateGameRequest.serializer(), "CreateGameRequest", "CreateGameRequest"),
            TsTypeSpec(DeleteGameRequest.serializer(), "DeleteGameRequest", "DeleteGameRequest"),
            TsTypeSpec(UpdateUserRequest.serializer(), "UpdateUserRequest", "UpdateUserRequest"),
            TsTypeSpec(ConnectToGameMessage.serializer(), "ConnectToGameMessage", "ConnectToGameMessage"),
            TsTypeSpec(GameMessage.serializer(), "GameMessage"),
            TsTypeSpec(GameInfoMessage.serializer(), "GameInfoMessage", "GameInfoMessage"),
            TsTypeSpec(ErorrGameMessage.serializer(), "ErorrGameMessage", "ErorrGameMessage"),
            TsTypeSpec(StartGameMessage.serializer(), "StartGameMessage", "StartGameMessage"),
            TsTypeSpec(JoinGameMessage.serializer(), "JoinGameMessage", "JoinGameMessage"),
            TsTypeSpec(AddBotGameMessage.serializer(), "AddBotGameMessage", "AddBotGameMessage"),
            TsTypeSpec(UpdateBotGameMessage.serializer(), "UpdateBotGameMessage", "UpdateBotGameMessage"),
            TsTypeSpec(KickPlayerMessage.serializer(), "KickPlayerMessage", "KickPlayerMessage"),
            TsTypeSpec(GameAction.serializer(), "GameAction"),
            TsTypeSpec(GameActionMessage.serializer(), "GameActionMessage", "GameActionMessage"),
            TsTypeSpec(CrateGameBackupMessage.serializer(), "CrateGameBackupMessage", "CrateGameBackupMessage"),
            TsTypeSpec(GetAdminGameMessage.serializer(), "GetAdminGameMessage", "GetAdminGameMessage"),
            TsTypeSpec(UpdateFullGameDataMessage.serializer(), "UpdateFullGameDataMessage", "UpdateFullGameDataMessage"),
            TsTypeSpec(ForceUpdateGameData.serializer(), "ForceUpdateGameData", "ForceUpdateGameData"),

            // server -> client
            TsTypeSpec(HandshakeResponse.serializer(), "HandshakeResponse", "HandshakeResponse"),
            TsTypeSpec(AllGamesResponse.serializer(), "AllGamesResponse", "AllGamesResponse"),
            TsTypeSpec(GameCreatedMessage.serializer(), "GameCreatedMessage", "GameCreatedMessage"),
            TsTypeSpec(GameDeletedMessage.serializer(), "GameDeletedMessage", "GameDeletedMessage"),
            TsTypeSpec(NotifyGameMessage.serializer(), "NotifyGameMessage", "NotifyGameMessage"),
            TsTypeSpec(GameMessageResponse.serializer(), "GameMessageResponse", "GameMessageResponse"),
            TsTypeSpec(
                AdminGameMessageResponse.serializer(),
                "AdminGameMessageResponse",
                "AdminGameMessageResponse"
            ),
            TsTypeSpec(
                UpdateFullGameDataResponse.serializer(),
                "UpdateFullGameDataResponse",
                "UpdateFullGameDataResponse"
            )
        )
        output.parentFile?.mkdirs()
        output.writeText(generateTs(types))
        println("Generated ${output.absolutePath}")
    }
}
