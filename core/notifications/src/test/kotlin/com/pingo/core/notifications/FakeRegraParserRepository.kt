package com.pingo.core.notifications

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.model.RegraParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class FakeRegraParserRepository(private val regras: List<RegraParser> = RegrasDeTeste.todas()) : RegraParserRepository {
    var coletas: Int = 0
        private set

    override fun observeRegrasAtivas(): Flow<List<RegraParser>> = flow {
        coletas += 1
        emit(regras)
    }

    override suspend fun buscar(publicId: String): RegraParser? = regras.firstOrNull { it.publicId == publicId }

    override suspend fun salvar(regra: RegraParser) = Unit

    override suspend fun salvarTodas(regras: List<RegraParser>) = Unit
}
