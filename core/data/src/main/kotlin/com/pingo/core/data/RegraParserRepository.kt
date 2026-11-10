package com.pingo.core.data

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import kotlinx.coroutines.flow.Flow

interface RegraParserRepository {
    fun observeRegrasAtivas(): Flow<List<RegraParser>>

    fun observeVersaoVigente(): Flow<Int?>

    suspend fun versoesEmCache(): Map<InstituicaoFinanceira, Int>

    suspend fun buscar(publicId: String): RegraParser?

    suspend fun salvar(regra: RegraParser)

    suspend fun salvarTodas(regras: List<RegraParser>)

    suspend fun substituirVigentes(regras: List<RegraParser>)
}
