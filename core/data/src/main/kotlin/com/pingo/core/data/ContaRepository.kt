package com.pingo.core.data

import com.pingo.core.model.Conta
import com.pingo.core.model.NovaConta
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ContaRepository {
    fun observeContas(): Flow<List<Conta>>

    suspend fun buscar(publicId: String): Conta?

    suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant)

    suspend fun salvar(conta: Conta)

    suspend fun salvarTodas(contas: List<Conta>)

    suspend fun excluir(publicId: String, quando: Instant)
}
