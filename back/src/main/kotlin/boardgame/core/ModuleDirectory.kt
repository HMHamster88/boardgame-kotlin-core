package boardgame.core

/**
 * Picks the module name for a game module from its release asset URL.
 *
 * Release assets are versioned (`cah-0.1.0.zip`): the trailing version is stripped to get the module
 * name, and the core installs the archive as `games-modules/<module>/<version>/` (the version comes
 * from the release tag). The front URL is `/games-modules/<module>/<version>/front/...`.
 */
internal fun moduleDirectoryName(downloadUrl: String): String {
    val base = downloadUrl.substringAfterLast('/').substringBeforeLast('.')
    val stable = base.replace(Regex("[-_]v?\\d+(\\.\\d+)*(-SNAPSHOT)?$", RegexOption.IGNORE_CASE), "")
    return (stable.ifBlank { base }).replace('.', '_')
}
