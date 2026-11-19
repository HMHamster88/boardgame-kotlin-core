import type { ObjectSync } from "./objectSync"
import type { GameAction } from "./wsMessages"
import type {
    Game,
    GamePublicState,
    GameSettings,
    GameState,
    GameStaticSettings,
    PlayerPrivateState
} from "./generated/protocol.js"

// Wire DTOs and base models are generated from the Kotlin `:api` models (`gradle :api:generateTs`).
export type {
    CreateGameProps,
    Game,
    GamePrivateState,
    GamePublicState,
    GameSettings,
    GameState,
    GameStaticSettings,
    GameStatistics,
    GameType,
    Player,
    PlayerPrivateState,
    PlayerPublicState,
    User
} from "./generated/protocol.js"

export enum ConnectStatus {
    CONNECTING = 'CONNECTING',
    CONNECTED = 'CONNECTED',
    DISCONNECTED = 'DISCONNECTED'
}

export enum UserRole {
    ADMIN = 'ADMIN'
}

export enum GameStatusEnum {
    CREATED = "CREATED",
    STARTED = "STARTED",
    FINISHED = "FINISHED",
}

// Front-facing plugin SPI (only relevant to game front-end modules; the back-end SPI is Kotlin).
export interface GameContext {
    game: Game
    gameSync: ObjectSync<Game>
    gameSettings: GameSettings
    gameSettingsSync: ObjectSync<GameSettings>
    gameState: GameState | undefined
    gamePublicStateSync: ObjectSync<GamePublicState> | undefined
    playerPrivateStateSync: Map<string, ObjectSync<PlayerPrivateState>>
    sendNotify(peerId: string | undefined, message: string, messageParams: any | undefined): void
}

export interface BotGameContext {
    game: Game
    gameSettings: GameSettings
    gameState: GameState
    performGameAction: (action: GameAction, playerId: string) => Promise<void>
}

export interface GameBackService {
    type: string
    localizedName: any
    gameStaticSettings: GameStaticSettings
    getDefaultSettings(): GameSettings
    performAction(gameContext: GameContext, gameAction: GameAction, playerId: string): Promise<any>
    startGame(game: Game, gameSettings: GameSettings): GameState
    runBotsActions(botGameContext: BotGameContext): Promise<void>
    version: string
    homepage: string
}

export interface GameBackModule {
    getGameBackService: () => GameBackService;
}
