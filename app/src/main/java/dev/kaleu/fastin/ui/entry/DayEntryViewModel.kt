package dev.kaleu.fastin.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Estado do formulário (PROJECT.md §3.2).
 *
 * [weightText] guarda o texto **cru** em vez de `Double?` de propósito: enquanto se digita
 * "82,4" o valor passa por "8", "82", "82," — estados que não convertem. Validar a cada
 * tecla rejeitaria digitação legítima. A conversão acontece uma vez, no salvar.
 */
data class DayEntryUiState(
    val date: LocalDate,
    val lastMealTime: LocalTime? = null,
    val firstMealTime: LocalTime? = null,
    val caloricDeficit: Tristate? = null,
    val mealQuality: Quality? = null,
    val water2l: Tristate? = null,
    val alcohol: YesNo? = null,
    val weightText: String = "",
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    /**
     * O que está gravado no banco para este dia, ou `null` quando não há linha nenhuma.
     * Baseline da comparação de [hasUnsavedChanges] — não é o estado do formulário.
     */
    val savedLog: FastingLog? = null,
) {
    /** Peso normalizado. Vírgula do teclado pt-BR vira ponto; lixo vira null, não zero. */
    val weight: Double?
        get() = weightText.replace(',', '.').toDoubleOrNull()

    /** O texto digitado não converte para número — o campo está preenchido mas inválido. */
    val hasInvalidWeight: Boolean
        get() = weightText.isNotBlank() && weight == null

    /**
     * Há algo que sair da tela perderia?
     *
     * A comparação é contra **o que seria gravado** ([toLog]), não campo a campo do
     * formulário: "82," e "82" produzem o mesmo peso, e alertar sobre uma vírgula em trânsito
     * treinaria o usuário a ignorar o aviso.
     *
     * Sempre `false` durante a carga. Acusar alteração sobre um formulário que ainda não
     * carregou é a mesma classe de erro que apagou dados na v1.0.2: tratar o estado em
     * branco como se fosse escolha do usuário.
     */
    val hasUnsavedChanges: Boolean
        get() = !isLoading && toLog() != (savedLog ?: FastingLog(date = date))

    fun toLog(): FastingLog = FastingLog(
        date = date,
        lastMealTime = lastMealTime,
        firstMealTime = firstMealTime,
        caloricDeficit = caloricDeficit,
        mealQuality = mealQuality,
        water2l = water2l,
        alcohol = alcohol,
        weight = weight,
        notes = notes.takeIf { it.isNotBlank() },
    )
}

class DayEntryViewModel(
    private val repository: FastingLogRepository,
    private val date: LocalDate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayEntryUiState(date = date))
    val uiState: StateFlow<DayEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repository.observeByDate(date).first()

            // **Mescla, nunca sobrescreve.** Antes isto era `_uiState.value = ...`, o que
            // descartava qualquer coisa que o usuário tivesse digitado enquanto o Room
            // respondia. Aqui o valor do banco só preenche campo que o usuário deixou vazio.
            _uiState.update { current ->
                if (existing == null) {
                    current.copy(isLoading = false, savedLog = null)
                } else {
                    current.copy(
                        savedLog = existing,
                        lastMealTime = current.lastMealTime ?: existing.lastMealTime,
                        firstMealTime = current.firstMealTime ?: existing.firstMealTime,
                        caloricDeficit = current.caloricDeficit ?: existing.caloricDeficit,
                        mealQuality = current.mealQuality ?: existing.mealQuality,
                        water2l = current.water2l ?: existing.water2l,
                        alcohol = current.alcohol ?: existing.alcohol,
                        // Sempre com ponto: o campo aceita vírgula na entrada, mas exibir o
                        // valor salvo com o separador do Locale complicaria o round-trip.
                        weightText = current.weightText.ifBlank { existing.weight?.toString() ?: "" },
                        notes = current.notes.ifBlank { existing.notes.orEmpty() },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setLastMealTime(time: LocalTime?) = _uiState.update { it.copy(lastMealTime = time) }
    fun setFirstMealTime(time: LocalTime?) = _uiState.update { it.copy(firstMealTime = time) }
    fun setCaloricDeficit(v: Tristate?) = _uiState.update { it.copy(caloricDeficit = v) }
    fun setMealQuality(v: Quality?) = _uiState.update { it.copy(mealQuality = v) }
    fun setWater2l(v: Tristate?) = _uiState.update { it.copy(water2l = v) }
    fun setAlcohol(v: YesNo?) = _uiState.update { it.copy(alcohol = v) }
    fun setWeightText(v: String) = _uiState.update { it.copy(weightText = v) }
    fun setNotes(v: String) = _uiState.update { it.copy(notes = v) }

    /**
     * Upsert. Salvar com tudo vazio é legítimo e **apaga** a linha — o repositório trata
     * isso, para o calendário não ficar com ponto em dia sem dado.
     *
     * **Recusa salvar enquanto a carga não terminou.** O `@Upsert` grava a linha inteira: um
     * save disparado sobre o estado ainda em branco zeraria campos que já estavam no banco e
     * que o usuário nem tocou. Foi assim que uma "primeira refeição" já registrada sumiu em
     * uso real.
     */
    fun save(onDone: () -> Unit = {}) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            val saved = _uiState.value.toLog()
            repository.save(saved)
            // O baseline passa a ser o que acabou de ser gravado: salvar zera o estado sujo,
            // senão o aviso de "alterações não salvas" perseguiria o usuário depois de salvar.
            _uiState.update { it.copy(isSaved = true, savedLog = saved) }
            onDone()
        }
    }
}
