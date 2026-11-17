package com.pingo.feature.onboarding.impl

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.ui.contas.EventoDoFormularioDeConta

sealed interface OnboardingContasEvent {
    data class AlternarBanco(val instituicao: InstituicaoFinanceira) : OnboardingContasEvent

    data class AlterarFormulario(val instituicao: InstituicaoFinanceira, val evento: EventoDoFormularioDeConta) : OnboardingContasEvent

    data object Continuar : OnboardingContasEvent

    data object Pular : OnboardingContasEvent
}
