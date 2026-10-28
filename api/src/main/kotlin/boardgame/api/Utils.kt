package boardgame.api

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val isoMillis: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

/** ISO-8601 with milliseconds, matching JS `Date.prototype.toISOString()`. */
fun nowIso(): String = isoMillis.format(Instant.now())

fun <T> List<T>.shuffledBy(random: Random = Random.Default): List<T> {
    val result = toMutableList()
    for (index in result.lastIndex downTo 1) {
        val other = random.nextInt(index + 1)
        val tmp = result[index]
        result[index] = result[other]
        result[other] = tmp
    }
    return result
}

fun <T> List<T>.randomElement(random: Random = Random.Default): T? =
    if (isEmpty()) null else this[random.nextInt(size)]

fun <T> MutableList<T>.removeElement(element: T): Boolean = remove(element)

fun <T> MutableList<T>.removeElements(elements: Collection<T>) {
    elements.forEach { remove(it) }
}

fun rangeList(count: Int): List<Int> = if (count <= 0) emptyList() else (0 until count).toList()
