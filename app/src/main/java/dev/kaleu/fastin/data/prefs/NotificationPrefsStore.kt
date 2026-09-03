package dev.kaleu.fastin.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.kaleu.fastin.domain.model.MilestoneHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notifications")

/** Instância de produção. O teste injeta a sua, com arquivo próprio. */
fun notificationPrefsDataStoreOf(context: Context): DataStore<Preferences> =
    context.notificationDataStore

/**
 * Preferências de marcos e notificações (PROJECT.md §4.5, EP-002).
 *
 * Existe porque a preferência morava só em memória: `FastinApplication.notificationsEnabled`
 * era um `var` volátil e `SettingsUiState` nascia `false`. Fechar o app derrubava o processo,
 * o campo voltava ao default e o usuário reencontrava o toggle desligado sem ter desligado
 * nada — junto com o coletor que reagenda os marcos, que nunca mais renascia.
 *
 * Arquivo próprio (`notifications`), separado do `dashboard`: são ciclos de vida diferentes e
 * um JSON de card corrompido não deve levar a preferência de notificação junto.
 *
 * O [DataStore] é injetado pela mesma razão registrada em [DashboardConfigStore]: o delegate
 * `by preferencesDataStore(...)` guarda **uma instância por processo**, e testes que o
 * criassem por conta própria disputariam o mesmo arquivo.
 */
class NotificationPrefsStore(private val dataStore: DataStore<Preferences>) {

    private val enabledKey = booleanPreferencesKey("notifications_enabled")

    /**
     * Guardado como conjunto de strings — é a única coleção que o Preferences DataStore
     * oferece. A conversão e a limpeza ficam na leitura: um valor estranho no arquivo vira
     * "ignorado", nunca crash.
     */
    private val hoursKey = stringSetPreferencesKey("milestone_hours")

    /** Desligado é o default: quem nunca escolheu não deve ter trabalho em background. */
    val enabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[enabledKey] ?: false }

    /**
     * Marcos escolhidos, em ordem crescente. Sem nada gravado, [MilestoneHours.DEFAULT] —
     * exatamente o que a v1.2 mostrava, para a atualização não mudar nada na cara do app.
     *
     * Conjunto gravado **vazio** é uma escolha ("não quero marco nenhum") e volta vazio;
     * só a ausência da chave cai no default.
     */
    val milestoneHours: Flow<List<Int>> = dataStore.data.map { prefs ->
        val raw = prefs[hoursKey] ?: return@map MilestoneHours.DEFAULT
        MilestoneHours.sanitize(raw.mapNotNull { it.toIntOrNull() })
    }

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { prefs -> prefs[enabledKey] = value }
    }

    suspend fun setMilestoneHours(hours: Collection<Int>) {
        val clean = MilestoneHours.sanitize(hours)
        dataStore.edit { prefs -> prefs[hoursKey] = clean.map { it.toString() }.toSet() }
    }
}
