# boardgame-kotlin-core

Kotlin replacement for the server half of `boardgame-web-ts`: the protocol API, the portable core
plugin and the Vue SPA that the launcher serves.

## Layout

```
api/         :api        protocol DTOs, plugin SPI, JSON diff/patch, shared utils (published)
back/        :core       portable core plugin: sessions, dispatch, game registry, bots
front/                   Vue SPA (copied from boardgame-web-ts/front)
common/                  TS front library (copied from boardgame-web-ts/common)
```

## Status

Implemented:

- `api/` — protocol DTOs, plugin SPI (`HostServices`/`Storage`/`Peer`/`GameBackService`), JSON diff/patch
  with the front's path grammar, semver, shared utils. Property-style tests for the diff.
- `back/` — portable core plugin: lobby, per-game sessions, `ObjectSync` patches, private-state
  routing, persistence, bots loop, module loading, release install.

Verified: `:api:test`, `:back:compileKotlin`, and an end-to-end run of the 31-step protocol scenario
against the launcher + CAH module (see `tools/e2e-kotlin.mjs` at the workspace root).

Not implemented yet: module download/signature verification polish, admin CLI in the launcher.

## Build / test

```bash
gradle :api:test
gradle :back:jar        # -> back/build/libs/back-<version>.jar (core plugin)
```

## Release packaging

```bash
npm run build-common && npm run build-front   # -> back/public
gradle :back:releaseZip                        # -> back/build/dist/boardgame-core-<v>.zip
```

The asset contains `back/core.jar` (dual-format: class files + `classes.dex` + resources + service
descriptor) and `front/**`. `d8` is taken from the Android SDK (`local.properties` / `ANDROID_HOME`);
the task fails with a clear message when the front build or the SDK is missing.

## TypeScript protocol codegen

```bash
gradle :api:generateTs   # -> common/src/back/generated/protocol.ts (from Kotlin DTOs)
npm run typecheck -w common
```

Models are discovered by scanning the compiled classes for `@GenerateTs`; use
`@GenerateTs(name = "GameState")` or `@GenerateTs(typeLiteral = "…")` when the TS name or a
discriminated `type` literal differs from the Kotlin class name. There is no manual type list.

`common/src/back/dto.ts` and `wsMessages.ts` are now **pure re-exports** of the generated protocol:
DTOs, base models (`GameSettings`, `GamePublicState`, …), requests, responses and sync messages.
Only front-only pieces stay hand-written: the `UserRole` / `GameStatusEnum` / `ConnectStatus`
enums, the front-facing plugin SPI, and the `TypedMessage` / `ObjectSync` helpers.

`GameState.publicState` / `FullGameData.settings` are `any` in the generated protocol (the wire does
not carry typed state); game fronts use their own generated state types (e.g. `CahGamePublicState`).

After changing `:api` DTOs:

```bash
gradle :api:generateTs
git diff --exit-code common/src/back/generated/protocol.ts   # CI drift guard
npm run build-common
npm run build-front
```

## Development build

```bash
gradle devBuild   # :api:generateTs + :back:jar
```

The launcher's `DEV=true` watcher reloads the core as soon as
`back/build/libs/back-0.1.0.jar` changes. IntelliJ IDEA: the shared **Build core (dev)** run
configuration (`.idea/runConfigurations`) runs the same task. Front changes still use
`npm run build-front` above.

## Front build

```bash
npm run build-common
npm run build-front
```

`front/package.json` already points at `boardgame-web-common` via `file:../common`.
