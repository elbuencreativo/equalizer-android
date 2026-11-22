package com.equalizer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.equalizer.app.ui.theme.Tokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = if (recommended) "★ $label" else label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) Tokens.OnBrand else Tokens.TextPrimary,
        modifier = modifier
            .clip(shape)
            .background(if (selected) Tokens.BrandPrimary else Tokens.BgSurface)
            .border(1.dp, if (selected) Tokens.BrandPrimary else Tokens.Border, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
