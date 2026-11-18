package com.pingo.core.data.assinatura

interface AgendadorDeAvisosDeAssinatura {
    suspend fun reconciliarTodas()

    suspend fun cancelarParaAssinatura(assinaturaPublicId: String)

    suspend fun cancelarTodos()
}
