@file:OptIn(ExperimentalTextApi::class)

package dev.pokedex.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.pokedex.core.designsystem.R

// Fonts are bundled so the app works offline. All three are under the SIL Open Font Licence (assets/licenses).
private fun variable(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Fredoka = FontFamily(
    variable(R.font.fredoka_variable, FontWeight.Medium),
    variable(R.font.fredoka_variable, FontWeight.SemiBold),
    variable(R.font.fredoka_variable, FontWeight.Bold),
)

val Nunito = FontFamily(
    variable(R.font.nunito_variable, FontWeight.Normal),
    variable(R.font.nunito_variable, FontWeight.SemiBold),
    variable(R.font.nunito_variable, FontWeight.Bold),
    variable(R.font.nunito_variable, FontWeight.ExtraBold),
)

/** Pixel font for Pokédex numbers. Pixelify Sans was dropped: its 5 reads as an S. */
val Silkscreen = FontFamily(Font(R.font.silkscreen_regular, FontWeight.Normal))

/** The Adventure pairing, one-to-one with the Figma text styles. */
@Immutable
data class DexTypography(
    val displayLarge: TextStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp),
    val headlineMedium: TextStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    val titleLarge: TextStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    val titleMedium: TextStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    val bodyLarge: TextStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    val bodyMedium: TextStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    val labelLarge: TextStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp),
    val labelMedium: TextStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    val labelSmall: TextStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    val numberLarge: TextStyle = TextStyle(fontFamily = Silkscreen, fontSize = 16.sp, lineHeight = 20.sp),
    val numberSmall: TextStyle = TextStyle(fontFamily = Silkscreen, fontSize = 12.sp, lineHeight = 16.sp),
) {
    fun toMaterial(): Typography = Typography(
        displayLarge = displayLarge,
        headlineMedium = headlineMedium,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )
}

val LocalDexTypography = staticCompositionLocalOf { DexTypography() }
