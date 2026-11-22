package com.equalizer.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDumpTest {
    private val spotify = "  AudioPlaybackConfiguration piid:65295 deviceIds:[3] type:AAudio u/pid:10335/28261 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_UNKNOWN source=DEFAULT flags=0x0  tags= bundle=null sessionId:98529 mutedState:none  FormatInfo{x}"
    private val pocket = "  AudioPlaybackConfiguration piid:70001 deviceIds:[3] type:android.media.AudioTrack u/pid:10403/7341 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_SPEECH flags=0x0  tags= bundle=null sessionId:98561 mutedState:none  FormatInfo{x}"
    private val soundPool = "  AudioPlaybackConfiguration piid:327 deviceIds:[] type:android.media.SoundPool u/pid:1000/1938 state:started attr:AudioAttributes: usage=USAGE_ASSISTANCE_SONIFICATION content=CONTENT_TYPE_SONIFICATION flags=0x800  tags= bundle=null sessionId:0 mutedState:none"
    private val idle = "  AudioPlaybackConfiguration piid:5 deviceIds:[] type:android.media.MediaPlayer u/pid:10400/15419 state:idle attr:AudioAttributes: usage=USAGE_UNKNOWN content=CONTENT_TYPE_UNKNOWN flags=0x800  tags= bundle=null sessionId:96705 mutedState:none"
    private val voice = "  AudioPlaybackConfiguration piid:6 deviceIds:[3] type:android.media.MediaPlayer u/pid:10049/2897 state:started attr:AudioAttributes: usage=USAGE_VOICE_COMMUNICATION content=CONTENT_TYPE_SONIFICATION flags=0x800  tags= bundle=null sessionId:35265 mutedState:none"

    @Test fun parsesStartedMediaSessions() {
        val r = activeMediaSessions(listOf(spotify, pocket).joinToString("\n"), ownUid = 99999)
        assertEquals(listOf(PlaybackSession(10335, 98529), PlaybackSession(10403, 98561)), r)
    }

    @Test fun ignoresSessionZeroIdleVoiceAndSystemUids() {
        val r = activeMediaSessions(listOf(soundPool, idle, voice).joinToString("\n"), ownUid = 99999)
        assertEquals(emptyList<PlaybackSession>(), r)
    }

    @Test fun ignoresOwnUid() {
        assertEquals(emptyList<PlaybackSession>(), activeMediaSessions(spotify, ownUid = 10335))
    }

    @Test fun garbageInput_returnsEmpty() {
        assertEquals(emptyList<PlaybackSession>(), activeMediaSessions("hola\n\nnada", ownUid = 1))
    }

    @Test fun presentSessionIds_includesAnyStateButNotSessionZeroOrSystemUids() {
        val dump = listOf(spotify, idle, soundPool, voice).joinToString("\n")
        assertEquals(setOf(98529, 96705, 35265), presentSessionIds(dump, ownUid = 99999))
    }

    @Test fun presentSessionIds_excludesOwnUid() {
        assertEquals(emptySet<Int>(), presentSessionIds(spotify, ownUid = 10335))
    }
}

class SessionlessPlaybackTest {
    private val brave = "  AudioPlaybackConfiguration piid:1 deviceIds:[3] type:AAudio u/pid:10354/29051 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_UNKNOWN source=DEFAULT flags=0x0 tags= bundle=null sessionId:-1 mutedState:none"

    @org.junit.Test fun sessionlessStream_mapsToGlobalSession0() {
        org.junit.Assert.assertEquals(listOf(PlaybackSession(10354, 0)), activeMediaSessions(brave, ownUid = 10000))
    }

    @org.junit.Test fun sessionlessStream_countsAsPresent() {
        org.junit.Assert.assertEquals(setOf(0), presentSessionIds(brave, ownUid = 10000))
    }
}
