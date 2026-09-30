@file:OptIn(ExperimentalMaterial3Api::class)

package dev.pokedex.feature.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pokedex.core.designsystem.component.DexPrimaryButton
import dev.pokedex.core.designsystem.component.PokemonAvatar
import dev.pokedex.core.designsystem.icon.DexIcons
import dev.pokedex.core.designsystem.theme.DexShape
import dev.pokedex.core.designsystem.theme.DexTheme
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.TEAM_SIZE
import dev.pokedex.core.model.Team

/** Pick a team for this Pokémon. Full teams, and teams it's already in, can't be chosen. */
@Composable
internal fun TeamChooser(pokemon: Pokemon, teams: List<Team>, onChoose: (Team) -> Unit, onNewTeam: () -> Unit, onDismiss: () -> Unit) {
    val colors = DexTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Add ${pokemon.name} to a team", style = DexTheme.type.headlineMedium, color = colors.text, modifier = Modifier.semantics { heading() })
            teams.forEach { team ->
                val already = team.filled.any { it.id == pokemon.id }
                val full = team.filled.size >= TEAM_SIZE
                val enabled = !already && !full
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(DexShape.large)
                        .clickable(enabled = enabled, role = Role.Button) { onChoose(team) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(team.name, style = DexTheme.type.titleMedium, color = if (enabled) colors.text else colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            team.filled.forEach { PokemonAvatar(it, 32.dp) }
                        }
                    }
                    Text(
                        when {
                            already -> "Already in"
                            full -> "Full"
                            else -> "${team.filled.size} / $TEAM_SIZE"
                        },
                        style = DexTheme.type.labelMedium,
                        color = colors.textSecondary,
                    )
                }
            }
            DexPrimaryButton(
                if (teams.isEmpty()) "Start a team with ${pokemon.name}" else "New team",
                onNewTeam,
                icon = DexIcons.Plus,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            )
        }
    }
}
