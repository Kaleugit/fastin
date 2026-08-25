package dev.kaleu.fastin.ui.entry

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import dev.kaleu.fastin.ui.components.ChoiceChipRow
import dev.kaleu.fastin.ui.components.CircleIconButton
import dev.kaleu.fastin.ui.components.FastinCard
import dev.kaleu.fastin.ui.components.NotesField
import dev.kaleu.fastin.ui.components.TimeField
import dev.kaleu.fastin.ui.components.WeightField
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinIcons
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val TITLE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR)

/**
 * Formulário do dia (PROJECT.md §3.2). **Todos os campos são opcionais** — não existe
 * validação bloqueante e o botão salvar nunca fica desabilitado por campo vazio.
 */
@Composable
fun DayEntryScreen(
    state: DayEntryUiState,
    onLastMealTime: (LocalTime?) -> Unit,
    onFirstMealTime: (LocalTime?) -> Unit,
    onCaloricDeficit: (Tristate?) -> Unit,
    onMealQuality: (Quality?) -> Unit,
    onWater2l: (Tristate?) -> Unit,
    onAlcohol: (YesNo?) -> Unit,
    onWeightText: (String) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun pickTime(current: LocalTime?, onPicked: (LocalTime) -> Unit) {
        val initial = current ?: LocalTime.of(20, 0)
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            true, // 24h: pt-BR não usa AM/PM
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FastinColors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CircleIconButton(
                icon = FastinIcons.ArrowLeft,
                contentDescription = "Voltar",
                onClick = onBack,
                modifier = Modifier.testTag("back"),
            )
            Text(
                text = state.date.format(TITLE_FORMAT)
                    .replaceFirstChar { it.titlecase(PT_BR) },
                style = FastinType.title,
                color = FastinColors.textPrimary,
                modifier = Modifier.testTag("entryTitle"),
            )
        }

        FastinCard(eyebrow = "Jejum") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                TimeField(
                    label = "Última refeição do dia",
                    value = state.lastMealTime,
                    onPick = { pickTime(state.lastMealTime, onLastMealTime) },
                    onClear = { onLastMealTime(null) },
                    testTag = "lastMealTime",
                )
                TimeField(
                    label = "Primeira refeição do dia",
                    value = state.firstMealTime,
                    onPick = { pickTime(state.firstMealTime, onFirstMealTime) },
                    onClear = { onFirstMealTime(null) },
                    testTag = "firstMealTime",
                )
            }
        }

        FastinCard(eyebrow = "Alimentação") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                ChoiceChipRow(
                    label = "Déficit calórico",
                    options = listOf(
                        Tristate.YES to "Sim",
                        Tristate.MAYBE to "Talvez",
                        Tristate.NO to "Não",
                    ),
                    selected = state.caloricDeficit,
                    onSelect = onCaloricDeficit,
                    testTagPrefix = "caloricDeficit",
                )
                ChoiceChipRow(
                    label = "Qualidade das refeições",
                    options = listOf(
                        Quality.GOOD to "Bom",
                        Quality.AVERAGE to "Médio",
                        Quality.BAD to "Ruim",
                    ),
                    selected = state.mealQuality,
                    onSelect = onMealQuality,
                    testTagPrefix = "mealQuality",
                )
                ChoiceChipRow(
                    label = "Água ≥ 2L",
                    options = listOf(
                        Tristate.YES to "Sim",
                        Tristate.MAYBE to "Talvez",
                        Tristate.NO to "Não",
                    ),
                    selected = state.water2l,
                    onSelect = onWater2l,
                    testTagPrefix = "water2l",
                )
                ChoiceChipRow(
                    label = "Álcool",
                    options = listOf(YesNo.YES to "Sim", YesNo.NO to "Não"),
                    selected = state.alcohol,
                    onSelect = onAlcohol,
                    testTagPrefix = "alcohol",
                )
            }
        }

        FastinCard(eyebrow = "Peso") {
            WeightField(
                label = "Peso do dia",
                value = state.weightText,
                onValueChange = onWeightText,
                testTag = "weight",
            )
            if (state.hasInvalidWeight) {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = Spacing.sm))
                Text(
                    text = "Não consegui ler esse número — o peso não será salvo.",
                    style = FastinType.label,
                    color = FastinColors.accentCore,
                    modifier = Modifier.testTag("weightWarning"),
                )
            }
        }

        FastinCard(eyebrow = "Observações") {
            NotesField(
                label = "Anotações do dia",
                value = state.notes,
                onValueChange = onNotes,
                placeholder = "jantar fora, gripado, treino pesado…",
                testTag = "notes",
            )
        }

        SaveButton(onClick = onSave)
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .pressable(onClick = onClick)
            .accentGlow(shape = FastinShapes.chip, elevation = 20.dp)
            .background(FastinColors.accentGradient, FastinShapes.chip)
            .testTag("save"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Salvar",
            style = FastinType.label,
            color = FastinColors.onAccent,
        )
    }
}
