package com.equalizer.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class DownloadTest {
    @get:Rule val tmp = TemporaryFolder()

    private val data = "contenido-apk".toByteArray()
    private val hash = sha256Hex(data)

    @Test fun matchingHash_isOk() {
        val f = tmp.newFile("a.apk")
        val r = downloadVerified({ ByteArrayInputStream(data) }, f, hash)
        assertTrue(r is DownloadResult.Ok)
        assertEquals(data.size.toLong(), f.length())
    }

    @Test fun noExpectedHash_isOk() {
        assertTrue(downloadVerified({ ByteArrayInputStream(data) }, tmp.newFile(), null) is DownloadResult.Ok)
    }

    @Test fun differentHash_failsAndDeletesFile() {
        val f = tmp.newFile("b.apk")
        val r = downloadVerified({ ByteArrayInputStream(data) }, f, "00".repeat(32))
        assertTrue(r is DownloadResult.Failed)
        assertFalse(f.exists())
    }

    @Test fun interruptedStream_failsAndDeletesFile() {
        val f = tmp.newFile("c.apk")
        val broken = object : InputStream() {
            private var n = 0
            override fun read(): Int = if (n++ < 3) 65 else throw IOException("conexión cortada")
        }
        val r = downloadVerified({ broken }, f, hash)
        assertTrue(r is DownloadResult.Failed)
        assertFalse(f.exists())
    }

    @Test fun release_withoutSha256_isRejected() {
        val r = checkReleaseVerifiable(ReleaseInfo("0.2.0", "https://x/a.apk", null))
        assertTrue(r is InstallResult.Failed)
        assertEquals(null, checkReleaseVerifiable(ReleaseInfo("0.2.0", "https://x/a.apk", hash)))
    }
}
