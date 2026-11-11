package com.pingo.feature.transactions.impl

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.Valores

@Composable
fun NovoLancamentoContent(
    state: NovoLancamentoUiState,
    onEvent: (NovoLancamentoEvent) -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tela-novo-lancamento")
            .background(PingoTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CabecalhoDoNovoLancamento(state = state, onEvent = onEvent, onFechar = onFechar)
            ColunaDeCampos(state = state, onEvent = onEvent, modifier = Modifier.weight(1f))
            TecladoNumerico(habilitado = !state.salvando && !state.salvo, onEvent = onEvent)
        }

        state.painel?.let { painel ->
            PainelDoNovoLancamento(state = state, painel = painel, onEvent = onEvent)
        }

        if (state.calendarioVisivel) {
            CalendarioDoLancamento(data = state.data, hoje = state.hoje, onEvent = onEvent)
        }
    }
}

@Composable
private fun CabecalhoDoNovoLancamento(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit, onFechar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .testTag("fechar-novo-lancamento")
                .clickable(role = Role.Button, onClick = onFechar),
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
            text = "Novo lançamento",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        PingoButton(
            text = "Salvar",
            onClick = { onEvent(NovoLancamentoEvent.Salvar) },
            size = PingoButtonSize.Small,
            enabled = state.podeSalvar,
            loading = state.salvando,
            modifier = Modifier.testTag("salvar-lancamento"),
        )
    }
}

@Composable
private fun ColunaDeCampos(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        SeletorDeTipo(tipo = state.tipo, onSelecionar = { onEvent(NovoLancamentoEvent.TipoSelecionado(it)) })
        BlocoDeValor(state = state)
        if (state.tipo == TipoDoLancamentoManual.TRANSFERENCIA) {
            CamposDeTransferencia(state = state, onEvent = onEvent)
        } else {
            CamposDeDespesaOuReceita(state = state, onEvent = onEvent)
        }
        CampoDeData(state = state, onEvent = onEvent)
        CampoDeEstabelecimento(state = state, onEvent = onEvent)
        Spacer(modifier = Modifier.height(PingoSpacing.sm))
    }
}

@Composable
private fun CamposDeDespesaOuReceita(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Instrumento",
            texto = state.instrumento?.rotulo ?: "Escolha um instrumento",
            preenchido = state.instrumento != null,
            icone = PingoIcons.Wallet,
            testTag = "campo-instrumento",
            erro = state.semInstrumentos || CampoDeLancamentoManual.INSTRUMENTO in state.erros,
            mensagem = RotulosDoNovoLancamento.mensagemDoInstrumento(state),
        ),
        onClick = { onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.INSTRUMENTO)) },
    )
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Categoria",
            texto = state.categoria?.nome ?: "Escolher categoria",
            preenchido = state.categoria != null,
            icone = PingoIcons.Tag,
            testTag = "campo-categoria",
            mensagem = if (state.categoriaSugerida) "Categoria sugerida pelo estabelecimento" else null,
        ),
        onClick = { onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.CATEGORIA)) },
    )
}

@Composable
private fun CamposDeTransferencia(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Origem",
            texto = state.instrumento?.rotulo ?: "Escolha a conta de origem",
            preenchido = state.instrumento != null,
            icone = PingoIcons.Landmark,
            testTag = "campo-origem",
            erro = CampoDeLancamentoManual.ORIGEM in state.erros,
            mensagem = RotulosDoNovoLancamento.mensagemDaOrigem(state),
        ),
        onClick = { onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.INSTRUMENTO)) },
    )
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Destino",
            texto = state.destino?.rotulo ?: "Escolha a conta de destino",
            preenchido = state.destino != null,
            icone = PingoIcons.Landmark,
            testTag = "campo-destino",
            erro = CampoDeLancamentoManual.DESTINO in state.erros,
            mensagem = RotulosDoNovoLancamento.mensagemDoDestino(state),
        ),
        onClick = { onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.DESTINO)) },
    )
}

@Composable
private fun SeletorDeTipo(tipo: TipoDoLancamentoManual, onSelecionar: (TipoDoLancamentoManual) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PingoRadius.md))
            .background(PingoTheme.colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TipoDoLancamentoManual.entries.forEach { candidato ->
            AbaDeTipo(
                tipo = candidato,
                selecionado = candidato == tipo,
                onClick = { onSelecionar(candidato) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AbaDeTipo(tipo: TipoDoLancamentoManual, selecionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(PingoRadius.sm))
            .background(if (selecionado) PingoTheme.colors.surface else Color.Transparent)
            .testTag("tipo-${tipo.name}")
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = tipo.rotulo,
            style = PingoTheme.typography.label,
            color = if (selecionado) CorDoTipo(tipo) else PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlocoDeValor(state: NovoLancamentoUiState) {
    val erro = CampoDeLancamentoManual.VALOR in state.erros
    val cor = if (erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (erro) PingoTheme.colors.expenseContainer else PingoTheme.colors.surfaceVariant,
                shape = RoundedCornerShape(PingoRadius.lg),
            )
            .padding(vertical = PingoSpacing.lg)
            .testTag("bloco-valor"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(text = "Valor", style = PingoTheme.typography.bodySmall, color = cor)
        PingoText(
            text = Valores.formatar(state.valorCentavos),
            style = PingoTheme.typography.amountLarge,
            color = if (erro) PingoTheme.colors.expense else PingoTheme.colors.primary,
        )
        PingoText(
            text = RotulosDoNovoLancamento.valor(state),
            style = PingoTheme.typography.caption,
            color = cor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CorDoTipo(tipo: TipoDoLancamentoManual): Color = when (tipo) {
    TipoDoLancamentoManual.DESPESA -> PingoTheme.colors.expense
    TipoDoLancamentoManual.RECEITA -> PingoTheme.colors.income
    TipoDoLancamentoManual.TRANSFERENCIA -> PingoTheme.colors.transfer
}
