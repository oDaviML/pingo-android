package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCategoryTone
import com.pingo.core.designsystem.component.PingoCheckbox
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoIconButton
import com.pingo.core.designsystem.component.PingoIconButtonStyle
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.AvatarDaInstituicao
import java.time.Instant
import java.time.ZoneId

@Composable
fun DetalheDoLancamentoContent(
    state: DetalheDoLancamentoUiState,
    onEvent: (DetalheDoLancamentoEvent) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    zona: ZoneId = ZoneId.systemDefault(),
    agora: Instant = Instant.now(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tela-detalhe-lancamento")
            .background(PingoTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CabecalhoDoDetalhe(state = state, onEvent = onEvent, onVoltar = onVoltar)
            when {
                state.carregando -> Carregando()
                state.naoEncontrado -> NaoEncontrado()
                else -> CorpoDoDetalhe(
                    state = state,
                    onEvent = onEvent,
                    zona = zona,
                    agora = agora,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PainelAtualDoDetalhe(state = state, onEvent = onEvent)
        SobreposicoesDoDetalhe(state = state, onEvent = onEvent, zona = zona)
    }
}

@Composable
private fun CabecalhoDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit, onVoltar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIconButton(
            icon = PingoIcons.ArrowLeft,
            contentDescription = "Voltar",
            onClick = onVoltar,
            style = PingoIconButtonStyle.Standard,
            modifier = Modifier.testTag("voltar-detalhe"),
        )
        PingoText(
            text = "Detalhe do lançamento",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PingoIconButton(
            icon = PingoIcons.Trash2,
            contentDescription = "Excluir lançamento",
            onClick = { onEvent(DetalheDoLancamentoEvent.SolicitarExclusao) },
            style = PingoIconButtonStyle.Standard,
            enabled = !state.carregando && !state.naoEncontrado && !state.excluindo,
            modifier = Modifier.testTag("excluir-detalhe"),
        )
    }
}

@Composable
private fun Carregando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PingoSpinner(size = 28.dp)
    }
}

@Composable
private fun NaoEncontrado() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PingoText(
            text = "Lançamento não encontrado",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CorpoDoDetalhe(
    state: DetalheDoLancamentoUiState,
    onEvent: (DetalheDoLancamentoEvent) -> Unit,
    zona: ZoneId,
    agora: Instant,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PingoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            BlocoDeValor(state = state, onEvent = onEvent)
            SeletorDeTipo(state = state, onEvent = onEvent)
            if (state.ehTransferencia) {
                CamposDaTransferencia(state = state, onEvent = onEvent)
            } else {
                CamposDoLancamento(state = state, onEvent = onEvent)
            }
            CampoDeDataDoDetalhe(state = state, onEvent = onEvent, zona = zona)
            if (state.ehTransferencia) {
                CartaoDoPar(state = state, zona = zona)
            } else {
                CampoDeEstabelecimentoDoDetalhe(state = state, onEvent = onEvent)
            }
            if (state.mensagem != null) {
                MensagemDoDetalhe(mensagem = state.mensagem, onEvent = onEvent)
            }
            state.origem?.let { origem -> CartaoDeOrigem(origem = origem, zona = zona, agora = agora) }
            AcoesDoDetalhe(state = state, onEvent = onEvent)
            Spacer(modifier = Modifier.height(PingoSpacing.lg))
        }
        if (state.tecladoVisivel) {
            TecladoNumerico(
                habilitado = state.podeAlternarValor,
                onDigito = { digito -> onEvent(DetalheDoLancamentoEvent.DigitoPressionado(digito)) },
                onApagar = { onEvent(DetalheDoLancamentoEvent.ApagarPressionado) },
            )
        }
    }
}

