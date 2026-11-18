package com.pingo.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoInstitution
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.finance.PingoInvoiceCard
import com.pingo.core.designsystem.component.finance.PingoInvoiceCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceStatus
import com.pingo.core.designsystem.component.finance.PingoVoucherCard
import com.pingo.core.designsystem.component.finance.PingoVoucherCardUi
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import com.pingo.core.ui.Valores
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

@Composable
internal fun BlocoFaturasCredito(
    estado: EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>,
    onVerContas: () -> Unit,
    onAbrirFatura: (String) -> Unit,
) {
    BlocoFinanceiroInicio(
        estado = estado,
        titulo = "Faturas de crédito",
        mensagemErro = "Não foi possível carregar as faturas",
        tagSecao = "secao-faturas-credito",
        tagCabecalho = "cabecalho-faturas-credito",
        tagErro = "erro-faturas-inicio",
        onVerContas = onVerContas,
    ) { faturas ->
        faturas.forEach { fatura ->
            PingoInvoiceCard(
                state = fatura.paraUi(),
                modifier = Modifier.testTag("fatura-inicio-${fatura.instrumentoPublicId}"),
                onClick = { onAbrirFatura(fatura.instrumentoPublicId) },
            )
        }
    }
}

@Composable
internal fun BlocoSaldosBeneficios(estado: EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>, onVerContas: () -> Unit) {
    BlocoFinanceiroInicio(
        estado = estado,
        titulo = "Saldos de benefícios",
        mensagemErro = "Não foi possível carregar os saldos de benefícios",
        tagSecao = "secao-saldos-beneficios",
        tagCabecalho = "cabecalho-saldos-beneficios",
        tagErro = "erro-vouchers-inicio",
        onVerContas = onVerContas,
    ) { vouchers ->
        vouchers.forEach { voucher ->
            PingoVoucherCard(
                state = voucher.paraUi(),
                modifier = Modifier.testTag("voucher-inicio-${voucher.instrumentoPublicId}"),
                onClick = onVerContas,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun <T> BlocoFinanceiroInicio(
    estado: EstadoBlocoFinanceiro<List<T>>,
    titulo: String,
    mensagemErro: String,
    tagSecao: String,
    tagCabecalho: String,
    tagErro: String,
    onVerContas: () -> Unit,
    conteudoSucesso: @Composable ColumnScope.(List<T>) -> Unit,
) {
    val dados = when (estado) {
        EstadoBlocoFinanceiro.Carregando -> return
        EstadoBlocoFinanceiro.Erro -> null
        is EstadoBlocoFinanceiro.Sucesso -> estado.dados.ifEmpty { return }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tagSecao),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        SectionHeader(
            title = titulo,
            actionLabel = "Ver contas",
            onAction = onVerContas,
            modifier = Modifier.testTag(tagCabecalho),
        )
        if (dados == null) {
            PingoBanner(
                title = mensagemErro,
                tone = PingoBannerTone.Error,
                modifier = Modifier.testTag(tagErro),
            )
        } else {
            conteudoSucesso(dados)
        }
    }
}

private fun FaturaAbertaInicio.paraUi(): PingoInvoiceCardUi {
    val uiStatus = status.paraUi()
    val progresso = when (uiStatus) {
        PingoInvoiceStatus.Fechada -> 1f
        PingoInvoiceStatus.Aberta -> ((30f - diasParaVencimento.coerceIn(0L, 30L)) / 30f).coerceIn(0.08f, 1f)
        PingoInvoiceStatus.Proxima, PingoInvoiceStatus.Incompleta -> null
    }
    return PingoInvoiceCardUi(
        institution = instituicao.paraDesignSystem(),
        title = apelidoCartao,
        status = uiStatus,
        subtitle = ultimosDigitos?.takeIf { it.isNotBlank() }?.let { "final $it" },
        amountFormatted = Valores.formatar(valorCentavos),
        cycleProgress = progresso,
        isOverdue = diasParaVencimento < 0L,
        cyclePeriodText = textoPeriodoCiclo(uiStatus, dataFechamento, dataVencimento),
        dueCountdownText = textoContagemVencimento(diasParaVencimento),
    )
}

private fun SaldoVoucherInicio.paraUi(): PingoVoucherCardUi = PingoVoucherCardUi(
    title = "${beneficio.name} · ${beneficio.rotulo()}",
    subtitle = apelido,
    balanceFormatted = Valores.formatar(saldoCentavos),
    institution = instituicao.paraDesignSystem(),
    lastRechargeText = ultimaRecarga?.let { recarga ->
        "Última recarga ${recarga.data.format(FORMATO_DIA_MES)} · ${Valores.formatar(recarga.valorCentavos)}"
    },
)

private fun StatusFatura.paraUi(): PingoInvoiceStatus = when (this) {
    StatusFatura.ABERTA -> PingoInvoiceStatus.Aberta
    StatusFatura.FECHADA -> PingoInvoiceStatus.Fechada
    StatusFatura.PAGA -> PingoInvoiceStatus.Proxima
}

private fun textoPeriodoCiclo(status: PingoInvoiceStatus, fechamento: LocalDate, vencimento: LocalDate): String =
    if (status == PingoInvoiceStatus.Fechada) {
        "Fechou dia ${fechamento.dayOfMonth} · pagar até ${vencimento.format(FORMATO_DIA_MES)}"
    } else {
        "Fecha dia ${fechamento.dayOfMonth} · vence dia ${vencimento.dayOfMonth}"
    }

private fun textoContagemVencimento(dias: Long): String = when {
    dias < 0L -> {
        val atraso = -dias
        val unidade = if (atraso == 1L) "dia" else "dias"
        "Venceu há $atraso $unidade"
    }
    dias == 0L -> "Vence hoje"
    dias == 1L -> "Vence em 1 dia"
    else -> "Vence em $dias dias"
}

private fun BeneficioVoucher.rotulo(): String = when (this) {
    BeneficioVoucher.VR -> "Refeição"
    BeneficioVoucher.VA -> "Alimentação"
    BeneficioVoucher.VT -> "Transporte"
}

private fun InstituicaoFinanceira.paraDesignSystem(): PingoInstitution = when (this) {
    InstituicaoFinanceira.NUBANK -> PingoInstitution.Nubank
    InstituicaoFinanceira.INTER -> PingoInstitution.Inter
    InstituicaoFinanceira.ITAU -> PingoInstitution.Itau
    InstituicaoFinanceira.MERCADO_PAGO -> PingoInstitution.MercadoPago
    InstituicaoFinanceira.CAJU -> PingoInstitution.Caju
    InstituicaoFinanceira.OUTRA -> PingoInstitution.Outra
}
