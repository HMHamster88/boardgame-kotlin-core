/**
 * Compile-time parity check between the hand-written protocol types (dto.ts / wsMessages.ts) and the
 * types generated from the Kotlin DTOs (generated/protocol.ts).
 *
 * The hand-written types are deliberately narrower in a few places (typed game states, enums, the
 * `Date`-turned-`string` fix), so most checks are directional: the hand-written type must be
 * assignable to the generated wire type. Fields that intentionally differ are asserted explicitly.
 *
 * Run `gradle :api:generateTs` after changing the Kotlin DTOs; `vue-tsc` checks this file.
 */
import type { Game, GameState, GameStaticSettings, GameType, Player, User } from './dto.js'
import type {
    AdminGameMessageResponse,
    AllGamesResponse,
    FullGameData,
    GameCreatedMessage,
    GameDeletedMessage,
    GameMessageResponse,
    HandshakeResponse,
    NotifyGameMessage,
    UpdateFullGameDataResponse
} from './wsMessages.js'
import type * as Gen from './generated/protocol.js'

type Equal<A, B> = [A] extends [B] ? ([B] extends [A] ? true : false) : false
type Extends<A, B> = [A] extends [B] ? true : false
type Assert<T extends true> = T

export type PlayerMatch = Assert<Equal<Player, Gen.Player>>
export type GameStaticSettingsMatch = Assert<Equal<GameStaticSettings, Gen.GameStaticSettings>>
export type GameTypeMatch = Assert<Equal<GameType, Gen.GameType>>

export type UserAssignable = Assert<Extends<User, Gen.User>>
export type GameAssignable = Assert<Extends<Game, Gen.Game>>
export type GameStateAssignable = Assert<Extends<GameState, Gen.GameState>>
export type FullGameDataAssignable = Assert<Extends<FullGameData, Gen.FullGameData>>

export type HandshakeAssignable = Assert<Extends<HandshakeResponse, Gen.HandshakeResponse>>
export type AllGamesAssignable = Assert<Extends<AllGamesResponse, Gen.AllGamesResponse>>
export type GameCreatedAssignable = Assert<Extends<GameCreatedMessage, Gen.GameCreatedMessage>>
export type GameDeletedAssignable = Assert<Extends<GameDeletedMessage, Gen.GameDeletedMessage>>
export type NotifyAssignable = Assert<Extends<NotifyGameMessage, Gen.NotifyGameMessage>>
export type AdminGameAssignable = Assert<Extends<AdminGameMessageResponse, Gen.AdminGameMessageResponse>>
export type UpdateFullGameDataAssignable = Assert<
    Extends<UpdateFullGameDataResponse, Gen.UpdateFullGameDataResponse>
>

// GameMessageResponse always carries an id on the wire; the hand-written base type has it optional.
export type GameMessageResponseAssignable = Assert<
    Extends<Omit<GameMessageResponse, 'id'>, Omit<Gen.GameMessageResponse, 'id'>>
>
export type GameMessageResponseId = Assert<Equal<Gen.GameMessageResponse['id'], string>>

// Explicit representation differences (documented, not drift).
export type RolesAreStrings = Assert<Equal<Gen.User['roles'], string[]>>
export type StatusIsWireString = Assert<Equal<Gen.Game['status'], string>>
export type CreatedIsIsoString = Assert<Equal<Gen.Game['created'], string>>
export type PublicStateIsUntyped = Assert<Equal<Gen.GameState['publicState'], any>>
export type PrivateStateIsUntyped = Assert<Equal<Gen.GameState['privateState'], any>>
export type SyncPathIsOptional = Assert<Equal<Gen.SyncPart['path'], string | undefined>>
export type NotifyParamsAreUntyped = Assert<Equal<Gen.NotifyGameMessage['messageParams'], any>>
export type AdminIdIsOptional = Assert<Equal<Gen.AdminGameMessageResponse['id'], string | undefined>>
export type UpdateErrorIsOptional = Assert<Equal<Gen.UpdateFullGameDataResponse['error'], string | undefined>>