@Composable
private fun BlocoDeValor(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    val erro = CampoDoDetalhe.VALOR in state.erros
    val corLegenda = if (erro) PingoTheme.colors.expense else PingoTheme.colors.onSurfaceVariant
    val corValor = if (erro) PingoTheme.colors.expense else PingoTheme.colors.brand

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (erro) PingoTheme.colors.expenseContainer else PingoTheme.colors.surfaceVariant,
                shape = RoundedCornerShape(PingoRadius.lg),
            )
            .testTag("bloco-valor-detalhe")
            .clickable(enabled = state.podeAlternarValor) { onEvent(DetalheDoLancamentoEvent.AlternarTeclado) }
            .padding(vertical = PingoSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(text = "Valor", style = PingoTheme.typography.bodySmall, color = corLegenda)
        ValorEmDestaque(valorCentavos = state.valorCentavos, cor = corValor)
        PingoText(
            text = RotulosDoDetalhe.legendaDoValor(erro = erro, transferencia = state.ehTransferencia),
            style = PingoTheme.typography.caption,
            color = corLegenda,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CamposDoLancamento(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Instrumento",
            texto = state.instrumento?.rotulo ?: "Escolha um instrumento",
            preenchido = state.instrumento != null,
            icone = PingoIcons.CreditCard,
            testTag = "campo-instrumento-detalhe",
            erro = CampoDoDetalhe.INSTRUMENTO in state.erros || state.semInstrumentos,
            mensagem = RotulosDoDetalhe.mensagemDoCampo(CampoDoDetalhe.INSTRUMENTO, state)
                ?.takeIf { CampoDoDetalhe.INSTRUMENTO in state.erros || state.semInstrumentos },
        ),
        onClick = { onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.INSTRUMENTO)) },
    )
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Categoria",
            texto = state.categoria?.nome ?: "Escolher categoria",
            preenchido = state.categoria != null,
            icone = PingoIcons.Tag,
            testTag = "campo-categoria-detalhe",
            erro = CampoDoDetalhe.CATEGORIA in state.erros,
            mensagem = RotulosDoDetalhe.mensagemDoCampo(CampoDoDetalhe.CATEGORIA, state)
                ?.takeIf { CampoDoDetalhe.CATEGORIA in state.erros },
        ),
        onClick = { onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.CATEGORIA)) },
    )
    if (state.categoria != null) {
        PingoCheckbox(
            checked = state.aplicarCategoriaATodos,
            onCheckedChange = { aplicar -> onEvent(DetalheDoLancamentoEvent.AplicarATodosAlterado(aplicar)) },
            label = "Aplicar a todos deste estabelecimento",
            modifier = Modifier.testTag("aplicar-a-todos-detalhe"),
        )
    }
}

@Composable
private fun CamposDaTransferencia(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Origem",
            texto = state.instrumento?.rotulo ?: "Escolha a conta de origem",
            preenchido = state.instrumento != null,
            icone = PingoIcons.Landmark,
            testTag = "campo-instrumento-detalhe",
            erro = CampoDoDetalhe.INSTRUMENTO in state.erros,
            mensagem = RotulosDoDetalhe.mensagemDoCampo(CampoDoDetalhe.INSTRUMENTO, state)
                ?.takeIf { CampoDoDetalhe.INSTRUMENTO in state.erros },
        ),
        onClick = { onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.INSTRUMENTO)) },
    )
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Destino",
            texto = state.destino?.rotulo ?: "Escolha a conta de destino",
            preenchido = state.destino != null,
            icone = PingoIcons.Landmark,
            testTag = "campo-destino-detalhe",
            erro = CampoDoDetalhe.DESTINO in state.erros,
            mensagem = RotulosDoDetalhe.mensagemDoCampo(CampoDoDetalhe.DESTINO, state)
                ?.takeIf { CampoDoDetalhe.DESTINO in state.erros },
        ),
        onClick = { onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.DESTINO)) },
    )
}

@Composable
private fun CampoDeDataDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit, zona: ZoneId) {
    CampoDeSelecao(
        campo = CampoSelecionavel(
            rotulo = "Data",
            texto = RotulosDoDetalhe.dataEHora(state.dataHora, zona),
            preenchido = true,
            icone = PingoIcons.CalendarDays,
            testTag = "campo-data-detalhe",
        ),
        onClick = { onEvent(DetalheDoLancamentoEvent.AbrirCalendario) },
    )
}

