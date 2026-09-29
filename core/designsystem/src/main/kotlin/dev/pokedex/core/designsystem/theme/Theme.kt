package dev.pokedex.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * Pokedex's own vibrant theme. Wallpaper-based dynamic colour is intentionally not used by default
 * (plan section 5), so the app keeps its look.
 */
@Composable
fun DexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DexColors.Dark else DexColors.Light
    val typography = remember { DexTypography() }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.highlight,
            onSecondary = colors.onHighlight,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            outlineVariant = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.highlight,
            onSecondary = colors.onHighlight,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            outlineVariant = colors.divider,
        )
    }
    CompositionLocalProvider(
        LocalDexColors provides colors,
        LocalDexTypography provides typography,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography.toMaterial(), content = content)
    }
}

object DexTheme {
    val colors: DexColors
        @Composable @ReadOnlyComposable get() = LocalDexColors.current

    val type: DexTypography
        @Composable @ReadOnlyComposable get() = LocalDexTypography.current
}
