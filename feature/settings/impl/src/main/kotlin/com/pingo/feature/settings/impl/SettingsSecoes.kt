package com.pingo.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SettingsRow
import com.pingo.core.designsystem.component.SettingsRowStyle
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TemaDoApp

@Composable
internal fun AparenciaCartao(aparencia: SettingsAparenciaUi) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        SettingsRow(
            title = "Tema",
            subtitle = aparencia.tema.rotulo(),
            icon = if (aparencia.tema == TemaDoApp.ESCURO) PingoIcons.Moon else PingoIcons.Sun,
            onClick = aparencia.onAbrirTema,
        )
    }
}

@Composable
internal fun ContaCartao(conta: SettingsContaUi) {
    val nome = conta.nomeUsuario?.takeIf { it.isNotBlank() } ?: "Conta Pingo"
    val email = conta.emailUsuario?.takeIf { it.isNotBlank() } ?: "Sessão ativa com Google"
    val shape = RoundedCornerShape(PingoRadius.xl)

    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        ContaCabecalho(nome = nome, email = email)
        SettingsDivider()
        SettingsRow(
            title = "Sair da conta",
            subtitle = "Mantém seus dados salvos neste aparelho",
            icon = PingoIcons.LogOut,
            onClick = conta.onSolicitarLogout,
        )
    }
}

@Composable
private fun ContaCabecalho(nome: String, email: String) {
    val inicial = nome.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = PingoTheme.colors.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PingoText(text = inicial, style = PingoTheme.typography.title, color = PingoTheme.colors.onPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(text = nome, style = PingoTheme.typography.bodyStrong, color = PingoTheme.colors.onBackground)
            PingoText(text = email, style = PingoTheme.typography.bodySmall, color = PingoTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SegurancaCartao(seguranca: SettingsSegurancaUi) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        PingoCard(
            modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
            contentPadding = 0.dp,
            gap = 0.dp,
        ) {
            ToggleSettingsRow(
                icon = PingoIcons.Fingerprint,
                title = "Biometria ao abrir o app",
                subtitle = "Exige digital, facial ou senha do aparelho",
                checked = seguranca.biometriaAtiva,
                onCheckedChange = seguranca.onToggleBiometria,
                testTag = "switch_biometria",
            )
            SettingsDivider()
            SettingsRow("Bloquear após", seguranca.tempoBloqueio.rotulo, PingoIcons.Lock, seguranca.onAbrirTempoBloqueio)
            SettingsDivider()
            SettingsRow(
                title = "Categorias e regras aprendidas",
                subtitle = "Criar, editar, arquivar e limpar regras",
                icon = PingoIcons.Tag,
                onClick = seguranca.onAbrirCategoriasERegras,
            )
        }
        seguranca.mensagemSeguranca?.let { mensagem ->
            PingoBanner(
                title = "Bloqueio por biometria",
                description = mensagem,
                tone = PingoBannerTone.Warning,
                onClick = if (seguranca.mostrarAtalhoCadastroBiometria) seguranca.onAbrirConfiguracoesSeguranca else null,
            )
        }
    }
}

@Composable
internal fun NotificacoesCartao(notificacoes: SettingsNotificacoesUi) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        ToggleSettingsRow(
            icon = PingoIcons.Bell,
            title = "Revisar pela notificação",
            subtitle = "Ações rápidas para confirmar ou corrigir categoria",
            checked = notificacoes.revisarPelaNotificacaoAtivo,
            onCheckedChange = notificacoes.onToggleRevisarPelaNotificacao,
            testTag = "switch_revisar_notificacao",
        )
        SettingsDivider()
        ToggleSettingsRow(
            icon = PingoIcons.TriangleAlert,
            title = "Alertar se captura parar",
            subtitle = "Avisa após 48h sem ler novas notificações",
            checked = notificacoes.alertaCapturaParadaAtivo,
            onCheckedChange = notificacoes.onToggleAlertaCapturaParada,
            testTag = "switch_alerta_captura_parada",
        )
        SettingsDivider()
        ToggleSettingsRow(
            icon = PingoIcons.Repeat,
            title = "Avisos de assinaturas",
            subtitle = "2 dias antes da cobrança estimada",
            checked = notificacoes.avisoAssinaturasAtivo,
            onCheckedChange = notificacoes.onToggleAvisoAssinaturas,
            testTag = "switch_avisos_assinaturas",
        )
    }
}

@Composable
internal fun PrivacidadeCartao(privacidade: SettingsPrivacidadeUi) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        ToggleSettingsRow(
            icon = PingoIcons.ChartPie,
            title = "Telemetria anônima",
            subtitle = "Estatísticas de captura sem textos bancários",
            checked = privacidade.telemetriaOptIn,
            onCheckedChange = privacidade.onToggleTelemetria,
            testTag = "switch_telemetria",
        )
        SettingsDivider()
        SettingsRow(
            title = "Termo de privacidade",
            subtitle = "Como o Pingo trata seus dados",
            icon = PingoIcons.FileText,
            onClick = privacidade.onShowTerm,
        )
        SettingsDivider()
        SettingsRow(
            title = "Excluir conta e dados",
            subtitle = "Apaga tudo no aparelho e na nuvem",
            icon = PingoIcons.Trash2,
            onClick = privacidade.onDeleteAccount,
            style = SettingsRowStyle.Danger,
        )
    }
}

