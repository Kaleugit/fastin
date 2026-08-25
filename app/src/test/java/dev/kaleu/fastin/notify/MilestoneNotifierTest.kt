package dev.kaleu.fastin.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Decisão de agendamento dos marcos (PROJECT.md §4.5).
 *
 * Testa a parte pura, sem WorkManager: é onde moram os erros que o usuário sentiria — aviso
 * retroativo de um marco já batido, aviso na hora errada, ou notificação ressuscitando um
 * jejum abandonado dias atrás.
 */
class MilestoneNotifierTest {

    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun at(date: String, hour: Int, minute: Int = 0) =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant()

    /** Jantar às 20:00 do dia 09. */
    private val start = at("2026-03-09", 20)

    @Test
    fun `agenda os tres marcos quando o jejum acabou de comecar`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-09", 20, 1))

        assertEquals(listOf(16, 18, 20), pending.map { it.hours })
        // 16h a partir de 20:00 cai às 12:00; faltam 15h59 às 20:01.
        assertEquals(Duration.ofHours(15).plusMinutes(59), pending.first().delay)
    }

    /**
     * O controle que importa: às 14h30 já se passaram 18h30 de jejum. Os marcos de 16h e 18h
     * **não** podem ser reagendados — o usuário receberia um aviso do que já aconteceu.
     */
    @Test
    fun `marco ja batido nao vira notificacao retroativa`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-10", 14, 30))

        assertEquals(listOf(20), pending.map { it.hours })
        // 20h a partir de 20:00 cai às 16:00; faltam 1h30.
        assertEquals(Duration.ofHours(1).plusMinutes(30), pending.single().delay)
    }

    @Test
    fun `sem jejum em andamento nao agenda nada`() {
        assertTrue(MilestoneNotifier.pendingMilestones(null, at("2026-03-10", 10)).isEmpty())
    }

    @Test
    fun `todos os marcos batidos nao agenda nada`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-10", 18))
        assertTrue("nada deveria sobrar após 22h de jejum", pending.isEmpty())
    }

    /**
     * Jejum "esquecido" há dias é dado velho, não jejum em andamento. Sem esta guarda, abrir
     * o app depois de uma semana sem registrar dispararia avisos sem sentido.
     */
    @Test
    fun `jejum abandonado ha mais de 48h nao agenda nada`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-15", 10))
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `so notifica 16 18 e 20 - o marco de 24h e apenas visual`() {
        // A spec §4.5 pede notificação de 16/18/20; 24h aparece no relógio mas não avisa.
        assertEquals(listOf(16, 18, 20), MilestoneNotifier.NOTIFIED_HOURS)
    }

    @Test
    fun `atrasos sao sempre positivos`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-10", 11, 59))
        assertTrue(
            "atraso negativo agendaria um worker para o passado",
            pending.all { !it.delay.isNegative && !it.delay.isZero },
        )
    }
}
