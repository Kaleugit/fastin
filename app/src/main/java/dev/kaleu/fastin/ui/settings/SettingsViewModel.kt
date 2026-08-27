package dev.kaleu.fastin.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kaleu.fastin.data.backup.CsvBackup
import dev.kaleu.fastin.data.prefs.NotificationPrefsStore
import dev.kaleu.fastin.data.repo.FastingLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class SettingsUiState(
    val totalDays: Int = 0,
    val notificationsEnabled: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(
    private val repository: FastingLogRepository,
    private val backup: CsvBackup,
    private val clock: Clock,
    private val notificationPrefs: NotificationPrefsStore,
    private val onNotificationsToggled: (Boolean) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { logs ->
                _uiState.update { it.copy(totalDays = logs.size) }
            }
        }
        // O toggle reflete o **disco**, nunca um default otimista. Antes o estado nascia
        // `false` e reabrir o app mostrava as notificações desligadas mesmo com os marcos
        // agendados — a tela mentia sobre o que o app estava fazendo.
        viewModelScope.launch {
            notificationPrefs.enabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun export() {
        viewModelScope.launch {
            runCatching { backup.exportToDownloads(LocalDate.now(clock)) }
                .onSuccess { name ->
                    _uiState.update { it.copy(message = "Exportado: Downloads/$name", isError = false) }
                }
                .onFailure { e ->
                    // Falha de export precisa ser visível: o usuário pode estar contando com
                    // esse arquivo antes de formatar o aparelho.
                    _uiState.update {
                        it.copy(message = "Falhou ao exportar: ${e.message}", isError = true)
                    }
                }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            runCatching { backup.importFrom(uri) }
                .onSuccess { result ->
                    val detail = buildString {
                        append("${result.imported} dias importados")
                        if (result.skipped > 0) append(", ${result.skipped} linhas ignoradas")
                        result.errors.firstOrNull()?.let { append(" · $it") }
                    }
                    _uiState.update {
                        it.copy(message = detail, isError = result.imported == 0)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(message = "Falhou ao importar: ${e.message}", isError = true)
                    }
                }
        }
    }

    /**
     * Não escreve no estado local: quem grava é o `FastinApplication`, e o coletor do `init`
     * traz o valor de volta. Uma escrita, uma leitura — dois donos do mesmo booleano é como
     * a tela e o agendador passariam a discordar.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        onNotificationsToggled(enabled)
    }
}
