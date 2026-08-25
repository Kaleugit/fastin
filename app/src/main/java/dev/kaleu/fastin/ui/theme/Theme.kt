package dev.kaleu.fastin.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Tema do app.
 *
 * Dark-only por ADR-002: `FastinTheme` **ignora** `isSystemInDarkTheme()` de propósito. As
 * referências de design são exclusivamente escuras e o neumorfismo depende de sombra dupla
 * sobre luminância média — um esquema claro exigiria um segundo sistema de sombras.
 *
 * Para reverter e seguir o sistema (a spec original), basta um `lightColors()` aqui e a
 * leitura de `isSystemInDarkTheme()`. Nenhum composable muda: todos leem [FastinColors].
 */
@Composable
fun FastinTheme(content: @Composable () -> Unit) {
    // O Material3 continua embaixo por causa de TextField, DatePicker e ripple. Mapeamos o
    // esquema dele para os nossos tokens, para nenhum componente Material aparecer com a
    // paleta roxa padrão caso escape do nosso design system.
    val scheme = darkColorScheme(
        primary = FastinColors.accent,
        onPrimary = FastinColors.onAccent,
        secondary = FastinColors.accentCore,
        onSecondary = FastinColors.onAccent,
        background = FastinColors.surfaceBase,
        onBackground = FastinColors.textPrimary,
        surface = FastinColors.surface,
        onSurface = FastinColors.textPrimary,
        surfaceVariant = FastinColors.surfaceRaised,
        onSurfaceVariant = FastinColors.textSecondary,
        outline = FastinColors.textMuted,
        error = FastinColors.accentDeep,
        onError = FastinColors.onAccent,
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalTextStyle provides FastinType.body.copy(color = FastinColors.textPrimary),
            content = content,
        )
    }
}

/**
 * Rótulo microscópico em caixa alta que precede todo card — a assinatura do sistema
 * (design-system.md §2). Recebe o texto já legível; a caixa alta é aplicada aqui para o
 * conteúdo em `strings.xml` continuar em português normal.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color = FastinColors.textMuted,
) {
    Text(
        text = text.uppercase(),
        style = FastinType.eyebrow,
        color = color,
        modifier = modifier,
    )
}

/** Atalho para aplicar cor a um estilo sem repetir `copy` em toda chamada. */
fun TextStyle.on(color: Color): TextStyle = this.copy(color = color)