@Composable
internal fun SobreCartao(sobre: SettingsSobreUi) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        PingoCard(
            modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
            contentPadding = 0.dp,
            gap = 0.dp,
        ) {
            SettingsRow(
                title = "Versão do Pingo",
                subtitle = "v${sobre.versaoApp} (build ${sobre.codigoVersaoApp})",
                icon = PingoIcons.Info,
                onClick = sobre.onVerificarAtualizacao,
            )
            SettingsDivider()
            SettingsRow(
                title = "Procurar atualização",
                subtitle = "Verifica novas versões direto no GitHub",
                icon = PingoIcons.RefreshCw,
                onClick = sobre.onVerificarAtualizacao,
            )
        }
        EstadoAtualizacaoBloco(sobre = sobre)
    }
}

@Composable
private fun EstadoAtualizacaoBloco(sobre: SettingsSobreUi) {
    when (val estado = sobre.estadoAtualizacao) {
        EstadoAtualizacaoAppUi.Ocioso -> Unit
        EstadoAtualizacaoAppUi.Verificando -> StatusAtualizacaoCard("Verificando atualizações...", "Consultando releases no GitHub.")
        is EstadoAtualizacaoAppUi.Atualizado ->
            StatusAtualizacaoCard("Você já está na versão mais recente", "Versão v${estado.versao} instalada.")
        is EstadoAtualizacaoAppUi.Disponivel -> AtualizacaoDisponivelCard(estado, sobre)
        is EstadoAtualizacaoAppUi.Baixando -> BaixandoAtualizacaoCard(estado)
        is EstadoAtualizacaoAppUi.ProntoParaInstalar -> ProntoParaInstalarCard(estado, sobre)
        is EstadoAtualizacaoAppUi.Falha -> FalhaAtualizacaoCard(estado, sobre)
    }
}

@Composable
private fun StatusAtualizacaoCard(titulo: String, descricao: String) {
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        PingoText(text = titulo, style = PingoTheme.typography.bodyStrong)
        PingoText(text = descricao, style = PingoTheme.typography.bodySmall, color = PingoTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun AtualizacaoDisponivelCard(estado: EstadoAtualizacaoAppUi.Disponivel, sobre: SettingsSobreUi) {
    val info = estado.info
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        PingoText(text = "Nova versão disponível: v${info.versao}", style = PingoTheme.typography.bodyStrong)
        if (info.notas.isNotBlank()) {
            PingoText(text = info.notas, style = PingoTheme.typography.bodySmall, color = PingoTheme.colors.onSurfaceVariant)
        }
        PingoButton(
            text = "Baixar atualização",
            onClick = { sobre.onBaixarAtualizacao(info) },
            modifier = Modifier.fillMaxWidth().testTag("botao_baixar_atualizacao"),
        )
        PingoButton(
            text = "Abrir página no GitHub",
            onClick = { sobre.onAbrirPaginaRelease(info.urlPaginaRelease) },
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BaixandoAtualizacaoCard(estado: EstadoAtualizacaoAppUi.Baixando) {
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        PingoText(
            text = "Baixando v${estado.info.versao} (${estado.percentual}%)",
            style = PingoTheme.typography.bodyStrong,
        )
        PingoLinearProgress(progress = estado.percentual / 100f)
    }
}

@Composable
private fun ProntoParaInstalarCard(estado: EstadoAtualizacaoAppUi.ProntoParaInstalar, sobre: SettingsSobreUi) {
    PingoCard(modifier = Modifier.fillMaxWidth()) {
        PingoText(text = "Atualização v${estado.info.versao} verificada", style = PingoTheme.typography.bodyStrong)
        val textoAcao = if (estado.precisaPermissaoFontes) {
            "Permitir instalação de fontes desconhecidas"
        } else {
            "Instalar APK"
        }
        val acao: () -> Unit = if (estado.precisaPermissaoFontes) {
            sobre.onAbrirPermissaoFontes
        } else {
            { sobre.onInstalarAtualizacao(estado.arquivoApk) }
        }
        PingoButton(
            text = textoAcao,
            onClick = acao,
            modifier = Modifier.fillMaxWidth().testTag("botao_instalar_atualizacao"),
        )
    }
}

@Composable
private fun FalhaAtualizacaoCard(estado: EstadoAtualizacaoAppUi.Falha, sobre: SettingsSobreUi) {
    PingoBanner(
        title = "Atualização não concluída",
        description = estado.mensagem,
        tone = PingoBannerTone.Warning,
        onClick = sobre.onVerificarAtualizacao,
    )
    estado.info?.let { info ->
        PingoButton(
            text = "Abrir release no GitHub",
            onClick = { sobre.onAbrirPaginaRelease(info.urlPaginaRelease) },
            type = PingoButtonType.Outline,
            size = PingoButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ToggleSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null,
) {
    val modifierBase = Modifier
        .fillMaxWidth()
        .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
        .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md)
    Row(
        modifier = if (testTag != null) modifierBase.testTag(testTag) else modifierBase,
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleSettingsIcon(icon = icon)
        ToggleSettingsTexts(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        PingoSwitch(checked = checked)
    }
}

@Composable
private fun ToggleSettingsIcon(icon: ImageVector) {
    val colors = PingoTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onPrimaryContainer)
    }
}

@Composable
private fun ToggleSettingsTexts(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = PingoTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PingoText(text = title, style = PingoTheme.typography.bodyStrong, color = colors.onBackground)
        PingoText(text = subtitle, style = PingoTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun PingoSwitch(checked: Boolean) {
    val colors = PingoTheme.colors
    val trackModifier = if (checked) {
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .padding(4.dp)
    } else {
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(width = 2.dp, color = colors.outline, shape = CircleShape)
            .padding(horizontal = 8.dp)
    }

    Box(modifier = trackModifier, contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .size(if (checked) 24.dp else 16.dp)
                .background(color = if (checked) colors.onPrimary else colors.onSurfaceVariant, shape = CircleShape),
        )
    }
}

@Composable
internal fun SettingsDivider(color: Color = PingoTheme.colors.outlineVariant) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
