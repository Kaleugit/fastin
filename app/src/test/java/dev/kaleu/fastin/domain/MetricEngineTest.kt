package dev.kaleu.fastin.domain

import dev.kaleu.fastin.domain.metrics.Aggregation
import dev.kaleu.fastin.domain.metrics.ChartCardConfig
import dev.kaleu.fastin.domain.metrics.ChartType
import dev.kaleu.fastin.domain.metrics.Metric
import dev.kaleu.fastin.domain.metrics.MetricEngine
import dev.kaleu.fastin.domain.metrics.Period
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MetricEngineTest {

    private val today = LocalDate.parse("2026-03-10")

    private fun log(
        day: String,
        last: String? = null,
        first: String? = null,
        weight: Double? = null,
        deficit: Tristate? = null,
        water: Tristate? = null,
        quality: Quality? = null,
        alcohol: YesNo? = null,
    ) = FastingLog(
        date = LocalDate.parse(day),
        lastMealTime = last?.let(LocalTime::parse),
        firstMealTime = first?.let(LocalTime::parse),
        caloricDeficit = deficit,
        mealQuality = quality,
        water2l = water,
        alcohol = alcohol,
        weight = weight,
    )

    private fun logs(vararg l: FastingLog) = l.associateBy { it.date }

    private fun config(
        metric: Metric,
        type: ChartType = ChartType.LINE,
        period: Period = Period.LAST_7,
        aggregation: Aggregation = Aggregation.AVERAGE,
    ) = ChartCardConfig("c", type, metric, period, aggregation)

    @Test
    fun `horas de jejum nao truncam os minutos`() {
        // 20:00 -> 11:50 = 15h50, que é 15,833h. toHours() devolveria 15 e perderia 50min.
        val data = MetricEngine.resolve(
            config(Metric.FASTING_HOURS),
            logs(log("2026-03-08", last = "20:00"), log("2026-03-09", first = "11:50")),
            today,
        )
        assertEquals(1, data.points.size)
        assertEquals(15.833, data.points.single().value, 0.01)
    }

    @Test
    fun `dia sem o campo preenchido fica fora da serie e nao vira zero`() {
        // Zerar dado ausente inventaria informação: "não registrei" != "não bebi".
        val data = MetricEngine.resolve(
            config(Metric.WEIGHT),
            logs(
                log("2026-03-08", weight = 80.0),
                log("2026-03-09"), // registrado, mas sem peso
                log("2026-03-10", weight = 79.0),
            ),
            today,
        )
        assertEquals(listOf(80.0, 79.0), data.points.map { it.value })
        assertEquals(79.5, data.scalar!!, 0.001)
    }

    @Test
    fun `taxa vira percentual e talvez vale meio`() {
        val data = MetricEngine.resolve(
            config(Metric.CALORIC_DEFICIT_RATE),
            logs(
                log("2026-03-08", deficit = Tristate.YES),
                log("2026-03-09", deficit = Tristate.MAYBE),
                log("2026-03-10", deficit = Tristate.NO),
            ),
            today,
        )
        // média(1 ; 0,5 ; 0) = 0,5 -> 50%
        assertEquals(50.0, data.scalar!!, 0.001)
        assertEquals(listOf(1.0, 0.5, 0.0), data.points.map { it.value })
    }

    @Test
    fun `qualidade media usa escala 1 a 3`() {
        val data = MetricEngine.resolve(
            config(Metric.MEAL_QUALITY_AVG),
            logs(
                log("2026-03-09", quality = Quality.GOOD),
                log("2026-03-10", quality = Quality.BAD),
            ),
            today,
        )
        assertEquals(2.0, data.scalar!!, 0.001)
    }

    @Test
    fun `dias com alcool somam apenas os sim`() {
        val data = MetricEngine.resolve(
            config(Metric.ALCOHOL_DAYS, aggregation = Aggregation.SUM),
            logs(
                log("2026-03-08", alcohol = YesNo.YES),
                log("2026-03-09", alcohol = YesNo.NO),
                log("2026-03-10", alcohol = YesNo.YES),
            ),
            today,
        )
        assertEquals(2.0, data.scalar!!, 0.001)
    }

    @Test
    fun `agregacoes fazem o que dizem`() {
        val values = listOf(10.0, 20.0, 30.0)
        assertEquals(20.0, MetricEngine.aggregate(values, Aggregation.AVERAGE)!!, 0.001)
        assertEquals(60.0, MetricEngine.aggregate(values, Aggregation.SUM)!!, 0.001)
        assertEquals(3.0, MetricEngine.aggregate(values, Aggregation.COUNT)!!, 0.001)
        assertEquals(10.0, MetricEngine.aggregate(values, Aggregation.MIN)!!, 0.001)
        assertEquals(30.0, MetricEngine.aggregate(values, Aggregation.MAX)!!, 0.001)
    }

    @Test
    fun `serie vazia devolve null exceto para contagem`() {
        // Média de nada é indefinida, não zero — exibir "0 kg" seria mentira.
        assertNull(MetricEngine.aggregate(emptyList(), Aggregation.AVERAGE))
        assertNull(MetricEngine.aggregate(emptyList(), Aggregation.MIN))
        assertEquals(0.0, MetricEngine.aggregate(emptyList(), Aggregation.COUNT)!!, 0.001)
    }

    @Test
    fun `periodos resolvem para as janelas certas`() {
        assertEquals(
            LocalDate.parse("2026-03-04") to today,
            MetricEngine.range(config(Metric.WEIGHT, period = Period.LAST_7), today),
        )
        assertEquals(
            LocalDate.parse("2026-02-09") to today,
            MetricEngine.range(config(Metric.WEIGHT, period = Period.LAST_30), today),
        )
        assertEquals(
            LocalDate.parse("2026-03-01") to LocalDate.parse("2026-03-31"),
            MetricEngine.range(config(Metric.WEIGHT, period = Period.CURRENT_MONTH), today),
        )
    }

    @Test
    fun `intervalo customizado invertido pelo usuario e corrigido em vez de vazio`() {
        val cfg = ChartCardConfig(
            id = "c",
            type = ChartType.LINE,
            metric = Metric.WEIGHT,
            period = Period.CUSTOM,
            customFrom = "2026-03-10",
            customTo = "2026-03-01",
        )
        assertEquals(
            LocalDate.parse("2026-03-01") to LocalDate.parse("2026-03-10"),
            MetricEngine.range(cfg, today),
        )
    }

    @Test
    fun `dado fora do periodo nao entra na serie`() {
        val data = MetricEngine.resolve(
            config(Metric.WEIGHT, period = Period.LAST_7),
            logs(
                log("2026-01-01", weight = 99.0), // muito antigo
                log("2026-03-09", weight = 80.0),
            ),
            today,
        )
        assertEquals(listOf(80.0), data.points.map { it.value })
    }

    @Test
    fun `base vazia produz card vazio, nao crash`() {
        val data = MetricEngine.resolve(config(Metric.WEIGHT), emptyMap(), today)
        assertTrue(data.points.isEmpty())
        assertNull(data.scalar)
        assertTrue(data.isEmpty)
    }
}
