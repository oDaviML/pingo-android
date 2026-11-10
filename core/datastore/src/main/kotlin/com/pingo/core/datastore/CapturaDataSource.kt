package com.pingo.core.datastore

import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.YearMonth

enum class EstadoListener {
    ATIVO,
    PARADO,
}

enum class MotivoParada {
    PERMISSAO_REVOGADA,
    DESCONECTADO,
}

data class EstadoListenerPersistido(val estado: EstadoListener, val motivo: MotivoParada? = null, val atualizadoEm: Instant? = null)

enum class ContadorCaptura {
    RECONHECIDAS,
    PENDENTES,
    IGNORADAS,
    NAO_ENTENDIDAS,
}

data class ContadoresDoMes(
    val mes: YearMonth,
    val reconhecidas: Int = 0,
    val pendentes: Int = 0,
    val ignoradas: Int = 0,
    val naoEntendidas: Int = 0,
)

interface CapturaDataSource {
    val estadoListener: Flow<EstadoListenerPersistido>

    val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>>

    fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes>

    suspend fun registrarAtivo(em: Instant)

    suspend fun registrarParado(motivo: MotivoParada, em: Instant)

    suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant)

    suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura)

    suspend fun clear()
}
