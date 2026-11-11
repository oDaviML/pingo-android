package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun PainelDeSelecao(
    titulo: String,
    opcoes: List<OpcaoDePainel>,
    selecionado: String?,
    prefixo: String,
    testTag: String,
    onSelecionar: (String) -> Unit,
    onFechar: () -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.scrim)
            .clickable(interactionSource = interacao, indication = null, onClick = onFechar),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .testTag(testTag)
                .background(
                    color = PingoTheme.colors.surface,
                    shape = RoundedCornerShape(topStart = PingoRadius.xl, topEnd = PingoRadius.xl),
                )
                .clickable(interactionSource = interacao, indication = null) {}
                .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        ) {
            PuxadorDePainel()
            CabecalhoDoPainel(titulo = titulo, onFechar = onFechar)
            ListaDeOpcoesDoPainel(
                opcoes = opcoes,
                selecionado = selecionado,
                prefixo = prefixo,
                onSelecionar = onSelecionar,
                modifier = Modifier.weight(1f),
            )
            PingoButton(
                text = "Cancelar",
                onClick = onFechar,
                type = PingoButtonType.Text,
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun CabecalhoDoPainel(titulo: String, onFechar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = titulo, style = PingoTheme.typography.title)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onFechar),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.X,
                contentDescription = "Fechar",
                modifier = Modifier.size(20.dp),
                tint = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListaDeOpcoesDoPainel(
    opcoes: List<OpcaoDePainel>,
    selecionado: String?,
    prefixo: String,
    onSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = PingoSpacing.md),
    ) {
        if (opcoes.isEmpty()) {
            PingoText(
                text = "Nenhuma opção disponível",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        opcoes.forEach { opcao ->
            OpcaoDoPainel(
                texto = opcao.rotulo,
                testTag = "$prefixo-${opcao.publicId}",
                atual = opcao.publicId == selecionado,
                onClick = { onSelecionar(opcao.publicId) },
            )
        }
    }
}

@Composable
private fun OpcaoDoPainel(texto: String, testTag: String, atual: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = texto,
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (atual) {
            PingoIcon(
                imageVector = PingoIcons.Check,
                contentDescription = "Selecionado",
                modifier = Modifier.size(18.dp),
                tint = PingoTheme.colors.primary,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PingoTheme.colors.outlineVariant),
    )
}
