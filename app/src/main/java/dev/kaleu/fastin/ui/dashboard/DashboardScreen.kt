package dev.kaleu.fastin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.domain.metrics.Aggregation
import dev.kaleu.fastin.domain.metrics.ChartCardConfig
import dev.kaleu.fastin.domain.metrics.ChartData
import dev.kaleu.fastin.domain.metrics.ChartType
import dev.kaleu.fastin.domain.metrics.Metric
import dev.kaleu.fastin.domain.metrics.Period
import dev.kaleu.fastin.ui.components.ChoiceChipRow
import dev.kaleu.fastin.ui.components.CircleIconButton
import dev.kaleu.fastin.ui.components.FastinCard
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.dashboard.charts.BigNumber
import dev.kaleu.fastin.ui.dashboard.charts.ChartEmptyState
import dev.kaleu.fastin.ui.dashboard.charts.HeatmapChart
import dev.kaleu.fastin.ui.dashboard.charts.LineChart
import dev.kaleu.fastin.ui.dashboard.charts.ScatterChart
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinIcons
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dashboard configurável (PROJECT.md §3.4).
 *
 * Reordenar por arrasto é explicitamente opcional na v1 pela spec, e ficou de fora: exige
 * gesto customizado sobre coluna rolável e o ganho num app pessoal é pequeno.
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    editor: CardEditorState?,
    onAdd: () -> Unit,
    onEdit: (ChartCardConfig) -> Unit,
    onRemove: (String) -> Unit,
    onDraftChange: ((ChartCardConfig) -> ChartCardConfig) -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(FastinColors.surfaceBase)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Dashboard", style = FastinType.title, color = FastinColors.textPrimary)
                CircleIconButton(
                    icon = FastinIcons.Plus,
                    contentDescription = "Adicionar card",
                    onClick = onAdd,
                    modifier = Modifier.testTag("addCard"),
                )
            }

            if (state.charts.isEmpty() && !state.isLoading) {
                FastinCard(eyebrow = "Dashboard vazio") {
                    Text(
                        "nenhum card configurado — toque em + para criar o primeiro",
                        style = FastinType.body,
                        color = FastinColors.textMuted,
                        modifier = Modifier.testTag("dashboardEmpty"),
                    )
                }
            }

            state.charts.forEach { chart ->
                ChartCard(
                    data = chart,
                    onEdit = { onEdit(chart.config) },
                    onRemove = { onRemove(chart.config.id) },
                )
            }

            Box(Modifier.height(Spacing.huge))
        }

        if (editor != null) {
            CardEditorSheet(
                state = editor,
                onDraftChange = onDraftChange,
                onConfirm = onConfirmEdit,
                onCancel = onCancelEdit,
            )
        }
    }
}

@Composable
private fun ChartCard(
    data: ChartData,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    FastinCard(modifier = Modifier.testTag("chart_${data.config.id}")) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                dev.kaleu.fastin.ui.theme.Eyebrow(data.config.metric.label)
                Text(
                    text = data.config.period.label,
                    style = FastinType.label,
                    color = FastinColors.textMuted,
                )
            }
            CircleIconButton(
                icon = FastinIcons.More,
                contentDescription = "Configurar ${data.config.metric.label}",
                onClick = onEdit,
                size = 32.dp,
                modifier = Modifier.testTag("edit_${data.config.id}"),
            )
            CircleIconButton(
                icon = FastinIcons.Close,
                contentDescription = "Remover ${data.config.metric.label}",
                onClick = onRemove,
                size = 32.dp,
                modifier = Modifier.testTag("remove_${data.config.id}"),
            )
        }

        Box(Modifier.height(Spacing.lg))

        if (data.isEmpty) {
            ChartEmptyState()
        } else {
            if (data.points.size == 1 && data.config.type != ChartType.BIG_NUMBER) {
                // Sem isto o usuário vê um ponto solto e conclui que o gráfico quebrou —
                // foi exatamente o que aconteceu no primeiro uso real.
                Text(
                    text = "1 dia com registro — a linha aparece a partir do segundo",
                    style = FastinType.label,
                    color = FastinColors.textMuted,
                    modifier = Modifier.testTag("hint_${data.config.id}"),
                )
                Box(Modifier.height(Spacing.sm))
            }
            when (data.config.type) {
                ChartType.LINE -> LineChart(data)
                ChartType.SCATTER -> ScatterChart(data)
                ChartType.HEATMAP -> HeatmapChart(data)
                ChartType.BIG_NUMBER -> BigNumber(
                    value = formatScalar(data),
                    unit = data.config.metric.unit,
                    caption = bigNumberCaption(data),
                    modifier = Modifier.testTag("value_${data.config.id}"),
                )
            }
        }
    }
}

