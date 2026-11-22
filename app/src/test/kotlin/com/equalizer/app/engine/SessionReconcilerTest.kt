package com.equalizer.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReconcilerTest {
    @Test fun opensStartedSessionsNotYetKnown() {
        val p = SessionReconciler().reconcile(known = emptySet(), started = setOf(1), present = setOf(1))
        assertEquals(setOf(1), p.open)
    }

    @Test fun doesNotReopenKnownSessions() {
        val p = SessionReconciler().reconcile(known = setOf(1), started = setOf(1), present = setOf(1))
        assertEquals(emptySet<Int>(), p.open)
    }

    @Test fun pausedButPresent_isNeverClosed() {
        val r = SessionReconciler()
        repeat(5) { assertEquals(emptySet<Int>(), r.reconcile(setOf(1), emptySet(), setOf(1)).close) }
    }

    @Test fun absentOnce_notClosed_absentTwice_closed() {
        val r = SessionReconciler()
        assertEquals(emptySet<Int>(), r.reconcile(setOf(1), emptySet(), emptySet()).close)
        assertEquals(setOf(1), r.reconcile(setOf(1), emptySet(), emptySet()).close)
    }

    @Test fun reappearing_resetsMissCounter() {
        val r = SessionReconciler()
        r.reconcile(setOf(1), emptySet(), emptySet())
        r.reconcile(setOf(1), emptySet(), setOf(1))
        assertEquals(emptySet<Int>(), r.reconcile(setOf(1), emptySet(), emptySet()).close)
    }
}