@Composable
private fun CampoDeEstabelecimentoDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(
            text = RotulosDoDetalhe.rotuloDoEstabelecimento(state.origem),
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        LinhaDoEstabelecimentoDoDetalhe(state = state, onEvent = onEvent)
    }
}

@Composable
private fun LinhaDoEstabelecimentoDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    val icone = if (state.origem is OrigemDoLancamentoUi.Manual) PingoIcons.Pencil else PingoIcons.Store

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(PingoTheme.colors.surface, RoundedCornerShape(PingoRadius.md))
            .border(1.5.dp, PingoTheme.colors.outline, RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = icone,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
        EntradaDoEstabelecimento(
            texto = state.estabelecimento,
            onTexto = { texto -> onEvent(DetalheDoLancamentoEvent.EstabelecimentoAlterado(texto)) },
            modifier = Modifier.weight(1f),
        )
        if (state.estabelecimento.isNotEmpty()) {
            BotaoLimparDoDetalhe { onEvent(DetalheDoLancamentoEvent.EstabelecimentoAlterado("")) }
        }
    }
}

@Composable
private fun BotaoLimparDoDetalhe(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .testTag("limpar-estabelecimento-detalhe")
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.X,
            contentDescription = "Limpar estabelecimento",
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EntradaDoEstabelecimento(texto: String, onTexto: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (texto.isEmpty()) {
            PingoText(
                text = "Opcional",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = texto,
            onValueChange = onTexto,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("campo-estabelecimento-detalhe"),
            textStyle = PingoTheme.typography.body.copy(color = PingoTheme.colors.onBackground),
            cursorBrush = SolidColor(PingoTheme.colors.primary),
            singleLine = true,
        )
    }
}

@Composable
private fun CartaoDeOrigem(origem: OrigemDoLancamentoUi, zona: ZoneId, agora: Instant) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier
            .testTag("origem-lancamento")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        gap = PingoSpacing.md,
    ) {
        PingoText(text = "ORIGEM", style = PingoTheme.typography.overline, color = PingoTheme.colors.onSurfaceVariant)
        when (origem) {
            is OrigemDoLancamentoUi.Manual -> OrigemManual(origem = origem, zona = zona)
            is OrigemDoLancamentoUi.Capturado -> OrigemCapturada(origem = origem, zona = zona, agora = agora)
        }
    }
}

