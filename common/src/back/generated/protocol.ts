// Generated from Kotlin models by the module's generateTs task. Do not edit.
// The hand-written types in the front are validated against this file.

export interface User {
    id: string
    name: string
    color: string
    roles: string[]
}

export interface GameType {
    type: string
    localizedName: any
    diectoryName: string
}

export interface Player {
    userId: string
    name: string
    color: string
    online: boolean
    isBot: boolean
}

export interface Game {
    id: string
    owner: string
    name: string
    type: string
    players: Player[]
    status: string
    created: string
}

export interface GameStaticSettings {
    minPlayers: number
    maxPlayers: number
}

export interface GameState {
    id: string
    publicState: any
    privateState?: any
}

export interface FullGameData {
    settings: any
    game: Game
    gameState?: GameState
}

export interface SyncPart {
    path?: string
    value: any
}

export interface ObjectSyncMessage {
    type: 'ObjectSyncMessage'
    objectId: string
    parts: SyncPart[]
}

export interface HandshakeResponse {
    type: 'HandshakeResponse'
    user: User
    gameTypes: GameType[]
}

export interface AllGamesResponse {
    type: 'AllGamesResponse'
    games: Game[]
}

export interface GameCreatedMessage {
    type: 'GameCreatedMessage'
    game: Game
}

export interface GameDeletedMessage {
    type: 'GameDeletedMessage'
    gameId: string
}

export interface NotifyGameMessage {
    type: 'NotifyGameMessage'
    message: string
    messageParams?: any
}

export interface GameMessageResponse {
    type: 'GameMessageResponse'
    id: string
    response: any
}

export interface AdminGameMessageResponse {
    type: 'AdminGameMessageResponse'
    id?: string
    fullGameData: FullGameData
}

export interface UpdateFullGameDataResponse {
    type: 'UpdateFullGameDataResponse'
    id?: string
    error?: string
}

