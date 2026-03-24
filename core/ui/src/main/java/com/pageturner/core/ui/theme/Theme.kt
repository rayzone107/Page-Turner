package com.pageturner.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PageTurnerDarkColorScheme = darkColorScheme(
    background        = PageTurnerColors.Background,
    surface           = PageTurnerColors.Surface,
    surfaceVariant    = PageTurnerColors.SurfaceVariant,
    onBackground      = PageTurnerColors.OnBackground,
    onSurface         = PageTurnerColors.OnSurface,
    onSurfaceVariant  = PageTurnerColors.OnSurfaceMuted,
    primary           = PageTurnerColors.Accent,
    onPrimary         = PageTurnerColors.OnAccent,
    secondary         = PageTurnerColors.Teal,
    onSecondary       = PageTurnerColors.OnAccent,
    tertiary          = PageTurnerColors.Teal,
    error             = PageTurnerColors.Error,
    onError           = PageTurnerColors.OnBackground,
    scrim             = PageTurnerColors.Background
)

private val PageTurnerTypography = Typography(
    bodyLarge   = PageTurnerType.Body,
    bodyMedium  = PageTurnerType.Body,
    bodySmall   = PageTurnerType.BodySmall,
    labelSmall  = PageTurnerType.Label,
    titleLarge  = PageTurnerType.AppTitle,
    titleMedium = PageTurnerType.CardTitle
)

/**
 * PageTurner Material3 theme — dark only.
 *
 * All screens and components must be wrapped in this theme. The theme enforces
 * the design system tokens defined in [PageTurnerColors], [PageTurnerSpacing],
 * and [PageTurnerType].
 */
@Composable
fun PageTurnerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PageTurnerDarkColorScheme,
        typography  = PageTurnerTypography,
        content     = content
    )
}
