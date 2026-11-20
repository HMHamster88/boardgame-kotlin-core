// Generated from Kotlin models by the module's generateTs task. Do not edit.
// Only model types shared with the back-end are generated; front-only helpers stay hand-written.

export interface AddBotGameMessage {
    type: 'AddBotGameMessage'
    player: Player
    id?: string
}

export interface AdminGameMessageResponse {
    type: 'AdminGameMessageResponse'
    id?: string
    fullGameData: FullGameData
}

export interface AllGamesResponse {
    type: 'AllGamesResponse'
    games: Game[]
}

export interface ChatErrorResponse {
    type: 'ChatErrorResponse'
    id?: string
    code: string
    message: string
}

export interface ChatHistoryRequest {
    type: 'ChatHistoryRequest'
    id?: string
    roomId: string
    beforeSeq?: number
    limit?: number
}

export interface ChatHistoryResponse {
    type: 'ChatHistoryResponse'
    id?: string
    roomId: string
    messages: ChatMessage[]
    hasMore: boolean
}

export interface ChatMessage {
    id: string
    roomId: string
    seq: number
    userId: string
    userName: string
    userColor: string
    text: string
    createdAt: string
    kind: ChatMessageKind
    clientMessageId?: string
    deleted: boolean
}

export type ChatMessageKind = 'USER' | 'SYSTEM'

export interface ChatMessageReceived {
    type: 'ChatMessageReceived'
    message: ChatMessage
}

export interface ChatSendRequest {
    type: 'ChatSendRequest'
    id?: string
    roomId: string
    text: string
    clientMessageId?: string
}

export interface ConnectToGameMessage {
    type: 'ConnectToGameMessage'
    gameId: string
    id?: string
}

export interface CrateGameBackupMessage {
    type: 'CrateGameBackupMessage'
    id?: string
}

export interface CreateGameProps {
    name: string
    type: string
    owner: string
}

export interface CreateGameRequest {
    type: 'CreateGameRequest'
    props: CreateGameProps
    id?: string
}

export interface DeleteGameRequest {
    type: 'DeleteGameRequest'
    gameId: string
    id?: string
}

export interface ErorrGameMessage {
    type: 'ErorrGameMessage'
    message: string
    messageParams?: any
    id?: string
}

export interface ForceUpdateGameData {
    type: 'ForceUpdateGameData'
    id?: string
}

export interface FullGameData {
    settings: any
    game: Game
    gameState?: GameState
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

export interface GameAction {
    type: string
}

export interface GameActionMessage {
    type: 'GameActionMessage'
    action: any
    id?: string
}

export interface GameCreatedMessage {
    type: 'GameCreatedMessage'
    game: Game
}

export interface GameDeletedMessage {
    type: 'GameDeletedMessage'
    gameId: string
}

export interface GameInfoMessage {
    type: 'GameInfoMessage'
    game: Game
    id?: string
}

export interface GameMessage {
    type: string
    id?: string
}

export interface GameMessageResponse {
    type: 'GameMessageResponse'
    id: string
    response: any
}

export interface GamePrivateState {
    playersStates?: PlayerPrivateState[]
}

export interface GamePublicState {
    activePlayerIndex: number
    playersStates?: PlayerPublicState[]
    winnersIds: string[]
    statistics: GameStatistics
}

export interface GameSettings {
    id: string
}

export interface GameState {
    id: string
    publicState: any
    privateState?: any
}

export interface GameStaticSettings {
    minPlayers: number
    maxPlayers: number
}

export interface GameStatistics {
    turnCount: number
}

export interface GameType {
    type: string
    localizedName: any
    directoryName: string
}

export interface GetAdminGameMessage {
    type: 'GetAdminGameMessage'
    gameId: string
    id?: string
}

export interface GetAllGamesRequest {
    type: 'GetAllGamesRequest'
}

export interface GetGameRequest {
    type: 'GetGameRequest'
}

export interface GetGameResponse {
    type: 'GetGameResponse'
    game: Game
    gameSettings: any
}

export interface HandshakeRequest {
    type: 'HandshakeRequest'
    userId: string
}

export interface HandshakeResponse {
    type: 'HandshakeResponse'
    user: User
    gameTypes: GameType[]
}

export interface JoinGameMessage {
    type: 'JoinGameMessage'
    id?: string
}

export interface KickPlayerMessage {
    type: 'KickPlayerMessage'
    playerId: string
    id?: string
}

export interface NotifyGameMessage {
    type: 'NotifyGameMessage'
    message: string
    messageParams?: any
}

export interface ObjectSyncMessage {
    type: 'ObjectSyncMessage'
    objectId: string
    parts: SyncPart[]
}

export interface Player {
    userId: string
    name: string
    color: string
    online: boolean
    isBot: boolean
}

export interface PlayerPrivateState {
    playerId: string
}

export interface PlayerPublicState {
    playerId: string
    points: number
}

export interface StartGameMessage {
    type: 'StartGameMessage'
    id?: string
}

export interface SyncPart {
    path?: string
    value: any
}

export interface UpdateBotGameMessage {
    type: 'UpdateBotGameMessage'
    player: Player
    id?: string
}

export interface UpdateFullGameDataMessage {
    type: 'UpdateFullGameDataMessage'
    fullGameData: FullGameData
    id?: string
}

export interface UpdateFullGameDataResponse {
    type: 'UpdateFullGameDataResponse'
    id?: string
    error?: string
}

export interface UpdateUserRequest {
    type: 'UpdateUserRequest'
    user: UserPayload
    id?: string
}

export interface User {
    id: string
    name: string
    color: string
    roles: string[]
}

export interface UserPayload {
    id: string
    name: string
    color: string
}

