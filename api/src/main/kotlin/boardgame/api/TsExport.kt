package boardgame.api

import boardgame.api.codegen.TsTypeSpec
import boardgame.api.codegen.generateTs
import java.io.File

/**
 * Entry point for `:api:generateTs`: writes the protocol TypeScript types consumed by the front.
 * The hand-written `common/src/back/{dto,wsMessages}.ts` are validated against this file.
 */
object TsExport {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = File(args[0])
        val types = listOf(
            TsTypeSpec(User.serializer(), "User"),
            TsTypeSpec(GameType.serializer(), "GameType"),
            TsTypeSpec(Player.serializer(), "Player"),
            TsTypeSpec(Game.serializer(), "Game"),
            TsTypeSpec(GameStaticSettings.serializer(), "GameStaticSettings"),
            TsTypeSpec(GameStatePayload.serializer(), "GameState"),
            TsTypeSpec(FullGameData.serializer(), "FullGameData"),
            TsTypeSpec(SyncPart.serializer(), "SyncPart"),
            TsTypeSpec(ObjectSyncMessage.serializer(), "ObjectSyncMessage", "ObjectSyncMessage"),
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
