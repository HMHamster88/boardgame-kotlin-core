package boardgame.core

/**
 * Game modules the core installs on first start, mirroring the TS launcher's default list.
 *
 * Each homepage must point at a repository whose latest release contains a module archive
 * (`back/` + `front/`). Repositories without a release yet are skipped with a log line and retried
 * on the next start, so this list can already contain the planned games.
 */
internal val DEFAULT_GAME_HOMEPAGES = listOf(
    "https://github.com/HMHamster88/boardgame-kotlin-cah",
    "https://github.com/HMHamster88/boardgame-kotlin-catan",
    "https://github.com/HMHamster88/boardgame-kotlin-ticket-to-ride",
    "https://github.com/HMHamster88/boardgame-kotlin-carcassonne"
)
