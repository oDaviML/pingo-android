package com.pingo.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.network.update.AtualizacaoDisponivel
import java.io.File

enum class OpcaoBloqueio(val segundos: Long, val rotulo: String) {
    IMEDIATO(0L, "Imediato"),
    UM_MINUTO(60L, "1 minuto"),
    CINCO_MINUTOS(300L, "5 minutos"),
    QUINZE_MINUTOS(900L, "15 minutos"),
    NUNCA(-1L, "Nunca"),
    ;

    companion object {
        fun deSegundos(segundos: Long): OpcaoBloqueio = entries.firstOrNull { it.segundos == segundos } ?: CINCO_MINUTOS
    }
}

sealed interface EstadoAtualizacaoAppUi {
    data object Ocioso : EstadoAtualizacaoAppUi

    data object Verificando : EstadoAtualizacaoAppUi

    data class Atualizado(val versao: String) : EstadoAtualizacaoAppUi

    data class Disponivel(val info: AtualizacaoDisponivel) : EstadoAtualizacaoAppUi

    data class Baixando(val info: AtualizacaoDisponivel, val percentual: Int) : EstadoAtualizacaoAppUi

    data class ProntoParaInstalar(val info: AtualizacaoDisponivel, val arquivoApk: File, val precisaPermissaoFontes: Boolean) :
        EstadoAtualizacaoAppUi

    data class Falha(val mensagem: String, val info: AtualizacaoDisponivel? = null) : EstadoAtualizacaoAppUi
}

@Immutable
data class SettingsUiState(
    val emailUsuario: String? = null,
    val nomeUsuario: String? = null,
    val biometriaAtiva: Boolean = false,
    val disponibilidadeBiometria: DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL,
    val tempoBloqueioSegundos: Long = OpcaoBloqueio.CINCO_MINUTOS.segundos,
    val revisarPelaNotificacaoAtivo: Boolean = true,
    val alertaCapturaParadaAtivo: Boolean = true,
    val avisoAssinaturasAtivo: Boolean = false,
    val permissaoNotificacaoNegada: Boolean = false,
    val telemetriaOptIn: Boolean = false,
    val versaoApp: String = "0.2.0",
    val codigoVersaoApp: Long = 1L,
    val estadoAtualizacao: EstadoAtualizacaoAppUi = EstadoAtualizacaoAppUi.Ocioso,
    val showLogoutDialog: Boolean = false,
    val saindoDaConta: Boolean = false,
    val loggedOut: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleting: Boolean = false,
    val deleteFailed: Boolean = false,
    val accountDeleted: Boolean = false,
    val mensagemSeguranca: String? = null,
    val mostrarAtalhoCadastroBiometria: Boolean = false,
    val mostrarSheetTempoBloqueio: Boolean = false,
)

@Immutable
data class SettingsContaUi(val nomeUsuario: String? = null, val emailUsuario: String? = null, val onSolicitarLogout: () -> Unit = {})

@Immutable
data class SettingsSegurancaUi(
    val biometriaAtiva: Boolean = false,
    val disponibilidadeBiometria: DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL,
    val tempoBloqueio: OpcaoBloqueio = OpcaoBloqueio.CINCO_MINUTOS,
    val mensagemSeguranca: String? = null,
    val mostrarAtalhoCadastroBiometria: Boolean = false,
    val onToggleBiometria: (Boolean) -> Unit = {},
    val onAbrirTempoBloqueio: () -> Unit = {},
    val onAbrirConfiguracoesSeguranca: () -> Unit = {},
    val onAbrirCategoriasERegras: () -> Unit = {},
)

@Immutable
data class SettingsNotificacoesUi(
    val avisoAssinaturasAtivo: Boolean = false,
    val permissaoNotificacaoNegada: Boolean = false,
    val onToggleAvisoAssinaturas: (Boolean) -> Unit = {},
    val onAbrirConfiguracoesNotificacao: () -> Unit = {},
    val revisarPelaNotificacaoAtivo: Boolean = true,
    val onToggleRevisarPelaNotificacao: (Boolean) -> Unit = {},
    val alertaCapturaParadaAtivo: Boolean = true,
    val onToggleAlertaCapturaParada: (Boolean) -> Unit = {},
)

@Immutable
data class SettingsPrivacidadeUi(
    val telemetriaOptIn: Boolean = false,
    val onToggleTelemetria: (Boolean) -> Unit = {},
    val onShowTerm: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
)

@Immutable
data class SettingsSobreUi(
    val versaoApp: String = "0.2.0",
    val codigoVersaoApp: Long = 1L,
    val estadoAtualizacao: EstadoAtualizacaoAppUi = EstadoAtualizacaoAppUi.Ocioso,
    val onVerificarAtualizacao: () -> Unit = {},
    val onBaixarAtualizacao: (AtualizacaoDisponivel) -> Unit = {},
    val onInstalarAtualizacao: (File) -> Unit = {},
    val onAbrirPermissaoFontes: () -> Unit = {},
    val onAbrirPaginaRelease: (String) -> Unit = {},
)
