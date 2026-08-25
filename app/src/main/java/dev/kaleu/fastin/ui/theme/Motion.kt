package dev.kaleu.fastin.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Movimento do sistema (design-system.md §6).
 *
 * `LinearEasing` e `FastOutSlowInEasing` são proibidos: entregam o movimento como
 * interpolação de computador. Todas as curvas aqui desaceleram longamente no fim, que é o
 * que simula massa.
 */
object FastinMotion {

    /** Troca de estado, troca de mês. Sai rápido, chega devagar. */
    val standardEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Entrada de tela e de card. Ainda mais longa na chegada. */
    val enterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val STANDARD_MS = 400
    const val ENTER_MS = 550

    fun <T> standard() = tween<T>(durationMillis = STANDARD_MS, easing = standardEasing)

    fun <T> enter(delayMillis: Int = 0) =
        tween<T>(durationMillis = ENTER_MS, delayMillis = delayMillis, easing = enterEasing)

    /** Compressão ao pressionar. Mola, não curva — o retorno precisa ter inércia. */
    fun <T> press() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Escala do alvo tocável pressionado. */
    const val PRESS_SCALE = 0.97f

    /** Atraso entre cards de uma lista que entra escalonada. */
    const val STAGGER_MS = 40
}
