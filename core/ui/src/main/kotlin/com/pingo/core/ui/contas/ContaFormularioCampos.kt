package com.pingo.core.ui.contas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoTextField
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

@Composable
fun ContaFormularioCampos(
    formulario: ContaFormulario,
    onEvento: (EventoDoFormularioDeConta) -> Unit,
    modifier: Modifier = Modifier,
    rotuloApelido: String = "Apelido da conta",
    acaoDoInstrumento: (@Composable (InstrumentoFormulario) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoTextField(
            value = formulario.apelido,
            onValueChange = { onEvento(EventoDoFormularioDeConta.AlterarApelidoConta(it)) },
            label = rotuloApelido,
            supportingText = if (formulario.apelidoValido()) null else MENSAGEM_APELIDO,
            isError = !formulario.apelidoValido(),
        )

        RotuloDoFormulario(texto = "Tipo de conta")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            TipoConta.entries.forEach { tipo ->
                PingoChoiceChip(
                    text = tipo.rotuloDeExibicao(),
                    selected = formulario.tipoConta == tipo,
                    onClick = { onEvento(EventoDoFormularioDeConta.AlterarTipoConta(tipo)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        RotuloDoFormulario(texto = "Instrumentos")
        formulario.instrumentos.forEachIndexed { indice, instrumento ->
            InstrumentoFormularioCampos(
                titulo = "${instrumento.tipo.rotuloDeExibicao()} ${formulario.numeroDoTipo(indice)}",
                instrumento = instrumento,
                podeRemover = formulario.podeRemoverInstrumento(),
                onEvento = onEvento,
                acao = acaoDoInstrumento?.let { acao -> { acao(instrumento) } },
            )
        }

        AdicionarInstrumentoBotoes(onEvento = onEvento)
    }
}

@Composable
private fun AdicionarInstrumentoBotoes(onEvento: (EventoDoFormularioDeConta) -> Unit) {
    RotuloDoFormulario(texto = "Adicionar instrumento")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        TipoInstrumento.entries.forEach { tipo ->
            PingoButton(
                text = "+ ${tipo.rotuloDeExibicao()}",
                onClick = { onEvento(EventoDoFormularioDeConta.AdicionarInstrumento(tipo)) },
                modifier = Modifier.weight(1f),
                type = PingoButtonType.Outline,
                size = PingoButtonSize.Small,
                fillWidth = true,
            )
        }
    }
}
