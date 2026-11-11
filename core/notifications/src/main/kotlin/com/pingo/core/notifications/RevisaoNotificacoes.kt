package com.pingo.core.notifications

interface RevisaoNotificacoes {
    suspend fun habilitada(): Boolean

    fun postar(pendencia: PendenciaParaNotificar, precisaCorrigir: Boolean): Boolean

    fun cancelar(publicId: String)

    fun cancelarAusentes(publicIds: Set<String>)
}
