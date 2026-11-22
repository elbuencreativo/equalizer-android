package com.equalizer.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationsTest {
    @Test fun phoneSpeaker_podcast_recommendsPodcast() =
        assertEquals(Preset.PODCAST, recommend(DeviceType.PHONE_SPEAKER, ContentType.PODCAST))

    @Test fun anyDevice_audiobook_recommendsPodcast() {
        (DeviceType.values().toList() + null).forEach {
            assertEquals(Preset.PODCAST, recommend(it, ContentType.AUDIOBOOK))
        }
    }

    @Test fun overEar_jazzClassical_recommendsJazzClassical() =
        assertEquals(Preset.JAZZ_CLASSICAL, recommend(DeviceType.OVER_EAR, ContentType.JAZZ_CLASSICAL))

    @Test fun clips_pop_recommendsBassBoost() =
        assertEquals(Preset.BASS_BOOST, recommend(DeviceType.CLIPS, ContentType.POP))

    @Test fun nullDevice_vocal_recommendsVocal() =
        assertEquals(Preset.VOCAL, recommend(null, ContentType.VOCAL))

    @Test fun recommendedFor_returnsEntryForEveryContentType() {
        assertEquals(ContentType.values().toSet(), recommendedFor(DeviceType.BUDS).keys)
    }
}
