package dev.kaleu.fastin.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.kaleu.fastin.domain.metrics.ChartCardConfig
import dev.kaleu.fastin.domain.metrics.ChartType
import dev.kaleu.fastin.domain.metrics.Metric
import dev.kaleu.fastin.domain.metrics.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dashboardDataStore by preferencesDataStore(name = "dashboard")

/** Instância de produção. O teste injeta a sua, com arquivo próprio. */
fun dashboardDataStoreOf(context: Context): DataStore<Preferences> = context.dashboardDataStore

/**
 * Configuração dos cards do dashboard (PROJECT.md §3.4: "persiste entre sessões").
 *
 * Guardado como uma string JSON única em vez de chaves separadas: a lista é pequena, a
 * ordem importa, e serializar o conjunto inteiro torna adicionar/remover/reordenar uma
 * escrita atômica.
 *
 * O [DataStore] é injetado, não obtido do `Context` aqui dentro: o delegate
 * `by preferencesDataStore(...)` guarda **uma instância por processo**, então testes que o
 * criassem por conta própria compartilhariam o mesmo arquivo e contaminariam uns aos outros.
 */
class DashboardConfigStore(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        // Config gravada por uma versão anterior não deve derrubar o app se um campo sumir.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key = stringPreferencesKey("cards")

    private fun decode(raw: String?): List<ChartCardConfig> {
        if (raw == null) return DEFAULT_CARDS
        return runCatching { json.decodeFromString<List<ChartCardConfig>>(raw) }
            // JSON corrompido não pode significar dashboard quebrado para sempre.
            .getOrDefault(DEFAULT_CARDS)
            .filter { it.isValid }
    }

    val cards: Flow<List<ChartCardConfig>> =
        dataStore.data.map { prefs -> decode(prefs[key]) }

    /**
     * Toda mutação passa por aqui: lê o estado atual **dentro** da transação do DataStore e
     * escreve o resultado. Ler fora e escrever depois abriria janela para perder uma edição
     * concorrente.
     */
    private suspend fun mutate(transform: (List<ChartCardConfig>) -> List<ChartCardConfig>) {
        dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(transform(decode(prefs[key])))
        }
    }

    suspend fun save(cards: List<ChartCardConfig>) = mutate { cards }

    suspend fun add(card: ChartCardConfig) = mutate { it + card }

    suspend fun remove(id: String) = mutate { list -> list.filterNot { it.id == id } }

    suspend fun update(card: ChartCardConfig) =
        mutate { list -> list.map { if (it.id == card.id) card else it } }

    companion object {
        /**
         * Dashboard de estreia. Um dashboard vazio na primeira abertura não ensina nada —
         * estes quatro mostram os tipos disponíveis com dados que o usuário já registrou.
         */
        val DEFAULT_CARDS = listOf(
            ChartCardConfig(
                id = "default-streak",
                type = ChartType.BIG_NUMBER,
                metric = Metric.FASTING_STREAK,
                period = Period.LAST_30,
            ),
            ChartCardConfig(
                id = "default-fasting",
                type = ChartType.LINE,
                metric = Metric.FASTING_HOURS,
                period = Period.LAST_30,
            ),
            ChartCardConfig(
                id = "default-weight",
                type = ChartType.LINE,
                metric = Metric.WEIGHT,
                period = Period.LAST_90,
            ),
            ChartCardConfig(
                id = "default-heatmap",
                type = ChartType.HEATMAP,
                metric = Metric.FASTING_HOURS,
                period = Period.LAST_90,
            ),
        )
    }
}
