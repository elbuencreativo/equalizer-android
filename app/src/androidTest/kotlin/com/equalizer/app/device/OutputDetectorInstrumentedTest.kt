package com.equalizer.app.device

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class OutputDetectorInstrumentedTest {
    @Test fun emitsBuiltInSpeakerWithoutHeadphones() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(ctx.cacheDir, "detector-test-${System.nanoTime()}.preferences_pb")
        val registry = DeviceRegistry(PreferenceDataStoreFactory.create { file })
        val detector = OutputDetector(ctx, registry, CoroutineScope(SupervisorJob() + Dispatchers.Default))
        detector.start()
        val out = withTimeout(5_000) { detector.active.first { it.key != ActiveOutput.NONE.key } }
        detector.stop()
        assertEquals(OutputKind.BUILT_IN_SPEAKER, out.kind)
    }
}
