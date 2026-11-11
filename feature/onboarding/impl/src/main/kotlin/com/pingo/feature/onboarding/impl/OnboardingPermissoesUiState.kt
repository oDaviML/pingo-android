package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable

enum class PassoDePermissoes(val numero: Int) {
    ACESSO_AS_NOTIFICACOES(4),
    NOTIFICACOES_DO_PINGO(5),
    BATERIA(6),
}

@Immutable
data class OnboardingPermissoesUiState(
    val passo: PassoDePermissoes = PassoDePermissoes.ACESSO_AS_NOTIFICACOES,
    val totalPassos: Int = 8,
    val acessoConcedido: Boolean = false,
    val modoManual: Boolean = false,
    val solicitouAjustes: Boolean = false,
    val notificacoesHabilitadas: Boolean = false,
    val fabricanteDetectado: FabricanteDoSistema = FabricanteDoSistema.OUTRO,
    val fabricanteSelecionado: FabricanteDoSistema = FabricanteDoSistema.OUTRO,
    val semRestricaoDeBateria: Boolean = false,
    val concluido: Boolean = false,
) {
    val instrucoes: InstrucoesDeBateria
        get() = InstrucoesPorFabricante.de(fabricanteSelecionado)
}
