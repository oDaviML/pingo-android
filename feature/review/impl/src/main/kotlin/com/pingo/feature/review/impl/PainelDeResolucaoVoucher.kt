package com.pingo.feature.review.impl

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion
import com.pingo.core.model.voucher.MotivoRevisaoRecarga
import com.pingo.core.ui.Valores

@Composable
internal fun PainelDeResolucaoVoucher(painel: ResolucaoDeVoucherDaRevisao, ocupado: Boolean, onEvent: (ReviewEvent) -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val titulo = when (painel.motivo) {
        MotivoRevisaoRecarga.BENEFICIO_AMBIGUO -> "Qual benefício recebeu?"
        MotivoRevisaoRecarga.DIVERGENCIA_RECARGA -> "Resolver divergência de recarga"
    }
    val subtitulo = "${painel.estabelecimento} · ${Valores.formatar(painel.valorCapturadoCentavos)}"
    val conteudoModifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 420.dp)
        .verticalScroll(rememberScrollState())
        .let { if (reducedMotion) it else it.animateContentSize() }

    PingoBottomSheet(
        onDismissRequest = { onEvent(ReviewEvent.FecharResolucaoVoucher) },
        title = titulo,
        subtitle = subtitulo,
        modifier = Modifier.testTag("painel-voucher-revisao"),
    ) {
        Column(
            modifier = conteudoModifier,
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            ExplicacaoDoMotivo(painel.motivo)
            SecaoDeVouchers(painel = painel, onEvent = onEvent)
            if (painel.voucherSelecionadoPublicId != null) {
                SecaoDeOcorrenciasEDecisao(painel = painel, onEvent = onEvent)
            }
        }
        AcoesDoPainelDeVoucher(painel = painel, ocupado = ocupado, onEvent = onEvent)
    }
}

