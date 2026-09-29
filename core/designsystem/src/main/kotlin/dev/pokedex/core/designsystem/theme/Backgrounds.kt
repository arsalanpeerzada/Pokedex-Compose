package dev.pokedex.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.pokedex.core.model.PokemonType
import dev.pokedex.core.model.WeatherScene

/** Weather backgrounds for Today, matching the colour board. Dark mode uses deeper, saturated stops. */
fun WeatherScene.brush(dark: Boolean): Brush {
    val (top, bottom) = when (this) {
        WeatherScene.Clear -> if (dark) 0xFF1E5AA8 to 0xFF0B1E45 else 0xFFFFE07A to 0xFF7CC8FF
        WeatherScene.Heat -> if (dark) 0xFFB8430F to 0xFF4A0E14 else 0xFFFFB347 to 0xFFFF5A36
        WeatherScene.Cloud -> if (dark) 0xFF3A4460 to 0xFF1A1F33 else 0xFFDCE2EE to 0xFFA7B3CC
        WeatherScene.Rain -> if (dark) 0xFF1D3F9E to 0xFF0A1540 else 0xFF7DB6FF to 0xFF3A62E0
        WeatherScene.Storm -> if (dark) 0xFF3B22B8 to 0xFF0E0A33 else 0xFF6A4FF0 to 0xFF231670
        WeatherScene.Snow -> if (dark) 0xFF2F5F8A to 0xFF0F2238 else 0xFFEAF8FF to 0xFFBFD9F5
        WeatherScene.Wind -> if (dark) 0xFF0F7E72 to 0xFF062B2A else 0xFFA6F2DC to 0xFF3CC7B4
        WeatherScene.Night -> if (dark) 0xFF1B1F6B to 0xFF070920 else 0xFF3B3F9E to 0xFF0F1240
    }
    return Brush.verticalGradient(listOf(Color(top), Color(bottom)))
}

fun DexColors.brandBrush(): Brush = Brush.verticalGradient(listOf(brandGradientTop, brandGradientBottom))

/** Detail-page background derived from the Pokémon's main type, standing in for artwork-based colour. */
data class DetailPalette(val background: Brush, val content: Color, val contentSecondary: Color)

fun PokemonType.detailPalette(dark: Boolean): DetailPalette {
    val base = colour().container
    return if (dark) {
        DetailPalette(
            background = Brush.verticalGradient(listOf(lerp(base, Color.Black, 0.58f), lerp(base, Color(0xFF0B0814), 0.9f))),
            content = Color(0xFFF7F2FF),
            contentSecondary = lerp(Color(0xFFF7F2FF), base, 0.22f),
        )
    } else {
        DetailPalette(
            background = Brush.verticalGradient(listOf(lerp(base, Color.White, 0.35f), lerp(base, Color.White, 0.85f))),
            content = Color(0xFF1C1426),
            contentSecondary = Color(0xFF5D5470),
        )
    }
}
