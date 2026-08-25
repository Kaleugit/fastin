package dev.kaleu.fastin.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta única do app (design-system.md §1). Dark-only por ADR-002.
 *
 * Este é o único arquivo onde literais de cor são permitidos. Um `Color(0xFF...)` em
 * qualquer composable é bug de review.
 */
object FastinColors {

    // Superfícies — luminância crescente = elevação crescente.
    val surfaceBase = Color(0xFF14161A)
    val surfaceSunken = Color(0xFF101216)
    val surface = Color(0xFF1B1E23)
    val surfaceRaised = Color(0xFF22262C)

    val hairline = Color(0x0FFFFFFF) // white @ 6%
    val shadowDark = Color(0x8C07080A) // @ 55%
    val shadowLight = Color(0x0AFFFFFF) // white @ 4%

    // Acento — a brasa. Nunca chapado; ver [accentGradient].
    val accentCore = Color(0xFFFF8A00)
    val accent = Color(0xFFFF4D00)
    val accentDeep = Color(0xFFE02D00)
    val accentGlow = Color(0x73FF4D00) // accent @ 45%

    // Texto — branco puro nunca.
    val textPrimary = Color(0xFFE8EAED)
    val textSecondary = Color(0xFF9AA0A8)
    val textMuted = Color(0xFF5A6069)
    val onAccent = Color(0xFFFFFFFF)

    /**
     * Semântica de qualidade derivada do acento. Verde/vermelho de semáforo são proibidos:
     * quebram a paleta e nenhum desses campos é erro — são graus.
     */
    val qualityHigh = accent
    val qualityMid = Color(0xFF8A7A6B)
    val qualityLow = Color(0xFF4A4F57)

    /** Diagonal topo-esquerda -> base-direita, coerente com a luz do neumorfismo. */
    val accentGradient: Brush
        get() = Brush.linearGradient(listOf(accentCore, accentDeep))

    /** Preenchimento sob a curva dos gráficos de linha: brasa que se dissolve no card. */
    val accentFadeGradient: Brush
        get() = Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.0f)),
        )
}
