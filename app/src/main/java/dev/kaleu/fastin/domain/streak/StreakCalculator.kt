package dev.kaleu.fastin.domain.streak

import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.domain.model.FastingLog
import java.time.Duration
import java.time.LocalDate

/**
 * Streak de jejum (PROJECT.md §4.1): dias consecutivos batendo a meta.
 *
 * Regra de borda que importa no uso real: o streak conta a partir de **hoje ou de ontem**.
 * Às 10h da manhã o jejum de hoje normalmente ainda não fechou (falta a primeira refeição),
 * e zerar o streak do usuário por isso seria punir quem ainda está jejuando. Se hoje ainda
 * não qualifica, a contagem começa em ontem.
 */
object StreakCalculator {

    val DEFAULT_GOAL: Duration = Duration.ofHours(16)

    fun currentStreak(
        logsByDate: Map<LocalDate, FastingLog>,
        today: LocalDate,
        goal: Duration = DEFAULT_GOAL,
    ): Int {
        val start = if (qualifies(today, logsByDate, goal)) today else today.minusDays(1)

        var count = 0
        var day = start
        while (qualifies(day, logsByDate, goal)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Melhor sequência já alcançada em toda a base. */
    fun longestStreak(
        logsByDate: Map<LocalDate, FastingLog>,
        goal: Duration = DEFAULT_GOAL,
    ): Int {
        if (logsByDate.isEmpty()) return 0
        val first = logsByDate.keys.min()
        val last = logsByDate.keys.max()

        var best = 0
        var running = 0
        var day = first
        while (!day.isAfter(last)) {
            if (qualifies(day, logsByDate, goal)) {
                running++
                if (running > best) best = running
            } else {
                running = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    private fun qualifies(
        day: LocalDate,
        logsByDate: Map<LocalDate, FastingLog>,
        goal: Duration,
    ): Boolean {
        val duration = FastingCalculator
            .window(day, logsByDate[day.minusDays(1)], logsByDate[day])
            ?.closedDuration
            ?: return false
        return duration >= goal
    }
}
