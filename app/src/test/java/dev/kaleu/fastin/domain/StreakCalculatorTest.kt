package dev.kaleu.fastin.domain

import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.streak.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StreakCalculatorTest {

    private val today = LocalDate.parse("2026-03-10")

    /** Constrói dias consecutivos jejuando [hours] horas, terminando em [lastDay]. */
    private fun streakOf(days: Int, hours: Int, lastDay: LocalDate): Map<LocalDate, FastingLog> {
        val map = mutableMapOf<LocalDate, FastingLog>()
        for (i in 0 until days) {
            val day = lastDay.minusDays(i.toLong())
            // Jejum de `day` = last de (day-1) até first de day.
            val first = LocalTime.of(12, 0)
            val last = first.minusHours(hours.toLong())
            map[day.minusDays(1)] = (map[day.minusDays(1)] ?: FastingLog(day.minusDays(1)))
                .copy(lastMealTime = last)
            map[day] = (map[day] ?: FastingLog(day)).copy(firstMealTime = first)
        }
        return map
    }

    @Test
    fun `conta dias consecutivos batendo a meta`() {
        val logs = streakOf(days = 5, hours = 16, lastDay = today)
        assertEquals(5, StreakCalculator.currentStreak(logs, today))
    }

    @Test
    fun `jejum abaixo da meta quebra a sequencia`() {
        val logs = streakOf(days = 5, hours = 16, lastDay = today).toMutableMap()
        // Jantar tarde no dia 07 (23:00) encurta o jejum do dia 08 para 13h — abaixo da
        // meta. Sobram 09 e 10.
        logs[LocalDate.parse("2026-03-07")] =
            logs[LocalDate.parse("2026-03-07")]!!.copy(lastMealTime = LocalTime.of(23, 0))

        assertEquals(2, StreakCalculator.currentStreak(logs, today))
    }

    /**
     * Regra de borda que importa no uso real: às 10h da manhã o jejum de hoje ainda não
     * fechou. Zerar o streak por isso puniria quem ainda está jejuando.
     */
    @Test
    fun `hoje ainda sem primeira refeicao nao zera o streak`() {
        val logs = streakOf(days = 3, hours = 16, lastDay = today.minusDays(1)).toMutableMap()
        // Última refeição de ontem existe, mas hoje ainda não comeu: hoje não qualifica.
        logs[today.minusDays(1)] = logs[today.minusDays(1)]!!.copy(lastMealTime = LocalTime.of(20, 0))

        assertEquals(3, StreakCalculator.currentStreak(logs, today))
    }

    @Test
    fun `base vazia tem streak zero`() {
        assertEquals(0, StreakCalculator.currentStreak(emptyMap(), today))
        assertEquals(0, StreakCalculator.longestStreak(emptyMap()))
    }

    @Test
    fun `streak zera quando o ultimo jejum foi ha muito tempo`() {
        val logs = streakOf(days = 5, hours = 16, lastDay = today.minusDays(30))
        assertEquals(0, StreakCalculator.currentStreak(logs, today))
    }

    @Test
    fun `melhor sequencia olha toda a base, nao so o fim`() {
        val old = streakOf(days = 8, hours = 18, lastDay = LocalDate.parse("2026-01-20"))
        val recent = streakOf(days = 3, hours = 17, lastDay = today)
        assertEquals(8, StreakCalculator.longestStreak(old + recent))
    }

    @Test
    fun `meta customizada e respeitada`() {
        val logs = streakOf(days = 4, hours = 17, lastDay = today)
        assertEquals(4, StreakCalculator.currentStreak(logs, today, java.time.Duration.ofHours(16)))
        // Com meta de 18h, nenhum dos jejuns de 17h qualifica.
        assertEquals(0, StreakCalculator.currentStreak(logs, today, java.time.Duration.ofHours(18)))
    }
}
