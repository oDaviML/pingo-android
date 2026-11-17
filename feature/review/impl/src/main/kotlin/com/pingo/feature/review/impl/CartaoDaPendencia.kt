package com.pingo.feature.review.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCategoryTone
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TipoLancamento
import com.pingo.core.ui.Valores
import com.pingo.core.ui.iconeDaCategoria

@Composable
internal fun CartaoDaPendencia(pendencia: PendenciaDaRevisao, onEvent: (ReviewEvent) -> Unit, onCorrigir: (String) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier
            .testTag("pendencia-${pendencia.publicId}")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        SeloDoMotivo(pendencia = pendencia)
        LinhaDoEstabelecimento(pendencia = pendencia)
        TrechoInterpretado(pendencia = pendencia)
        if (!pendencia.podeConfirmar) {
            PingoText(
                text = RotulosDaRevisao.INSTRUMENTO_NECESSARIO,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.warning,
                modifier = Modifier.testTag("orientacao-${pendencia.publicId}"),
            )
        }
        AcoesDaPendencia(pendencia = pendencia, onEvent = onEvent, onCorrigir = onCorrigir)
    }
}

@Composable
private fun SeloDoMotivo(pendencia: PendenciaDaRevisao) {
    Box(
        modifier = Modifier
            .background(color = PingoTheme.colors.warningContainer, shape = RoundedCornerShape(PingoRadius.sm))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("motivo-${pendencia.publicId}"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PingoIcon(
                imageVector = PingoIcons.Sparkles,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PingoTheme.colors.onWarningContainer,
            )
            PingoText(
                text = RotulosDaRevisao.motivo(pendencia),
                style = PingoTheme.typography.labelSmall,
                color = PingoTheme.colors.onWarningContainer,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LinhaDoEstabelecimento(pendencia: PendenciaDaRevisao) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconeDaPendencia(pendencia),
            contentDescription = pendencia.categoriaNome?.let { "Categoria $it" } ?: "Categoria não identificada",
            tone = tomDaPendencia(pendencia.tipo),
            size = 44.dp,
            iconSize = 22.dp,
            cornerRadius = PingoRadius.md,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(
                text = pendencia.estabelecimento ?: "Lançamento",
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PingoText(
                text = "${pendencia.vinculo} · ${pendencia.horario}",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PingoText(
            text = Valores.assinado(pendencia.valorCentavos, pendencia.tipo),
            style = PingoTheme.typography.amount,
            color = Valores.corDoTipo(pendencia.tipo, PingoTheme.colors),
            maxLines = 1,
        )
    }
}

private fun iconeDaPendencia(pendencia: PendenciaDaRevisao): ImageVector = when (pendencia.tipo) {
    TipoLancamento.TRANSFERENCIA -> PingoIcons.ArrowLeftRight
    TipoLancamento.RECEITA -> if (pendencia.categoriaNome == null) PingoIcons.ArrowDownLeft else iconeDaCategoria(pendencia.categoriaNome)
    TipoLancamento.DESPESA, TipoLancamento.AJUSTE -> iconeDaCategoria(pendencia.categoriaNome)
}

private fun tomDaPendencia(tipo: TipoLancamento): PingoCategoryTone = when (tipo) {
    TipoLancamento.RECEITA -> PingoCategoryTone.Income
    TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> PingoCategoryTone.Transfer
    TipoLancamento.DESPESA -> PingoCategoryTone.Default
}

@Composable
private fun TrechoInterpretado(pendencia: PendenciaDaRevisao) {
    val disponivel = pendencia.textoInterpretado != null
    val forma = RoundedCornerShape(PingoRadius.md)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surfaceVariant, shape = forma)
            .border(width = 1.dp, color = PingoTheme.colors.outlineVariant, shape = forma)
            .padding(PingoSpacing.md)
            .testTag("trecho-${pendencia.publicId}"),
    ) {
        PingoText(
            text = pendencia.textoInterpretado?.let { "“$it”" } ?: RotulosDaRevisao.TEXTO_INDISPONIVEL,
            style = PingoTheme.typography.bodySmall,
            color = if (disponivel) PingoTheme.colors.onBackground else PingoTheme.colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AcoesDaPendencia(pendencia: PendenciaDaRevisao, onEvent: (ReviewEvent) -> Unit, onCorrigir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        AcoesSecundarias(pendencia = pendencia, onEvent = onEvent, onCorrigir = onCorrigir)
        AcoesPrincipais(pendencia = pendencia, onEvent = onEvent)
    }
}

@Composable
private fun AcoesSecundarias(pendencia: PendenciaDaRevisao, onEvent: (ReviewEvent) -> Unit, onCorrigir: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(
            text = "Corrigir",
            onClick = { onCorrigir(pendencia.publicId) },
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            modifier = Modifier.testTag("corrigir-${pendencia.publicId}"),
        )
        PingoButton(
            text = "Marcar transferência",
            onClick = { onEvent(ReviewEvent.AbrirMarcarTransferencia(pendencia.publicId)) },
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            modifier = Modifier.testTag("marcar-transferencia-${pendencia.publicId}"),
        )
    }
}

@Composable
private fun AcoesPrincipais(pendencia: PendenciaDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(
            text = "Descartar",
            onClick = { onEvent(ReviewEvent.Descartar(pendencia.publicId)) },
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            modifier = Modifier.testTag("descartar-${pendencia.publicId}"),
        )
        Box(modifier = Modifier.weight(1f))
        PingoButton(
            text = "Trocar categoria",
            onClick = { onEvent(ReviewEvent.AbrirTrocaDeCategoria(pendencia.publicId)) },
            type = PingoButtonType.Outline,
            size = PingoButtonSize.Small,
            modifier = Modifier.testTag("trocar-categoria-${pendencia.publicId}"),
        )
        PingoButton(
            text = "Confirmar",
            onClick = { onEvent(ReviewEvent.Confirmar(pendencia.publicId)) },
            type = PingoButtonType.Primary,
            size = PingoButtonSize.Small,
            enabled = pendencia.podeConfirmar,
            modifier = Modifier.testTag("confirmar-${pendencia.publicId}"),
        )
    }
}
