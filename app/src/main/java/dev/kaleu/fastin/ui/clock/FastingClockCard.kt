package dev.kaleu.fastin.ui.clock

import androidx.compose.animation.core.animateFloatAsState
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
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.semantics.testTag as semanticsTestTag

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm", PT_BR)

/**
 * Card fixo do topo da tela inicial (PROJECT.md §3.3).
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

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow("Jejum em andamento", modifier = Modifier.fillMaxWidth())
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.xl))

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

        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.xl))
        MilestoneRow(milestones = state.milestones, zone = zone)
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

    Canvas(Modifier.size(220.dp)) {
        val stroke = 10.dp.toPx()
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

/** Marcos de 16/18/20/24h com o horário previsto e indicação de batido (spec §3.3). */
@Composable
private fun MilestoneRow(milestones: List<Milestone>, zone: ZoneId) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        milestones.forEach { milestone ->
            MilestonePill(milestone = milestone, zone = zone, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MilestonePill(
    milestone: Milestone,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = Spacing.xs)
            .clearAndSetSemantics {
                semanticsTestTag = "milestone_${milestone.hours}"
                contentDescription = buildString {
                    append("marco de ${milestone.hours} horas")
                    append(if (milestone.isReached) ", batido" else ", pendente")
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
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
        Text(
            text = "${milestone.hours}h",
            style = FastinType.label,
            color = if (milestone.isReached) FastinColors.textPrimary else FastinColors.textMuted,
        )
        Text(
            text = milestone.reachedAt.atZone(zone).format(HOUR_FORMAT),
            style = FastinType.eyebrow,
            color = FastinColors.textMuted,
        )
    }
}
