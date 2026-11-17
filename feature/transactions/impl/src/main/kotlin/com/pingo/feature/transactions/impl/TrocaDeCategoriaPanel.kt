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
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoSheetHandle
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.iconeDaCategoria

private val CategorySheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun PainelDeTrocaDeCategoria(troca: TrocaDeCategoria, onSelecionar: (String) -> Unit, onFechar: () -> Unit) {
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
                .fillMaxHeight(0.75f)
                .testTag("painel-categoria")
                .pingoElevation(level = PingoElevationLevel.Level3, shape = CategorySheetShape, offsetY = (-8).dp)
                .background(PingoTheme.colors.surface, CategorySheetShape)
                .clickable(interactionSource = interacao, indication = null) {}
                .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
        ) {
            PingoSheetHandle()
            CabecalhoDaTroca(troca = troca, onFechar = onFechar)
            OpcoesDaTroca(troca = troca, onSelecionar = onSelecionar, modifier = Modifier.weight(1f))
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
private fun CabecalhoDaTroca(troca: TrocaDeCategoria, onFechar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = "Trocar categoria", style = PingoTheme.typography.title)
        Box(
            modifier = Modifier
                .size(PingoLayout.minTouchTarget)
                .testTag("fechar-categoria")
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
    PingoText(
        text = troca.estabelecimento,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(top = PingoSpacing.xs),
    )
}

@Composable
private fun OpcoesDaTroca(troca: TrocaDeCategoria, onSelecionar: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = PingoSpacing.md),
    ) {
        troca.opcoes.forEach { opcao ->
            OpcaoDeCategoria(
                nome = opcao.nome,
                publicId = opcao.publicId,
                atual = opcao.publicId == troca.categoriaAtualPublicId,
                onClick = { onSelecionar(opcao.publicId) },
            )
        }
    }
}

@Composable
private fun OpcaoDeCategoria(nome: String, publicId: String, atual: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("categoria-opcao-$publicId")
            .clickable(onClick = onClick)
            .padding(vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconeDaCategoria(nome),
            size = 36.dp,
            iconSize = 18.dp,
            cornerRadius = PingoRadius.sm,
        )
        PingoText(
            text = nome,
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (atual) {
            PingoIcon(
                imageVector = PingoIcons.Check,
                contentDescription = "Categoria atual",
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
