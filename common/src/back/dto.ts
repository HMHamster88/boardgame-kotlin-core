import type { ObjectSync } from "./objectSync"
import type { GameAction } from "./wsMessages"

// Wire DTOs are generated from the Kotlin `:api` models (`gradle :api:generateTs`).
import type { Game, GameStaticSettings, GameType, Player, User } from "./generated/protocol.js"
export type { Game, GameStaticSettings, GameType, Player, User }

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


export interface CreateGameProps {
    name: string
    type: string
    owner: string
}

export interface GameSettings {
    id: string
}


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

export interface PlayerPublicState {
    playerId: string
    points: number
}

export interface PlayerPrivateState {
    playerId: string
}

export interface GameStatistics {
    turnCount: number
}

export interface GamePublicState {
    activePlayerIndex: number
    playersStates: PlayerPublicState[] | null
    winnersIds: string[]
    statistics: GameStatistics
}

export interface GamePrivateState {
    playersStates: PlayerPrivateState[] | null
}

/**
 * Generic so game fronts can narrow the states: `GameState<CahGamePublicState, CahGamePrivateState>`.
 * The wire shape is generated (`generated/protocol.ts`); the generics keep the typed admin/game views.
 */
export interface GameState<P = GamePublicState, S = GamePrivateState> {
    id: string // equals to game id
    publicState: P
    privateState?: S
}