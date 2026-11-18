package com.pingo.feature.accounts.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.finance.PingoDebitCard
import com.pingo.core.designsystem.component.finance.PingoDebitCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceCard
import com.pingo.core.designsystem.component.finance.PingoInvoiceCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceStatus
import com.pingo.core.designsystem.component.finance.PingoVoucherCard
import com.pingo.core.designsystem.component.finance.PingoVoucherCardUi
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.model.Conta
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.ui.Valores
import com.pingo.core.ui.contas.nomeDeExibicao
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.calcularProgressoCiclo
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarContagemVencimento
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarPeriodoCiclo
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarResumoRecarga
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.paraDesignSystem
import java.time.LocalDate

@Composable
internal fun SubcardsDaConta(
    conta: Conta,
    indicadores: List<IndicadorDeInstrumento>,
    hoje: LocalDate,
    onAbrirFaturas: (String) -> Unit,
    onEvent: (AccountsEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        indicadores.forEach { indicador ->
            when (indicador) {
                is IndicadorDeInstrumento.Credito -> SubcardCredito(
                    conta = conta,
                    indicador = indicador,
                    hoje = hoje,
                    onAbrirFaturas = onAbrirFaturas,
                    onEvent = onEvent,
                )
                is IndicadorDeInstrumento.Debito -> SubcardDebito(
                    conta = conta,
                    indicador = indicador,
                )
                is IndicadorDeInstrumento.Voucher -> SubcardVoucher(
                    conta = conta,
                    indicador = indicador,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun SubcardCredito(
    conta: Conta,
    indicador: IndicadorDeInstrumento.Credito,
    hoje: LocalDate,
    onAbrirFaturas: (String) -> Unit,
    onEvent: (AccountsEvent) -> Unit,
) {
    val instrumento = indicador.instrumento
    val titulo = instrumento.rotuloNaListaDeContas()
    val instituicaoDs = conta.instituicao.paraDesignSystem()
    val shape = RoundedCornerShape(PingoRadius.xl)
    val cardUi = when (val estado = indicador.estadoFatura) {
        EstadoFaturaCartao.ConfiguracaoIncompleta -> PingoInvoiceCardUi(
            institution = instituicaoDs,
            title = titulo,
            status = PingoInvoiceStatus.Incompleta,
            incompleteMessage = "Datas não configuradas",
            completeDatesLabel = "Completar datas",
        )
        is EstadoFaturaCartao.Configurada -> PingoInvoiceCardUi(
            institution = instituicaoDs,
            title = titulo,
            status = estado.status.paraDesignSystem(),
            amountFormatted = Valores.formatar(estado.totalCentavos),
            cycleProgress = calcularProgressoCiclo(estado.dataFechamento, hoje),
            isOverdue = estado.vencida,
            cyclePeriodText = formatarPeriodoCiclo(estado.status, estado.dataFechamento, estado.dataVencimento),
            dueCountdownText = formatarContagemVencimento(estado.status, estado.diasParaVencimento, estado.vencida),
        )
    }

    val incompleta = indicador.estadoFatura is EstadoFaturaCartao.ConfiguracaoIncompleta
    PingoInvoiceCard(
        state = cardUi,
        modifier = Modifier
            .testTag("instrumento-credito-${instrumento.publicId}")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        onClick = if (incompleta) null else ({ onAbrirFaturas(instrumento.publicId) }),
        onCompleteDatesClick = if (incompleta) {
            { onEvent(AccountsEvent.AbrirCompletarDatas(instrumento.publicId, titulo)) }
        } else {
            null
        },
    )
}

@Composable
private fun SubcardDebito(conta: Conta, indicador: IndicadorDeInstrumento.Debito) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoDebitCard(
        state = PingoDebitCardUi(
            institution = conta.instituicao.paraDesignSystem(),
            title = indicador.instrumento.rotuloNaListaDeContas(),
            amountFormatted = Valores.formatar(indicador.gastoDoMesCentavos),
            subtitle = "Gasto do mês",
        ),
        modifier = Modifier
            .testTag("instrumento-debito-${indicador.instrumento.publicId}")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
    )
}

@Composable
private fun SubcardVoucher(conta: Conta, indicador: IndicadorDeInstrumento.Voucher, onEvent: (AccountsEvent) -> Unit) {
    val instrumento = indicador.instrumento
    val titulo = instrumento.rotuloNaListaDeContas()
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoVoucherCard(
        state = PingoVoucherCardUi(
            title = titulo,
            subtitle = "${conta.instituicao.nomeDeExibicao()} · benefício",
            balanceFormatted = Valores.formatar(indicador.saldoCentavos),
            institution = conta.instituicao.paraDesignSystem(),
            lastRechargeText = formatarResumoRecarga(instrumento, indicador.ultimaRecarga),
            adjustLabel = "Ajustar saldo",
        ),
        modifier = Modifier
            .testTag("instrumento-voucher-${instrumento.publicId}")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        onAdjustBalanceClick = {
            onEvent(
                AccountsEvent.AbrirAjustarSaldo(
                    instrumentoPublicId = instrumento.publicId,
                    tituloVoucher = titulo,
                    saldoAtualCentavos = indicador.saldoCentavos,
                ),
            )
        },
    )
}

private fun StatusFatura.paraDesignSystem(): PingoInvoiceStatus = when (this) {
    StatusFatura.ABERTA -> PingoInvoiceStatus.Aberta
    StatusFatura.FECHADA, StatusFatura.PAGA -> PingoInvoiceStatus.Fechada
}
