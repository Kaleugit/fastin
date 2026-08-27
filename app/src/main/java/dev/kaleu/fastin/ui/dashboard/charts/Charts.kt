package dev.kaleu.fastin.ui.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.domain.metrics.ChartData
import dev.kaleu.fastin.domain.metrics.MetricPoint
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Renderizadores em Canvas puro (ADR-001).
 *
 * Nenhum deles sabe o que é jejum, peso ou álcool — recebem [ChartData] e desenham. Métrica
 * nova não toca em nada aqui.
 */

private const val CHART_HEIGHT_DP = 140

/**
 * Margem interna do plot.
 *
 * Sem ela o primeiro ponto cai em `x = 0` e o último em `x = largura`, e os marcadores
 * ficam metade fora do card — visível em qualquer volume de dados. Com um ponto só, o
 * gráfico parecia vazio: era um disco de 5dp desenhado meio para fora da borda esquerda.
 */
private const val PLOT_INSET_DP = 8

/** Lado do quadradinho da legenda do heatmap. */
private const val LEGEND_SWATCH_DP = 10

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** "28 ago". Ano fica de fora: o período mais longo do app é 90 dias. */
private val AXIS_DATE = DateTimeFormatter.ofPattern("d MMM", PT_BR)

/**
 * Rótulo de valor do eixo Y.
 *
 * Taxas são inteiras — "62%" e não "62,0%". O resto ganha uma casa decimal, com vírgula,
 * pela mesma razão do `formatScalar` do dashboard: "82,40000001 kg" é ruído.
 */
private fun axisValue(value: Double, data: ChartData): String {
    val metric = data.config.metric
    val n = if (metric.isRate) "%.0f".format(value) else "%.1f".format(value).replace('.', ',')
    return if (metric.unit.isEmpty()) n else "$n${metric.unit}"
}

@Composable
private fun AxisLabel(text: String, testTag: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = FastinType.label,
        color = FastinColors.textMuted,
        modifier = modifier.testTag(testTag),
    )
}

/**
 * Moldura de eixos em volta do plot.
 *
 * Existe porque o gráfico não dizia **quanto** nem **quando**: o Canvas desenhava só o traço
 * e o usuário via uma curva sem escala nenhuma. Os rótulos são `Text` do Compose, nunca
 * `drawText` nativo — ADR-001, e canvas nativo não é acelerado por GPU.
 *
 * Ficam **fora** da área de desenho, não sobrepostos: [CHART_HEIGHT_DP] continua sendo a
 * altura do plot, e o card cresce o tanto que os rótulos ocupam. Sobrepor economizaria
 * espaço ao custo de texto em cima da curva, que é onde o traço é mais denso.
 */
@Composable
private fun PlotFrame(
    data: ChartData,
    modifier: Modifier = Modifier,
    plot: @Composable () -> Unit,
) {
    val id = data.config.id
    val single = data.points.size == 1 || data.min == data.max

    Column(modifier.fillMaxWidth()) {
        AxisLabel(axisValue(data.max, data), "axisMax_$id")

        plot()

        // Série constante (ou de um ponto) tem um valor só. Repetir o mesmo número em cima
        // e embaixo sugeriria uma variação que não existe.
        if (!single) {
            AxisLabel(axisValue(data.min, data), "axisMin_$id")
        }

        Box(Modifier.height(Spacing.xs))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AxisLabel(data.points.first().date.format(AXIS_DATE), "axisFrom_$id")
            if (data.points.size > 1) {
                AxisLabel(data.points.last().date.format(AXIS_DATE), "axisTo_$id")
            }
        }
    }
}

/**
 * Escala vertical com folga de 8% em cima e embaixo.
 *
 * Série constante (todo peso igual, por exemplo) tem min == max e dividiria por zero; nesse
 * caso a linha vai ao meio do card, que é a leitura honesta de "não variou".
 */
private fun yScale(data: ChartData): Pair<Double, Double> {
    val min = data.min
    val max = data.max
    if (min == max) return (min - 1) to (max + 1)
    val padding = (max - min) * 0.08
    return (min - padding) to (max + padding)
}

private fun DrawScope.pointOffsets(
    data: ChartData,
    size: Size,
): List<Offset> {
    val inset = PLOT_INSET_DP.dp.toPx()
    val plotW = (size.width - inset * 2).coerceAtLeast(1f)
    val plotH = (size.height - inset * 2).coerceAtLeast(1f)

    // Ponto único não tem eixo X: centralizar é a única posição honesta. Deixá-lo na
    // borda esquerda fazia o card parecer vazio.
    if (data.points.size == 1) {
        return listOf(Offset(size.width / 2f, size.height / 2f))
    }

    val (lo, hi) = yScale(data)
    val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0
    val first = data.points.first().date
    val totalDays = ChronoUnit.DAYS.between(first, data.points.last().date)
        .toDouble().takeIf { it > 0 } ?: 1.0

    return data.points.map { p ->
        // Eixo X por **data real**, não por índice: dias sem registro precisam deixar
        // lacuna proporcional, senão uma semana ausente vira um passo de um dia.
        val x = inset + (ChronoUnit.DAYS.between(first, p.date) / totalDays) * plotW
        val y = inset + (plotH - ((p.value - lo) / span * plotH))
        Offset(x.toFloat(), y.toFloat())
    }
}

@Composable
fun LineChart(data: ChartData, modifier: Modifier = Modifier) {
    if (data.points.isEmpty()) return
    PlotFrame(data, modifier) { LinePlot(data) }
}

