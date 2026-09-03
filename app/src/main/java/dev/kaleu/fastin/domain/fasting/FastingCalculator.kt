package dev.kaleu.fastin.domain.fasting

import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.FastingWindow
import dev.kaleu.fastin.domain.model.Milestone
import dev.kaleu.fastin.domain.model.MilestoneHours
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regra central do app (PROJECT.md §2):
 *
 * > O jejum **do dia D** começa em `lastMealTime` de **D-1** e termina em `firstMealTime` de **D**.
 *
 * Objeto sem estado. Não lê o relógio: quem precisa de "agora" recebe um [Instant] de fora.
 * Isso é o que torna virada de dia e horário de verão testáveis (ADR-004).
 */
object FastingCalculator {

    /**
     * Acima disso o par de horários é incoerente (esquecimento de registro), não um jejum.
     *
     * Era 48h. Subiu para 100h na v1.3 por decisão do usuário (EPICOS.md, DA-016): o maior
     * marco escolhível é 48h e, com o limite igual a ele, o jejum virava "abandonado" no
     * exato instante em que o marco chegava — o relógio esvaziava e o aviso nunca saía.
     */
    val MAX_PLAUSIBLE: Duration = Duration.ofHours(100)

    /**
     * Monta a janela de jejum atribuída a [day].
     *
     * @param previous registro de `day - 1`, de onde sai o início.
     * @param current registro de [day], de onde sai o fim.
     * @return null se não há início registrado, ou se a janela resultante é implausível.
     */
    fun window(
        day: LocalDate,
        previous: FastingLog?,
        current: FastingLog?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): FastingWindow? {
        val lastMeal = previous?.lastMealTime ?: return null

        // A conversão para instante acontece aqui, na borda. Usar ZonedDateTime (e não
        // LocalDateTime aritmético) é o que faz a duração ficar correta na virada do
        // horário de verão: a janela encurta ou alonga como o relógio de parede manda.
        val start = day.minusDays(1).atTime(lastMeal).atZone(zone).toInstant()
        val end = current?.firstMealTime?.let { day.atTime(it).atZone(zone).toInstant() }

        if (end != null) {
            val d = Duration.between(start, end)
            if (d.isNegative || d > MAX_PLAUSIBLE) return null
        }
        return FastingWindow(day = day, start = start, end = end)
    }

    /**
     * Duração decorrida. Para janela fechada é a duração final; para aberta, mede contra [now].
     * Nunca negativa.
     */
    fun elapsed(window: FastingWindow, now: Instant): Duration {
        val until = window.end ?: now
        val d = Duration.between(window.start, until)
        return if (d.isNegative) Duration.ZERO else d
    }

    /**
     * Marcos escolhidos pelo usuário ([MilestoneHours]) com o instante previsto e se já foram
     * batidos em [now]. Para janela fechada, "batido" é medido contra o fim real — não contra
     * o relógio.
     */
    fun milestones(
        window: FastingWindow,
        now: Instant,
        hours: List<Int> = MilestoneHours.DEFAULT,
    ): List<Milestone> {
        val reference = window.end ?: now
        return hours.map { h ->
            val at = window.start.plus(Duration.ofHours(h.toLong()))
            Milestone(hours = h, reachedAt = at, isReached = !reference.isBefore(at))
        }
    }

    /**
     * A janela em andamento agora, se houver: é a do dia seguinte à última refeição
     * registrada, e só existe enquanto nenhuma primeira refeição a encerrou.
     *
     * @param logsByDate todos os registros indexados por data.
     */
    fun currentWindow(
        logsByDate: Map<LocalDate, FastingLog>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): FastingWindow? {
        val lastMealDay = logsByDate.entries
            .filter { it.value.lastMealTime != null }
            .maxByOrNull { it.key } ?: return null

        val day = lastMealDay.key.plusDays(1)
        val w = window(
            day = day,
            previous = lastMealDay.value,
            current = logsByDate[day],
            zone = zone,
        ) ?: return null

        // Já encerrada, ou aberta há tempo demais para ser um jejum real: nada em andamento.
        if (!w.isOpen) return null
        if (Duration.between(w.start, now) > MAX_PLAUSIBLE) return null
        if (now.isBefore(w.start)) return null
        return w
    }
}
