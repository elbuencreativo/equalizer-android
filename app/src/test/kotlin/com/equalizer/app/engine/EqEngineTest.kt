package com.equalizer.app.engine

import com.equalizer.app.core.Combined
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqEngineTest {
    private class FakeHandle : EffectHandle {
        var applied: Combined? = null
        var released = false
        override fun apply(c: Combined) { applied = c }
        override fun release() { released = true }
    }

    private val curveA = Combined(List(10) { 1f }, -1f)
    private val curveB = Combined(List(10) { 2f }, -2f)

    private fun engine(handles: MutableList<FakeHandle>, fail: Boolean = false) =
        EqEngine { _ ->
            if (fail) throw IllegalStateException("boom")
            FakeHandle().also { handles.add(it) }
        }

    @Test fun sessionOpened_createsAndAppliesCurve() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.setCurve(curveA)
        e.onSessionOpened(1, "pkg")
        assertEquals(1, h.size)
        assertEquals(curveA, h[0].applied)
        assertEquals(mapOf(1 to "pkg"), e.sessions.value)
    }

    @Test fun sameSessionOpenedTwice_createsOneHandle() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.onSessionOpened(1, "pkg")
        e.onSessionOpened(1, "pkg")
        assertEquals(1, h.size)
    }

    @Test fun sessionClosed_releasesHandle() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.onSessionOpened(1, "pkg")
        e.onSessionClosed(1)
        assertTrue(h[0].released)
        assertTrue(e.sessions.value.isEmpty())
    }

    @Test fun setEnabled_false_releasesAllHandles() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.onSessionOpened(1, "a"); e.onSessionOpened(2, "b")
        e.setEnabled(false)
        assertTrue(h.all { it.released })
        assertEquals(2, e.sessions.value.size)
    }

    @Test fun sessionOpenedWhileDisabled_createsNoHandle() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.setEnabled(false)
        e.onSessionOpened(1, "a")
        assertEquals(0, h.size)
    }

    @Test fun setEnabled_true_recreatesForKnownSessions() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.setCurve(curveA)
        e.onSessionOpened(1, "a")
        e.setEnabled(false)
        e.setEnabled(true)
        assertEquals(2, h.size)
        assertEquals(curveA, h[1].applied)
    }

    @Test fun setCurve_reappliesToAllLiveSessions() {
        val h = mutableListOf<FakeHandle>()
        val e = engine(h)
        e.onSessionOpened(1, "a"); e.onSessionOpened(2, "b")
        e.setCurve(curveB)
        assertTrue(h.all { it.applied == curveB })
    }

    @Test fun factoryThrows_isCaughtAndSessionMarkedFailed() {
        val e = engine(mutableListOf(), fail = true)
        e.onSessionOpened(7, "pkg")
        assertEquals(setOf(7), e.failed.value)
        assertEquals(mapOf(7 to "pkg"), e.sessions.value)
    }
}
