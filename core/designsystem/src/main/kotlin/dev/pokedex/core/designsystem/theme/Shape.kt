package dev.pokedex.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Figma collection "Spacing and shape". */
object DexSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object DexShape {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val extraLarge = RoundedCornerShape(28.dp)
    val xlButton = RoundedCornerShape(20.dp)
    val album = RoundedCornerShape(18.dp)
    val full = RoundedCornerShape(50)
}
