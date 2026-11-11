package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import kotlin.math.roundToInt

@Composable
internal fun LinhaComGesto(chave: String, onExcluir: () -> Unit, onTrocarCategoria: () -> Unit, conteudo: @Composable () -> Unit) {
    val densidade = LocalDensity.current
    val limite = with(densidade) { 96.dp.toPx() }
    var deslocamento by remember(chave) { mutableFloatStateOf(0f) }
    var largura by remember(chave) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { largura = it.width }
            .pointerInput(chave) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            deslocamento <= -limite -> onExcluir()
                            deslocamento >= limite -> onTrocarCategoria()
                        }
                        deslocamento = 0f
                    },
                    onDragCancel = { deslocamento = 0f },
                    onHorizontalDrag = { mudanca, arraste ->
                        mudanca.consume()
                        val maximo = (largura.toFloat() * 0.6f).coerceAtLeast(limite)
                        deslocamento = (deslocamento + arraste).coerceIn(-maximo, maximo)
                    },
                )
            },
    ) {
        AcoesDoGesto(modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(deslocamento.roundToInt(), 0) },
        ) {
            conteudo()
        }
    }
}

@Composable
private fun AcoesDoGesto(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
    ) {
        AcaoDoGesto(
            texto = "Categoria",
            icone = PingoIcons.Tag,
            fundo = PingoTheme.colors.primary,
            conteudo = PingoTheme.colors.onPrimary,
            modifier = Modifier
                .fillMaxHeight()
                .width(96.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        AcaoDoGesto(
            texto = "Excluir",
            icone = PingoIcons.Trash2,
            fundo = PingoTheme.colors.expense,
            conteudo = PingoTheme.colors.onPrimary,
            modifier = Modifier
                .fillMaxHeight()
                .width(96.dp),
        )
    }
}

@Composable
private fun AcaoDoGesto(texto: String, icone: ImageVector, fundo: Color, conteudo: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(fundo),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs, Alignment.CenterVertically),
    ) {
        PingoIcon(
            imageVector = icone,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = conteudo,
        )
        PingoText(text = texto, style = PingoTheme.typography.labelSmall, color = conteudo, maxLines = 1)
    }
}
