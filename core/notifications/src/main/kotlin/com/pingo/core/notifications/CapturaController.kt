package com.pingo.core.notifications

import com.pingo.core.datastore.EstadoListenerPersistido

interface CapturaController {
    fun iniciarServico()

    fun pararServico()

    fun registrarConectado()

    fun registrarDesconectado()

    suspend fun reconectar(): EstadoListenerPersistido

    suspend fun reconciliar(): EstadoListenerPersistido
}
