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

Scaffold: `front/` and `common/` copied; Kotlin modules are not created yet.

## Front build

```bash
npm run build-common
npm run build-front
```

`front/package.json` already points at `boardgame-web-common` via `file:../common`.
