package com.pingo.feature.transactions.impl

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun CampoDeSelecao(campo: CampoSelecionavel, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(
            text = campo.rotulo,
            style = PingoTheme.typography.bodySmall,
            color = if (campo.erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant,
        )
        LinhaDeSelecao(campo = campo, onClick = onClick)
        if (campo.mensagem != null) {
            PingoText(
                text = campo.mensagem,
                style = PingoTheme.typography.caption,
                color = if (campo.erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaDeSelecao(campo: CampoSelecionavel, onClick: () -> Unit) {
    val forma = RoundedCornerShape(PingoRadius.md)
    val cor = if (campo.erro) PingoTheme.colors.expense else PingoTheme.colors.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(color = PingoTheme.colors.surface, shape = forma)
            .border(width = 1.5.dp, color = cor, shape = forma)
            .testTag(campo.testTag)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = PingoSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = campo.icone,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (campo.erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = campo.texto,
            style = PingoTheme.typography.body,
            color = if (campo.preenchido) PingoTheme.colors.onBackground else PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PingoIcon(
            imageVector = if (campo.erro) PingoIcons.TriangleAlert else PingoIcons.ChevronDown,
            contentDescription = if (campo.erro) "Campo com erro" else null,
            modifier = Modifier.size(20.dp),
            tint = if (campo.erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CampoDeData(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Data",
            texto = RotulosDoNovoLancamento.data(state.data, state.hoje),
            preenchido = true,
            icone = PingoIcons.Calendar,
            testTag = "campo-data",
        ),
        onClick = { onEvent(NovoLancamentoEvent.AbrirCalendario) },
    )
}

@Composable
internal fun CampoDeEstabelecimento(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    val rotulo = if (state.tipo == TipoDoLancamentoManual.TRANSFERENCIA) "Descrição" else "Estabelecimento ou descrição"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(
            text = rotulo,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        LinhaDoEstabelecimento(state = state, onEvent = onEvent)
    }
}

@Composable
private fun LinhaDoEstabelecimento(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    val forma = RoundedCornerShape(PingoRadius.md)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(color = PingoTheme.colors.surface, shape = forma)
            .border(width = 1.5.dp, color = PingoTheme.colors.outline, shape = forma)
            .padding(horizontal = PingoSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Tag,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
        EntradaDeEstabelecimento(
            texto = state.estabelecimento,
            onTexto = { onEvent(NovoLancamentoEvent.EstabelecimentoAlterado(it)) },
            modifier = Modifier.weight(1f),
        )
        if (state.estabelecimento.isNotEmpty()) {
            BotaoLimparEstabelecimento(onClick = { onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("")) })
        }
    }
}

@Composable
private fun EntradaDeEstabelecimento(texto: String, onTexto: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (texto.isEmpty()) {
            PingoText(
                text = "Opcional",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = texto,
            onValueChange = onTexto,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("campo-estabelecimento"),
            textStyle = PingoTheme.typography.body.copy(color = PingoTheme.colors.onBackground),
            cursorBrush = SolidColor(PingoTheme.colors.primary),
            singleLine = true,
        )
    }
}

@Composable
private fun BotaoLimparEstabelecimento(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .testTag("limpar-estabelecimento")
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.X,
            contentDescription = "Limpar estabelecimento",
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
    }
}
