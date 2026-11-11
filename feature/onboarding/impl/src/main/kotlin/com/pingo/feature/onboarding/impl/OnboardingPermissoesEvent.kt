package com.pingo.feature.onboarding.impl

sealed interface OnboardingPermissoesEvent {
    data object AbrirAjustesDeNotificacoes : OnboardingPermissoesEvent

    data object NegarAcesso : OnboardingPermissoesEvent

    data object ContinuarSemAcesso : OnboardingPermissoesEvent

    data object ContinuarComAcesso : OnboardingPermissoesEvent

    data object PedirNotificacoes : OnboardingPermissoesEvent

    data object CorrigirNotificacoes : OnboardingPermissoesEvent

    data object PularNotificacoes : OnboardingPermissoesEvent

    data class SelecionarFabricante(val fabricante: FabricanteDoSistema) : OnboardingPermissoesEvent

    data object AbrirAjustesDeBateria : OnboardingPermissoesEvent

    data object AbrirInicioAutomatico : OnboardingPermissoesEvent

    data object Concluir : OnboardingPermissoesEvent

    data object Pular : OnboardingPermissoesEvent
}
