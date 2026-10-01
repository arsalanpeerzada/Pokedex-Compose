package dev.pokedex.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Our own 24dp outline icon set, the same paths as the Figma designs. No official Pokémon symbols.
 * Colour comes from the Icon tint, so paths are drawn in black.
 */
object DexIcons {
    val Back by lazy { stroke("Back", "M15 5l-7 7 7 7") }
    val Search by lazy { stroke("Search", circle(11f, 11f, 6f), "M20 20l-4.5-4.5") }
    val Settings by lazy { stroke("Settings", "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12", circle(16f, 6f, 2f), circle(10f, 12f, 2f), circle(18f, 18f, 2f)) }
    val Filter by lazy { stroke("Filter", "M4 5h16l-6 7v6l-4 2v-8z") }
    val TypeChart by lazy { stroke("TypeChart", roundedRect(4f, 4f, 16f, 16f, 2f), "M4 10h16M10 4v16") }
    val Sort by lazy { stroke("Sort", "M7 4v16M4 17l3 3 3-3M17 20V4M14 7l3-3 3 3") }

    /** Two cards facing each other, with arrows between: "compare". */
    val Compare by lazy { stroke("Compare", roundedRect(3f, 5f, 7f, 14f, 1.5f), roundedRect(14f, 5f, 7f, 14f, 1.5f), "M10.5 9.5h3M12.5 8l1.5 1.5L12.5 11M13.5 14.5h-3M11.5 13L10 14.5l1.5 1.5") }
    val Heart by lazy { stroke("Heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val Share by lazy { stroke("Share", circle(6f, 12f, 2.5f), circle(18f, 6f, 2.5f), circle(18f, 18f, 2.5f), "M8.3 10.9l7.4-3.8M8.3 13.1l7.4 3.8") }
    val Check by lazy { stroke("Check", "M5 12.5l4.5 4.5L19 7") }
    val Plus by lazy { stroke("Plus", "M12 5v14M5 12h14") }
    val Close by lazy { stroke("Close", "M6 6l12 12M18 6L6 18") }
    val Pin by lazy { stroke("Pin", "M12 21s-6-5.4-6-11a6 6 0 1 1 12 0c0 5.6-6 11-6 11z", circle(12f, 10f, 2.5f)) }
    val Storm by lazy { stroke("Storm", "M7 16a4 4 0 1 1 .9-7.9A5 5 0 0 1 17.5 9A3.5 3.5 0 0 1 17 16", "M12.5 12l-2 4h3l-2 4") }
    val Moon by lazy { stroke("Moon", "M20 14.5A8 8 0 1 1 9.5 4a7.5 7.5 0 0 0 10.5 10.5z") }
    val Sun by lazy { stroke("Sun", circle(12f, 12f, 4f), "M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4") }
    val Grid by lazy { stroke("Grid", roundedRect(4f, 4f, 7f, 7f, 1.5f), roundedRect(13f, 4f, 7f, 7f, 1.5f), roundedRect(4f, 13f, 7f, 7f, 1.5f), roundedRect(13f, 13f, 7f, 7f, 1.5f)) }
    val Team by lazy { stroke("Team", circle(8f, 9f, 3f), circle(16f, 9f, 3f), "M3 19c0-2.8 2.2-5 5-5s5 2.2 5 5M11 19c0-2.8 2.2-5 5-5s5 2.2 5 5") }
    val Star by lazy { stroke("Star", "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z") }
    val Play by lazy { filled("Play", "M8 5v14l11-7z") }
    val Bolt by lazy { filled("Bolt", "M13 2L4 14h7l-1 8 9-12h-7z") }

    private fun builder(name: String) =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)

    private fun stroke(name: String, vararg paths: String): ImageVector = builder(name).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    private fun filled(name: String, vararg paths: String): ImageVector = builder(name).apply {
        paths.forEach { data -> addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black)) }
    }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r h${-(w - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} z"
}
