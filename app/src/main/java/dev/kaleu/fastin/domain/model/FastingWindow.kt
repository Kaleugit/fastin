package dev.kaleu.fastin.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Janela de jejum atribuída a [day]: começa na última refeição de `day - 1` e termina na
 * primeira refeição de [day].
 *
 * [end] nulo significa janela **aberta** — o jejum ainda está correndo.
 */
data class FastingWindow(
    val day: LocalDate,
    val start: Instant,
    val end: Instant?,
) {
    val isOpen: Boolean get() = end == null

    /** Duração fechada da janela, ou null se ainda aberta. */
    val closedDuration: Duration? get() = end?.let { Duration.between(start, it) }
}

/** Marco de jejum (16h/18h/20h/24h) com o instante previsto para ser atingido. */
data class Milestone(
    val hours: Int,
    val reachedAt: Instant,
    val isReached: Boolean,
)
