package com.pingo.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ConexaoAtual(val conectado: Boolean, val versao: Long)

@Singleton
class ConexaoDoListener @Inject constructor() {
    private val conectado = MutableStateFlow(false)
    private val trava = Any()
    private var versao = 0L

    val conexao: StateFlow<Boolean> = conectado.asStateFlow()

    fun marcarConectado(): Long = registrar(conectado = true)

    fun marcarDesconectado(): Long = registrar(conectado = false)

    fun instantaneo(): ConexaoAtual = synchronized(trava) { ConexaoAtual(conectado = conectado.value, versao = versao) }

    fun vigente(versao: Long): Boolean = synchronized(trava) { this.versao == versao }

    private fun registrar(conectado: Boolean): Long = synchronized(trava) {
        versao += 1
        this.conectado.value = conectado
        versao
    }
}
