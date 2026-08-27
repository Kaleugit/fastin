package dev.kaleu.fastin.ui.entry

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dev.kaleu.fastin.ui.theme.sunken
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

    // Sair da tela com horário informado e não salvo era perda silenciosa: `onBack` chamava
    // `popBackStack()` direto e nada comparava o formulário com o que estava no banco.
    var showExitPrompt by rememberSaveable { mutableStateOf(false) }

    fun leave() {
        if (state.hasUnsavedChanges) showExitPrompt = true else onBack()
    }

    // O botão voltar do sistema é a saída mais usada; cobrir só o botão da tela deixaria o
    // caminho principal desprotegido.
    BackHandler(enabled = !showExitPrompt) { leave() }

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

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                    onClick = { leave() },
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

            if (state.isLoading) {
                // Formulário editável antes da carga era como os dados se perdiam: o usuário
                // preenchia sobre um estado em branco e o save gravava a linha inteira por cima
                // do que já existia. Enquanto carrega, não há o que tocar.
                FastinCard(eyebrow = "Carregando") {
                    Text(
                        text = "buscando o registro deste dia…",
                        style = FastinType.body,
                        color = FastinColors.textMuted,
                        modifier = Modifier.testTag("entryLoading"),
                    )
                }
                return@Column
            }

            // Ordem **cronológica do dia**: quem preenche o dia come primeiro de manhã e pela
            // última vez à noite. A regra do domínio continua sendo outra e não muda com isto —
            // o jejum de D vai de `lastMealTime(D-1)` até `firstMealTime(D)` (PROJECT.md §2).
            FastinCard(eyebrow = "Jejum") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    TimeField(
                        label = "Primeira refeição do dia",
                        value = state.firstMealTime,
                        onPick = { pickTime(state.firstMealTime, onFirstMealTime) },
                        onClear = { onFirstMealTime(null) },
                        testTag = "firstMealTime",
                    )
                    TimeField(
                        label = "Última refeição do dia",
                        value = state.lastMealTime,
                        onPick = { pickTime(state.lastMealTime, onLastMealTime) },
                        onClear = { onLastMealTime(null) },
                        testTag = "lastMealTime",
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

        if (showExitPrompt) {
            UnsavedChangesPrompt(
                onSaveAndLeave = {
                    showExitPrompt = false
                    onSave()
                },
                onDiscard = {
                    showExitPrompt = false
                    onBack()
                },
                onCancel = { showExitPrompt = false },
            )
        }
    }
}

/**
 * Aviso de saída com alteração pendente.
 *
 * Três saídas de propósito, e **nenhum autosave**: `save()` é upsert da linha inteira e um
 * formulário vazio apaga o dia. Gravar a cada tecla transformaria cada campo limpo numa
 * escrita imediata, reabrindo a classe de bug que apagou a "primeira refeição" na v1.0.2.
 *
 * Sobreposição em tela cheia em vez de `AlertDialog`: é o padrão que o editor de card do
 * dashboard já usa, e o do Material traria as cores do MaterialTheme para dentro de um app
 * cujo tema é custom.
 */
@Composable
private fun UnsavedChangesPrompt(
    onSaveAndLeave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(FastinColors.scrim)
            // Consome o toque para que nada atrás do aviso continue clicável.
            .pressable(onClick = onCancel)
            .padding(Spacing.lg)
            .testTag("unsavedPrompt"),
        contentAlignment = Alignment.Center,
    ) {
        FastinCard(eyebrow = "Alterações não salvas") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = "Você mexeu neste dia e ainda não salvou. O que faço?",
                    style = FastinType.body,
                    color = FastinColors.textSecondary,
                )
                PromptButton("Salvar e sair", onSaveAndLeave, "saveAndLeave", primary = true)
                PromptButton("Sair sem salvar", onDiscard, "discardAndLeave")
                PromptButton("Continuar editando", onCancel, "cancelExit")
            }
        }
    }
}

@Composable
private fun PromptButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    primary: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .pressable(onClick = onClick)
            .then(
                if (primary) {
                    Modifier
                        .accentGlow(shape = FastinShapes.chip, elevation = 16.dp)
                        .background(FastinColors.accentGradient, FastinShapes.chip)
                } else {
                    Modifier.sunken(shape = FastinShapes.chip)
                },
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = FastinType.label,
            color = if (primary) FastinColors.onAccent else FastinColors.textSecondary,
        )
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
