package com.pingo.feature.accounts.impl

import com.pingo.core.ui.contas.EventoDoFormularioDeConta

sealed interface ContaFormEvent {
    data class Formulario(val evento: EventoDoFormularioDeConta) : ContaFormEvent

    data object Salvar : ContaFormEvent

    data object TentarDeNovo : ContaFormEvent

    data object SolicitarArquivamentoDaConta : ContaFormEvent

    data class SolicitarArquivamentoDoInstrumento(val chave: String) : ContaFormEvent

    data object ConfirmarArquivamento : ContaFormEvent

    data object CancelarArquivamento : ContaFormEvent

    data object ReativarConta : ContaFormEvent

    data class ReativarInstrumento(val chave: String) : ContaFormEvent
}
