package boardgame.core

/**
 * Picks the install directory for a game module from its release asset URL.
 *
 * Release assets are versioned (`cah-0.1.0.zip`), but the extracted directory (and therefore the
 * front URL, `/games-modules/<directory>/front/...`) must stay stable across updates: strip a
 * trailing version (`-0.1.0`, `-v1.2.3`, `-0.1.0-SNAPSHOT`).
 */
internal fun moduleDirectoryName(downloadUrl: String): String {
    val base = downloadUrl.substringAfterLast('/').substringBeforeLast('.')
    val stable = base.replace(Regex("[-_]v?\\d+(\\.\\d+)*(-SNAPSHOT)?$", RegexOption.IGNORE_CASE), "")
    return (stable.ifBlank { base }).replace('.', '_')
}
