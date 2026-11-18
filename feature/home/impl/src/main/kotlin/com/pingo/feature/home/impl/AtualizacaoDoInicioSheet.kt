package com.pingo.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoBottomSheet
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.EstadoAtualizacaoDoApp
import com.pingo.core.network.update.FormatadorDeChangelog
import com.pingo.core.ui.ChangelogAtualizacaoBloco
import com.pingo.core.ui.ItemChangelogUi
import com.pingo.core.ui.SecaoChangelogUi
import java.io.File

@Composable
internal fun AtualizacaoDoInicioSheet(state: HomeUiState, acoes: AcoesAtualizacaoHome) {
    val estado = state.estadoAtualizacao
    val cabecalho = cabecalhoDoSheet(estado, state.versaoApp)

    PingoBottomSheet(
        onDismissRequest = acoes.onFechar,
        title = cabecalho.first,
        subtitle = cabecalho.second,
        modifier = Modifier.testTag("sheet-atualizacao-home"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            CorpoDoSheetDeAtualizacao(estado = estado, acoes = acoes)
        }
    }
}

@Composable
private fun CorpoDoSheetDeAtualizacao(estado: EstadoAtualizacaoDoApp, acoes: AcoesAtualizacaoHome) {
    when (estado) {
        EstadoAtualizacaoDoApp.Ocioso -> BlocoAtualizado(
            descricao = "Nenhuma nova atualização encontrada até o momento.",
            onVerificar = acoes.onVerificar,
        )
        EstadoAtualizacaoDoApp.Verificando -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PingoSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            PingoSpinner(size = 28.dp)
        }
        is EstadoAtualizacaoDoApp.Atualizado -> BlocoAtualizado(
            descricao = "Você já está usando a versão mais recente (v${estado.versao}).",
            onVerificar = acoes.onVerificar,
        )
        is EstadoAtualizacaoDoApp.Disponivel -> BlocoDisponivel(estado.info, acoes)
        is EstadoAtualizacaoDoApp.Baixando -> BlocoBaixando(estado)
        is EstadoAtualizacaoDoApp.ProntoParaInstalar -> BlocoProntoParaInstalar(estado, acoes)
        is EstadoAtualizacaoDoApp.Falha -> BlocoFalha(estado, acoes)
    }
}

@Composable
private fun BlocoAtualizado(descricao: String, onVerificar: () -> Unit) {
    PingoText(
        text = descricao,
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
    PingoButton(
        text = "Verificar novamente",
        onClick = onVerificar,
        type = PingoButtonType.Outline,
        fillWidth = true,
        modifier = Modifier.testTag("botao-verificar-atualizacao-home"),
    )
}

@Composable
private fun BlocoDisponivel(info: AtualizacaoDisponivel, acoes: AcoesAtualizacaoHome) {
    ChangelogResumidaDoSheet(notas = info.notas)
    PingoButton(
        text = "Baixar e atualizar",
        onClick = { acoes.onBaixar(info) },
        fillWidth = true,
        modifier = Modifier.testTag("botao-baixar-atualizacao-home"),
    )
    PingoButton(
        text = "Abrir página no GitHub",
        onClick = { acoes.onAbrirRelease(info.urlPaginaRelease) },
        type = PingoButtonType.Text,
        size = PingoButtonSize.Small,
        fillWidth = true,
    )
}

@Composable
private fun BlocoBaixando(estado: EstadoAtualizacaoDoApp.Baixando) {
    ChangelogResumidaDoSheet(notas = estado.info.notas)
    PingoText(
        text = "Baixando v${estado.info.versao} (${estado.percentual}%)",
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
    PingoLinearProgress(progress = estado.percentual / 100f)
}

@Composable
private fun BlocoProntoParaInstalar(estado: EstadoAtualizacaoDoApp.ProntoParaInstalar, acoes: AcoesAtualizacaoHome) {
    ChangelogResumidaDoSheet(notas = estado.info.notas)
    val textoAcao = if (estado.precisaPermissaoFontes) {
        "Permitir instalação de fontes desconhecidas"
    } else {
        "Instalar atualização"
    }
    val onClick: () -> Unit = if (estado.precisaPermissaoFontes) {
        acoes.onPermitirFontes
    } else {
        { acoes.onInstalar(estado.arquivoApk) }
    }
    PingoButton(
        text = textoAcao,
        onClick = onClick,
        fillWidth = true,
        modifier = Modifier.testTag("botao-instalar-atualizacao-home"),
    )
}

@Composable
private fun BlocoFalha(estado: EstadoAtualizacaoDoApp.Falha, acoes: AcoesAtualizacaoHome) {
    PingoBanner(
        title = "Não foi possível atualizar",
        description = estado.mensagem,
        tone = PingoBannerTone.Warning,
    )
    val info = estado.info
    if (info != null) {
        PingoButton(
            text = "Tentar download novamente",
            onClick = { acoes.onBaixar(info) },
            fillWidth = true,
        )
        PingoButton(
            text = "Abrir release no GitHub",
            onClick = { acoes.onAbrirRelease(info.urlPaginaRelease) },
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            fillWidth = true,
        )
    } else {
        PingoButton(
            text = "Tentar novamente",
            onClick = acoes.onVerificar,
            type = PingoButtonType.Outline,
            fillWidth = true,
        )
    }
}

@Composable
private fun ChangelogResumidaDoSheet(notas: String) {
    val secoes = remember(notas) {
        FormatadorDeChangelog.formatar(notas).secoes.map { secao ->
            SecaoChangelogUi(
                titulo = secao.titulo,
                itens = secao.itens.map { item -> ItemChangelogUi(item.destaque, item.descricao) },
            )
        }
    }
    ChangelogAtualizacaoBloco(secoes = secoes)
}

private fun cabecalhoDoSheet(estado: EstadoAtualizacaoDoApp, versaoInstalada: String): Pair<String, String?> = when (estado) {
    EstadoAtualizacaoDoApp.Ocioso -> "Atualizações do app" to versaoInstalada.takeIf { it.isNotBlank() }?.let { "Versão instalada v$it" }
    EstadoAtualizacaoDoApp.Verificando -> "Verificando atualizações" to "Consultando releases no GitHub"
    is EstadoAtualizacaoDoApp.Atualizado -> "Pingo atualizado" to "Versão v${estado.versao}"
    is EstadoAtualizacaoDoApp.Disponivel -> "Atualização disponível" to "Pingo v${estado.info.versao}"
    is EstadoAtualizacaoDoApp.Baixando -> "Baixando atualização" to "Pingo v${estado.info.versao}"
    is EstadoAtualizacaoDoApp.ProntoParaInstalar -> "Pronto para instalar" to "Pingo v${estado.info.versao}"
    is EstadoAtualizacaoDoApp.Falha -> "Atualização" to estado.info?.let { "Pingo v${it.versao}" }
}
