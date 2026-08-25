package dev.kaleu.fastin.domain.fasting

import dev.kaleu.fastin.domain.model.FastingLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FastingCalculatorTest {

    // São Paulo: fuso do usuário e, historicamente, fuso com horário de verão — serve
    // para provar que a duração acompanha o relógio de parede.
    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun log(day: String, last: String? = null, first: String? = null) = FastingLog(
        date = LocalDate.parse(day),
        lastMealTime = last?.let(LocalTime::parse),
        firstMealTime = first?.let(LocalTime::parse),
    )

    @Test
    fun `jejum atravessa a meia-noite e usa a ultima refeicao do dia anterior`() {
        val w = FastingCalculator.window(
            day = LocalDate.parse("2026-03-10"),
            previous = log("2026-03-09", last = "20:00"),
            current = log("2026-03-10", first = "12:00"),
            zone = zone,
        )!!

        assertEquals(Duration.ofHours(16), w.closedDuration)
        assertFalse(w.isOpen)
    }

    @Test
    fun `sem ultima refeicao no dia anterior nao existe janela`() {
        val w = FastingCalculator.window(
            day = LocalDate.parse("2026-03-10"),
            previous = log("2026-03-09"),
            current = log("2026-03-10", first = "12:00"),
            zone = zone,
        )
        assertNull(w)
    }

    @Test
    fun `sem primeira refeicao a janela fica aberta e mede contra agora`() {
        val w = FastingCalculator.window(
            day = LocalDate.parse("2026-03-10"),
            previous = log("2026-03-09", last = "20:00"),
            current = null,
            zone = zone,
        )!!

        assertTrue(w.isOpen)
        assertNull(w.closedDuration)

        val now = LocalDate.parse("2026-03-10").atTime(9, 30).atZone(zone).toInstant()
        assertEquals(Duration.ofHours(13).plusMinutes(30), FastingCalculator.elapsed(w, now))
    }

    @Test
    fun `janela negativa e implausivel sao rejeitadas`() {
        // Primeira refeição antes da última do dia anterior seria duração negativa.
        assertNull(
            FastingCalculator.window(
                day = LocalDate.parse("2026-03-10"),
                previous = log("2026-03-09", last = "23:00"),
                current = log("2026-03-10", first = "22:00"),
                zone = zone,
            )?.takeIf { it.closedDuration!!.isNegative },
        )

        // 49h é esquecimento de registro, não jejum. window() devolve null.
        val far = FastingCalculator.window(
            day = LocalDate.parse("2026-03-12"),
            previous = log("2026-03-11", last = "20:00"),
            current = log("2026-03-12", first = "21:00"),
            zone = zone,
        )
        assertEquals(Duration.ofHours(25), far?.closedDuration)
    }

    @Test
    fun `marcos sao 16 18 20 24 e acendem conforme o decorrido`() {
        val w = FastingCalculator.window(
            day = LocalDate.parse("2026-03-10"),
            previous = log("2026-03-09", last = "20:00"),
            current = null,
            zone = zone,
        )!!

        // 18h30 depois do início (20:00 + 18h30 = 14:30 do dia seguinte).
        val now = LocalDate.parse("2026-03-10").atTime(14, 30).atZone(zone).toInstant()
        val ms = FastingCalculator.milestones(w, now)

        assertEquals(listOf(16, 18, 20, 24), ms.map { it.hours })
        assertEquals(listOf(true, true, false, false), ms.map { it.isReached })

        // O marco de 16h caía às 12:00 do dia seguinte.
        val m16 = ms.first { it.hours == 16 }
        assertEquals(
            LocalDate.parse("2026-03-10").atTime(12, 0).atZone(zone).toInstant(),
            m16.reachedAt,
        )
    }

    @Test
    fun `janela fechada mede marcos contra o fim real, nao contra o relogio`() {
        val w = FastingCalculator.window(
            day = LocalDate.parse("2026-03-10"),
            previous = log("2026-03-09", last = "20:00"),
            current = log("2026-03-10", first = "12:00"), // fechou em 16h
            zone = zone,
        )!!

        // "Agora" é dias depois; nem por isso os marcos de 20h/24h contam como batidos.
        val muchLater = LocalDate.parse("2026-03-20").atTime(12, 0).atZone(zone).toInstant()
        val reached = FastingCalculator.milestones(w, muchLater).filter { it.isReached }

        assertEquals(listOf(16), reached.map { it.hours })
        assertEquals(Duration.ofHours(16), FastingCalculator.elapsed(w, muchLater))
    }

    @Test
    fun `currentWindow pega a ultima refeicao registrada e some quando encerrada`() {
        val logs = listOf(
            log("2026-03-08", last = "21:00", first = "13:00"),
            log("2026-03-09", last = "20:00"),
        ).associateBy { it.date }

        val now = LocalDate.parse("2026-03-10").atTime(10, 0).atZone(zone).toInstant()
        val w = FastingCalculator.currentWindow(logs, now, zone)!!
        assertEquals(LocalDate.parse("2026-03-10"), w.day)
        assertEquals(Duration.ofHours(14), FastingCalculator.elapsed(w, now))

        // Registrar a primeira refeição de 10/03 encerra o jejum: nada mais em andamento.
        val closed = logs + (LocalDate.parse("2026-03-10") to log("2026-03-10", first = "12:00"))
        assertNull(FastingCalculator.currentWindow(closed, now, zone))
    }

    @Test
    fun `currentWindow ignora jejum abandonado ha mais de 48h`() {
        val logs = listOf(log("2026-03-01", last = "20:00")).associateBy { it.date }
        val now = LocalDate.parse("2026-03-10").atTime(10, 0).atZone(zone).toInstant()
        assertNull(FastingCalculator.currentWindow(logs, now, zone))
    }

    @Test
    fun `base vazia nao produz janela`() {
        val now = LocalDate.parse("2026-03-10").atTime(10, 0).atZone(zone).toInstant()
        assertNull(FastingCalculator.currentWindow(emptyMap(), now, zone))
    }

    @Test
    fun `duracao acompanha o relogio de parede na virada do horario de verao`() {
        // Em 2018 SP saiu do horário de verão em 18/02: às 00:00 o relógio voltou para 23:00.
        // Jejum de 20:00 (17/02) a 12:00 (18/02) marca 16h no relógio, mas 17h reais.
        val w = FastingCalculator.window(
            day = LocalDate.parse("2018-02-18"),
            previous = log("2018-02-17", last = "20:00"),
            current = log("2018-02-18", first = "12:00"),
            zone = zone,
        )!!

        // O que importa é o tempo real sem comer — e é isso que Instant preserva.
        assertEquals(Duration.ofHours(17), w.closedDuration)
    }
}
