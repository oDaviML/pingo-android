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
import com.pingo.core.designsystem.component.PingoButton
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
import com.pingo.core.ui.contas.nomeDeExibicao

internal const val MENSAGEM_ERRO_AO_SALVAR_CONTA = "Não foi possível salvar sua conta. Tente de novo."

@Composable
internal fun ContaFormContent(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit, onVoltar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        CabecalhoDoFormulario(edicao = state.edicao, onVoltar = onVoltar)

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
}

@Composable
private fun CabecalhoDoFormulario(edicao: Boolean, onVoltar: () -> Unit) {
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
            text = if (edicao) "Editar conta" else "Nova conta",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
        )
    }
}

@Composable
private fun FormularioDaConta(state: ContaFormUiState, onEvent: (ContaFormEvent) -> Unit) {
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
                onSelecionar = { onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarInstituicao(it))) },
            )
        }

        ContaFormularioCampos(
            formulario = state.formulario,
            onEvento = { onEvent(ContaFormEvent.Formulario(it)) },
        )

        if (state.falhouAoSalvar) {
            PingoText(
                text = MENSAGEM_ERRO_AO_SALVAR_CONTA,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.expense,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        PingoButton(
            text = "Salvar conta",
            onClick = { onEvent(ContaFormEvent.Salvar) },
            enabled = state.podeSalvar,
            loading = state.salvando,
            fillWidth = true,
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
