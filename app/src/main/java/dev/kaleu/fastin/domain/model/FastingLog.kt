package dev.kaleu.fastin.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Registro de um dia. Modelo de domínio — não conhece Room.
 *
 * Todos os campos além de [date] são nulos por design: o usuário preenche parcialmente e
 * volta depois. Ver PROJECT.md §2.
 */
data class FastingLog(
    val date: LocalDate,
    val lastMealTime: LocalTime? = null,
    val firstMealTime: LocalTime? = null,
    val caloricDeficit: Tristate? = null,
    val mealQuality: Quality? = null,
    val water2l: Tristate? = null,
    val alcohol: YesNo? = null,
    val weight: Double? = null,
    val notes: String? = null,
) {
    /** Um dia "em branco" não deve virar ponto no calendário nem linha no CSV. */
    val isEmpty: Boolean
        get() = lastMealTime == null && firstMealTime == null && caloricDeficit == null &&
            mealQuality == null && water2l == null && alcohol == null && weight == null &&
            notes.isNullOrBlank()
}

enum class Tristate { YES, MAYBE, NO }

enum class Quality { GOOD, AVERAGE, BAD }

enum class YesNo { YES, NO }
