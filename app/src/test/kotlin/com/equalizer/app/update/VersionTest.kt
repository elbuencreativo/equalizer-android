package com.equalizer.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test fun isNewer_patchBump_true() = assertTrue(isNewer("0.1.0", "0.1.1"))
    @Test fun isNewer_minorBeatsPatch_true() = assertTrue(isNewer("0.9.9", "0.10.0"))
    @Test fun isNewer_sameVersion_false() = assertFalse(isNewer("0.1.0", "0.1.0"))
    @Test fun isNewer_older_false() = assertFalse(isNewer("0.2.0", "0.1.9"))
    @Test fun isNewer_vPrefixIgnored() = assertTrue(isNewer("0.1.0", "v0.1.1"))
    @Test fun isNewer_invalidString_false() {
        assertFalse(isNewer("0.1.0", "latest"))
        assertFalse(isNewer("abc", "0.1.1"))
    }
}
