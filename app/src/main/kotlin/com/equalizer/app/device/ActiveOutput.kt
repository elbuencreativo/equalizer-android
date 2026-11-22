package com.equalizer.app.device

import android.media.AudioDeviceInfo
import com.equalizer.app.core.DeviceType

enum class OutputKind { BUILT_IN_SPEAKER, WIRED, BLUETOOTH, EXTERNAL, UNKNOWN }

enum class TypeSource { ASSIGNED, DETECTED, UNASSIGNED }

data class OutputCandidate(val key: String, val displayName: String, val kind: OutputKind)

data class ActiveOutput(
    val key: String,
    val displayName: String,
    val kind: OutputKind,
    val deviceType: DeviceType?,
    val typeSource: TypeSource,
) {
    companion object {
        val NONE = ActiveOutput("none", "Sin salida", OutputKind.UNKNOWN, null, TypeSource.UNASSIGNED)
    }
}

fun mapKind(androidDeviceType: Int): OutputKind = when (androidDeviceType) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputKind.BUILT_IN_SPEAKER
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_LINE_ANALOG -> OutputKind.WIRED
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_BLE_BROADCAST,
    AudioDeviceInfo.TYPE_HEARING_AID -> OutputKind.BLUETOOTH
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_ACCESSORY,
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    AudioDeviceInfo.TYPE_HDMI_EARC,
    AudioDeviceInfo.TYPE_DOCK -> OutputKind.EXTERNAL
    else -> OutputKind.UNKNOWN
}

fun defaultTypeFor(kind: OutputKind, isTablet: Boolean): DeviceType? = when (kind) {
    OutputKind.BUILT_IN_SPEAKER -> if (isTablet) DeviceType.TABLET_SPEAKER else DeviceType.PHONE_SPEAKER
    OutputKind.WIRED -> DeviceType.IN_EAR_WIRED
    OutputKind.BLUETOOTH, OutputKind.EXTERNAL, OutputKind.UNKNOWN -> null
}

/**
 * Si Android informa por dónde se enruta el audio (`routedKeys`), manda esa salida.
 * Si no, Bluetooth manda sobre cable/externo y estos sobre el parlante interno.
 */
fun pickActive(candidates: List<OutputCandidate>, routedKeys: Set<String> = emptySet()): OutputCandidate? {
    val routed = candidates.filter { it.key in routedKeys }
    if (routed.isNotEmpty()) return routed.first()
    fun rank(k: OutputKind) = when (k) {
        OutputKind.BLUETOOTH -> 3
        OutputKind.WIRED, OutputKind.EXTERNAL -> 2
        OutputKind.BUILT_IN_SPEAKER -> 1
        OutputKind.UNKNOWN -> 0
    }
    return candidates.maxByOrNull { rank(it.kind) }
}

fun buildActive(c: OutputCandidate, assigned: DeviceType?, isTablet: Boolean): ActiveOutput {
    val default = defaultTypeFor(c.kind, isTablet)
    return when {
        assigned != null -> ActiveOutput(c.key, c.displayName, c.kind, assigned, TypeSource.ASSIGNED)
        default != null -> ActiveOutput(c.key, c.displayName, c.kind, default, TypeSource.DETECTED)
        else -> ActiveOutput(c.key, c.displayName, c.kind, null, TypeSource.UNASSIGNED)
    }
}
