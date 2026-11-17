package com.pingo.feature.accounts.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoActionDialog
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TipoConta
import com.pingo.core.ui.contas.ContaFormularioCampos
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import com.pingo.core.ui.contas.InstrumentoFormulario
import com.pingo.core.ui.contas.nomeDeExibicao
import com.pingo.core.ui.contas.rotuloDeExibicao

internal const val MENSAGEM_ERRO_AO_SALVAR_CONTA = "Não foi possível salvar sua conta. Tente de novo."
internal const val MENSAGEM_ERRO_AO_ARQUIVAR_CONTA = "Não foi possível arquivar. Tente de novo."

@Composable
internal fun ContaFormContent(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit, onVoltar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        CabecalhoDoFormulario(edicao = state.edicao, contaArquivada = state.contaArquivada, onVoltar = onVoltar)
        CorpoDoFormulario(state = state, onEvent = onEvent)
    }

    DialogoDeConfirmacaoSeVisivel(state = state, onEvent = onEvent)
}

@Composable
private fun CorpoDoFormulario(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
    when {
        state.carregando -> EstadoCentralizado(marcador = "conta-carregando") {
            PingoSpinner(size = 28.dp)
            PingoText(
                text = "Carregando conta…",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }

        state.falhaDeLeitura -> EstadoCentralizado(marcador = "conta-erro") {
            PingoText(
                text = "Não foi possível carregar esta conta.",
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
            )
            PingoButton(
                text = "Tentar de novo",
                onClick = { onEvent(ContaFormEvent.TentarDeNovo) },
                type = PingoButtonType.Outline,
            )
        }

        else -> FormularioDaConta(state = state, onEvent = onEvent)
    }
}

@Composable
private fun DialogoDeConfirmacaoSeVisivel(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
    state.dialogo?.let { dialogo ->
        DialogoDeConfirmacao(
            dialogo = dialogo,
            processando = state.processandoArquivamento,
            onConfirmar = { onEvent(ContaFormEvent.ConfirmarArquivamento) },
            onCancelar = { onEvent(ContaFormEvent.CancelarArquivamento) },
        )
    }
}

@Composable
private fun DialogoDeConfirmacao(dialogo: DialogoDeArquivamento, processando: Boolean, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    when (dialogo) {
        DialogoDeArquivamento.Conta -> PingoActionDialog(
            title = "Arquivar esta conta?",
            description = "Ela sai das listas e os instrumentos ativos são arquivados junto. " +
                "O histórico de lançamentos continua disponível em Lançamentos e relatórios.",
            confirmText = "Arquivar",
            onConfirm = onConfirmar,
            onDismissRequest = onCancelar,
            confirmLoading = processando,
            confirmModifier = Modifier.testTag("confirmar-arquivamento"),
        )

        is DialogoDeArquivamento.Instrumento -> PingoActionDialog(
            title = "Arquivar ${dialogo.apelido}?",
            description = "Ele deixa de aparecer nos seletores e na captura de novas compras. " +
                "O histórico continua disponível em Lançamentos e relatórios.",
            confirmText = "Arquivar",
            onConfirm = onConfirmar,
            onDismissRequest = onCancelar,
            confirmLoading = processando,
            confirmModifier = Modifier.testTag("confirmar-arquivamento"),
        )
    }
}

@Composable
private fun CabecalhoDoFormulario(edicao: Boolean, contaArquivada: Boolean, onVoltar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onVoltar),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.X,
                contentDescription = "Fechar",
                modifier = Modifier.size(24.dp),
                tint = PingoTheme.colors.onBackground,
            )
        }
        PingoText(
            text = when {
                contaArquivada -> "Conta arquivada"
                edicao -> "Editar conta"
                else -> "Nova conta"
            },
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
        )
    }
}

