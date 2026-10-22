package com.pingo.core.data

import com.pingo.core.model.RegraCategoria
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface RegraCategoriaRepository {
    fun observeRegras(): Flow<List<RegraCategoria>>

    suspend fun buscar(publicId: String): RegraCategoria?

    suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoria?

    suspend fun salvar(regra: RegraCategoria)

    suspend fun salvarTodas(regras: List<RegraCategoria>)

    suspend fun excluir(publicId: String, quando: Instant)
}
