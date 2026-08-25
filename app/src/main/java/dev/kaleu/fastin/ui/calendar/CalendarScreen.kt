package dev.kaleu.fastin.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.testTag as semanticsTestTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.ui.components.CircleIconButton
import dev.kaleu.fastin.ui.components.FastinCard
import dev.kaleu.fastin.ui.components.HairlineDivider
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinIcons
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")

/**
 * Tela inicial: calendário do mês (PROJECT.md §3.1).
 *
 * O card do relógio de jejum entra acima desta grade na Etapa 3; o slot já está reservado
 * por [header].
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FastinColors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        header()
        MonthCard(
            state = state,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDayClick = onDayClick,
        )
    }
}

@Composable
private fun MonthCard(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    // Padding 0 na casca: o cabeçalho vai até a borda e o divisor atravessa o card inteiro,
    // exatamente como o card de setembro da img-ref01.
    FastinCard(contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIconButton(
                icon = FastinIcons.ArrowLeft,
                contentDescription = "Mês anterior",
                onClick = onPreviousMonth,
                modifier = Modifier.testTag("prevMonth"),
            )
            Text(
                text = buildString {
                    append(
                        state.month.month
                            .getDisplayName(JavaTextStyle.FULL_STANDALONE, PT_BR)
                            .replaceFirstChar { it.titlecase(PT_BR) },
                    )
                    // O ano só aparece quando não é o corrente — reduz ruído no uso diário.
                    if (state.month.year != LocalDate.now().year) append(" ${state.month.year}")
                },
                style = FastinType.title,
                color = FastinColors.textPrimary,
                modifier = Modifier.testTag("monthLabel"),
            )
            CircleIconButton(
                icon = FastinIcons.ArrowRight,
                contentDescription = "Próximo mês",
                onClick = onNextMonth,
                modifier = Modifier.testTag("nextMonth"),
            )
        }

        HairlineDivider()

        Column(Modifier.padding(Spacing.md)) {
            WeekdayHeader()
            androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.sm))
            DayGrid(days = state.days, onDayClick = onDayClick)
        }
    }
}

@Composable
private fun WeekdayHeader() {
    // Segunda a domingo, como a referência. DayOfWeek.MONDAY.value == 1.
    val labels = (1..7).map { value ->
        java.time.DayOfWeek.of(value)
            .getDisplayName(JavaTextStyle.SHORT_STANDALONE, PT_BR)
            .take(3)
            .uppercase(PT_BR)
    }
    Row(Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = FastinType.eyebrow,
                color = FastinColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayGrid(days: List<CalendarDay>, onDayClick: (LocalDate) -> Unit) {
    // Grade em Rows de 7 em vez de LazyVerticalGrid: o mês inteiro cabe na tela, e grade
    // preguiçosa dentro de coluna rolável exige altura fixa — pior de manter.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        day.isToday -> FastinColors.onAccent
        !day.inCurrentMonth -> FastinColors.textMuted
        day.hasData -> FastinColors.textPrimary
        else -> FastinColors.textSecondary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pressable(onClick = onClick)
            .clearAndSetSemantics {
                semanticsTestTag = "day_${day.date}"
                contentDescription = buildString {
                    append("${day.date.dayOfMonth} de ")
                    append(day.date.month.getDisplayName(JavaTextStyle.FULL, PT_BR))
                    if (day.isToday) append(", hoje")
                    if (day.hasData) append(", com registro") else append(", sem registro")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Hoje: o disco aceso da img-ref01. Um por mês — é o que faz o acento significar
        // alguma coisa. Marcar assim todo dia com registro vira parede de laranja: com uso
        // real quase todo dia tem dado, e o indicador deixa de informar.
        if (day.isToday) {
            Box(
                Modifier
                    .size(36.dp)
                    .accentGlow(shape = CircleShape, elevation = 14.dp)
                    .background(FastinColors.accentGradient, CircleShape),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = FastinType.calendarDay,
                color = textColor,
            )
            // O "indicador visual leve" que a spec §3.1 pede: ponto de 4dp sob o número.
            Box(Modifier.height(6.dp), contentAlignment = Alignment.Center) {
                if (day.hasData && day.inCurrentMonth) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .background(
                                if (day.isToday) FastinColors.onAccent else FastinColors.accent,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}
