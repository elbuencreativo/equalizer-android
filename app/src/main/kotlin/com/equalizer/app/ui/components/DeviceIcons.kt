package com.equalizer.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Earbuds
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import com.equalizer.app.core.DeviceType

fun deviceIcon(t: DeviceType?): ImageVector = when (t) {
    DeviceType.OVER_EAR -> Icons.Filled.Headphones
    DeviceType.BUDS -> Icons.Filled.Earbuds
    DeviceType.CLIPS -> Icons.Filled.Hearing
    DeviceType.IN_EAR_WIRED -> Icons.Filled.Headset
    DeviceType.PHONE_SPEAKER -> Icons.Filled.PhoneAndroid
    DeviceType.TABLET_SPEAKER -> Icons.Filled.TabletAndroid
    DeviceType.BT_SPEAKER -> Icons.Filled.Speaker
    null -> Icons.Filled.HelpOutline
}
