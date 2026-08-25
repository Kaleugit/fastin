package dev.kaleu.fastin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.ui.theme.Eyebrow
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinMotion
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import dev.kaleu.fastin.ui.theme.sunken

/**
 * Segmented control para os campos enum do registro (sim/talvez/não, bom/médio/ruim).
 *
 * Todos os campos são opcionais, então **tocar na opção já selecionada limpa o campo**.
 * Sem isso não haveria como desfazer um toque errado — e o formulário passaria a ter
 * campos obrigatórios de fato, violando PROJECT.md §2.
 *
 * @param options pares de (valor, rótulo). O valor `null` nunca aparece como opção; é o
 *   resultado de desmarcar.
 */
@Composable
fun <T> ChoiceChipRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "",
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))

        // A trilha é um poço; os chips saem dela. Mesma lógica do slider da img-ref01.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sunken(shape = FastinShapes.chip)
                .padding(Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                ChoiceChip(
                    text = text,
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) null else value) },
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (testTagPrefix.isNotEmpty()) {
                                Modifier.testTag("${testTagPrefix}_$value")
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) FastinColors.onAccent else FastinColors.textSecondary,
        animationSpec = FastinMotion.standard(),
        label = "chipTextColor",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .semantics { this.selected = selected; role = Role.Tab }
            .pressable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier
                        .accentGlow(shape = FastinShapes.chip, elevation = 12.dp)
                        .background(FastinColors.accentGradient, FastinShapes.chip)
                } else {
                    Modifier
                },
            )
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = FastinType.label,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
