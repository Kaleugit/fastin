package dev.kaleu.fastin.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notifications")

/** Instância de produção. O teste injeta a sua, com arquivo próprio. */
fun notificationPrefsDataStoreOf(context: Context): DataStore<Preferences> =
    context.notificationDataStore

/**
 * Preferência de notificações de marco (PROJECT.md §4.5).
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

    private val key = booleanPreferencesKey("notifications_enabled")

    /** Desligado é o default: quem nunca escolheu não deve ter trabalho em background. */
    val enabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[key] ?: false }

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { prefs -> prefs[key] = value }
    }
}
