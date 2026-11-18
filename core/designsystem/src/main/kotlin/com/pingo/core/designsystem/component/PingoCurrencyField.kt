package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Suppress("LongParameterList")
@Composable
fun PingoCurrencyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    allowNegative: Boolean = false,
    showLabel: Boolean = true,
) {
    val colors = PingoTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        if (showLabel) {
            PingoText(
                text = label,
                style = PingoTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        PingoCurrencyFieldBox(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            enabled = enabled,
            allowNegative = allowNegative,
        )

        if (supportingText != null) {
            PingoText(
                text = supportingText,
                style = PingoTheme.typography.caption,
                color = if (isError) colors.expense else colors.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PingoCurrencyFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    enabled: Boolean,
    allowNegative: Boolean,
) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.md)
    val digitos = MascaraMonetaria.extrairDigitosParaCampo(value)
    val negativo = allowNegative && MascaraMonetaria.ehNegativo(value)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .background(color = colors.surface, shape = shape)
            .border(width = 1.5.dp, color = if (isError) colors.expense else colors.outline, shape = shape)
            .padding(horizontal = PingoSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        if (allowNegative) {
            BotaoSinalMonetario(
                negativo = negativo,
                habilitado = enabled && digitos.isNotEmpty() && digitos != "0",
                onClick = { onValueChange(MascaraMonetaria.alternarSinal(value)) },
            )
        }
        CampoBasicoMonetario(
            value = value,
            digitos = digitos,
            prefixo = if (negativo) "- R$ " else "R$ ",
            label = label,
            enabled = enabled,
            allowNegative = allowNegative,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PingoSpacing.md),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun CampoBasicoMonetario(
    value: String,
    digitos: String,
    prefixo: String,
    label: String,
    enabled: Boolean,
    allowNegative: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PingoTheme.colors

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (digitos.isEmpty()) {
            PingoText(
                text = "R$ 0,00",
                style = PingoTheme.typography.body,
                color = colors.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = TextFieldValue(text = digitos, selection = TextRange(digitos.length)),
            onValueChange = { atualizado ->
                onValueChange(
                    MascaraMonetaria.aplicarAlteracao(
                        valorAtual = value,
                        novaEntrada = atualizado.text,
                        permitirNegativo = allowNegative,
                    ),
                )
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            textStyle = PingoTheme.typography.body.copy(
                color = if (enabled) colors.onBackground else colors.onSurfaceVariant,
                fontFeatureSettings = "tnum",
            ),
            cursorBrush = SolidColor(colors.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = TransformacaoMonetaria(prefixo = prefixo),
        )
    }
}

@Composable
private fun BotaoSinalMonetario(negativo: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    val colors = PingoTheme.colors
    val forma = RoundedCornerShape(PingoRadius.sm)
    val fundo = if (negativo) colors.expenseContainer else colors.primaryContainer
    val conteudo = if (negativo) colors.expense else colors.onPrimaryContainer

    Box(
        modifier = Modifier
            .clip(forma)
            .background(color = fundo, shape = forma)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = if (negativo) "−" else "+",
            style = PingoTheme.typography.bodyStrong,
            color = if (habilitado) conteudo else colors.onSurfaceVariant,
        )
    }
}

private class TransformacaoMonetaria(private val prefixo: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val bruto = text.text
        if (bruto.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }
        val centavos = bruto.toLongOrNull() ?: 0L
        val formatado = "$prefixo${MascaraMonetaria.formatarCentavos(centavos)}"
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formatado.length

            override fun transformedToOriginal(offset: Int): Int = bruto.length
        }
        return TransformedText(AnnotatedString(formatado), mapping)
    }
}
