package com.pingo.feature.accounts.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.contas.nomeDeExibicao
import com.pingo.core.ui.contas.rotuloDeExibicao

@Composable
internal fun AccountsContent(
    state: AccountsUiState,
    onNovaConta: () -> Unit,
    onAbrirConta: (String) -> Unit,
    onTentarDeNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.xxl),
        ) {
            PingoText(
                text = "Contas",
                style = PingoTheme.typography.headline,
                color = PingoTheme.colors.onBackground,
                modifier = Modifier.padding(vertical = PingoSpacing.lg),
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.carregando -> CarregandoContas()
                    state.falha -> FalhaAoLerContas(onTentarDeNovo = onTentarDeNovo)
                    state.vazia -> ContasVazias(onNovaConta = onNovaConta)
                    else -> ListaDeContas(state = state, onNovaConta = onNovaConta, onAbrirConta = onAbrirConta)
                }
            }
        }

        PingoBottomBar(selecionada = AbaInferior.CONTAS, onSelecionar = onSelecionarAba)
    }
}

@Composable
private fun CarregandoContas() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contas-carregando"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PingoSpinner(size = 28.dp)
        Spacer(modifier = Modifier.height(PingoSpacing.sm))
        PingoText(
            text = "Carregando contas…",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun FalhaAoLerContas(onTentarDeNovo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contas-erro"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PingoText(
            text = "Não foi possível carregar suas contas.",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = "Tente de novo para ver suas contas.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(PingoSpacing.lg))
        PingoButton(text = "Tentar de novo", onClick = onTentarDeNovo, type = PingoButtonType.Outline)
    }
}

@Composable
private fun ContasVazias(onNovaConta: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contas-vazio"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PingoText(
            text = "Nenhuma conta cadastrada",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = "Adicione sua primeira conta para organizar cartões e benefícios.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(PingoSpacing.lg))
        PingoButton(text = "Adicionar conta", onClick = onNovaConta, fillWidth = true)
    }
}

@Composable
private fun ListaDeContas(state: AccountsUiState, onNovaConta: () -> Unit, onAbrirConta: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        state.grupos.forEach { grupo ->
            SectionHeader(title = grupo.instituicao.nomeDeExibicao())
            grupo.contas.forEach { item ->
                ContaCard(conta = item, onAbrirConta = onAbrirConta)
            }
        }

        if (state.arquivadas.isNotEmpty()) {
            SectionHeader(title = "Contas arquivadas")
            state.arquivadas.forEach { item ->
                ContaArquivadaCard(conta = item, onAbrirConta = onAbrirConta)
            }
        }

        PingoButton(
            text = "Adicionar conta",
            onClick = onNovaConta,
            type = PingoButtonType.Outline,
            fillWidth = true,
        )
    }
}

@Composable
private fun ContaCard(conta: ContaDaLista, onAbrirConta: (String) -> Unit) {
    PingoCard(
        modifier = Modifier
            .testTag("conta-${conta.conta.publicId}")
            .clickable(role = Role.Button, onClick = { onAbrirConta(conta.conta.publicId) }),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.xs,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PingoText(
                text = conta.conta.apelido,
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            PingoText(
                text = conta.conta.tipo.rotuloDeExibicao(),
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }

        if (conta.instrumentos.isEmpty()) {
            PingoText(
                text = "Nenhum instrumento ativo",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        } else {
            conta.instrumentos.forEach { instrumento ->
                PingoText(
                    text = instrumento.rotuloNaListaDeContas(),
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContaArquivadaCard(conta: ContaArquivadaDaLista, onAbrirConta: (String) -> Unit) {
    PingoCard(
        modifier = Modifier
            .testTag("conta-arquivada-${conta.conta.publicId}")
            .clickable(role = Role.Button, onClick = { onAbrirConta(conta.conta.publicId) }),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.xs,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PingoText(
                text = conta.conta.apelido,
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            PingoText(
                text = "Arquivada",
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        PingoText(
            text = conta.conta.instituicao.nomeDeExibicao(),
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = if (conta.instrumentos.isEmpty()) {
                "Sem instrumentos"
            } else {
                conta.instrumentos.joinToString(" · ") { it.rotuloNaListaDeContas() }
            },
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}