@Composable
private fun ExplicacaoDoMotivo(motivo: MotivoRevisaoRecarga) {
    val texto = when (motivo) {
        MotivoRevisaoRecarga.BENEFICIO_AMBIGUO ->
            "Não foi possível identificar automaticamente qual benefício (VR, VA ou VT) recebeu a recarga. Escolha o voucher de destino."
        MotivoRevisaoRecarga.DIVERGENCIA_RECARGA ->
            "O valor ou a data capturada difere da recarga prevista. Confira os valores e escolha como registrar."
    }
    PingoText(
        text = texto,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun SecaoDeVouchers(painel: ResolucaoDeVoucherDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    if (painel.motivo == MotivoRevisaoRecarga.DIVERGENCIA_RECARGA && painel.vouchersElegiveis.size <= 1) {
        painel.voucherSelecionado?.let { voucher ->
            VoucherIdentificadoCard(voucher = voucher)
        }
        return
    }

    PingoText(
        text = "Benefício de destino",
        style = PingoTheme.typography.label,
        color = PingoTheme.colors.onBackground,
    )
    if (painel.vouchersElegiveis.isEmpty()) {
        PingoText(
            text = "Nenhum voucher ativo encontrado.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.warning,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
            painel.vouchersElegiveis.forEach { voucher ->
                OpcaoSelecionavel(
                    titulo = voucher.titulo,
                    subtitulo = voucher.detalhe,
                    selecionado = voucher.publicId == painel.voucherSelecionadoPublicId,
                    onClick = { onEvent(ReviewEvent.SelecionarVoucher(voucher.publicId)) },
                    modifier = Modifier.testTag("voucher-opcao-${voucher.publicId}"),
                )
            }
        }
    }
}

@Composable
private fun VoucherIdentificadoCard(voucher: VoucherElegivelDaRevisao) {
    val shape = RoundedCornerShape(PingoRadius.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surfaceVariant, shape = shape)
            .border(width = 1.dp, color = PingoTheme.colors.outlineVariant, shape = shape)
            .padding(PingoSpacing.md)
            .testTag("voucher-identificado"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PingoText(
            text = "Voucher identificado: ${voucher.titulo}",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        voucher.detalhe?.let { detalhe ->
            PingoText(
                text = detalhe,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecaoDeOcorrenciasEDecisao(painel: ResolucaoDeVoucherDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    val ocorrencias = painel.ocorrenciasDoVoucherSelecionado
    if (painel.motivo == MotivoRevisaoRecarga.BENEFICIO_AMBIGUO && ocorrencias.isEmpty()) {
        PingoText(
            text = "Nenhuma recarga prevista encontrada para este benefício. Será registrada como recarga extra.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.testTag("voucher-sem-previsao"),
        )
        return
    }

    if (painel.motivo == MotivoRevisaoRecarga.DIVERGENCIA_RECARGA) {
        PingoText(
            text = "Valor capturado: ${Valores.formatar(painel.valorCapturadoCentavos)}",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.testTag("voucher-valor-capturado"),
        )
    }

    if (ocorrencias.isNotEmpty()) {
        ListaDeOcorrenciasPrevistas(
            ocorrencias = ocorrencias,
            selecionadaPublicId = painel.ocorrenciaSelecionadaPublicId,
            onEvent = onEvent,
        )
    }

    OpcoesDeDecisaoVoucher(
        temOcorrencias = ocorrencias.isNotEmpty(),
        decisaoSelecionada = painel.decisaoSelecionada,
        onEvent = onEvent,
    )
}

@Composable
private fun ListaDeOcorrenciasPrevistas(
    ocorrencias: List<OcorrenciaPrevistaDaRevisao>,
    selecionadaPublicId: String?,
    onEvent: (ReviewEvent) -> Unit,
) {
    PingoText(
        text = if (ocorrencias.size > 1) "Escolha a recarga prevista" else "Recarga prevista",
        style = PingoTheme.typography.label,
        color = PingoTheme.colors.onBackground,
    )
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        ocorrencias.forEach { ocorrencia ->
            OpcaoSelecionavel(
                titulo = "Previsto: ${Valores.formatar(ocorrencia.valorPrevistoCentavos)}",
                subtitulo = "Data prevista: ${ocorrencia.dataPrevistaFormatada}",
                selecionado = ocorrencia.publicId == selecionadaPublicId,
                onClick = { onEvent(ReviewEvent.SelecionarOcorrenciaVoucher(ocorrencia.publicId)) },
                modifier = Modifier.testTag("ocorrencia-opcao-${ocorrencia.publicId}"),
            )
        }
    }
}

@Composable
private fun OpcoesDeDecisaoVoucher(temOcorrencias: Boolean, decisaoSelecionada: TipoDecisaoVoucher?, onEvent: (ReviewEvent) -> Unit) {
    PingoText(
        text = "Como registrar?",
        style = PingoTheme.typography.label,
        color = PingoTheme.colors.onBackground,
    )
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        if (temOcorrencias) {
            OpcaoSelecionavel(
                titulo = "Substituir recarga prevista",
                subtitulo = "Vincula este depósito à previsão selecionada.",
                selecionado = decisaoSelecionada == TipoDecisaoVoucher.SUBSTITUIR_PREVISAO,
                onClick = { onEvent(ReviewEvent.SelecionarDecisaoVoucher(TipoDecisaoVoucher.SUBSTITUIR_PREVISAO)) },
                modifier = Modifier.testTag("decisao-substituir-previsao"),
            )
        }
        OpcaoSelecionavel(
            titulo = "Registrar recarga extra",
            subtitulo = "Mantém as previsões futuras sem alterações.",
            selecionado = decisaoSelecionada == TipoDecisaoVoucher.REGISTRAR_EXTRA,
            onClick = { onEvent(ReviewEvent.SelecionarDecisaoVoucher(TipoDecisaoVoucher.REGISTRAR_EXTRA)) },
            modifier = Modifier.testTag("decisao-registrar-extra"),
        )
    }
}

@Composable
private fun OpcaoSelecionavel(
    titulo: String,
    subtitulo: String?,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PingoRadius.md)
    val fundo = if (selecionado) PingoTheme.colors.primaryContainer else PingoTheme.colors.surfaceVariant
    val borda = if (selecionado) PingoTheme.colors.primary else PingoTheme.colors.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .background(color = fundo, shape = shape)
            .border(width = if (selecionado) 1.5.dp else 1.dp, color = borda, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextoDaOpcao(titulo = titulo, subtitulo = subtitulo, selecionado = selecionado, modifier = Modifier.weight(1f))
        if (selecionado) {
            PingoIcon(
                imageVector = PingoIcons.Check,
                contentDescription = "Selecionado",
                modifier = Modifier.size(18.dp),
                tint = PingoTheme.colors.primary,
            )
        } else {
            Box(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TextoDaOpcao(titulo: String, subtitulo: String?, selecionado: Boolean, modifier: Modifier = Modifier) {
    val corTexto = if (selecionado) PingoTheme.colors.onPrimaryContainer else PingoTheme.colors.onBackground
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PingoText(text = titulo, style = PingoTheme.typography.bodyStrong, color = corTexto)
        subtitulo?.let {
            PingoText(
                text = it,
                style = PingoTheme.typography.bodySmall,
                color = if (selecionado) corTexto else PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AcoesDoPainelDeVoucher(painel: ResolucaoDeVoucherDaRevisao, ocupado: Boolean, onEvent: (ReviewEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(
            text = "Cancelar",
            onClick = { onEvent(ReviewEvent.FecharResolucaoVoucher) },
            type = PingoButtonType.Text,
            enabled = !ocupado,
            modifier = Modifier.testTag("cancelar-voucher-revisao"),
        )
        PingoButton(
            text = "Confirmar",
            onClick = { onEvent(ReviewEvent.ConfirmarResolucaoVoucher) },
            type = PingoButtonType.Primary,
            enabled = painel.podeConfirmar && !ocupado,
            modifier = Modifier.testTag("confirmar-voucher-revisao"),
        )
    }
}
