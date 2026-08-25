package dev.kaleu.fastin.ui.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.domain.model.FastingWindow
import dev.kaleu.fastin.domain.model.Milestone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class FastingClockUiState(
    val window: FastingWindow? = null,
    val elapsed: Duration = Duration.ZERO,
    val milestones: List<Milestone> = emptyList(),
) {
    val isRunning: Boolean get() = window != null

    /** Progresso rumo a 24h, para o anel. Estoura em 1f — jejum mais longo não "desenrola". */
    val progressTo24h: Float
        get() = (elapsed.seconds.toFloat() / Duration.ofHours(24).seconds).coerceIn(0f, 1f)

    /** Próximo marco ainda não batido, se houver. */
    val nextMilestone: Milestone? get() = milestones.firstOrNull { !it.isReached }
}

/**
 * Relógio de jejum (PROJECT.md §3.3).
 *
 * O tick vive **aqui**, não no composable: um `LaunchedEffect` com `delay` dentro da tela
 * seria recriado a cada recomposição e o relógio andaria irregular.
 *
 * Tick de 1s porque a spec pede segundo a segundo. O custo é uma recomposição por segundo
 * de um único `Text` — irrelevante; e os numerais tabulares (design-system.md §2) evitam
 * que o texto trema a cada troca de dígito.
 */
class FastingClockViewModel(
    private val repository: FastingLogRepository,
    private val clock: Clock,
    private val tickInterval: Duration = Duration.ofSeconds(1),
) : ViewModel() {

    private fun ticker(): Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(tickInterval.toMillis())
        }
    }

    val uiState: StateFlow<FastingClockUiState> =
        combine(repository.observeAllByDate(), ticker()) { logs, now ->
            val window = FastingCalculator.currentWindow(logs, now, clock.zone)
                ?: return@combine FastingClockUiState()

            FastingClockUiState(
                window = window,
                elapsed = FastingCalculator.elapsed(window, now),
                milestones = FastingCalculator.milestones(window, now),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FastingClockUiState(),
        )
}
