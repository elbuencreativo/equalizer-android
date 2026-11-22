package com.equalizer.app.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateControllerTest {
    private val release = ReleaseInfo("0.2.0", "https://x/a.apk", null)

    @Test fun check_newerRelease_isAvailable() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.1.0", { release }, { InstallResult.Started })
        c.check()
        assertEquals(UpdateState.Available(release), c.state.first { it is UpdateState.Available })
    }

    @Test fun check_sameVersion_isUpToDate() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.2.0", { release }, { InstallResult.Started })
        c.check()
        c.state.first { it == UpdateState.UpToDate }
    }

    @Test fun check_nullRelease_failsWithMessage() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.1.0", { null }, { InstallResult.Started })
        c.check()
        assertTrue(c.state.first { it is UpdateState.Failed } is UpdateState.Failed)
    }

    @Test fun install_failure_isReported() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.1.0", { release }, { InstallResult.Failed("hash distinto") })
        c.check()
        c.state.first { it is UpdateState.Available }
        c.install()
        assertEquals(UpdateState.Failed("hash distinto"), c.state.first { it is UpdateState.Failed })
    }

    @Test fun install_throwing_isReportedAsFailed() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.1.0", { release }, { throw IllegalStateException("sin instalador") })
        c.check()
        c.state.first { it is UpdateState.Available }
        c.install()
        assertTrue(c.state.first { it is UpdateState.Failed } is UpdateState.Failed)
    }

    @Test fun check_throwing_isReportedAsFailed() = runTest(UnconfinedTestDispatcher()) {
        val c = UpdateController(backgroundScope, "0.1.0", { throw java.io.IOException("sin red") }, { InstallResult.Started })
        c.check()
        assertTrue(c.state.first { it is UpdateState.Failed } is UpdateState.Failed)
    }
}
