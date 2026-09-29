@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.pokedex.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.remember
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode

/** Provided by the app around NavDisplay, so screens can share elements across destinations. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provided per destination with Navigation 3's animated content scope. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Shared element that quietly does nothing in previews or outside navigation. */
@Composable
fun Modifier.dexSharedElement(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (shared == null || animated == null) return this
    return with(shared) {
        this@dexSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}

/** Cards and buttons shrink slightly while pressed, then spring back. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A slow bob, for hero artwork. Off in previews so screenshots stay stable. */
@Composable
fun Modifier.floating(distancePx: Float = 14f, periodMillis: Int = 2600): Modifier {
    if (LocalInspectionMode.current) return this
    val transition = rememberInfiniteTransition(label = "floating")
    val offset by transition.animateFloat(
        initialValue = -distancePx / 2,
        targetValue = distancePx / 2,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2), RepeatMode.Reverse),
        label = "floatOffset",
    )
    return graphicsLayer { translationY = offset }
}

/** Content that fades and rises into place, each [index] a beat after the previous one. */
@Composable
fun StaggeredEntrance(index: Int, content: @Composable () -> Unit) {
    val inPreview = LocalInspectionMode.current
    val state = remember { MutableTransitionState(inPreview).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(420, delayMillis = 70 * index)) +
            slideInVertically(tween(420, delayMillis = 70 * index, easing = FastOutSlowInEasing)) { it / 3 },
    ) { content() }
}

/** A gentle breathing pulse, for glows. */
@Composable
fun rememberPulse(from: Float = 0.9f, to: Float = 1.1f, periodMillis: Int = 2200): Float {
    if (LocalInspectionMode.current) return 1f
    val transition = rememberInfiniteTransition(label = "pulse")
    val value by transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2), RepeatMode.Reverse),
        label = "pulseValue",
    )
    return value
}
