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

## Version

The version is resolved in this order:

1. `-PcoreVersion=…`
2. `RELEASE_VERSION` environment variable
3. the latest reachable git tag (`v0.2.0` → `0.2.0`)
4. default `0.1.0`

Tag the release before building (`git tag v0.2.0 && git push --tags`, or create the release on GitHub
and run `git fetch --tags`); the **Build core release** configuration then produces
`boardgame-core-0.2.0.zip` and `back-0.2.0.jar` with `version.properties = 0.2.0` automatically.
Pass `-PcoreVersion=…` in the run configuration's Arguments field for a one-off override.

The git tag must match the installed `version.properties` (the launcher strips the `v` and compares
them; a mismatch makes it re-download on every update check).

## Default game modules

On first start the core installs the modules listed in
`back/src/main/kotlin/boardgame/core/DefaultGames.kt`: CAH now, plus the planned Catan / Ticket to
Ride / Carcassonne repositories (they are skipped with a log line until they have a release).
Missing modules are downloaded from their latest GitHub release; when the host enables update checks
(the launcher defaults `CHECK_FOR_UPDATES` to true outside dev mode) installed ones are updated too.
`SKIP_DEFAULT_GAMES=1` disables the step (offline/tests).

Modules install as `games-modules/<module>/<version>/` (same shape as `core/<version>/`); the loader
picks the newest version and still accepts the old `games-modules/<module>/back` layout.

Installs are written next to any running jar under a unique name and the newest jar is used, so an
update never tries to replace a jar the JVM has open (Windows).

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

`@GenerateTs` also works on Kotlin enums: they become string TS enums, and map keys use the generated
enum type (`Record<CatanResourceType, number>`).

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
gradle devBuild   # :api:generateTs + npm run build-all + :back:jar
```

This regenerates the protocol types, builds the common package and Vue front into `back/public`, and
builds the core plugin jar. The launcher's `DEV=true` watcher reloads the core as soon as
`back/build/libs/back-0.1.0.jar` changes; the rebuilt front is served directly (refresh the page).
Requires `npm` on `PATH`; for back-only iterations run `gradle :back:jar` to skip the front build.

IntelliJ IDEA: the shared **Build core (dev)** run configuration (`.idea/runConfigurations`) runs
`devBuild`.

## Front build

```bash
npm run build-common
npm run build-front
```

`front/package.json` already points at `boardgame-web-common` via `file:../common`.

## Release build

```bash
gradle releaseBuild   # -> back/build/dist/boardgame-core-<version>.zip
```

Builds the front, the dual-format plugin jar (`back/core.jar`: class files + `classes.dex` produced
with `d8`) and packages them into the release zip. Requires `npm` and the Android SDK (`sdk.dir` in
`local.properties` or `ANDROID_HOME`). IntelliJ IDEA: the shared **Build core release** run
configuration (`.idea/runConfigurations`) runs the same task.

## Publishing a release

```bash
gradle publishRelease              # latest release
gradle publishRelease -PghDraft=true  # draft for review
```

Runs `releaseBuild`, then uses the GitHub CLI to push the local `v<version>` tag (the version comes
from that tag) and create the GitHub release with generated notes and the core zip as its only asset.
Requires `gh` installed and authenticated (`gh auth login` or `GH_TOKEN`), a clean working tree, and
`HEAD` at the release tag. Draft releases are not returned by GitHub's latest-release endpoint, so the
launcher will not pick them up until published.
