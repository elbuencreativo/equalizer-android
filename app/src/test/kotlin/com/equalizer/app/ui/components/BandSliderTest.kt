package com.equalizer.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BandSliderTest {
    @Test fun topIsPlusMaxAndBottomIsMinusMax() {
        assertEquals(12f, yToGainDb(0f, 200f), 0f)
        assertEquals(-12f, yToGainDb(200f, 200f), 0f)
    }
    @Test fun centerIsZero() = assertEquals(0f, yToGainDb(100f, 200f), 0f)
    @Test fun outsideTheBoxIsClamped() {
        assertEquals(12f, yToGainDb(-50f, 200f), 0f)
        assertEquals(-12f, yToGainDb(900f, 200f), 0f)
    }
    @Test fun snapsToHalfDecibels() = assertEquals(6.5f, yToGainDb(45.9f, 200f), 0f)
    @Test fun zeroHeightIsFlat() = assertEquals(0f, yToGainDb(10f, 0f), 0f)
}
