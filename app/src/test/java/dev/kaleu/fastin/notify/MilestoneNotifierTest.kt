package dev.kaleu.fastin.notify

import dev.kaleu.fastin.domain.model.MilestoneHours
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
    fun `agenda todos os marcos do default quando o jejum acabou de comecar`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-09", 20, 1))

        assertEquals(listOf(16, 18, 20, 24), pending.map { it.hours })
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

        assertEquals(listOf(20, 24), pending.map { it.hours })
        // 20h a partir de 20:00 cai às 16:00; faltam 1h30.
        assertEquals(Duration.ofHours(1).plusMinutes(30), pending.first().delay)
    }

    @Test
    fun `sem jejum em andamento nao agenda nada`() {
        assertTrue(MilestoneNotifier.pendingMilestones(null, at("2026-03-10", 10)).isEmpty())
    }

    @Test
    fun `todos os marcos batidos nao agenda nada`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-10", 21))
        assertTrue("nada deveria sobrar após 25h de jejum", pending.isEmpty())
    }

    /**
     * Jejum "esquecido" há dias é dado velho, não jejum em andamento. Sem esta guarda, abrir
     * o app depois de uma semana sem registrar dispararia avisos sem sentido.
     */
    @Test
    fun `jejum abandonado ha mais de 100h nao agenda nada`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-15", 10))
        assertTrue(pending.isEmpty())
    }

    /**
     * O marco de 48h tem que ser agendável: era o buraco que fez o limite de abandono subir
     * de 48h para 100h (DA-016). Aos 47h de jejum, 48h ainda está a 1h no futuro.
     */
    @Test
    fun `marco de 48h e agendado a uma hora de bater`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-11", 19), hours = listOf(48))
        assertEquals(listOf(48), pending.map { it.hours })
        assertEquals(Duration.ofHours(1), pending.single().delay)
    }

    // --- Marcos escolhidos pelo usuário (EP-002) -------------------------------------------

    /**
     * A lista de aviso é a que o usuário escolheu — a mesma do relógio. Par positivo e
     * negativo: 12h e 48h entram, 16h (que está no default) **não** entra.
     */
    @Test
    fun `notifica exatamente os marcos escolhidos`() {
        val pending = MilestoneNotifier.pendingMilestones(
            start,
            now = at("2026-03-09", 20, 1),
            hours = listOf(48, 12),
        )

        assertEquals("ordenado, mesmo recebendo fora de ordem", listOf(12, 48), pending.map { it.hours })
        assertTrue("16h não foi escolhido", pending.none { it.hours == 16 })
    }

    @Test
    fun `lista vazia nao agenda nada mesmo com jejum em andamento`() {
        val pending = MilestoneNotifier.pendingMilestones(start, now = at("2026-03-09", 20, 1), hours = emptyList())
        assertTrue(pending.isEmpty())
    }

    /**
     * Registro da decisão DA-011: 24h passa a notificar por default, porque relógio e
     * notificação agora compartilham uma lista só e a v1.2 mostrava 24h no relógio.
     */
    @Test
    fun `o default de notificacao e o mesmo do relogio`() {
        assertEquals(MilestoneHours.DEFAULT, MilestoneNotifier.pendingMilestones(start, now = start.plusSeconds(60)).map { it.hours })
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
