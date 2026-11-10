package com.pingo.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstadoDoServicoDeCaptura @Inject constructor() {
    private val executando = MutableStateFlow(false)

    val emExecucao: StateFlow<Boolean> = executando.asStateFlow()

    fun atualizar(emExecucao: Boolean) {
        executando.value = emExecucao
    }
}
