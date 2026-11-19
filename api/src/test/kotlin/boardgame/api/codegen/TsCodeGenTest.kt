package boardgame.api.codegen

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertContains

class TsCodeGenTest {

    @Serializable
    enum class TestColor { RED, GREEN }

    @Serializable
    data class TestPalette(val main: TestColor, val counts: Map<TestColor, Int>)

    @Test
    fun emitsEnumsAndEnumKeyedMaps() {
        val ts = generateTs(
            listOf(
                TsTypeSpec(TestColor.serializer(), "TestColor"),
                TsTypeSpec(TestPalette.serializer(), "TestPalette")
            )
        )
        assertContains(ts, "export enum TestColor {")
        assertContains(ts, "RED = 'RED'")
        assertContains(ts, "main: TestColor")
        assertContains(ts, "counts: Record<TestColor, number>")
    }
}
