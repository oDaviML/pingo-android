package com.pingo.core.data

import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.NovaConta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface ContaRepository {
    fun observeContas(): Flow<List<Conta>>

    fun observeContasAtivas(): Flow<List<Conta>> =
        observeContas().map { contas -> contas.filter { conta -> conta.excluidoEm == null && !conta.arquivada } }

    suspend fun buscar(publicId: String): Conta?

    suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant)

    suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo

    suspend fun salvar(conta: Conta)

    suspend fun salvarTodas(contas: List<Conta>)

    suspend fun excluir(publicId: String, quando: Instant)

    suspend fun arquivar(publicId: String, quando: Instant)

    suspend fun reativar(publicId: String, quando: Instant)
}
