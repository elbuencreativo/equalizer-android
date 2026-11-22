package com.equalizer.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedUrlTest {
    @Test fun githubHttpsHosts_areTrusted() {
        assertTrue(isTrustedDownloadUrl("https://github.com/o/r/releases/download/v1/app.apk"))
        assertTrue(isTrustedDownloadUrl("https://api.github.com/repos/o/r/releases/latest"))
        assertTrue(isTrustedDownloadUrl("https://objects.githubusercontent.com/x"))
    }

    @Test fun plainHttp_otherHosts_andLookalikes_areRejected() {
        assertFalse(isTrustedDownloadUrl("http://github.com/o/r/app.apk"))
        assertFalse(isTrustedDownloadUrl("https://evil.example.com/app.apk"))
        assertFalse(isTrustedDownloadUrl("https://github.com.evil.com/app.apk"))
        assertFalse(isTrustedDownloadUrl("https://evilgithub.com/app.apk"))
        assertFalse(isTrustedDownloadUrl("not a url"))
    }

    @Test fun releaseWithUntrustedApkUrl_isIgnored() {
        val body = """{"tag_name":"v9.9.9","assets":[{"name":"a.apk","browser_download_url":"https://evil.example.com/a.apk","digest":"sha256:abc"}]}"""
        val checker = UpdateChecker({ HttpResult(200, body) }, "o", "r")
        kotlinx.coroutines.runBlocking { assertTrue(checker.latest() == null) }
    }
}
