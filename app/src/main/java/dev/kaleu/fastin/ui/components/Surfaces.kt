package dev.kaleu.fastin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.ui.theme.Eyebrow
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinMotion
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.neumorphic

/**
 * Compressão física ao pressionar (design-system.md §6). Substitui o ripple do Material: o
 * feedback é a peça afundando, não uma onda de cor.
 *
 * `indication = null` é intencional — o ripple sobre superfície neumórfica lava a sombra.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) FastinMotion.PRESS_SCALE else 1f,
        animationSpec = FastinMotion.press(),
        label = "pressScale",
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/**
 * Card do sistema: casca neumórfica + [eyebrow] opcional + conteúdo.
 *
 * O padding interno de 24dp não é negociável — é o que faz o card respirar
 * (design-system.md §3).
 */
@Composable
fun FastinCard(
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    shape: Shape = FastinShapes.card,
    contentPadding: Dp = Spacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(shape = shape)
            .padding(contentPadding),
    ) {
        if (eyebrow != null) {
            Eyebrow(eyebrow)
            androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.lg))
        }
        content()
    }
}

/** Divisor de 1dp em `hairline` — o mesmo que separa cabeçalho e grade na `img-ref01`. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FastinColors.hairline),
    )
}

/**
 * Botão circular de ícone — as setas de mês da `img-ref01`.
 *
 * O desenho tem 40dp mas o alvo de toque é 48dp: o `Box` externo é maior que o círculo
 * visível (design-system.md §3).
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(Spacing.touchTarget)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .neumorphic(shape = CircleShape, color = FastinColors.surfaceRaised, elevation = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) FastinColors.textSecondary else FastinColors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Estado vazio desenhado. Nunca deixar tela em branco (design-system.md §8) — um card sem
 * dado precisa dizer o que fazer, não sumir.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        androidx.compose.material3.Text(
            text = message,
            style = dev.kaleu.fastin.ui.theme.FastinType.body,
            color = FastinColors.textMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        action?.invoke()
    }
}
