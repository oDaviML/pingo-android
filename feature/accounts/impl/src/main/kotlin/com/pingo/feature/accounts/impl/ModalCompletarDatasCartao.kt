package com.pingo.feature.accounts.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.MENSAGEM_DIA_INVALIDO

@Suppress("LongParameterList")
@Composable
internal fun CompletarDatasBottomSheet(
    state: ModalCompletarDatasUiState,
    onAlterarFechamento: (String) -> Unit,
    onAlterarVencimento: (String) -> Unit,
    onSalvar: () -> Unit,
    onFechar: () -> Unit,
) {
    PingoBottomSheet(
        onDismissRequest = { if (!state.salvando) onFechar() },
        modifier = Modifier.testTag("modal-completar-datas"),
        title = "Completar datas do cartão",
        subtitle = state.tituloCartao,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            CamposCompletarDatas(
                state = state,
                onAlterarFechamento = onAlterarFechamento,
                onAlterarVencimento = onAlterarVencimento,
            )
            state.erro?.let { mensagemErro ->
                PingoText(
                    text = mensagemErro,
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.expense,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            AcoesModalCompletarDatas(
                salvando = state.salvando,
                podeSalvar = state.podeSalvar,
                onFechar = onFechar,
                onSalvar = onSalvar,
            )
        }
    }
}

@Composable
private fun CamposCompletarDatas(
    state: ModalCompletarDatasUiState,
    onAlterarFechamento: (String) -> Unit,
    onAlterarVencimento: (String) -> Unit,
) {
    val erroFechamento = state.fechamentoDia.isNotBlank() && !state.fechamentoValido
    val erroVencimento = state.vencimentoDia.isNotBlank() && !state.vencimentoValido

    PingoTextField(
        value = state.fechamentoDia,
        onValueChange = onAlterarFechamento,
        label = "Dia de fechamento",
        modifier = Modifier.testTag("campo-fechamento-cartao"),
        supportingText = if (erroFechamento) MENSAGEM_DIA_INVALIDO else "Dia do mês em que a fatura fecha (1 a 31)",
        isError = erroFechamento,
        enabled = !state.salvando,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    PingoTextField(
        value = state.vencimentoDia,
        onValueChange = onAlterarVencimento,
        label = "Dia de vencimento",
        modifier = Modifier.testTag("campo-vencimento-cartao"),
        supportingText = if (erroVencimento) MENSAGEM_DIA_INVALIDO else "Dia do mês em que a fatura vence (1 a 31)",
        isError = erroVencimento,
        enabled = !state.salvando,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun AcoesModalCompletarDatas(salvando: Boolean, podeSalvar: Boolean, onFechar: () -> Unit, onSalvar: () -> Unit) {
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
            text = "Salvar datas",
            onClick = onSalvar,
            type = PingoButtonType.Primary,
            enabled = podeSalvar,
            loading = salvando,
            modifier = Modifier
                .weight(1f)
                .testTag("salvar-datas-cartao"),
            fillWidth = true,
        )
    }
}
