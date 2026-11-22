package com.equalizer.app.engine

import com.equalizer.app.core.Combined
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqEngineMethodTest {
    private class H(val id: Int) : EffectHandle {
        var released = false
        var throwOnApply = false
        override fun apply(c: Combined) { if (throwOnApply) throw IllegalStateException("x") }
        override fun release() { released = true }
    }

    private class Rig(method: EqMethod, val failIds: Set<Int> = emptySet()) {
        val handles = mutableListOf<H>()
        val engine = EqEngine(method) { id ->
            if (id in failIds) throw IllegalStateException("bloqueado")
            H(id).also { handles.add(it) }
        }
        fun live() = handles.filter { !it.released }.map { it.id }.sorted()
    }

    @Test fun global_enabled_createsOnlyTheGlobalHandle() {
        val r = Rig(EqMethod.GLOBAL)
        r.engine.onSessionOpened(5, "spotify")
        r.engine.setEnabled(true)
        assertEquals(listOf(0), r.live())
        assertEquals(EngineStatus(EqMethod.GLOBAL, false), r.engine.status.value)
    }

    @Test fun global_closingSession0_keepsGlobalHandle() {
        val r = Rig(EqMethod.GLOBAL)
        r.engine.setEnabled(true)
        r.engine.onSessionOpened(0, "brave")
        r.engine.onSessionClosed(0)
        assertEquals(listOf(0), r.live())
    }

    @Test fun global_disabled_releasesHandle() {
        val r = Rig(EqMethod.GLOBAL)
        r.engine.setEnabled(true)
        r.engine.setEnabled(false)
        assertEquals(emptyList<Int>(), r.live())
    }

    @Test fun global_createFails_fallsBackToPerApp() {
        val r = Rig(EqMethod.GLOBAL, failIds = setOf(0))
        r.engine.onSessionOpened(5, "spotify")
        r.engine.setEnabled(true)
        assertEquals(listOf(5), r.live())
        assertEquals(EngineStatus(EqMethod.PER_APP, true), r.engine.status.value)
        assertTrue(r.engine.failed.value.isEmpty())
    }

    @Test fun global_applyFails_fallsBackToPerApp() {
        val r = Rig(EqMethod.GLOBAL)
        r.engine.onSessionOpened(5, "spotify")
        r.engine.setEnabled(true)
        r.handles.first { it.id == 0 }.throwOnApply = true
        r.engine.setCurve(Combined(List(10) { 1f }, -1f))
        assertEquals(listOf(5), r.live())
        assertTrue(r.engine.status.value.fellBack)
    }

    @Test fun perApp_ignoresSession0() {
        val r = Rig(EqMethod.PER_APP)
        r.engine.setEnabled(true)
        r.engine.onSessionOpened(0, "brave")
        r.engine.onSessionOpened(7, "spotify")
        assertEquals(listOf(7), r.live())
        assertTrue(r.engine.failed.value.isEmpty())
    }

    @Test fun setMethod_switchesHandlesWithoutOverlap() {
        val r = Rig(EqMethod.GLOBAL)
        r.engine.onSessionOpened(5, "spotify")
        r.engine.setEnabled(true)
        r.engine.setMethod(EqMethod.PER_APP)
        assertEquals(listOf(5), r.live())
        r.engine.setMethod(EqMethod.GLOBAL)
        assertEquals(listOf(0), r.live())
    }

    @Test fun setMethod_global_retriesAfterFallback() {
        var block = true
        val handles = mutableListOf<H>()
        val e = EqEngine(EqMethod.GLOBAL) { id ->
            if (id == 0 && block) throw IllegalStateException("bloqueado")
            H(id).also { handles.add(it) }
        }
        e.setEnabled(true)
        assertTrue(e.status.value.fellBack)
        block = false
        e.setMethod(EqMethod.GLOBAL)
        assertFalse(e.status.value.fellBack)
        assertEquals(EqMethod.GLOBAL, e.status.value.active)
        assertTrue(handles.any { it.id == 0 && !it.released })
    }

    @Test fun sessionFlood_isCapped() {
        val r = Rig(EqMethod.PER_APP)
        r.engine.setEnabled(true)
        (1..500).forEach { r.engine.onSessionOpened(it, "spam") }
        assertTrue(r.engine.sessions.value.size <= 64)
    }
}
