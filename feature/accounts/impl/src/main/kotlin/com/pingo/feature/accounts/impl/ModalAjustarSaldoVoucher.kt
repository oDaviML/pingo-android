package com.pingo.feature.accounts.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.pingo.core.data.voucher.ValorAjusteSaldoVoucher
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoCurrencyField
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.Valores
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.parsearValorAjuste

enum class ModoAjusteVoucher {
    SALDO_ALVO,
    DELTA,
}

@Immutable
data class ModalAjustarSaldoUiState(
    val instrumentoPublicId: String,
    val tituloVoucher: String,
    val saldoAtualCentavos: Long,
    val modo: ModoAjusteVoucher = ModoAjusteVoucher.SALDO_ALVO,
    val valorTexto: String = "",
    val observacao: String = "",
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val entradaCalculada: ValorAjusteSaldoVoucher?
        get() = parsearValorAjuste(modo = modo, texto = valorTexto)

    val valorValido: Boolean
        get() = entradaCalculada != null

    val podeSalvar: Boolean
        get() = !salvando && valorValido
}

@Suppress("LongParameterList")
@Composable
internal fun AjustarSaldoBottomSheet(
    state: ModalAjustarSaldoUiState,
    onAlterarModo: (ModoAjusteVoucher) -> Unit,
    onAlterarValor: (String) -> Unit,
    onAlterarObservacao: (String) -> Unit,
    onSalvar: () -> Unit,
    onFechar: () -> Unit,
) {
    PingoBottomSheet(
        onDismissRequest = { if (!state.salvando) onFechar() },
        modifier = Modifier.testTag("modal-ajustar-saldo"),
        title = "Ajustar saldo",
        subtitle = "${state.tituloVoucher} · Saldo atual ${Valores.formatar(state.saldoAtualCentavos)}",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            SeletorModoAjuste(modo = state.modo, habilitado = !state.salvando, onAlterarModo = onAlterarModo)
            CamposAjustarSaldo(state = state, onAlterarValor = onAlterarValor, onAlterarObservacao = onAlterarObservacao)
            state.erro?.let { mensagemErro ->
                PingoText(
                    text = mensagemErro,
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.expense,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            AcoesModalAjustarSaldo(
                salvando = state.salvando,
                podeSalvar = state.podeSalvar,
                onFechar = onFechar,
                onSalvar = onSalvar,
            )
        }
    }
}

@Composable
private fun CamposAjustarSaldo(state: ModalAjustarSaldoUiState, onAlterarValor: (String) -> Unit, onAlterarObservacao: (String) -> Unit) {
    val valorInvalido = state.valorTexto.isNotBlank() && !state.valorValido
    val rotuloCampo = if (state.modo == ModoAjusteVoucher.SALDO_ALVO) "Novo saldo (R$)" else "Diferença (R$)"
    val dicaCampo = if (state.modo == ModoAjusteVoucher.SALDO_ALVO) {
        "Informe o saldo atual correto em reais"
    } else {
        "Informe o ajuste em reais, ex.: 25,00 ou -25,00"
    }

    PingoCurrencyField(
        value = state.valorTexto,
        onValueChange = onAlterarValor,
        label = rotuloCampo,
        modifier = Modifier.testTag("campo-valor-ajuste"),
        supportingText = if (valorInvalido) "Informe um valor em reais." else dicaCampo,
        isError = valorInvalido,
        enabled = !state.salvando,
        allowNegative = state.modo == ModoAjusteVoucher.DELTA,
    )
    PingoTextField(
        value = state.observacao,
        onValueChange = onAlterarObservacao,
        label = "Observação (opcional)",
        modifier = Modifier.testTag("campo-observacao-ajuste"),
        enabled = !state.salvando,
    )
}

@Composable
private fun SeletorModoAjuste(modo: ModoAjusteVoucher, habilitado: Boolean, onAlterarModo: (ModoAjusteVoucher) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoChoiceChip(
            text = "Novo saldo",
            selected = modo == ModoAjusteVoucher.SALDO_ALVO,
            onClick = { onAlterarModo(ModoAjusteVoucher.SALDO_ALVO) },
            modifier = Modifier
                .weight(1f)
                .testTag("modo-ajuste-saldo-alvo"),
            enabled = habilitado,
        )
        PingoChoiceChip(
            text = "Diferença",
            selected = modo == ModoAjusteVoucher.DELTA,
            onClick = { onAlterarModo(ModoAjusteVoucher.DELTA) },
            modifier = Modifier
                .weight(1f)
                .testTag("modo-ajuste-delta"),
            enabled = habilitado,
        )
    }
}

@Composable
private fun AcoesModalAjustarSaldo(salvando: Boolean, podeSalvar: Boolean, onFechar: () -> Unit, onSalvar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoButton(
            text = "Cancelar",
            onClick = onFechar,
            type = PingoButtonType.Outline,
            enabled = !salvando,
            modifier = Modifier.weight(1f),
            fillWidth = true,
        )
        PingoButton(
            text = "Salvar ajuste",
            onClick = onSalvar,
            type = PingoButtonType.Primary,
            enabled = podeSalvar,
            loading = salvando,
            modifier = Modifier
                .weight(1f)
                .testTag("salvar-ajuste-voucher"),
            fillWidth = true,
        )
    }
}
