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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoCurrencyField
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
    habilitado: Boolean = true,
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
            habilitado = habilitado,
            onEvento = onEvento,
            acao = acao,
        )

        PingoTextField(
            value = instrumento.apelido,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarApelidoInstrumento(instrumento.chave, it)) },
            label = "Apelido do ${instrumento.tipo.rotuloDeExibicao().lowercase()}",
            supportingText = if (instrumento.apelidoValido()) null else MENSAGEM_APELIDO,
            isError = !instrumento.apelidoValido(),
            enabled = habilitado,
        )

        if (instrumento.tipo != TipoInstrumento.VOUCHER) {
            UltimosDigitosCampo(instrumento, habilitado, onEvento)
        }

        when (instrumento.tipo) {
            TipoInstrumento.CREDITO -> CreditoCampos(instrumento, habilitado, onEvento)
            TipoInstrumento.VOUCHER -> VoucherCampos(instrumento, habilitado, onEvento)
            TipoInstrumento.DEBITO -> Unit
        }
    }
}

@Composable
private fun CabecalhoDoInstrumento(
    titulo: String,
    instrumento: InstrumentoFormulario,
    podeRemover: Boolean,
    habilitado: Boolean,
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
                enabled = habilitado,
            )
        }
    }
}

@Composable
private fun UltimosDigitosCampo(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    val valido = instrumento.ultimosDigitosValidos()

    PingoTextField(
        value = instrumento.ultimosDigitos,
        onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarUltimosDigitos(instrumento.chave, it)) },
        label = "Últimos 4 dígitos (opcional)",
        supportingText = if (valido || instrumento.ultimosDigitos.isBlank()) null else MENSAGEM_ULTIMOS_DIGITOS,
        isError = !valido && instrumento.ultimosDigitos.isNotBlank(),
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun CreditoCampos(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvento: (EventoDoFormularioDeConta) -> Unit) {
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
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PingoTextField(
            value = instrumento.vencimentoDia,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarVencimento(instrumento.chave, it)) },
            label = "Vencimento",
            modifier = Modifier.weight(1f),
            supportingText = erroDeDia(instrumento.vencimentoDia, instrumento.vencimentoValido()),
            isError = instrumento.vencimentoDia.isNotBlank() && !instrumento.vencimentoValido(),
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun VoucherCampos(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    BeneficioEscolha(instrumento = instrumento, habilitado = habilitado, onEvento = onEvento)

    val saldoValido = instrumento.saldoValido()
    PingoCurrencyField(
        value = instrumento.saldoInicial,
        onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarSaldoInicial(instrumento.chave, it)) },
        label = "Saldo inicial (R$)",
        supportingText = if (saldoValido || instrumento.saldoInicial.isBlank()) null else MENSAGEM_VALOR,
        isError = !saldoValido && instrumento.saldoInicial.isNotBlank(),
        enabled = habilitado,
    )

    RecargaCampos(instrumento = instrumento, habilitado = habilitado, onEvento = onEvento)
}

@Composable
private fun BeneficioEscolha(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvento: (EventoDoFormularioDeConta) -> Unit) {
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
                enabled = habilitado,
            )
        }
    }
}

@Composable
private fun RecargaCampos(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvento: (EventoDoFormularioDeConta) -> Unit) {
    val erroDia = erroDeDia(instrumento.diaRecarga, instrumento.diaRecargaValido())
    val erroValor = if (instrumento.valorRecargaValido() || instrumento.valorRecarga.isBlank()) null else MENSAGEM_VALOR

    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        RotulosEmDuasColunas(
            rotuloEsquerda = "Dia da recarga (opcional)",
            rotuloDireita = "Valor da recarga (R$)",
        )
        CamposDeRecargaEmLinha(
            instrumento = instrumento,
            habilitado = habilitado,
            erroDia = erroDia != null,
            erroValor = erroValor != null,
            onEvento = onEvento,
        )
        if (erroDia != null || erroValor != null) {
            ErrosEmDuasColunas(erroEsquerda = erroDia, erroDireita = erroValor)
        }
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
private fun RotulosEmDuasColunas(rotuloEsquerda: String, rotuloDireita: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.Bottom,
    ) {
        PingoText(
            text = rotuloEsquerda,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        PingoText(
            text = rotuloDireita,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CamposDeRecargaEmLinha(
    instrumento: InstrumentoFormulario,
    habilitado: Boolean,
    erroDia: Boolean,
    erroValor: Boolean,
    onEvento: (EventoDoFormularioDeConta) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        PingoTextField(
            value = instrumento.diaRecarga,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarDiaRecarga(instrumento.chave, it)) },
            label = "Dia da recarga (opcional)",
            modifier = Modifier
                .weight(1f)
                .testTag("campo-dia-recarga"),
            isError = erroDia,
            enabled = habilitado,
            showLabel = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PingoCurrencyField(
            value = instrumento.valorRecarga,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarValorRecarga(instrumento.chave, it)) },
            label = "Valor da recarga (R$)",
            modifier = Modifier
                .weight(1f)
                .testTag("campo-valor-recarga"),
            isError = erroValor,
            enabled = habilitado,
            showLabel = false,
        )
    }
}

@Composable
private fun ErrosEmDuasColunas(erroEsquerda: String?, erroDireita: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (erroEsquerda != null) {
                PingoText(text = erroEsquerda, style = PingoTheme.typography.caption, color = PingoTheme.colors.expense)
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (erroDireita != null) {
                PingoText(text = erroDireita, style = PingoTheme.typography.caption, color = PingoTheme.colors.expense)
            }
        }
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
