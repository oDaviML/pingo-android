package com.pingo.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConexaoDoListener @Inject constructor() {
    private val conectado = MutableStateFlow(false)

    val conexao: StateFlow<Boolean> = conectado.asStateFlow()

    fun marcarConectado() {
        conectado.value = true
    }

    fun marcarDesconectado() {
        conectado.value = false
    }
}
