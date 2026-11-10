package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingContasUiState(
    val escolhas: List<EscolhaBanco> = emptyList(),
    val salvamento: SalvamentoUiState = SalvamentoUiState.Idle,
    val concluido: Boolean = false,
) {
    val podeContinuar: Boolean
        get() = escolhas.isNotEmpty() && escolhas.all { it.valida() } && salvamento != SalvamentoUiState.Loading
}

enum class SalvamentoUiState {
    Idle,
    Loading,
    Error,
}
