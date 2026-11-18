package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.auth.DisponibilidadeAutenticacao

@Immutable
data class OnboardingBiometriaUiState(
    val disponibilidade: DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL,
    val autenticando: Boolean = false,
    val semCadastroNoAparelho: Boolean = false,
    val mensagemErro: String? = null,
    val concluido: Boolean = false,
)
