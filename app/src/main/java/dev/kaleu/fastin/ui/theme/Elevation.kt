package dev.kaleu.fastin.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Superfícies do sistema (design-system.md §4).
 *
 * A tentação em neumorfismo é desenhar a sombra em canvas nativo com `BlurMaskFilter`. É o
 * que a maioria das libs faz e é errado: mask filter **não é acelerado por GPU**, então
 * cada card vira repaint em software durante o scroll.
 *
 * Da API 28 em diante `Modifier.shadow()` aceita `spotColor`/`ambientColor` — sombra real
 * do render pipeline, com blur de verdade e acelerada. É essa, tingida.
 *
 * Em API 26–27 o `spotColor` é ignorado e a sombra sai preta padrão: menos refinado, nunca
 * incorreto.
 */

/**
 * Peça elevada: sombra tingida + preenchimento + hairline de luz no topo.
 *
 * O hairline é o passo que quase todo mundo pula, e é exatamente o que faz a superfície
 * parecer iluminada por cima em vez de um retângulo cinza. A luz **sempre** vem de cima —
 * inverter em um componente quebra a coerência da tela inteira.
 */
fun Modifier.neumorphic(
    shape: Shape = FastinShapes.card,
    color: Color = FastinColors.surface,
    elevation: Dp = 12.dp,
    hairline: Boolean = true,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = FastinColors.shadowDark,
        spotColor = FastinColors.shadowDark,
    )
    .background(color = color, shape = shape)
    .then(
        if (hairline) {
            Modifier.border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    // A luz morre em ~45% da altura: mais que isso vira contorno, não brilho.
                    0.0f to Color.White.copy(alpha = 0.08f),
                    0.45f to Color.Transparent,
                    1.0f to Color.Transparent,
                ),
                shape = shape,
            )
        } else {
            Modifier
        },
    )

/**
 * Poço: a peça afunda em vez de sair.
 *
 * Compose não tem inset shadow. Simulamos invertendo a direção da luz na borda interna —
 * escuro no topo, claro na base. É a inversão que o olho lê como "recuado".
 */
fun Modifier.sunken(
    shape: Shape = FastinShapes.control,
    color: Color = FastinColors.surfaceSunken,
): Modifier = this
    .background(color = color, shape = shape)
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.45f),
            0.55f to Color.Transparent,
            1.0f to Color.White.copy(alpha = 0.05f),
        ),
        shape = shape,
    )

/**
 * Halo do acento: a sombra do elemento laranja é da cor do acento, não preta. É o que faz o
 * dia selecionado no calendário parecer **aceso**, como na `img-ref01`.
 *
 * Sem preenchimento — quem chama decide o `background`, normalmente [FastinColors.accentGradient].
 */
fun Modifier.accentGlow(
    shape: Shape = FastinShapes.chip,
    elevation: Dp = 16.dp,
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = FastinColors.accentGlow,
    spotColor = FastinColors.accentGlow,
)
