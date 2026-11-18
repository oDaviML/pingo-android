package com.pingo.core.data.assinatura

import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendadorDeAvisosOpcional @Inject constructor(private val opcional: Optional<AgendadorDeAvisosDeAssinatura>) {
    constructor() : this(Optional.empty())

    suspend fun reconciliarTodas() {
        if (opcional.isPresent) {
            opcional.get().reconciliarTodas()
        }
    }

    suspend fun cancelarParaAssinatura(assinaturaPublicId: String) {
        if (opcional.isPresent) {
            opcional.get().cancelarParaAssinatura(assinaturaPublicId)
        }
    }

    suspend fun cancelarTodos() {
        if (opcional.isPresent) {
            opcional.get().cancelarTodos()
        }
    }
}
