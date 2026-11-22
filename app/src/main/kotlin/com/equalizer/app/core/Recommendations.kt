package com.equalizer.app.core

// Tabla editable: excepciones por (dispositivo, contenido); lo demás usa el preset natural del contenido.
val RECOMMENDATION_OVERRIDES: Map<Pair<DeviceType?, ContentType>, Preset> = mapOf(
    (DeviceType.CLIPS to ContentType.POP) to Preset.BASS_BOOST,
    (DeviceType.CLIPS to ContentType.ELECTRONIC) to Preset.BASS_BOOST,
)

private fun natural(content: ContentType): Preset = when (content) {
    ContentType.POP -> Preset.POP
    ContentType.ROCK -> Preset.ROCK
    ContentType.ELECTRONIC -> Preset.ELECTRONIC
    ContentType.JAZZ_CLASSICAL -> Preset.JAZZ_CLASSICAL
    ContentType.VOCAL -> Preset.VOCAL
    ContentType.PODCAST, ContentType.AUDIOBOOK -> Preset.PODCAST
}

fun recommend(device: DeviceType?, content: ContentType): Preset =
    RECOMMENDATION_OVERRIDES[device to content] ?: natural(content)

fun recommendedFor(device: DeviceType?): Map<ContentType, Preset> =
    ContentType.values().associateWith { recommend(device, it) }
