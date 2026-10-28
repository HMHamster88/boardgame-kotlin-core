package boardgame.api

/** Minimal semver comparison for release tags (`1.2.3`, optional `-pre` / `+build`). */
object Semver {
    /** Returns <0, 0 or >0 like `semver.compare`. Invalid versions sort as 0.0.0. */
    fun compare(left: String, right: String): Int {
        val a = parse(left)
        val b = parse(right)
        for (index in 0 until maxOf(a.size, b.size)) {
            val result = (a.getOrNull(index) ?: 0).compareTo(b.getOrNull(index) ?: 0)
            if (result != 0) return result
        }
        return 0
    }

    fun isNewer(candidate: String, installed: String): Boolean = compare(candidate, installed) > 0

    private fun parse(version: String): List<Int> {
        val core = version.trim().removePrefix("v").substringBefore('-').substringBefore('+')
        val parts = core.split('.').map { it.toIntOrNull() ?: 0 }
        return if (parts.isEmpty()) listOf(0) else parts
    }
}
