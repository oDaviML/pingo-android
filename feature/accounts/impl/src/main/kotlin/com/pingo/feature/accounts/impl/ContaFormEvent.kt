package com.pingo.feature.accounts.impl

import com.pingo.core.ui.contas.EventoDoFormularioDeConta

sealed interface ContaFormEvent {
    data class Formulario(val evento: EventoDoFormularioDeConta) : ContaFormEvent

    data object Salvar : ContaFormEvent

    data object TentarDeNovo : ContaFormEvent
}