@Composable
private fun FormularioDaConta(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
    if (state.contaArquivada) {
        ContaArquivada(state = state, onEvent = onEvent)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        if (state.edicao) {
            ContaCabecalho(state = state)
        } else {
            InstituicaoEscolha(
                selecionada = state.formulario.instituicao,
                habilitado = state.formularioEditavel,
                onSelecionar = { onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarInstituicao(it))) },
            )
        }

        ContaFormularioCampos(
            formulario = state.formulario,
            habilitado = state.formularioEditavel,
            onEvento = { onEvent(ContaFormEvent.Formulario(it)) },
            acaoDoInstrumento = { instrumento ->
                AcaoDoInstrumento(
                    instrumento = instrumento,
                    habilitado = state.formularioEditavel,
                    onEvent = onEvent,
                )
            },
        )

        AcoesDoFormulario(state = state, onEvent = onEvent)
    }
}

@Composable
private fun AcoesDoFormulario(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg)) {
        if (state.falhouAoSalvar) {
            MensagemDeFalha(texto = MENSAGEM_ERRO_AO_SALVAR_CONTA)
        }

        if (state.falhouAoArquivar) {
            MensagemDeFalha(texto = MENSAGEM_ERRO_AO_ARQUIVAR_CONTA)
        }

        PingoButton(
            text = "Salvar conta",
            onClick = { onEvent(ContaFormEvent.Salvar) },
            enabled = state.podeSalvar,
            loading = state.salvando,
            fillWidth = true,
        )

        if (state.edicao) {
            PingoButton(
                text = "Excluir conta",
                onClick = { onEvent(ContaFormEvent.SolicitarArquivamentoDaConta) },
                modifier = Modifier.testTag("excluir-conta"),
                enabled = state.formularioEditavel,
                type = PingoButtonType.Text,
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun AcaoDoInstrumento(instrumento: InstrumentoFormulario, habilitado: Boolean, onEvent: (ContaFormEvent) -> Unit) {
    if (instrumento.publicId == null) {
        return
    }

    if (instrumento.arquivado) {
        PingoButton(
            text = "Reativar",
            onClick = { onEvent(ContaFormEvent.ReativarInstrumento(instrumento.chave)) },
            modifier = Modifier.testTag("reativar-instrumento-${instrumento.chave}"),
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            enabled = habilitado,
        )
    } else {
        PingoButton(
            text = "Arquivar",
            onClick = { onEvent(ContaFormEvent.SolicitarArquivamentoDoInstrumento(instrumento.chave)) },
            modifier = Modifier.testTag("arquivar-instrumento-${instrumento.chave}"),
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            enabled = habilitado,
        )
    }
}

@Composable
private fun MensagemDeFalha(texto: String) {
    PingoText(
        text = texto,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.expense,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ContaArquivada(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.lg)
            .testTag("conta-arquivada"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        ContaCabecalho(state = state)
        PingoText(
            text = "Esta conta está arquivada. Ela não aparece nas seleções de novos lançamentos nem na captura. " +
                "O histórico continua disponível em Lançamentos e relatórios.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = "Instrumentos",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        InstrumentosDaContaArquivada(instrumentos = state.formulario.instrumentos)

        if (state.falhouAoArquivar) {
            MensagemDeFalha(texto = MENSAGEM_ERRO_AO_ARQUIVAR_CONTA)
        }

        PingoButton(
            text = "Reativar conta",
            onClick = { onEvent(ContaFormEvent.ReativarConta) },
            modifier = Modifier.testTag("reativar-conta"),
            loading = state.processandoArquivamento,
            fillWidth = true,
        )
    }
}

@Composable
private fun InstrumentosDaContaArquivada(instrumentos: List<InstrumentoFormulario>) {
    instrumentos.forEach { instrumento ->
        val rotulo = "${instrumento.tipo.rotuloDeExibicao()} · ${instrumento.apelido}"

        PingoText(
            text = rotulo + if (instrumento.arquivado) " · Arquivado" else "",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContaCabecalho(state: ContaFormUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        PingoText(
            text = state.formulario.instituicao.nomeDeExibicao(),
            style = PingoTheme.typography.title,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = if (state.formulario.tipoConta == TipoConta.BENEFICIO) "Conta benefício" else "Conta corrente",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EstadoCentralizado(marcador: String, conteudo: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(marcador),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md, Alignment.CenterVertically),
        content = { conteudo() },
    )
}
