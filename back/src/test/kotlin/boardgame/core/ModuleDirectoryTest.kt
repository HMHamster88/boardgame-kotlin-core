package boardgame.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleDirectoryTest {

    @Test
    fun stripsVersionSuffix() {
        assertEquals("cah", moduleDirectoryName("https://github.com/x/releases/download/v0.1.0/cah-0.1.0.zip"))
    }

    @Test
    fun keepsUnversionedAssetName() {
        assertEquals("cah", moduleDirectoryName("https://github.com/x/releases/download/v0.1.0/cah.zip"))
    }

    @Test
    fun stripsVPrefixAndUnderscoreSeparator() {
        assertEquals(
            "cards_against_humanity",
            moduleDirectoryName("https://example.com/cards_against_humanity-v1.2.3.zip")
        )
    }

    @Test
    fun stripsSnapshotSuffix() {
        assertEquals("cah", moduleDirectoryName("https://example.com/cah-0.1.0-SNAPSHOT.zip"))
    }

    @Test
    fun toleratesVersionOnlyName() {
        assertEquals("0_1_0", moduleDirectoryName("https://example.com/0.1.0.zip"))
    }
}