@Composable
private fun LinePlot(data: ChartData) {
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp)) {
        if (data.points.isEmpty()) return@Canvas
        val offsets = pointOffsets(data, size)

        if (offsets.size == 1) {
            // Um ponto não tem tendência. A régua horizontal dá ao disco um contexto de
            // "este é o nível medido", em vez de deixá-lo boiando no vazio.
            val c = offsets.first()
            drawLine(
                color = FastinColors.hairline,
                start = Offset(PLOT_INSET_DP.dp.toPx(), c.y),
                end = Offset(size.width - PLOT_INSET_DP.dp.toPx(), c.y),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(FastinColors.accentCore, radius = 5.dp.toPx(), center = c)
            return@Canvas
        }

        val line = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }

        // Preenchimento sob a curva: a brasa que se dissolve no card.
        val fill = Path().apply {
            addPath(line)
            lineTo(offsets.last().x, size.height)
            lineTo(offsets.first().x, size.height)
            close()
        }

        drawPath(fill, brush = FastinColors.accentFadeGradient)

        drawPath(
            path = line,
            brush = FastinColors.accentGradient,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Só o último ponto ganha marcador: destacar todos vira ruído numa série de 90 dias.
        drawCircle(FastinColors.accentCore, radius = 4.dp.toPx(), center = offsets.last())
    }
}

@Composable
fun ScatterChart(data: ChartData, modifier: Modifier = Modifier) {
    if (data.points.isEmpty()) return
    PlotFrame(data, modifier) { ScatterPlot(data) }
}

@Composable
private fun ScatterPlot(data: ChartData) {
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp)) {
        pointOffsets(data, size).forEach { offset ->
            drawCircle(FastinColors.accent.copy(alpha = 0.75f), radius = 3.5.dp.toPx(), center = offset)
        }
    }
}

/**
 * Heatmap estilo GitHub: colunas são semanas, linhas são dias da semana (segunda no topo).
 *
 * Intensidade proporcional ao valor. Dia sem registro fica com a cor do poço — ausência de
 * dado é visualmente distinta de valor baixo, que é o ponto do gráfico.
 */
@Composable
fun HeatmapChart(data: ChartData, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        HeatmapPlot(data)
        Box(Modifier.height(Spacing.sm))
        HeatmapLegend(data)
    }
}

/**
 * Legenda de escala. O heatmap codifica valor em opacidade, e opacidade sem referência não
 * é lida como número — o usuário via manchas mais claras e mais escuras sem saber o que
 * separava uma da outra.
 *
 * Os degraus repetem o piso de 0.22 do desenho: o dia mais fraco precisa continuar visível
 * como "houve dado".
 */
@Composable
private fun HeatmapLegend(data: ChartData) {
    val id = data.config.id
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AxisLabel(data.from.format(AXIS_DATE), "axisFrom_$id")

        Box(Modifier.weight(1f))

        AxisLabel("menos", "legendLess_$id")
        listOf(0.22f, 0.42f, 0.61f, 0.81f, 1f).forEach { alpha ->
            Box(
                Modifier
                    .size(LEGEND_SWATCH_DP.dp)
                    .background(FastinColors.accent.copy(alpha = alpha), FastinShapes.small),
            )
        }
        AxisLabel("mais", "legendMore_$id")

        Box(Modifier.weight(1f))

        AxisLabel(data.to.format(AXIS_DATE), "axisTo_$id")
    }
}

@Composable
private fun HeatmapPlot(data: ChartData) {
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp)) {
        val byDate = data.points.associateBy { it.date }
        val start = data.from.minusDays((data.from.dayOfWeek.value - 1).toLong())
        val weeks = (ChronoUnit.DAYS.between(start, data.to) / 7).toInt() + 1
        if (weeks <= 0) return@Canvas

        val gap = 2.dp.toPx()
        val cell = ((size.width - gap * (weeks - 1)) / weeks).coerceAtLeast(1f)
        val cellH = ((size.height - gap * 6) / 7).coerceAtLeast(1f)
        val side = minOf(cell, cellH)

        val lo = data.min
        val hi = data.max
        val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0

        for (w in 0 until weeks) {
            for (d in 0 until 7) {
                val date = start.plusDays((w * 7 + d).toLong())
                if (date.isBefore(data.from) || date.isAfter(data.to)) continue

                val point: MetricPoint? = byDate[date]
                val topLeft = Offset(w * (side + gap), d * (side + gap))

                if (point == null) {
                    drawRoundRect(
                        color = FastinColors.surfaceSunken,
                        topLeft = topLeft,
                        size = Size(side, side),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                } else {
                    // Piso de 0.22 para o dia mais fraco ainda ser visível como "houve dado".
                    val intensity = (0.22 + ((point.value - lo) / span) * 0.78).toFloat()
                    drawRoundRect(
                        color = FastinColors.accent.copy(alpha = intensity.coerceIn(0.22f, 1f)),
                        topLeft = topLeft,
                        size = Size(side, side),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
    }
}

/** Número grande com unidade (spec §3.4: "big number com média/total/streak"). */
@Composable
fun BigNumber(
    value: String,
    unit: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = FastinType.displayLarge, color = FastinColors.textPrimary)
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = FastinType.label,
                    color = FastinColors.textMuted,
                    modifier = Modifier.padding(start = Spacing.sm, bottom = 6.dp),
                )
            }
        }
        Text(text = caption, style = FastinType.label, color = FastinColors.textSecondary)
    }
}

/** Estado vazio de um card: nunca deixar a área do gráfico em branco. */
@Composable
fun ChartEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "sem dados no período",
                style = FastinType.body,
                color = FastinColors.textMuted,
            )
        }
    }
}
