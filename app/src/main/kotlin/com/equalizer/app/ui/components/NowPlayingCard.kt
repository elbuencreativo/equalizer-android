package com.equalizer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.equalizer.app.ui.NowPlayingItem
import com.equalizer.app.ui.theme.Tokens

@Composable
fun NowPlayingCard(
    items: List<NowPlayingItem>,
    trackAccess: Boolean,
    onEnableTrackAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Ahora suena", style = MaterialTheme.typography.labelMedium, color = Tokens.TextTertiary)
            if (items.isEmpty()) {
                Text("Nada suena en este momento", style = MaterialTheme.typography.bodyLarge, color = Tokens.TextSecondary)
            } else {
                items.forEach { item ->
                    Text(
                        item.title ?: item.appName,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = Tokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(item.artist, item.appName.takeIf { item.title != null }).joinToString(" · ")
                            .ifEmpty { "Reproduciendo" },
                        style = MaterialTheme.typography.bodySmall, color = Tokens.TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!trackAccess) {
                    TextButton(onClick = onEnableTrackAccess, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("Mostrar también la canción", color = Tokens.BrandPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
