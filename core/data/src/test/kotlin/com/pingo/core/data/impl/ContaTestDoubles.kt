package com.pingo.core.data.impl

import com.pingo.core.data.TransacaoLocal
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal val INSTANTE_DA_CRIACAO: Instant = Instant.parse("2026-11-10T12:00:00Z")

internal val INSTANTE_DA_EDICAO: Instant = Instant.parse("2026-11-10T12:01:00Z")

internal class FakeTransacaoLocal : TransacaoLocal {
    var execucoes = 0
        private set

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++

        return bloco()
    }
}

internal class FakeContaDao : ContaDao {
    private val tabela = MutableStateFlow<List<ContaEntity>>(emptyList())

    fun gravadas(): List<ContaEntity> = tabela.value

    override fun observeAtivas(): Flow<List<ContaEntity>> =
        tabela.map { contas -> contas.filter { it.excluidoEm == null }.sortedBy { it.apelido } }

    override suspend fun buscar(publicId: String): ContaEntity? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun atualizadasDesde(desde: Instant): List<ContaEntity> = tabela.value.filter { it.atualizadoEm >= desde }

    override suspend fun upsert(conta: ContaEntity) {
        upsertAll(listOf(conta))
    }

    override suspend fun upsertAll(contas: List<ContaEntity>) {
        tabela.value = tabela.value.filter { atual -> contas.none { it.publicId == atual.publicId } } + contas
    }

    override suspend fun marcarExcluida(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando, atualizadoEm = quando) else it }
    }

    override suspend fun definirArquivamento(publicId: String, arquivada: Boolean, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(arquivada = arquivada, atualizadoEm = quando) else it }
    }
}

internal class FakeInstrumentoDao : InstrumentoDao {
    private val tabela = MutableStateFlow<List<InstrumentoEntity>>(emptyList())

    fun gravados(): List<InstrumentoEntity> = tabela.value

    override fun observeAtivos(): Flow<List<InstrumentoEntity>> =
        tabela.map { instrumentos -> instrumentos.filter { it.excluidoEm == null }.sortedBy { it.apelido } }

    override fun observePorConta(contaPublicId: String): Flow<List<InstrumentoEntity>> =
        observeAtivos().map { instrumentos -> instrumentos.filter { it.contaPublicId == contaPublicId } }

    override suspend fun buscar(publicId: String): InstrumentoEntity? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<InstrumentoEntity> =
        tabela.value.filter { it.excluidoEm == null && !it.arquivado }.sortedBy { it.apelido }

    override suspend fun atualizadosDesde(desde: Instant): List<InstrumentoEntity> = tabela.value.filter { it.atualizadoEm >= desde }

    override suspend fun listarPorConta(contaPublicId: String): List<InstrumentoEntity> =
        tabela.value.filter { it.contaPublicId == contaPublicId && it.excluidoEm == null }.sortedBy { it.apelido }

    override suspend fun upsert(instrumento: InstrumentoEntity) {
        upsertAll(listOf(instrumento))
    }

    override suspend fun upsertAll(instrumentos: List<InstrumentoEntity>) {
        tabela.value = tabela.value.filter { atual -> instrumentos.none { it.publicId == atual.publicId } } + instrumentos
    }

    override suspend fun marcarExcluido(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando, atualizadoEm = quando) else it }
    }

    override suspend fun marcarArquivado(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(arquivado = true, atualizadoEm = quando) else it }
    }

    override suspend fun marcarReativado(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map {
            if (it.publicId == publicId) it.copy(arquivado = false, arquivadoPorCascata = false, atualizadoEm = quando) else it
        }
    }

    override suspend fun arquivarAtivosPorCascata(contaPublicId: String, quando: Instant) {
        tabela.value = tabela.value.map {
            if (it.contaPublicId == contaPublicId && it.excluidoEm == null && !it.arquivado) {
                it.copy(arquivado = true, arquivadoPorCascata = true, atualizadoEm = quando)
            } else {
                it
            }
        }
    }

    override suspend fun reativarArquivadosPorCascata(contaPublicId: String, quando: Instant) {
        tabela.value = tabela.value.map {
            if (it.contaPublicId == contaPublicId && arquivadoPorCascata(it)) {
                it.copy(arquivado = false, arquivadoPorCascata = false, atualizadoEm = quando)
            } else {
                it
            }
        }
    }

    private fun arquivadoPorCascata(instrumento: InstrumentoEntity): Boolean =
        instrumento.excluidoEm == null && instrumento.arquivado && instrumento.arquivadoPorCascata
}
