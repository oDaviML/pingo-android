package com.pingo.core.data

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

interface InstrumentoRepository {
    fun observeInstrumentos(): Flow<List<Instrumento>>

    fun observeInstrumentosAtivos(): Flow<List<Instrumento>> = observeInstrumentos().map { instrumentos ->
        instrumentos.filter { instrumento -> instrumento.excluidoEm == null && !instrumento.arquivado }
    }

    fun observePorConta(contaPublicId: String): Flow<List<Instrumento>>

    suspend fun buscar(publicId: String): Instrumento?

    suspend fun buscarAtivo(publicId: String): Instrumento? = observeInstrumentosAtivos().first().firstOrNull { it.publicId == publicId }

    suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento>

    suspend fun salvar(instrumento: Instrumento)

    suspend fun salvarTodos(instrumentos: List<Instrumento>)

    suspend fun excluir(publicId: String, quando: Instant)

    suspend fun arquivar(publicId: String, quando: Instant)

    suspend fun reativar(publicId: String, quando: Instant)
}
