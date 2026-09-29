package dev.pokedex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme

data class NavItem(val label: String, val icon: ImageVector)

/** Glass navigation bar with a pill indicator, as in the Figma screens. */
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(width = 64.dp, height = 32.dp)
                        .clip(DexShape.large)
                        .background(if (selected) colors.navIndicator else colors.navIndicator.copy(alpha = 0f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (selected) colors.primary else colors.textSecondary, modifier = Modifier.size(24.dp))
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
