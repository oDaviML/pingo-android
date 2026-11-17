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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoLargeTopBar
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.ui.AvatarDaInstituicao
import com.pingo.core.ui.EstadoVazioCard
import com.pingo.core.ui.contas.nomeDeExibicao
import com.pingo.core.ui.contas.rotuloDeExibicao

@Suppress("LongParameterList")
@Composable
internal fun AccountsContent(
    state: AccountsUiState,
    onNovaConta: () -> Unit,
    onAbrirConta: (String) -> Unit,
    onTentarDeNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    onOpenNovo: (() -> Unit)? = null,
    mostrarBarraInferior: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            PingoLargeTopBar(title = "Contas")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = PingoSpacing.xl),
            ) {
                when {
                    state.carregando -> CarregandoContas()
                    state.falha -> FalhaAoLerContas(onTentarDeNovo = onTentarDeNovo)
                    state.vazia -> ContasVazias(onNovaConta = onNovaConta)
                    else -> ListaDeContas(state = state, onNovaConta = onNovaConta, onAbrirConta = onAbrirConta)
                }
            }
        }

        if (mostrarBarraInferior) {
            PingoBottomBar(
                selecionada = AbaInferior.CONTAS,
                onSelecionar = onSelecionarAba,
                onNovo = onOpenNovo,
            )
        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contas-vazio"),
        contentAlignment = Alignment.Center,
    ) {
        EstadoVazioCard(
            titulo = "Nenhuma conta cadastrada",
            descricao = "Adicione sua primeira conta para organizar cartões e benefícios.",
            acaoTexto = "Adicionar conta",
            onAcao = onNovaConta,
        )
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
        Spacer(modifier = Modifier.height(PingoSpacing.lg))
    }
}

@Composable
private fun ContaCard(conta: ContaDaLista, onAbrirConta: (String) -> Unit) {
    val linhas = if (conta.instrumentos.isEmpty()) {
        listOf("Nenhum instrumento ativo")
    } else {
        conta.instrumentos.map { it.rotuloNaListaDeContas() }
    }

    CartaoDeConta(
        instituicao = conta.conta.instituicao,
        titulo = conta.conta.apelido,
        selo = conta.conta.tipo.rotuloDeExibicao(),
        linhas = linhas,
        testTag = "conta-${conta.conta.publicId}",
        onClick = { onAbrirConta(conta.conta.publicId) },
    )
}

@Composable
private fun ContaArquivadaCard(conta: ContaArquivadaDaLista, onAbrirConta: (String) -> Unit) {
    val instrumentosTexto = if (conta.instrumentos.isEmpty()) {
        "Sem instrumentos"
    } else {
        conta.instrumentos.joinToString(" · ") { it.rotuloNaListaDeContas() }
    }

    CartaoDeConta(
        instituicao = conta.conta.instituicao,
        titulo = conta.conta.apelido,
        selo = "Arquivada",
        linhas = listOf(conta.conta.instituicao.nomeDeExibicao(), instrumentosTexto),
        testTag = "conta-arquivada-${conta.conta.publicId}",
        onClick = { onAbrirConta(conta.conta.publicId) },
    )
}

@Composable
private fun CartaoDeConta(
    instituicao: InstituicaoFinanceira,
    titulo: String,
    selo: String,
    linhas: List<String>,
    testTag: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier
            .testTag(testTag)
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.xs,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarDaInstituicao(instituicao = instituicao, size = 40.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CabecalhoDoCartaoDeConta(titulo = titulo, selo = selo)
                linhas.forEach { linha ->
                    PingoText(text = linha, style = PingoTheme.typography.bodySmall, color = PingoTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CabecalhoDoCartaoDeConta(titulo: String, selo: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PingoText(
            text = titulo,
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        PingoText(
            text = selo,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}
