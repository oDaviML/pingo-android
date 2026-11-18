package com.pingo.feature.onboarding.impl

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.auth.AutenticadorDoSistema
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.auth.GerenciadorDeBloqueioDoApp
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingBiometriaViewModel @Inject constructor(
    private val autenticador: AutenticadorDoSistema,
    private val preferences: UserPreferencesDataSource,
    private val gerenciadorDeBloqueio: GerenciadorDeBloqueioDoApp,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingBiometriaUiState())
    val uiState: StateFlow<OnboardingBiometriaUiState> = mutableUiState.asStateFlow()

    init {
        atualizarDisponibilidade()
    }

    fun atualizarDisponibilidade() {
        val disponibilidade = autenticador.disponibilidade()
        mutableUiState.update { state ->
            state.copy(
                disponibilidade = disponibilidade,
                semCadastroNoAparelho = disponibilidade == DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO,
            )
        }
    }

    fun ativar(activity: Activity?) {
        if (mutableUiState.value.autenticando || mutableUiState.value.concluido) {
            return
        }
        if (activity == null) {
            mutableUiState.update { it.copy(mensagemErro = MENSAGEM_ERRO_AUTENTICACAO) }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(autenticando = true, mensagemErro = null) }
            gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(true)
            val resultado = try {
                autenticador.autenticar(
                    activity = activity,
                    titulo = "Ativar bloqueio do Pingo",
                    subtitulo = "Confirme com sua biometria ou senha do aparelho",
                )
            } finally {
                gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(false)
            }
            tratarResultado(resultado)
        }
    }

    fun abrirConfiguracoesDeSeguranca(activity: Activity) {
        autenticador.abrirConfiguracoesDeSeguranca(activity)
    }

    fun pularOuAgoraNao() {
        if (mutableUiState.value.autenticando || mutableUiState.value.concluido) {
            return
        }
        viewModelScope.launch {
            preferences.setBiometriaAtiva(false)
            preferences.setBiometriaOnboardingConcluido(true)
            mutableUiState.update { it.copy(concluido = true) }
        }
    }

    private suspend fun tratarResultado(resultado: ResultadoAutenticacao) {
        when (resultado) {
            ResultadoAutenticacao.Autenticado -> {
                gerenciadorDeBloqueio.marcarDesbloqueado()
                preferences.setBiometriaAtiva(true)
                preferences.setBiometriaOnboardingConcluido(true)
                mutableUiState.update { it.copy(autenticando = false, concluido = true) }
            }

            ResultadoAutenticacao.SemCadastroNoAparelho -> mutableUiState.update {
                it.copy(
                    autenticando = false,
                    semCadastroNoAparelho = true,
                    mensagemErro = null,
                )
            }

            ResultadoAutenticacao.Indisponivel -> mutableUiState.update {
                it.copy(
                    autenticando = false,
                    disponibilidade = DisponibilidadeAutenticacao.INDISPONIVEL,
                    mensagemErro = MENSAGEM_INDISPONIVEL,
                )
            }

            ResultadoAutenticacao.Cancelado -> mutableUiState.update {
                it.copy(autenticando = false)
            }

            is ResultadoAutenticacao.Falha -> mutableUiState.update {
                it.copy(autenticando = false, mensagemErro = MENSAGEM_ERRO_AUTENTICACAO)
            }
        }
    }

    private companion object {
        const val MENSAGEM_ERRO_AUTENTICACAO =
            "Não foi possível confirmar sua identidade. Tente de novo ou ative depois em Configurações."
        const val MENSAGEM_INDISPONIVEL =
            "Este aparelho não possui biometria nem bloqueio de tela compatível."
    }
}
