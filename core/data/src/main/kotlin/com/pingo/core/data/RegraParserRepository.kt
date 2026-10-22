package com.pingo.core.data

import com.pingo.core.model.RegraParser
import kotlinx.coroutines.flow.Flow

interface RegraParserRepository {
    fun observeRegrasAtivas(): Flow<List<RegraParser>>

    suspend fun buscar(publicId: String): RegraParser?

    suspend fun salvar(regra: RegraParser)

    suspend fun salvarTodas(regras: List<RegraParser>)
}
