package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TAMANHO_MAXIMO_APELIDO
import com.pingo.core.model.TAMANHO_ULTIMOS_DIGITOS
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

private const val MENSAGEM_APELIDO = "Use de 1 a $TAMANHO_MAXIMO_APELIDO caracteres."
private const val MENSAGEM_ULTIMOS_DIGITOS = "Use $TAMANHO_ULTIMOS_DIGITOS números ou deixe em branco."

@Composable
internal fun ContaEscolhaCampos(escolha: EscolhaBanco, onEvent: (OnboardingContasEvent) -> Unit) {
    val instituicao = escolha.instituicao
    val apelidoOk = escolha.apelidoEhValido()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoTextField(
            value = escolha.apelido,
            onValueChange = { onEvent(OnboardingContasEvent.AlterarApelidoConta(instituicao, it)) },
            label = "Apelido da conta",
            supportingText = if (apelidoOk) null else MENSAGEM_APELIDO,
            isError = !apelidoOk,
        )

        EscolhaRotulo(texto = "Tipo de conta")
        TipoContaSeletor(
            selecionado = escolha.tipoConta,
            onSelecionar = { onEvent(OnboardingContasEvent.AlterarTipoConta(instituicao, it)) },
        )

        EscolhaRotulo(texto = "Instrumentos")
        InstrumentosSeletor(
            escolha = escolha,
            onAlternar = { onEvent(OnboardingContasEvent.AlternarInstrumento(instituicao, it)) },
        )

        escolha.instrumentos.forEach { instrumento ->
            InstrumentoEscolhidoCampos(instituicao = instituicao, instrumento = instrumento, onEvent = onEvent)
        }
    }
}

@Composable
private fun TipoContaSeletor(selecionado: TipoConta, onSelecionar: (TipoConta) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        TipoConta.entries.forEach { tipo ->
            PingoChoiceChip(
                text = tipo.rotulo(),
                selected = selecionado == tipo,
                onClick = { onSelecionar(tipo) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InstrumentosSeletor(escolha: EscolhaBanco, onAlternar: (TipoInstrumento) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        TipoInstrumento.entries.forEach { tipo ->
            PingoChoiceChip(
                text = tipo.rotulo(),
                selected = escolha.temInstrumento(tipo),
                onClick = { onAlternar(tipo) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InstrumentoEscolhidoCampos(
    instituicao: InstituicaoFinanceira,
    instrumento: InstrumentoEscolhido,
    onEvent: (OnboardingContasEvent) -> Unit,
) {
    val apelidoOk = instrumento.apelidoEhValido()
    val digitosOk = instrumento.ultimosDigitosSaoValidos()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoTextField(
            value = instrumento.apelido,
            onValueChange = { onEvent(OnboardingContasEvent.AlterarApelidoInstrumento(instituicao, instrumento.tipo, it)) },
            label = "Apelido do ${instrumento.tipo.rotulo().lowercase()}",
            supportingText = if (apelidoOk) null else MENSAGEM_APELIDO,
            isError = !apelidoOk,
        )
        PingoTextField(
            value = instrumento.ultimosDigitos,
            onValueChange = { onEvent(OnboardingContasEvent.AlterarUltimosDigitos(instituicao, instrumento.tipo, it)) },
            label = "Últimos 4 dígitos (opcional)",
            supportingText = if (digitosOk) null else MENSAGEM_ULTIMOS_DIGITOS,
            isError = !digitosOk,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun EscolhaRotulo(texto: String) {
    PingoText(
        text = texto,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
}
