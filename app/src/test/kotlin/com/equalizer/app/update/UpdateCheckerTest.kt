package com.equalizer.app.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {
    private val json = """
        {"tag_name":"v0.2.0","assets":[
          {"name":"notes.txt","browser_download_url":"https://x/notes.txt"},
          {"name":"equalizer-0.2.0.apk","browser_download_url":"https://x/equalizer-0.2.0.apk","digest":"sha256:ABCDEF"}
        ]}
    """.trimIndent()

    @Test fun latest_parsesReleaseJson() = runTest {
        val r = UpdateChecker({ HttpResult(200, json) }, "o", "r").latest()
        assertEquals(ReleaseInfo("0.2.0", "https://x/equalizer-0.2.0.apk", "abcdef"), r)
    }

    @Test fun latest_noApkAsset_returnsNull() = runTest {
        val r = UpdateChecker({ HttpResult(200, """{"tag_name":"v1.0.0","assets":[]}""") }, "o", "r").latest()
        assertNull(r)
    }

    @Test fun latest_httpError_returnsNull() = runTest {
        assertNull(UpdateChecker({ HttpResult(404, "") }, "o", "r").latest())
    }

    @Test fun latest_networkException_returnsNull() = runTest {
        assertNull(UpdateChecker({ throw java.io.IOException("sin red") }, "o", "r").latest())
    }

    @Test fun latest_blankRepo_returnsNull() = runTest {
        assertNull(UpdateChecker({ HttpResult(200, json) }, "", "").latest())
    }
}
