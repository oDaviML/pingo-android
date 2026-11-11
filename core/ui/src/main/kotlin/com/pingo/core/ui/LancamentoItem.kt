package com.pingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoIcon
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
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeDaCategoria(nome = categoria?.nome)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TituloDoLancamento(lancamento = lancamento)
            DescricaoDoVinculo(lancamento = lancamento, instrumento = instrumento, conta = conta, zona = zona)
        }
        PingoText(
            text = Valores.assinado(lancamento.valorCentavos, lancamento.tipo),
            style = PingoTheme.typography.amount,
            color = Valores.corDoTipo(lancamento.tipo, cores),
            maxLines = 1,
        )
    }
}

@Composable
private fun IconeDaCategoria(nome: String?) {
    val descricao = nome?.let { "Categoria $it" } ?: "Categoria não identificada"

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color = PingoTheme.colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = iconeDaCategoria(nome),
            contentDescription = descricao,
            modifier = Modifier.size(22.dp),
            tint = PingoTheme.colors.onPrimaryContainer,
        )
    }
}

@Composable
private fun TituloDoLancamento(lancamento: Lancamento) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = lancamento.estabelecimento ?: tituloPadrao(lancamento.tipo),
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (lancamento.status == StatusLancamento.PENDENTE) {
            Selo(texto = "Pendente", fundo = PingoTheme.colors.warningContainer, conteudo = PingoTheme.colors.onWarningContainer)
        }
        if (lancamento.origem == OrigemLancamento.MANUAL) {
            Selo(texto = "Manual", fundo = PingoTheme.colors.surfaceVariant, conteudo = PingoTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Selo(texto: String, fundo: Color, conteudo: Color) {
    Box(
        modifier = Modifier
            .background(color = fundo, shape = RoundedCornerShape(PingoRadius.full))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        PingoText(text = texto, style = PingoTheme.typography.labelSmall, color = conteudo, maxLines = 1)
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
        PingoIcon(
            imageVector = if (identificado) PingoIcons.Landmark else PingoIcons.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = texto,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 240.dp),
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
