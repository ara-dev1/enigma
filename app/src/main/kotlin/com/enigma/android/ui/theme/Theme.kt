package com.enigma.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmberPrimary = Color(0xFFFFC107)
val AmberOnPrimary = Color(0xFF1E1E1E)
val BrassAccent = Color(0xFFD4AF37)
val OakBackground = Color(0xFF1A1A1A)
val SurfaceDark = Color(0xFF242424)
val SurfaceVariantDark = Color(0xFF333333)
val LampGold = Color(0xFFFFD700)
val TextLight = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    secondary = BrassAccent,
    background = OakBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = TextLight,
    onSurface = TextLight
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    secondary = BrassAccent,
    background = Color(0xFFF5F5F0),
    surface = Color(0xFFEAEAE0),
    surfaceVariant = Color(0xFFDDDDCF),
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222)
)

@Composable
fun EnigmaTheme(
    darkTheme: Boolean = true, // Default to vintage dark Enigma theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
