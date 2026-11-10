package com.pingo.feature.onboarding.impl

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

sealed interface OnboardingContasEvent {
    data class AlternarBanco(val instituicao: InstituicaoFinanceira) : OnboardingContasEvent

    data class AlterarApelidoConta(val instituicao: InstituicaoFinanceira, val apelido: String) : OnboardingContasEvent

    data class AlterarTipoConta(val instituicao: InstituicaoFinanceira, val tipo: TipoConta) : OnboardingContasEvent

    data class AlternarInstrumento(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento) : OnboardingContasEvent

    data class AlterarApelidoInstrumento(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento, val apelido: String) :
        OnboardingContasEvent

    data class AlterarUltimosDigitos(val instituicao: InstituicaoFinanceira, val tipo: TipoInstrumento, val digitos: String) :
        OnboardingContasEvent

    data object Continuar : OnboardingContasEvent

    data object Pular : OnboardingContasEvent
}
