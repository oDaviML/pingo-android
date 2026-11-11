package com.pingo.core.datastore

import kotlinx.coroutines.flow.Flow

enum class PermissaoDoPingo {
    NAO_PERGUNTADA,
    PERMITIDA,
    NEGADA,
}

interface PermissoesDataSource {
    val acessoAsNotificacoes: Flow<PermissaoDoPingo>

    val notificacoesDoPingo: Flow<PermissaoDoPingo>

    suspend fun registrarAcessoAsNotificacoes(permissao: PermissaoDoPingo)

    suspend fun registrarNotificacoesDoPingo(permissao: PermissaoDoPingo)

    suspend fun clear()
}
