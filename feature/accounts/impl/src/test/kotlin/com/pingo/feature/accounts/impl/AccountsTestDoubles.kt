package com.pingo.feature.accounts.impl

import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.NovaConta
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class FakeContaRepository : ContaRepository {
    val contas = MutableStateFlow<List<Conta>>(emptyList())
    val criadas = mutableListOf<List<NovaConta>>()
    val salvas = mutableListOf<ContaEditada>()
    var contaPorId: Conta? = null
    var falhaAoObservar: Throwable? = null
    var falhaAoSalvar: Throwable? = null
    var gateDeSalvamento: CompletableDeferred<Unit>? = null

    override fun observeContas(): Flow<List<Conta>> = flow {
        falhaAoObservar?.let { throw it }
        emitAll(contas)
    }

    override suspend fun buscar(publicId: String): Conta? = contaPorId

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) {
        gateDeSalvamento?.await()
        falhaAoSalvar?.let { throw it }
        criadas.add(contas)
    }

    override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo {
        gateDeSalvamento?.await()
        falhaAoSalvar?.let { throw it }
        salvas.add(edicao)

        return CadastroSalvo(
            conta = Conta(
                publicId = edicao.publicId,
                instituicao = contaPorId?.instituicao ?: InstituicaoFinanceira.NUBANK,
                apelido = edicao.apelido,
                tipo = edicao.tipo,
                criadoEm = contaPorId?.criadoEm ?: agora,
                atualizadoEm = agora,
            ),
            instrumentos = emptyList(),
        )
    }

    override suspend fun salvar(conta: Conta) = Unit

    override suspend fun salvarTodas(contas: List<Conta>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun reativar(publicId: String, quando: Instant) = Unit
}

internal class FakeInstrumentoRepository : InstrumentoRepository {
    val instrumentos = MutableStateFlow<List<Instrumento>>(emptyList())
    var porConta: List<Instrumento> = emptyList()

    override fun observeInstrumentos(): Flow<List<Instrumento>> = instrumentos

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        instrumentos.map { lista -> porConta.ifEmpty { lista.filter { it.contaPublicId == contaPublicId } } }

    override suspend fun buscar(publicId: String): Instrumento? = instrumentos.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> =
        instrumentos.value.filter { !it.arquivado }

    override suspend fun salvar(instrumento: Instrumento) = Unit

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun reativar(publicId: String, quando: Instant) = Unit
}
