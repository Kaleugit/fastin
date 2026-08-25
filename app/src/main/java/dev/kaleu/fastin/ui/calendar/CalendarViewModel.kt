package dev.kaleu.fastin.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.domain.model.FastingLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

/**
 * Um dia da grade. [inCurrentMonth] separa o mês corrente do preenchimento das bordas —
 * a `img-ref01` mostra 29/30/31 do mês anterior em cinza.
 */
data class CalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val hasData: Boolean,
    val isToday: Boolean,
    /** Duração do jejum atribuído a este dia, quando calculável. Alimenta o brilho do ponto. */
    val fastingDuration: Duration?,
)

data class CalendarUiState(
    val month: YearMonth,
    val days: List<CalendarDay> = emptyList(),
    val isLoading: Boolean = true,
)

class CalendarViewModel(
    private val repository: FastingLogRepository,
    private val clock: Clock,
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    private val visibleMonth = MutableStateFlow(YearMonth.from(today))

    val uiState: StateFlow<CalendarUiState> =
        combine(visibleMonth, repository.observeAllByDate()) { month, logs ->
            CalendarUiState(
                month = month,
                days = buildGrid(month, logs),
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(month = YearMonth.from(today)),
        )

    fun previousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    fun goToMonth(month: YearMonth) {
        visibleMonth.value = month
    }

    /**
     * Grade sempre de semanas inteiras começando na segunda — como a referência
     * (`MON TUE WED THU FRI SAT SUN`). Tamanho varia entre 35 e 42 células conforme o mês.
     */
    private fun buildGrid(
        month: YearMonth,
        logs: Map<LocalDate, FastingLog>,
    ): List<CalendarDay> {
        val first = month.atDay(1)
        // DayOfWeek.value: segunda = 1. Quantos dias recuar para chegar na segunda anterior.
        val leading = first.dayOfWeek.value - 1
        val start = first.minusDays(leading.toLong())

        val last = month.atEndOfMonth()
        val trailing = 7 - last.dayOfWeek.value
        val end = last.plusDays(trailing.toLong())

        val total = Duration.between(
            start.atStartOfDay(),
            end.plusDays(1).atStartOfDay(),
        ).toDays().toInt()

        return (0 until total).map { offset ->
            val date = start.plusDays(offset.toLong())
            val log = logs[date]
            CalendarDay(
                date = date,
                inCurrentMonth = YearMonth.from(date) == month,
                // "Tem dado" é o que o repositório guardou: linhas vazias são apagadas no save.
                hasData = log != null,
                isToday = date == today,
                fastingDuration = FastingCalculator
                    .window(date, logs[date.minusDays(1)], log)
                    ?.closedDuration,
            )
        }
    }
}
