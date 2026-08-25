package dev.kaleu.fastin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.ui.theme.Eyebrow
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.FastinType
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.sunken
import java.time.LocalTime

/**
 * Campo de hora. Abre o time picker do sistema — reimplementar um relógio custaria muito e
 * o do sistema já é acessível e familiar.
 *
 * Vazio é um estado legítimo e visível: mostra "--:--" e um botão de limpar quando
 * preenchido. Todo campo é opcional (PROJECT.md §2).
 */
@Composable
fun TimeField(
    label: String,
    value: LocalTime?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sunken()
                .defaultMinSize(minHeight = Spacing.touchTarget)
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
                .pressable(onClick = onPick)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--",
                style = FastinType.displayMedium,
                color = if (value != null) FastinColors.textPrimary else FastinColors.textMuted,
            )
            if (value != null) {
                CircleIconButton(
                    icon = dev.kaleu.fastin.ui.theme.FastinIcons.Close,
                    contentDescription = "Limpar $label",
                    onClick = onClear,
                    size = 32.dp,
                )
            }
        }
    }
}

/**
 * Campo de peso. Teclado decimal.
 *
 * A validação é permissiva de propósito: o texto cru fica no estado do formulário e só vira
 * `Double` no salvar. Rejeitar dígito a dígito enquanto se digita "8" antes de "82.4" é o
 * tipo de esperteza que atrapalha.
 */
@Composable
fun WeightField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sunken()
                .defaultMinSize(minHeight = Spacing.touchTarget)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("--,-", style = FastinType.displayMedium, color = FastinColors.textMuted)
                }
                BasicTextField(
                    value = value,
                    // Aceita vírgula porque é o separador do teclado pt-BR; normaliza na leitura.
                    onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' || it == ',' }) },
                    textStyle = FastinType.displayMedium.copy(color = FastinColors.textPrimary),
                    cursorBrush = SolidColor(FastinColors.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
                )
            }
            Text(
                text = "kg",
                style = FastinType.label,
                color = FastinColors.textMuted,
                modifier = Modifier.padding(start = Spacing.sm, bottom = 4.dp),
            )
        }
    }
}

/** Observações livres do dia (PROJECT.md §2 `notes`). Multilinha, sem limite de tamanho. */
@Composable
fun NotesField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))

        Box(
            Modifier
                .fillMaxWidth()
                .sunken(shape = FastinShapes.control)
                .defaultMinSize(minHeight = 96.dp)
                .padding(Spacing.lg),
        ) {
            if (value.isEmpty()) {
                Text(placeholder, style = FastinType.body, color = FastinColors.textMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.copy(color = FastinColors.textPrimary),
                cursorBrush = SolidColor(FastinColors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
            )
        }
    }
}
