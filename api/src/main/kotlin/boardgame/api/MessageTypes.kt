package boardgame.api

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
