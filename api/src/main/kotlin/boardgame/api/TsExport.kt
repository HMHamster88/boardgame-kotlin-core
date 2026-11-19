package boardgame.api

import boardgame.api.codegen.TsScanner
import java.io.File

/**
 * Entry point for `:api:generateTs`: scans compiled classes for `@GenerateTs` models and writes the
 * protocol TypeScript types. Args: `<output.ts> <classesDir>...`.
 */
object TsExport {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = File(args.firstOrNull() ?: error("usage: TsExport <output> <classesDir>..."))
        val classDirs = args.drop(1).map(::File).filter { it.isDirectory }
        val count = TsScanner.generate(output, classDirs)
        println("Generated $count types into ${output.absolutePath}")
    }
}
