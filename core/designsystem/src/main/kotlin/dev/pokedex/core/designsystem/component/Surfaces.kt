package dev.pokedex.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.SceneGlass
import dev.pokedex.core.designsystem.theme.SceneGlassEdge

/** See-through card with a 1dp light edge, used over gradients in both themes. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = DexShape.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = DexTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassEdge, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** Darker glass for weather scenes, where white text needs the extra backing. */
@Composable
fun SceneCard(
    modifier: Modifier = Modifier,
    shape: Shape = DexShape.extraLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SceneGlass)
            .border(1.dp, SceneGlassEdge, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Fills from empty on first show, then follows changes smoothly. */
@Composable
fun DexProgressBar(progress: Float, modifier: Modifier = Modifier, track: Color = DexTheme.colors.track, bar: Color = DexTheme.colors.primary) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) { animated.animateTo(progress.coerceIn(0.02f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    Box(
        modifier
            .height(8.dp)
            .clip(DexShape.full)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.value.coerceAtLeast(0.02f))
                .height(8.dp)
                .clip(DexShape.full)
                .background(bar),
        )
    }
}
