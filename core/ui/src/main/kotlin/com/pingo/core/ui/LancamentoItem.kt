package com.pingo.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoCategoryTone
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import java.time.ZoneId

@Composable
fun LancamentoItem(
    lancamento: Lancamento,
    categoria: Categoria?,
    instrumento: Instrumento?,
    conta: Conta?,
    modifier: Modifier = Modifier,
    zona: ZoneId = ZoneId.systemDefault(),
) {
    val cores = PingoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconeDoLancamento(tipo = lancamento.tipo, categoria = categoria),
            contentDescription = categoria?.nome?.let { "Categoria $it" } ?: "Categoria não identificada",
            tone = tomDoLancamento(lancamento.tipo),
            size = 44.dp,
            iconSize = 22.dp,
            cornerRadius = PingoRadius.md,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            CabecalhoDoLancamento(lancamento = lancamento)
            DescricaoDoVinculo(lancamento = lancamento, instrumento = instrumento, conta = conta, zona = zona)
            TagsDoLancamento(lancamento = lancamento)
        }
    }
}

private fun iconeDoLancamento(tipo: TipoLancamento, categoria: Categoria?): ImageVector = when (tipo) {
    TipoLancamento.TRANSFERENCIA -> PingoIcons.ArrowLeftRight
    TipoLancamento.RECEITA -> if (categoria == null || categoria.nome.equals("outros", ignoreCase = true)) {
        PingoIcons.ArrowDownLeft
    } else {
        iconeDaCategoria(categoria.nome)
    }
    TipoLancamento.DESPESA, TipoLancamento.AJUSTE -> iconeDaCategoria(categoria?.nome)
}

private fun tomDoLancamento(tipo: TipoLancamento): PingoCategoryTone = when (tipo) {
    TipoLancamento.RECEITA -> PingoCategoryTone.Income
    TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> PingoCategoryTone.Transfer
    TipoLancamento.DESPESA -> PingoCategoryTone.Default
}

@Composable
private fun CabecalhoDoLancamento(lancamento: Lancamento) {
    val cores = PingoTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        PingoText(
            text = lancamento.estabelecimento ?: tituloPadrao(lancamento.tipo),
            style = PingoTheme.typography.bodyStrong,
            color = cores.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PingoText(
            text = Valores.assinado(lancamento.valorCentavos, lancamento.tipo),
            style = PingoTheme.typography.amount,
            color = Valores.corDoTipo(lancamento.tipo, cores),
            maxLines = 1,
        )
    }
}

@Composable
private fun TagsDoLancamento(lancamento: Lancamento) {
    val temParcelas = lancamento.totalParcelas > 1
    val pendente = lancamento.status == StatusLancamento.PENDENTE
    val manual = lancamento.origem == OrigemLancamento.MANUAL
    if (!temParcelas && !pendente && !manual) return

    val cores = PingoTheme.colors
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        if (temParcelas) {
            PingoTag(
                label = "${lancamento.parcelaNumero}/${lancamento.totalParcelas}",
                containerColor = cores.primaryContainer,
                contentColor = cores.onPrimaryContainer,
            )
        }
        if (pendente) {
            PingoTag(
                label = "Pendente",
                containerColor = cores.warningContainer,
                contentColor = cores.onWarningContainer,
            )
        }
        if (manual) {
            PingoTag(
                label = "Manual",
                containerColor = cores.surfaceVariant,
                contentColor = cores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DescricaoDoVinculo(lancamento: Lancamento, instrumento: Instrumento?, conta: Conta?, zona: ZoneId) {
    val identificado = instrumento != null
    val horario = Valores.horario(lancamento.dataHora, zona)
    val texto = if (identificado) {
        listOfNotNull(nomeDaConta(instrumento, conta), rotuloDoInstrumento(instrumento), horario).joinToString(" · ")
    } else {
        "Instrumento não identificado · $horario"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!identificado) {
            PingoIcon(
                imageVector = PingoIcons.CircleAlert,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = PingoTheme.colors.warning,
            )
        }
        PingoText(
            text = texto,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun nomeDaConta(instrumento: Instrumento, conta: Conta?): String = conta?.apelido ?: instrumento.apelido

private fun rotuloDoInstrumento(instrumento: Instrumento): String = when (instrumento.tipo) {
    TipoInstrumento.CREDITO -> "crédito"
    TipoInstrumento.DEBITO -> "débito"
    TipoInstrumento.VOUCHER -> rotuloDoVoucher(instrumento.beneficio)
}

private fun rotuloDoVoucher(beneficio: BeneficioVoucher?): String = beneficio?.name ?: "voucher"

private fun tituloPadrao(tipo: TipoLancamento): String = when (tipo) {
    TipoLancamento.TRANSFERENCIA -> "Transferência"
    TipoLancamento.AJUSTE -> "Ajuste"
    TipoLancamento.RECEITA -> "Receita"
    TipoLancamento.DESPESA -> "Lançamento"
}
