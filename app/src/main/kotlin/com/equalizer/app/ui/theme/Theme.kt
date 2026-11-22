package com.equalizer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Family = FontFamily.SansSerif // Inter pendiente de aprobación del TTF

val EqTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

@Composable
fun EqTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Tokens.BrandPrimary,
            onPrimary = Tokens.OnBrand,
            background = Tokens.BgScreen,
            onBackground = Tokens.TextPrimary,
            surface = Tokens.BgSurface,
            onSurface = Tokens.TextPrimary,
            outline = Tokens.Border,
        ),
        typography = EqTypography,
        content = content,
    )
}