/**
 * Formatação do escalar. Streak e contagens são inteiros; o resto ganha uma casa decimal —
 * "82,4 kg" é útil, "82,40000001 kg" é ruído.
 */
private fun formatScalar(data: ChartData): String {
    val v = data.scalar ?: return "—"
    val isWholeMetric = data.config.metric == Metric.FASTING_STREAK ||
        data.config.aggregation == Aggregation.COUNT ||
        data.config.metric.isRate
    return if (isWholeMetric) v.roundToInt().toString() else "%.1f".format(v).replace('.', ',')
}

private fun bigNumberCaption(data: ChartData): String = when {
    data.config.metric == Metric.FASTING_STREAK -> "consecutivos batendo a meta de 16h"
    data.points.isEmpty() -> data.config.period.label
    else -> "${data.config.aggregation.label} · ${data.points.size} dias com registro"
}

/**
 * Sheet de configuração do card. Reaproveita [ChoiceChipRow] do formulário — a mesma
 * linguagem de seleção nas duas telas.
 */
@Composable
private fun CardEditorSheet(
    state: CardEditorState,
    onDraftChange: ((ChartCardConfig) -> ChartCardConfig) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(FastinColors.surfaceBase)
            .testTag("cardEditor"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (state.isNew) "Novo card" else "Editar card",
                    style = FastinType.title,
                    color = FastinColors.textPrimary,
                )
                CircleIconButton(
                    icon = FastinIcons.Close,
                    contentDescription = "Cancelar",
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancelEdit"),
                )
            }

            FastinCard(eyebrow = "Métrica") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Metric.entries.forEach { metric ->
                        SelectableRow(
                            label = metric.label,
                            selected = state.config.metric == metric,
                            testTag = "metric_${metric.name}",
                            onClick = {
                                onDraftChange { c ->
                                    // Streak só existe como número grande: trocar para ela
                                    // ajusta o tipo junto, em vez de deixar um card inválido.
                                    if (metric == Metric.FASTING_STREAK) {
                                        c.copy(metric = metric, type = ChartType.BIG_NUMBER)
                                    } else {
                                        c.copy(metric = metric)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            FastinCard(eyebrow = "Visualização") {
                ChoiceChipRow(
                    label = "Tipo",
                    options = ChartType.entries.map { it to typeLabel(it) },
                    selected = state.config.type,
                    onSelect = { type -> type?.let { t -> onDraftChange { it.copy(type = t) } } },
                    testTagPrefix = "type",
                )
            }

            FastinCard(eyebrow = "Período") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Period.entries.filter { it != Period.CUSTOM }.forEach { period ->
                        SelectableRow(
                            label = period.label,
                            selected = state.config.period == period,
                            testTag = "period_${period.name}",
                            onClick = { onDraftChange { it.copy(period = period) } },
                        )
                    }
                }
            }

            if (state.config.type == ChartType.BIG_NUMBER &&
                state.config.metric != Metric.FASTING_STREAK
            ) {
                FastinCard(eyebrow = "Agregação") {
                    ChoiceChipRow(
                        label = "Como resumir",
                        options = Aggregation.entries.map { it to it.label },
                        selected = state.config.aggregation,
                        onSelect = { agg -> agg?.let { a -> onDraftChange { it.copy(aggregation = a) } } },
                        testTagPrefix = "agg",
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressable(onClick = onConfirm)
                    .accentGlow(shape = FastinShapes.chip, elevation = 20.dp)
                    .background(FastinColors.accentGradient, FastinShapes.chip)
                    .testTag("confirmEdit"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Salvar card", style = FastinType.label, color = FastinColors.onAccent)
            }

            Box(Modifier.height(Spacing.huge))
        }
    }
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(Spacing.touchTarget)
            .pressable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = FastinType.body,
            color = if (selected) FastinColors.textPrimary else FastinColors.textSecondary,
        )
        if (selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .accentGlow(shape = androidx.compose.foundation.shape.CircleShape, elevation = 10.dp)
                    .background(FastinColors.accentGradient, androidx.compose.foundation.shape.CircleShape),
            )
        }
    }
}

private fun typeLabel(type: ChartType) = when (type) {
    ChartType.LINE -> "Linha"
    ChartType.SCATTER -> "Pontos"
    ChartType.HEATMAP -> "Mapa"
    ChartType.BIG_NUMBER -> "Número"
}
