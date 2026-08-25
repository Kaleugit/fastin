package dev.kaleu.fastin.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.kaleu.fastin.R

/**
 * Tipografia do app (design-system.md §2).
 *
 * Roboto é proibido — é o default do sistema e entrega o app como template. Duas famílias
 * variáveis empacotadas em `res/font/`: o app é offline, downloadable fonts não servem.
 *
 * `FontVariation` exige API 26, que é o nosso minSdk (ADR-005).
 */
@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: Int) = Font(
    R.font.outfit_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Display: geométrica, numerais circulares. Números e títulos. */
val Outfit = FontFamily(outfit(200), outfit(300), outfit(400), outfit(500))

/** UI: rótulos, corpo, notas. */
val Manrope = FontFamily(manrope(400), manrope(500), manrope(600))

/**
 * Numerais tabulares. Obrigatório onde o número muda sozinho (relógio, contadores):
 * sem isso os dígitos têm larguras diferentes e o texto treme a cada segundo.
 */
private const val TABULAR = "tnum"

object FastinType {

    /** Relógio de jejum. O maior elemento da tela inicial. */
    val clock = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight(200),
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.036).em,
        fontFeatureSettings = TABULAR,
    )

    val displayLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight(300),
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.037).em,
        fontFeatureSettings = TABULAR,
    )

    val displayMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight(300),
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.018).em,
        fontFeatureSettings = TABULAR,
    )

    val title = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight(400),
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    /** Número do dia na grade do calendário. Tabular para a grade não dançar. */
    val calendarDay = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        fontFeatureSettings = TABULAR,
    )

    val body = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight(400),
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    val label = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight(500),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.015.em,
    )

    /**
     * A assinatura do sistema: rótulo microscópico, caixa alta, tracking largo, precedendo
     * todo card. É o que dá o ar editorial das referências.
     */
    val eyebrow = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight(600),
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.16.em,
    )
}
