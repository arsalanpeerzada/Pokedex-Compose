package dev.pokedex.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours, one-to-one with the Figma collections "Colour / Light" and "Colour / Dark".
 * Each Figma variable's Android code syntax (for example DexColors.Light.surfaceGlass) points here.
 */
@Immutable
data class DexColors(
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceGlassEdge: Color,
    val text: Color,
    val textSecondary: Color,
    val outline: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val highlight: Color,
    val onHighlight: Color,
    val navBackground: Color,
    val navIndicator: Color,
    val track: Color,
    val silhouette: Color,
    val brandGradientTop: Color,
    val brandGradientBottom: Color,
    val onScene: Color,
    val onSceneSecondary: Color,
    val isDark: Boolean,
) {
    companion object {
        val Light = DexColors(
            background = Color(0xFFFFF8F1),
            surface = Color(0xFFFFFFFF),
            surfaceGlass = Color(0xB8FFFFFF),
            surfaceGlassEdge = Color(0xF2FFFFFF),
            text = Color(0xFF1C1426),
            textSecondary = Color(0xFF5D5470),
            outline = Color(0xFF8C8398),
            divider = Color(0xFFEADFD6),
            primary = Color(0xFFD62246),
            onPrimary = Color(0xFFFFFFFF),
            accent = Color(0xFFF2385A),
            highlight = Color(0xFFFFD23F),
            onHighlight = Color(0xFF1C1426),
            navBackground = Color(0xE6FFFFFF),
            navIndicator = Color(0x24D62246),
            track = Color(0xFFEADFD6),
            silhouette = Color(0x381C1426),
            brandGradientTop = Color(0xFFFFE1E6),
            brandGradientBottom = Color(0xFFFFF1C7),
            onScene = Color(0xFFFFFFFF),
            onSceneSecondary = Color(0xFFE9E4FF),
            isDark = false,
        )

        val Dark = DexColors(
            background = Color(0xFF110C1A),
            surface = Color(0xFF1E1828),
            surfaceGlass = Color(0x14FFFFFF),
            surfaceGlassEdge = Color(0x24FFFFFF),
            text = Color(0xFFF7F2FF),
            textSecondary = Color(0xFFBDB3D1),
            outline = Color(0xFF857A99),
            divider = Color(0xFF2C2438),
            primary = Color(0xFFFF6B81),
            onPrimary = Color(0xFF1C1426),
            accent = Color(0xFFFF6B81),
            highlight = Color(0xFFFFD23F),
            onHighlight = Color(0xFF1C1426),
            navBackground = Color(0xEB1E1828),
            navIndicator = Color(0x33FF6B81),
            track = Color(0xFF2C2438),
            silhouette = Color(0x38FFFFFF),
            brandGradientTop = Color(0xFF2E1024),
            brandGradientBottom = Color(0xFF15102B),
            onScene = Color(0xFFFFFFFF),
            onSceneSecondary = Color(0xFFE9E4FF),
            isDark = true,
        )
    }
}

val LocalDexColors = staticCompositionLocalOf { DexColors.Light }

/** Dark glass used on weather scenes, where white text needs a darker backing to pass 4.5:1. */
val SceneGlass = Color(0x61120B3A)
val SceneGlassEdge = Color(0x4DFFFFFF)
val SceneSilhouette = Color(0xF0120B3A)
