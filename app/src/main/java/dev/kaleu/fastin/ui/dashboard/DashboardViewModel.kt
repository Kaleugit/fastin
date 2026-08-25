package dev.kaleu.fastin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kaleu.fastin.data.prefs.DashboardConfigStore
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.metrics.Aggregation
import dev.kaleu.fastin.domain.metrics.ChartCardConfig
import dev.kaleu.fastin.domain.metrics.ChartData
import dev.kaleu.fastin.domain.metrics.ChartType
import dev.kaleu.fastin.domain.metrics.Metric
import dev.kaleu.fastin.domain.metrics.MetricEngine
import dev.kaleu.fastin.domain.metrics.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class DashboardUiState(
    val charts: List<ChartData> = emptyList(),
    val isLoading: Boolean = true,
)

/** Card sendo editado no sheet de configuração. Null = sheet fechado. */
data class CardEditorState(
    val config: ChartCardConfig,
    val isNew: Boolean,
)

class DashboardViewModel(
    private val repository: FastingLogRepository,
    private val configStore: DashboardConfigStore,
    private val clock: Clock,
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    val uiState: StateFlow<DashboardUiState> =
        combine(configStore.cards, repository.observeAllByDate()) { cards, logs ->
            DashboardUiState(
                charts = cards.map { MetricEngine.resolve(it, logs, today) },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )

    private val _editor = MutableStateFlow<CardEditorState?>(null)
    val editor: StateFlow<CardEditorState?> = _editor.asStateFlow()

    fun startAdd() {
        _editor.value = CardEditorState(
            config = ChartCardConfig(
                // Sem Math.random/UUID no domínio: o instante do relógio injetado já é
                // único o suficiente e mantém o comportamento determinístico no teste.
                id = "card-${clock.millis()}",
                type = ChartType.LINE,
                metric = Metric.FASTING_HOURS,
                period = Period.LAST_30,
                aggregation = Aggregation.AVERAGE,
            ),
            isNew = true,
        )
    }

    fun startEdit(config: ChartCardConfig) {
        _editor.value = CardEditorState(config = config, isNew = false)
    }

    fun updateDraft(transform: (ChartCardConfig) -> ChartCardConfig) {
        _editor.value = _editor.value?.let { it.copy(config = transform(it.config)) }
    }

    fun cancelEdit() {
        _editor.value = null
    }

    fun confirmEdit() {
        val current = _editor.value ?: return
        // Combinação inválida (streak em linha) nunca chega ao store — seria um card que
        // não desenha nada e o usuário não entenderia o porquê.
        val config = if (current.config.isValid) {
            current.config
        } else {
            current.config.copy(type = ChartType.BIG_NUMBER)
        }
        viewModelScope.launch {
            if (current.isNew) configStore.add(config) else configStore.update(config)
            _editor.value = null
        }
    }

    fun removeCard(id: String) {
        viewModelScope.launch { configStore.remove(id) }
    }
}
