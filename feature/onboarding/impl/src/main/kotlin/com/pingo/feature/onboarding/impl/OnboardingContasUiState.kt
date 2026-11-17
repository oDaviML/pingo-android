package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.ui.contas.ContaFormulario

@Immutable
data class OnboardingContasUiState(
    val contas: List<ContaFormulario> = emptyList(),
    val salvamento: SalvamentoUiState = SalvamentoUiState.Idle,
    val concluido: Boolean = false,
    val recuperando: Boolean = false,
) {
    val formularioEditavel: Boolean
        get() = !recuperando && salvamento != SalvamentoUiState.Loading

    val podeContinuar: Boolean
        get() = formularioEditavel && contas.isNotEmpty() && contas.all(ContaFormulario::valida)
}

enum class SalvamentoUiState {
    Idle,
    Loading,
    Error,
}
