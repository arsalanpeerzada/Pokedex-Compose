package dev.pokedex.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.designsystem.theme.SceneGlass
import dev.pokedex.core.designsystem.theme.SceneGlassEdge
import dev.pokedex.core.designsystem.theme.colour
import dev.pokedex.core.model.PokemonType

/**
 * Pill filter. Selected pills also show a check, so selection never relies on colour alone.
 * With [multiSelect] it behaves as a checkbox; otherwise as one tab of a set.
 */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    selectedContainer: Color = DexTheme.colors.primary,
    selectedContent: Color = DexTheme.colors.onPrimary,
) {
    val colors = DexTheme.colors
    val background by animateColorAsState(if (selected) selectedContainer else colors.surfaceGlass, label = "pillBackground")
    val border by animateColorAsState(if (selected) selectedContainer else colors.outline, label = "pillBorder")
    val content by animateColorAsState(if (selected) selectedContent else colors.text, label = "pillContent")
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(DexShape.full)
            .background(background)
            .border(1.dp, border, DexShape.full)
            .then(
                if (multiSelect) {
                    Modifier.toggleable(value = selected, onValueChange = { onClick() }, role = Role.Checkbox)
                } else {
                    Modifier.selectable(selected = selected, onClick = onClick, role = Role.Tab)
                },
            )
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(DexIcons.Check, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        }
        Text(label, style = DexTheme.type.labelMedium, color = content)
    }
}

/** Outlined chip that opens a filter. A [badge] count shows how many filters are active. */
@Composable
fun DexChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, badge: Int = 0) {
    val colors = DexTheme.colors
    val border by animateColorAsState(if (badge > 0) colors.primary else colors.outline, label = "chipBorder")
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(DexShape.medium)
            .border(1.dp, border, DexShape.medium)
            .clickable(onClick = onClick, role = Role.Button)
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(16.dp))
        Text(label, style = DexTheme.type.labelMedium, color = colors.text)
        AnimatedVisibility(badge > 0, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Text(
                badge.toString(),
                style = DexTheme.type.labelSmall,
                color = colors.onPrimary,
                modifier = Modifier.clip(DexShape.full).background(colors.primary).padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

/** Solid type badge in the type's own colour, with a contrast-checked label. */
@Composable
fun TypeBadge(type: PokemonType, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = type.colour()
    Row(
        modifier = modifier
            .clip(DexShape.full)
            .background(c.container)
            .padding(start = if (icon != null) 12.dp else 14.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.content, modifier = Modifier.size(16.dp))
        Text(type.displayName, style = DexTheme.type.labelLarge, color = c.content)
    }
}

/** Small translucent tag used on type-coloured cards. */
@Composable
fun TypeTag(type: PokemonType, contentColor: Color, modifier: Modifier = Modifier) {
    val onWhiteText = contentColor == Color.White || contentColor == Color(0xFFFFFFFF)
    Text(
        text = type.displayName,
        style = DexTheme.type.labelSmall,
        color = contentColor,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(DexShape.full)
            .background(Color.White.copy(alpha = if (onWhiteText) 0.22f else 0.45f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Information pill on a weather scene. Not interactive. */
@Composable
fun SceneChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(DexShape.full)
            .background(SceneGlass)
            .border(1.dp, SceneGlassEdge, DexShape.full)
            .padding(start = 12.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DexTheme.colors.onScene, modifier = Modifier.size(16.dp))
        Text(label, style = DexTheme.type.labelMedium, color = DexTheme.colors.onScene)
    }
}
