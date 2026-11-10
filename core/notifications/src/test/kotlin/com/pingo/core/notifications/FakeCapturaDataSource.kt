package com.pingo.core.notifications

import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.YearMonth

internal class FakeCapturaDataSource : CapturaDataSource {
    private val estado = MutableStateFlow(EstadoListenerPersistido(EstadoListener.PARADO))
    private val leituras = MutableStateFlow<Map<InstituicaoFinanceira, Instant>>(emptyMap())
    private val contadores = MutableStateFlow<Map<Pair<YearMonth, ContadorCaptura>, Int>>(emptyMap())

    override val estadoListener: Flow<EstadoListenerPersistido> = estado.asStateFlow()

    override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = leituras.asStateFlow()

    fun ultimaLeitura(instituicao: InstituicaoFinanceira): Instant? = leituras.value[instituicao]

    fun valorDoContador(mes: YearMonth, contador: ContadorCaptura): Int = contadores.value[mes to contador] ?: 0

    override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> = flowOf(
        ContadoresDoMes(
            mes = mes,
            reconhecidas = valorDoContador(mes, ContadorCaptura.RECONHECIDAS),
            pendentes = valorDoContador(mes, ContadorCaptura.PENDENTES),
            ignoradas = valorDoContador(mes, ContadorCaptura.IGNORADAS),
            naoEntendidas = valorDoContador(mes, ContadorCaptura.NAO_ENTENDIDAS),
        ),
    )

    override suspend fun registrarAtivo(em: Instant) {
        estado.value = EstadoListenerPersistido(estado = EstadoListener.ATIVO, motivo = null, atualizadoEm = em)
    }

    override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
        estado.value = EstadoListenerPersistido(estado = EstadoListener.PARADO, motivo = motivo, atualizadoEm = em)
    }

    override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) {
        leituras.value = leituras.value + (instituicao to em)
    }

    override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) {
        contadores.value = contadores.value + ((mes to contador) to (valorDoContador(mes, contador) + 1))
    }

    override suspend fun clear() {
        estado.value = EstadoListenerPersistido(EstadoListener.PARADO)
        leituras.value = emptyMap()
        contadores.value = emptyMap()
    }
}
