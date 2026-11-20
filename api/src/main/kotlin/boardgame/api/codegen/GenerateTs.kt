package boardgame.api.codegen

/**
 * Marks a `@Serializable` class for TypeScript generation.
 *
 * @param name TS interface name; defaults to the Kotlin class name.
 * @param typeLiteral value for a `type` discriminator field; when empty the field stays `string`.
 * @param asUnion emit enums as a string-literal union (`export type X = 'a' | 'b'`) instead of an
 *   `enum`, which keeps wire values as plain strings on the front.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GenerateTs(
    val name: String = "",
    val typeLiteral: String = "",
    val asUnion: Boolean = false
)
