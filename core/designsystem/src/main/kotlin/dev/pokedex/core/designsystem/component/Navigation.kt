package dev.pokedex.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme

data class NavItem(val label: String, val icon: ImageVector)

/** Glass navigation bar. The indicator pill springs open under the selected tab. */
@Composable
fun DexNavigationBar(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.navBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 12.dp, bottom = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val pillWidth by animateDpAsState(
                targetValue = if (selected) 64.dp else 32.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "pillWidth",
            )
            val pill by animateColorAsState(if (selected) colors.navIndicator else colors.navIndicator.copy(alpha = 0f), label = "pill")
            val tint by animateColorAsState(if (selected) colors.primary else colors.textSecondary, label = "tint")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .width(pillWidth)
                        .height(32.dp)
                        .clip(DexShape.large)
                        .background(pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Text(
                    item.label,
                    style = if (selected) DexTheme.type.labelMedium else DexTheme.type.labelSmall,
                    color = if (selected) colors.text else colors.textSecondary,
                )
            }
        }
    }
}

/** The same navigation as a side rail, for tablets, foldables and landscape. */
@Composable
fun DexNavigationRail(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DexTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(colors.navBackground)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(vertical = 24.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val pillHeight by animateDpAsState(
                targetValue = if (selected) 40.dp else 32.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "railPill",
            )
            val pill by animateColorAsState(if (selected) colors.navIndicator else colors.navIndicator.copy(alpha = 0f), label = "railPillColour")
            val tint by animateColorAsState(if (selected) colors.primary else colors.textSecondary, label = "railTint")
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .width(56.dp)
                        .height(pillHeight)
                        .clip(DexShape.large)
                        .background(pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Text(
                    item.label,
                    style = if (selected) DexTheme.type.labelMedium else DexTheme.type.labelSmall,
                    color = if (selected) colors.text else colors.textSecondary,
                )
            }
        }
    }
}
