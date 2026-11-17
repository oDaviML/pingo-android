package com.pingo.core.ui.contas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.MENSAGEM_DIA_INVALIDO
import com.pingo.core.model.TAMANHO_MAXIMO_APELIDO
import com.pingo.core.model.TipoInstrumento

internal const val MENSAGEM_APELIDO = "Use de 1 a $TAMANHO_MAXIMO_APELIDO caracteres."
internal const val MENSAGEM_ULTIMOS_DIGITOS = "Use 4 números ou deixe em branco."
internal const val MENSAGEM_VALOR = "Informe um valor em reais."
internal const val MENSAGEM_RECARGA_INCOMPLETA = "Informe dia e valor da recarga juntos."

@Composable
internal fun InstrumentoFormularioCampos(
    titulo: String,
    instrumento: InstrumentoFormulario,
    podeRemover: Boolean,
    onEvento: (EventoDoFormularioDeConta) -> Unit,
    acao: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PingoTheme.colors.outlineVariant),
        )
        CabecalhoDoInstrumento(
            titulo = titulo,
            instrumento = instrumento,
            podeRemover = podeRemover,
            onEvento = onEvento,
            acao = acao,
        )

        PingoTextField(
            value = instrumento.apelido,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarApelidoInstrumento(instrumento.chave, it)) },
            label = "Apelido do ${instrumento.tipo.rotuloDeExibicao().lowercase()}",
            supportingText = if (instrumento.apelidoValido()) null else MENSAGEM_APELIDO,
            isError = !instrumento.apelidoValido(),
        )

        if (instrumento.tipo != TipoInstrumento.VOUCHER) {
            UltimosDigitosCampo(instrumento, onEvento)
        }

        when (instrumento.tipo) {
            TipoInstrumento.CREDITO -> CreditoCampos(instrumento, onEvento)
            TipoInstrumento.VOUCHER -> VoucherCampos(instrumento, onEvento)
            TipoInstrumento.DEBITO -> Unit
        }
    }
}

@Composable
private fun CabecalhoDoInstrumento(
    titulo: String,
    instrumento: InstrumentoFormulario,
    podeRemover: Boolean,
    onEvento: (EventoDoFormularioDeConta) -> Unit,
    acao: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoText(
            text = titulo,
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        if (instrumento.arquivado) {
            PingoText(
                text = "Arquivado",
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.weight(1f))
        acao?.invoke()
        if (podeRemover && instrumento.publicId == null) {
            PingoButton(
                text = "Remover",
                onClick = { onEvento(EventoDoFormularioDeConta.RemoverInstrumento(instrumento.chave)) },
                type = PingoButtonType.Text,
                size = PingoButtonSize.Small,
            )
        }
    }
}

@Composable
private fun UltimosDigitosCampo(instrumento: InstrumentoFormulario, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    val valido = instrumento.ultimosDigitosValidos()

    PingoTextField(
        value = instrumento.ultimosDigitos,
        onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarUltimosDigitos(instrumento.chave, it)) },
        label = "Últimos 4 dígitos (opcional)",
        supportingText = if (valido || instrumento.ultimosDigitos.isBlank()) null else MENSAGEM_ULTIMOS_DIGITOS,
        isError = !valido && instrumento.ultimosDigitos.isNotBlank(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun CreditoCampos(instrumento: InstrumentoFormulario, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoTextField(
            value = instrumento.fechamentoDia,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarFechamento(instrumento.chave, it)) },
            label = "Fechamento",
            modifier = Modifier.weight(1f),
            supportingText = erroDeDia(instrumento.fechamentoDia, instrumento.fechamentoValido()),
            isError = instrumento.fechamentoDia.isNotBlank() && !instrumento.fechamentoValido(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PingoTextField(
            value = instrumento.vencimentoDia,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarVencimento(instrumento.chave, it)) },
            label = "Vencimento",
            modifier = Modifier.weight(1f),
            supportingText = erroDeDia(instrumento.vencimentoDia, instrumento.vencimentoValido()),
            isError = instrumento.vencimentoDia.isNotBlank() && !instrumento.vencimentoValido(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun VoucherCampos(instrumento: InstrumentoFormulario, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    BeneficioEscolha(instrumento = instrumento, onEvento = onEvento)

    val saldoValido = instrumento.saldoValido()
    PingoTextField(
        value = instrumento.saldoInicial,
        onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarSaldoInicial(instrumento.chave, it)) },
        label = "Saldo inicial (R$)",
        supportingText = if (saldoValido || instrumento.saldoInicial.isBlank()) null else MENSAGEM_VALOR,
        isError = !saldoValido && instrumento.saldoInicial.isNotBlank(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )

    RecargaCampos(instrumento = instrumento, onEvento = onEvento)
}

@Composable
private fun BeneficioEscolha(instrumento: InstrumentoFormulario, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    RotuloDoFormulario(texto = "Benefício")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        BeneficioVoucher.entries.forEach { beneficio ->
            PingoChoiceChip(
                text = beneficio.rotuloDeExibicao(),
                selected = instrumento.beneficio == beneficio,
                onClick = { onEvento(EventoDoFormularioDeConta.AlterarBeneficio(instrumento.chave, beneficio)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecargaCampos(instrumento: InstrumentoFormulario, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoTextField(
            value = instrumento.diaRecarga,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarDiaRecarga(instrumento.chave, it)) },
            label = "Dia da recarga (opcional)",
            modifier = Modifier.weight(1f),
            supportingText = erroDeDia(instrumento.diaRecarga, instrumento.diaRecargaValido()),
            isError = instrumento.diaRecarga.isNotBlank() && !instrumento.diaRecargaValido(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PingoTextField(
            value = instrumento.valorRecarga,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarValorRecarga(instrumento.chave, it)) },
            label = "Valor da recarga (R$)",
            modifier = Modifier.weight(1f),
            supportingText = if (instrumento.valorRecargaValido() || instrumento.valorRecarga.isBlank()) null else MENSAGEM_VALOR,
            isError = !instrumento.valorRecargaValido() && instrumento.valorRecarga.isNotBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }

    if (!instrumento.recargaValida()) {
        PingoText(
            text = MENSAGEM_RECARGA_INCOMPLETA,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.expense,
        )
    }
}

@Composable
internal fun RotuloDoFormulario(texto: String) {
    PingoText(
        text = texto,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
}

private fun erroDeDia(valor: String, valido: Boolean): String? = if (valor.isNotBlank() && !valido) MENSAGEM_DIA_INVALIDO else null
