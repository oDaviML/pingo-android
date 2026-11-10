package com.pingo.navigation

import com.pingo.core.auth.SessionState

enum class DestinoInicial {
    ONBOARDING,
    CONTAS,
    HOME,
}

fun destinoInicialPara(sessao: SessionState, consentimentoRegistrado: Boolean, contasConfiguradas: Boolean): DestinoInicial = when {
    sessao != SessionState.LoggedIn || !consentimentoRegistrado -> DestinoInicial.ONBOARDING
    !contasConfiguradas -> DestinoInicial.CONTAS
    else -> DestinoInicial.HOME
}
