package com.pingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Immutable
data class ItemChangelogUi(val destaque: String?, val descricao: String)

@Immutable
data class SecaoChangelogUi(val titulo: String?, val itens: List<ItemChangelogUi>)

@Composable
fun ChangelogAtualizacaoBloco(secoes: List<SecaoChangelogUi>, modifier: Modifier = Modifier) {
    if (secoes.isEmpty()) {
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surfaceVariant, RoundedCornerShape(PingoRadius.md))
            .padding(PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        secoes.forEach { secao ->
            secao.titulo?.let { titulo ->
                PingoText(
                    text = titulo,
                    style = PingoTheme.typography.label,
                    color = PingoTheme.colors.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
                secao.itens.forEach { item ->
                    LinhaDeChangelog(item)
                }
            }
        }
    }
}

@Composable
private fun LinhaDeChangelog(item: ItemChangelogUi) {
    val corTexto = PingoTheme.colors.onBackground
    val corSecundaria = PingoTheme.colors.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .background(color = PingoTheme.colors.primary, shape = CircleShape),
        )
        PingoText(
            text = buildAnnotatedString {
                if (item.destaque != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = corTexto)) {
                        append(item.destaque)
                    }
                    if (item.descricao.isNotBlank()) {
                        withStyle(SpanStyle(color = corSecundaria)) {
                            append(": ${item.descricao}")
                        }
                    }
                } else {
                    withStyle(SpanStyle(color = corSecundaria)) {
                        append(item.descricao)
                    }
                }
            },
            style = PingoTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
