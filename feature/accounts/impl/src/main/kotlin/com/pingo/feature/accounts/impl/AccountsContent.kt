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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoLargeTopBar
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
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
    onAbrirFaturas: (String) -> Unit = {},
    onEvent: (AccountsEvent) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        CorpoPrincipalContas(
            state = state,
            onNovaConta = onNovaConta,
            onAbrirConta = onAbrirConta,
            onTentarDeNovo = onTentarDeNovo,
            onSelecionarAba = onSelecionarAba,
            onOpenNovo = onOpenNovo,
            mostrarBarraInferior = mostrarBarraInferior,
            onAbrirFaturas = onAbrirFaturas,
            onEvent = onEvent,
        )
        ModaisDeContas(state = state, onEvent = onEvent)
    }
}

@Suppress("LongParameterList")
@Composable
private fun CorpoPrincipalContas(
    state: AccountsUiState,
    onNovaConta: () -> Unit,
    onAbrirConta: (String) -> Unit,
    onTentarDeNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    onOpenNovo: (() -> Unit)?,
    mostrarBarraInferior: Boolean,
    onAbrirFaturas: (String) -> Unit,
    onEvent: (AccountsEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                    else -> ListaDeContas(
                        state = state,
                        onNovaConta = onNovaConta,
                        onAbrirConta = onAbrirConta,
                        onAbrirFaturas = onAbrirFaturas,
                        onEvent = onEvent,
                    )
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
private fun ModaisDeContas(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    state.modalCompletarDatas?.let { modal ->
        CompletarDatasBottomSheet(
            state = modal,
            onAlterarFechamento = { onEvent(AccountsEvent.AlterarFechamentoModal(it)) },
            onAlterarVencimento = { onEvent(AccountsEvent.AlterarVencimentoModal(it)) },
            onSalvar = { onEvent(AccountsEvent.SalvarDatasCartao) },
            onFechar = { onEvent(AccountsEvent.FecharModalCompletarDatas) },
        )
    }
    state.modalAjustarSaldo?.let { modal ->
        AjustarSaldoBottomSheet(
            state = modal,
            onAlterarModo = { onEvent(AccountsEvent.AlterarModoAjuste(it)) },
            onAlterarValor = { onEvent(AccountsEvent.AlterarValorAjuste(it)) },
            onAlterarObservacao = { onEvent(AccountsEvent.AlterarObservacaoAjuste(it)) },
            onSalvar = { onEvent(AccountsEvent.SalvarAjusteVoucher) },
            onFechar = { onEvent(AccountsEvent.FecharModalAjustarSaldo) },
        )
    }
}

@Composable
private fun CarregandoContas() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contas-carregando"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "Carregando contas…",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoSkeletonList(count = 4)
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
private fun ListaDeContas(
    state: AccountsUiState,
    onNovaConta: () -> Unit,
    onAbrirConta: (String) -> Unit,
    onAbrirFaturas: (String) -> Unit,
    onEvent: (AccountsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        state.grupos.forEach { grupo ->
            SectionHeader(title = grupo.instituicao.nomeDeExibicao())
            grupo.contas.forEach { item ->
                BlocoContaAtiva(
                    conta = item,
                    hoje = state.hoje,
                    onAbrirConta = onAbrirConta,
                    onAbrirFaturas = onAbrirFaturas,
                    onEvent = onEvent,
                )
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
private fun BlocoContaAtiva(
    conta: ContaDaLista,
    hoje: java.time.LocalDate,
    onAbrirConta: (String) -> Unit,
    onAbrirFaturas: (String) -> Unit,
    onEvent: (AccountsEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        CabecalhoContaCard(conta = conta, onAbrirConta = onAbrirConta)
        if (conta.indicadores.isEmpty()) {
            PingoText(
                text = "Nenhum instrumento ativo",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = PingoSpacing.xs),
            )
        } else {
            SubcardsDaConta(
                conta = conta.conta,
                indicadores = conta.indicadores,
                hoje = hoje,
                onAbrirFaturas = onAbrirFaturas,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun CabecalhoContaCard(conta: ContaDaLista, onAbrirConta: (String) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier
            .testTag("conta-${conta.conta.publicId}")
            .heightIn(min = PingoLayout.minTouchTarget)
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape)
            .clickable(role = Role.Button, onClick = { onAbrirConta(conta.conta.publicId) }),
        contentPadding = PingoSpacing.md,
        gap = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarDaInstituicao(instituicao = conta.conta.instituicao, size = 36.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PingoText(
                    text = conta.conta.apelido,
                    style = PingoTheme.typography.bodyStrong,
                    color = PingoTheme.colors.onBackground,
                )
                PingoText(
                    text = "${conta.conta.instituicao.nomeDeExibicao()} · ${conta.conta.tipo.rotuloDeExibicao()}",
                    style = PingoTheme.typography.caption,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
            PingoIcon(
                imageVector = PingoIcons.Pencil,
                contentDescription = "Editar conta ${conta.conta.apelido}",
                modifier = Modifier.size(18.dp),
                tint = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
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
            .heightIn(min = PingoLayout.minTouchTarget)
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