@Composable
private fun OrigemManual(origem: OrigemDoLancamentoUi.Manual, zona: ZoneId) {
    Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        CategoryAvatar(
            icon = PingoIcons.Pencil,
            tone = PingoCategoryTone.Default,
            size = 44.dp,
            iconSize = 20.dp,
            cornerRadius = PingoRadius.md,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(text = "Manual", style = PingoTheme.typography.bodyStrong, color = PingoTheme.colors.onBackground)
            PingoText(
                text = RotulosDoDetalhe.registradoEm(origem.registradoEm, zona),
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OrigemCapturada(origem: OrigemDoLancamentoUi.Capturado, zona: ZoneId, agora: Instant) {
    Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        AvatarDaInstituicao(instituicao = origem.instituicao, size = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(
                text = RotulosDoDetalhe.nomeDaInstituicao(origem.instituicao),
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
                modifier = Modifier.testTag("origem-instituicao"),
            )
            PingoText(
                text = RotulosDoDetalhe.lidaEm(origem.lidoEm, agora, zona),
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        PingoStatusTag(
            label = RotulosDoDetalhe.confianca(origem.confianca),
            containerColor = PingoTheme.colors.incomeContainer,
            contentColor = PingoTheme.colors.income,
            dotColor = PingoTheme.colors.income,
            modifier = Modifier.testTag("origem-confianca"),
        )
    }
    TextoInterpretado(origem = origem)
}

@Composable
private fun TextoInterpretado(origem: OrigemDoLancamentoUi.Capturado) {
    val forma = RoundedCornerShape(PingoRadius.md)
    PingoText(
        text = "Texto interpretado (fica só neste aparelho)",
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surfaceVariant, forma)
            .border(width = 1.dp, color = PingoTheme.colors.outlineVariant, shape = forma)
            .padding(PingoSpacing.md)
            .testTag("origem-texto"),
    ) {
        PingoText(
            text = RotulosDoDetalhe.textoInterpretadoDisponivel(origem.textoBruto),
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartaoDoPar(state: DetalheDoLancamentoUiState, zona: ZoneId) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape)) {
        PingoText(
            text = "PAR DA TRANSFERÊNCIA",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        val par = state.par
        if (par == null) {
            PingoText(
                text = "Par não encontrado",
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
                modifier = Modifier.testTag("par-transferencia-ausente"),
            )
        } else {
            LinhaDoPar(par = par, zona = zona)
        }
    }
    if (state.par != null) {
        PingoBanner(
            title = "Duas notificações, um lançamento",
            description = "O Pingo juntou a saída e a entrada para não contar em dobro.",
            tone = PingoBannerTone.Info,
            icon = PingoIcons.Bell,
            showTrailingChevron = false,
        )
    }
}

@Composable
private fun LinhaDoPar(par: ParDaTransferenciaUi, zona: ZoneId) {
    Row(
        modifier = Modifier.testTag("par-transferencia"),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = PingoIcons.ArrowLeftRight,
            tone = PingoCategoryTone.Transfer,
            size = 44.dp,
            iconSize = 22.dp,
            cornerRadius = PingoRadius.md,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(
                text = RotulosDoDetalhe.tituloDoPar(par.entrada, par.rotuloDoVinculo),
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PingoText(
                text = "${par.rotuloDoVinculo} · ${RotulosDoDetalhe.horarioDoPar(par.dataHora, zona)}",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PingoText(
            text = RotulosDoDetalhe.valorDoPar(par.valorCentavos),
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.transfer,
            maxLines = 1,
        )
    }
}

@Composable
private fun AcoesDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoButton(
            text = "Salvar alterações",
            onClick = { onEvent(DetalheDoLancamentoEvent.Salvar) },
            enabled = state.podeSalvar,
            loading = state.salvando,
            fillWidth = true,
            modifier = Modifier.testTag("salvar-detalhe"),
        )
        BotaoDaTransferencia(state = state, onEvent = onEvent)
        PingoButton(
            text = "Excluir lançamento",
            onClick = { onEvent(DetalheDoLancamentoEvent.SolicitarExclusao) },
            type = PingoButtonType.Text,
            fillWidth = true,
            modifier = Modifier.testTag("excluir-lancamento"),
        )
    }
}

@Composable
private fun BotaoDaTransferencia(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    if (state.ehTransferencia) {
        PingoButton(
            text = "Desfazer transferência",
            onClick = { onEvent(DetalheDoLancamentoEvent.DesfazerTransferencia) },
            type = PingoButtonType.Outline,
            enabled = state.podeSalvar,
            fillWidth = true,
            modifier = Modifier.testTag("desfazer-transferencia"),
        )
    } else {
        PingoButton(
            text = "Marcar como transferência",
            onClick = { onEvent(DetalheDoLancamentoEvent.SolicitarTransferencia) },
            type = PingoButtonType.Outline,
            enabled = state.podeSalvar,
            fillWidth = true,
            modifier = Modifier.testTag("marcar-transferencia"),
        )
    }
}

@Composable
private fun MensagemDoDetalhe(mensagem: MensagemDoDetalhe, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    PingoCard(modifier = Modifier.testTag("mensagem-detalhe"), contentPadding = PingoSpacing.md) {
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            PingoIcon(
                imageVector = PingoIcons.TriangleAlert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PingoTheme.colors.warning,
            )
            PingoText(
                text = RotulosDoDetalhe.mensagem(mensagem),
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onWarningContainer,
                modifier = Modifier.weight(1f),
            )
            PingoButton(
                text = "OK",
                onClick = { onEvent(DetalheDoLancamentoEvent.DispensarMensagem) },
                type = PingoButtonType.Text,
                size = PingoButtonSize.Small,
            )
        }
    }
}
