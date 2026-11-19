import type {
    CreateGameProps,
    Game,
    GamePrivateState,
    GamePublicState,
    GameSettings,
    GameState,
    Player,
    User
} from "./dto.js"
import type { TypedMessage } from "./messageHandler.js"

// Server -> client messages generated from the Kotlin models.
export type {
    AllGamesResponse,
    GameCreatedMessage,
    GameDeletedMessage,
    GameMessageResponse,
    HandshakeResponse,
    NotifyGameMessage,
    UpdateFullGameDataResponse
} from "./generated/protocol.js"

export interface HandshakeRequest extends TypedMessage {
    type: 'HandshakeRequest'
    userId: string
}

export interface GetAllGamesRequest extends TypedMessage {
    type: 'GetAllGamesRequest'
}

export interface GetGameRequest extends TypedMessage {
    type: 'GetGameRequest'
}

export interface GetGameResponse extends TypedMessage {
    type: 'GetGameResponse'
    game: Game
    gameSettings: GameSettings
}

export interface CreateGameRequest extends TypedMessage {
    type: 'CreateGameRequest'
    props: CreateGameProps
}

export interface DeleteGameRequest extends TypedMessage {
    type: 'DeleteGameRequest'
    gameId: string
}

export interface UpdateUserRequest extends TypedMessage {
    type: 'UpdateUserRequest'
    user: Omit<User, 'roles'>
}

export interface ConnectToGameMessage extends TypedMessage {
    type: 'ConnectToGameMessage'
    gameId: string
}

export interface GameMessage {
    id?: string
    type: string
}

export interface GameInfoMessage extends GameMessage {
    type: 'GameInfoMessage',
    game: Game
}

export interface ErorrGameMessage extends GameMessage {
    type: 'ErorrGameMessage',
    message: string,
    messageParams?: any
}

export interface StartGameMessage extends GameMessage {
    type: 'StartGameMessage'
}

export interface JoinGameMessage extends GameMessage {
    type: 'JoinGameMessage'
}

export interface AddBotGameMessage extends GameMessage {
    type: 'AddBotGameMessage'
    player: Player
}

export interface UpdateBotGameMessage extends GameMessage {
    type: 'UpdateBotGameMessage'
    player: Player
}

export interface KickPlayerMessage extends GameMessage {
    type: 'KickPlayerMessage'
    playerId: string
}

export interface GameAction {
    type: string
}

export interface GameActionMessage extends GameMessage {
    type: 'GameActionMessage',
    action: GameAction
}

export interface CrateGameBackupMessage extends GameMessage {
    type: 'CrateGameBackupMessage'
}

export interface GetAdminGameMessage extends GameMessage {
    type: 'GetAdminGameMessage'
    gameId: string
}

export interface FullGameData<P = GamePublicState, S = GamePrivateState> {
    settings: GameSettings
    game: Game
    gameState?: GameState<P, S>
}

export interface AdminGameMessageResponse extends GameMessage {
    type: 'AdminGameMessageResponse'
    fullGameData: FullGameData
}

export interface UpdateFullGameDataMessage extends GameMessage {
    type: 'UpdateFullGameDataMessage'
    fullGameData: FullGameData
}

export interface ForceUpdateGameData extends GameMessage {
    type: 'ForceUpdateGameData'
}

