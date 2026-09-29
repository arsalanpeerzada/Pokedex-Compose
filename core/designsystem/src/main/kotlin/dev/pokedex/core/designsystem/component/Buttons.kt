package dev.pokedex.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.SceneGlass
import dev.pokedex.core.designsystem.theme.SceneGlassEdge

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/** Filled Dex Red. Minimum height, not a fixed one, so labels still fit at 200% font size. */
@Composable
fun DexPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = DexTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = DexShape.full,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        contentPadding = ButtonPadding,
    ) { ButtonContent(text, icon) }
}

/** See-through button with a 3:1 outline. */
@Composable
fun DexGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = DexTheme.colors.text,
) {
    val colors = DexTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = DexShape.full,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surfaceGlass, contentColor = contentColor),
        contentPadding = ButtonPadding,
    ) { ButtonContent(text, icon) }
}

/** Button on a weather scene: dark glass, white label. Disabled, it fades back into the scene. */
@Composable
fun DexSceneButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val onScene = DexTheme.colors.onScene
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = DexShape.xlButton,
        border = BorderStroke(1.dp, if (enabled) SceneGlassEdge else SceneGlassEdge.copy(alpha = 0.15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SceneGlass,
            contentColor = onScene,
            disabledContainerColor = SceneGlass.copy(alpha = 0.2f),
            disabledContentColor = onScene.copy(alpha = 0.5f),
        ),
        contentPadding = ButtonPadding,
    ) { Text(text, style = DexTheme.type.titleMedium) }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = DexTheme.type.labelLarge)
}

/** 48dp touch target, optionally on a glass disc. */
@Composable
fun DexIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = DexTheme.colors.text,
    glass: Boolean = false,
) {
    val colors = DexTheme.colors
    val base = modifier.size(48.dp)
    IconButton(
        onClick = onClick,
        modifier = if (glass) {
            base
                .clip(DexShape.full)
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceGlassEdge, DexShape.full)
        } else {
            base
        },
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}
