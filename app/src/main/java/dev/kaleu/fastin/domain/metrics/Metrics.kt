package dev.kaleu.fastin.domain.metrics

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Vocabulário do dashboard (PROJECT.md §3.4). Todos os enums são serializados **pelo nome**
 * no DataStore, então renomear uma constante quebra a configuração salva do usuário.
 */

@Serializable
enum class ChartType { LINE, SCATTER, HEATMAP, BIG_NUMBER }

@Serializable
enum class Metric(
    val label: String,
    /** Sufixo exibido junto do valor. Vazio quando o número fala por si. */
    val unit: String,
    /** Métricas de proporção são exibidas como 0–100%, não como 0–1. */
    val isRate: Boolean = false,
) {
    FASTING_HOURS("Horas de jejum", "h"),
    WEIGHT("Peso", "kg"),
    CALORIC_DEFICIT_RATE("Dias com déficit calórico", "%", isRate = true),
    WATER_RATE("Dias com água ≥ 2L", "%", isRate = true),
    MEAL_QUALITY_AVG("Qualidade das refeições", ""),
    ALCOHOL_DAYS("Dias com álcool", ""),
    FASTING_STREAK("Streak de jejum ≥ 16h", "dias"),
}

@Serializable
enum class Period(val label: String) {
    LAST_7("Últimos 7 dias"),
    LAST_30("Últimos 30 dias"),
    LAST_90("Últimos 90 dias"),
    CURRENT_MONTH("Mês atual"),
    CUSTOM("Intervalo customizado"),
}

@Serializable
enum class Aggregation(val label: String) {
    AVERAGE("Média"),
    SUM("Soma"),
    COUNT("Contagem"),
    MIN("Mínimo"),
    MAX("Máximo"),
}

/**
 * Configuração de um card do dashboard. Persistida em JSON no DataStore, por isso
 * [Serializable] e por isso [id] é estável — é a chave de remoção e reordenação.
 */
@Serializable
data class ChartCardConfig(
    val id: String,
    val type: ChartType,
    val metric: Metric,
    val period: Period = Period.LAST_30,
    val aggregation: Aggregation = Aggregation.AVERAGE,
    /** Só usados quando [period] é [Period.CUSTOM]. ISO-8601. */
    val customFrom: String? = null,
    val customTo: String? = null,
) {
    /** Streak só faz sentido como número grande — não existe "série de streak" por dia. */
    val isValid: Boolean
        get() = metric != Metric.FASTING_STREAK || type == ChartType.BIG_NUMBER
}

/** Um ponto da série. [value] já normalizado na unidade da métrica. */
data class MetricPoint(val date: LocalDate, val value: Double)

/**
 * Resultado pronto para desenhar. Os renderizadores conhecem só isto — não sabem o que é
 * jejum, peso ou álcool. Métrica nova = mexer só em [MetricEngine].
 */
data class ChartData(
    val config: ChartCardConfig,
    val points: List<MetricPoint>,
    /** Valor agregado, para `BIG_NUMBER`. Null quando não há dado no período. */
    val scalar: Double?,
    val from: LocalDate,
    val to: LocalDate,
) {
    val isEmpty: Boolean get() = points.isEmpty() && scalar == null
    val min: Double get() = points.minOfOrNull { it.value } ?: 0.0
    val max: Double get() = points.maxOfOrNull { it.value } ?: 0.0
}
