package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.ui.contas.ContaFormulario

@Immutable
data class OnboardingContasUiState(
    val contas: List<ContaFormulario> = emptyList(),
    val salvamento: SalvamentoUiState = SalvamentoUiState.Idle,
    val concluido: Boolean = false,
) {
    val podeContinuar: Boolean
        get() = contas.isNotEmpty() && contas.all(ContaFormulario::valida) && salvamento != SalvamentoUiState.Loading
}

enum class SalvamentoUiState {
    Idle,
    Loading,
    Error,
}
