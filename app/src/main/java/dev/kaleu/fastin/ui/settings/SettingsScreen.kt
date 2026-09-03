package dev.kaleu.fastin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.domain.model.MilestoneHours
import dev.kaleu.fastin.ui.components.FastinCard
import dev.kaleu.fastin.ui.components.ToggleChipGrid
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import dev.kaleu.fastin.ui.theme.sunken

/**
 * Ajustes: backup CSV, marcos de jejum e notificações (PROJECT.md §4.2 e §4.5).
 *
 * O texto sobre backup é deliberadamente enfático: é sideload, não existe nuvem, e o
 * usuário precisa entender que trocar de aparelho sem exportar perde o histórico.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onExport: () -> Unit,
    onPickImport: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleMilestoneHour: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(FastinColors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text("Ajustes", style = FastinType.title, color = FastinColors.textPrimary)

        FastinCard(eyebrow = "Backup") {
            Text(
                text = "Este app não tem nuvem. O CSV é o seu único backup — exporte antes " +
                    "de trocar de aparelho ou reinstalar.",
                style = FastinType.body,
                color = FastinColors.textSecondary,
            )
            Box(Modifier.height(Spacing.lg))

            Text(
                text = "${state.totalDays} dias registrados",
                style = FastinType.displayMedium,
                color = FastinColors.textPrimary,
                modifier = Modifier.testTag("totalDays"),
            )
            Box(Modifier.height(Spacing.lg))

            PrimaryButton(
                label = "Exportar para Downloads",
                onClick = onExport,
                testTag = "export",
            )
            Box(Modifier.height(Spacing.md))
            SecondaryButton(
                label = "Importar de um arquivo",
                onClick = onPickImport,
                testTag = "import",
            )

            if (state.message != null) {
                Box(Modifier.height(Spacing.lg))
                Text(
                    text = state.message,
                    style = FastinType.label,
                    color = if (state.isError) FastinColors.accentCore else FastinColors.textSecondary,
                    modifier = Modifier.testTag("backupMessage"),
                )
            }
        }

        // Os marcos ficam sempre visíveis, mesmo com os avisos desligados: eles também
        // definem o que o relógio da tela inicial mostra (EP-002).
        FastinCard(eyebrow = "Marcos de jejum") {
            Text(
                text = "Os marcos escolhidos aparecem no relógio de jejum e, com os avisos " +
                    "ligados, viram notificação. De ${MilestoneHours.MIN}h a ${MilestoneHours.MAX}h.",
                style = FastinType.body,
                color = FastinColors.textSecondary,
            )
            Box(Modifier.height(Spacing.lg))
            ToggleChipGrid(
                options = MilestoneHours.OPTIONS.map { it to "${it}h" },
                selected = state.milestoneHours,
                onToggle = onToggleMilestoneHour,
                testTagPrefix = "hour",
            )
        }

        FastinCard(eyebrow = "Notificações") {
            Text(
                text = "Avisar ao atingir cada marco escolhido acima. Funciona offline, sem " +
                    "Play Services.",
                style = FastinType.body,
                color = FastinColors.textSecondary,
            )
            Box(Modifier.height(Spacing.lg))
            SecondaryButton(
                label = if (state.notificationsEnabled) "Desativar avisos" else "Ativar avisos",
                onClick = { onToggleNotifications(!state.notificationsEnabled) },
                testTag = "toggleNotifications",
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, testTag: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressable(onClick = onClick)
            .accentGlow(shape = FastinShapes.chip, elevation = 18.dp)
            .background(FastinColors.accentGradient, FastinShapes.chip)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = FastinType.label, color = FastinColors.onAccent)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit, testTag: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressable(onClick = onClick)
            .sunken(shape = FastinShapes.chip)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = FastinType.label, color = FastinColors.textSecondary)
    }
}
