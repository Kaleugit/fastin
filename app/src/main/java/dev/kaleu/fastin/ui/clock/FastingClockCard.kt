package dev.kaleu.fastin.ui.clock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.domain.model.Milestone
import dev.kaleu.fastin.ui.components.FastinCard
import dev.kaleu.fastin.ui.theme.Eyebrow
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinMotion
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.ui.semantics.testTag as semanticsTestTag

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm", PT_BR)

/** A partir de quantos marcos a lista se divide em duas colunas para não esticar o card. */
private const val SINGLE_COLUMN_MAX = 5

/**
 * Card fixo do topo da tela inicial (PROJECT.md §3.3).
 *
 * Na v1.3 (EP-002) o card ficou com **metade da altura**: o anel encolheu e foi para a
 * esquerda, os marcos viraram uma lista à direita. O motivo é o calendário — com o card de
 * 386dp de antes ele nunca cabia inteiro na tela sem rolar.
 *
 * O número **não é animado** de propósito (design-system.md §6): ele muda a cada segundo e
 * interpolar seria ruído. Anima só o anel de progresso.
 */
@Composable
fun FastingClockCard(
    state: FastingClockUiState,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    FastinCard(modifier = modifier.testTag("fastingClock")) {
        if (!state.isRunning) {
            EmptyClock()
        } else {
            RunningClock(state = state, zone = zone)
        }
    }
}

@Composable
private fun EmptyClock() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Eyebrow("Sem jejum em andamento")
        Text(
            // Texto exato pedido na spec §3.3.
            text = "nenhum jejum em andamento — registre sua última refeição",
            style = FastinType.body,
            color = FastinColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("clockEmpty"),
        )
    }
}

@Composable
private fun RunningClock(state: FastingClockUiState, zone: ZoneId) {
    val hours = state.elapsed.toHours()
    val minutes = state.elapsed.toMinutes() % 60
    val seconds = state.elapsed.seconds % 60

    Column(Modifier.fillMaxWidth()) {
        Eyebrow("Jejum em andamento", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(progress = state.progressTo24h)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%d:%02d".format(hours, minutes),
                        style = FastinType.clock,
                        color = FastinColors.textPrimary,
                        modifier = Modifier
                            // Sem isso o leitor de tela anunciaria o número inteiro a cada
                            // segundo, tornando o app inutilizável com TalkBack.
                            .clearAndSetSemantics {
                                semanticsTestTag = "clockValue"
                                contentDescription = "$hours horas e $minutes minutos de jejum"
                            },
                    )
                    Text(
                        text = "%02ds".format(seconds),
                        style = FastinType.label,
                        color = FastinColors.textMuted,
                        modifier = Modifier.clearAndSetSemantics { semanticsTestTag = "clockSeconds" },
                    )
                }
            }

            Spacer(Modifier.width(Spacing.lg))

            MilestoneList(
                milestones = state.milestones,
                zone = zone,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Anel de progresso rumo a 24h. Começa às 12h (topo) e corre no sentido horário; o
 * gradiente vai da brasa ao vermelho profundo, como as refs.
 */
@Composable
private fun ProgressRing(progress: Float) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = FastinMotion.standard(),
        label = "ringProgress",
    )

    Canvas(Modifier.size(140.dp)) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)

        // Trilha: o poço por onde o arco corre.
        drawArc(
            color = FastinColors.surfaceSunken,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        if (animated > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to FastinColors.accentCore,
                    0.5f to FastinColors.accent,
                    1.0f to FastinColors.accentDeep,
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Marcos escolhidos em Ajustes, com o horário previsto e indicação de batido (spec §3.3).
 *
 * Lista vertical, não linha: cabe ao lado do anel e aceita de 0 a 9 marcos sem espremer o
 * horário. Acima de [SINGLE_COLUMN_MAX] divide em duas colunas para a lista não passar da
 * altura do anel.
 */
@Composable
private fun MilestoneList(
    milestones: List<Milestone>,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    if (milestones.isEmpty()) {
        Text(
            text = "nenhum marco escolhido — ajuste em Ajustes",
            style = FastinType.label,
            color = FastinColors.textMuted,
            modifier = modifier.testTag("milestonesEmpty"),
        )
        return
    }

    val columns = if (milestones.size > SINGLE_COLUMN_MAX) 2 else 1
    val perColumn = ceil(milestones.size / columns.toFloat()).toInt()

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        milestones.chunked(perColumn).forEach { column ->
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                column.forEach { MilestoneRow(milestone = it, zone = zone) }
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone, zone: ZoneId) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                semanticsTestTag = "milestone_${milestone.hours}"
                contentDescription = buildString {
                    append("marco de ${milestone.hours} horas")
                    append(if (milestone.isReached) ", batido" else ", pendente")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .then(
                    if (milestone.isReached) {
                        Modifier
                            .accentGlow(shape = CircleShape, elevation = 10.dp)
                            .background(FastinColors.accentGradient, CircleShape)
                    } else {
                        Modifier.background(FastinColors.qualityLow, CircleShape)
                    },
                ),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = "${milestone.hours}h",
            style = FastinType.label,
            color = if (milestone.isReached) FastinColors.textPrimary else FastinColors.textMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = milestone.reachedAt.atZone(zone).format(HOUR_FORMAT),
            style = FastinType.eyebrow,
            color = FastinColors.textMuted,
        )
    }
}
