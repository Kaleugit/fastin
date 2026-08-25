package dev.kaleu.fastin.domain.metrics

import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import dev.kaleu.fastin.domain.streak.StreakCalculator
import java.time.LocalDate
import java.time.YearMonth

/**
 * Traduz [ChartCardConfig] + registros em [ChartData]. Kotlin puro, sem Android.
 *
 * Decisão central: **métricas de proporção também produzem série diária**, com valor
 * 1 / 0,5 / 0 por dia. Assim `CALORIC_DEFICIT_RATE` funciona tanto como linha (o dia a dia)
 * quanto como número grande (a média × 100 = o percentual da spec), sem dois caminhos de
 * código que podem divergir.
 *
 * Dias sem o campo preenchido **não entram na série** — não viram zero. Zerar um dado
 * ausente inventaria informação: "não registrei se bebi água" não é "não bebi água".
 */
object MetricEngine {

    fun resolve(
        config: ChartCardConfig,
        logsByDate: Map<LocalDate, FastingLog>,
        today: LocalDate,
    ): ChartData {
        val (from, to) = range(config, today)

        if (config.metric == Metric.FASTING_STREAK) {
            // Streak é sempre "até hoje", não uma série no período.
            val streak = StreakCalculator.currentStreak(logsByDate, today)
            return ChartData(config, emptyList(), streak.toDouble(), from, to)
        }

        val points = series(config.metric, logsByDate, from, to)
        return ChartData(
            config = config,
            points = points,
            scalar = aggregate(points.map { it.value }, config.aggregation)
                ?.let { if (config.metric.isRate) it * 100 else it },
            from = from,
            to = to,
        )
    }

    fun range(config: ChartCardConfig, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (config.period) {
            Period.LAST_7 -> today.minusDays(6) to today
            Period.LAST_30 -> today.minusDays(29) to today
            Period.LAST_90 -> today.minusDays(89) to today
            Period.CURRENT_MONTH -> YearMonth.from(today).atDay(1) to YearMonth.from(today).atEndOfMonth()
            Period.CUSTOM -> {
                val f = config.customFrom?.let(LocalDate::parse) ?: today.minusDays(29)
                val t = config.customTo?.let(LocalDate::parse) ?: today
                // Intervalo invertido pelo usuário não deve produzir série vazia silenciosa.
                if (f.isAfter(t)) t to f else f to t
            }
        }

    fun series(
        metric: Metric,
        logsByDate: Map<LocalDate, FastingLog>,
        from: LocalDate,
        to: LocalDate,
    ): List<MetricPoint> {
        val days = generateSequence(from) { d -> d.plusDays(1).takeIf { !it.isAfter(to) } }
        return days.mapNotNull { date ->
            valueFor(metric, date, logsByDate)?.let { MetricPoint(date, it) }
        }.toList()
    }

    /** Valor da métrica num dia, ou null se o dado não foi registrado. */
    private fun valueFor(
        metric: Metric,
        date: LocalDate,
        logsByDate: Map<LocalDate, FastingLog>,
    ): Double? {
        val log = logsByDate[date]
        return when (metric) {
            Metric.FASTING_HOURS -> FastingCalculator
                .window(date, logsByDate[date.minusDays(1)], log)
                ?.closedDuration
                // Minutos / 60 em vez de toHours(): toHours trunca, e 15h50 viraria 15h.
                ?.let { it.toMinutes() / 60.0 }

            Metric.WEIGHT -> log?.weight

            Metric.CALORIC_DEFICIT_RATE -> log?.caloricDeficit?.let(::tristateValue)
            Metric.WATER_RATE -> log?.water2l?.let(::tristateValue)

            Metric.MEAL_QUALITY_AVG -> when (log?.mealQuality) {
                Quality.GOOD -> 3.0
                Quality.AVERAGE -> 2.0
                Quality.BAD -> 1.0
                null -> null
            }

            Metric.ALCOHOL_DAYS -> when (log?.alcohol) {
                YesNo.YES -> 1.0
                YesNo.NO -> 0.0
                null -> null
            }

            Metric.FASTING_STREAK -> null // tratado em resolve()
        }
    }

    /** "Talvez" vale meio: é a leitura honesta de uma resposta incerta numa média. */
    private fun tristateValue(t: Tristate): Double = when (t) {
        Tristate.YES -> 1.0
        Tristate.MAYBE -> 0.5
        Tristate.NO -> 0.0
    }

    fun aggregate(values: List<Double>, aggregation: Aggregation): Double? {
        // COUNT é o único que faz sentido sobre lista vazia: zero dias registrados é zero.
        if (values.isEmpty()) return if (aggregation == Aggregation.COUNT) 0.0 else null
        return when (aggregation) {
            Aggregation.AVERAGE -> values.average()
            Aggregation.SUM -> values.sum()
            Aggregation.COUNT -> values.size.toDouble()
            Aggregation.MIN -> values.min()
            Aggregation.MAX -> values.max()
        }
    }
}
