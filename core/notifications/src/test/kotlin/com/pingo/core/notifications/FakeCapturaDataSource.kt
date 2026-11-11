package com.pingo.core.notifications

import com.pingo.core.datastore.AlertaDeCaptura
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
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
    private val estadoRegras = MutableStateFlow(EstadoAtualizacaoRegras())
    private val observando = MutableStateFlow<Instant?>(null)
    private val alerta = MutableStateFlow<AlertaDeCaptura?>(null)

    override val estadoListener: Flow<EstadoListenerPersistido> = estado.asStateFlow()

    override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = leituras.asStateFlow()

    override val observandoDesde: Flow<Instant?> = observando.asStateFlow()

    override val estadoAtualizacaoRegras: Flow<EstadoAtualizacaoRegras> = estadoRegras.asStateFlow()

    override val alertaDeCaptura: Flow<AlertaDeCaptura?> = alerta.asStateFlow()

    fun ultimaLeitura(instituicao: InstituicaoFinanceira): Instant? = leituras.value[instituicao]

    fun estadoDasRegras(): EstadoAtualizacaoRegras = estadoRegras.value

    fun valorDoContador(mes: YearMonth, contador: ContadorCaptura): Int = contadores.value[mes to contador] ?: 0

    fun observarDesde(em: Instant?) {
        observando.value = em
    }

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
        if (estado.value.estado != EstadoListener.ATIVO || observando.value == null) {
            observando.value = em
        }
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

    override suspend fun registrarRegrasAtualizadas(em: Instant) {
        estadoRegras.value = EstadoAtualizacaoRegras(atualizadoEm = em)
    }

    override suspend fun registrarFalhaAoAtualizarRegras(motivo: String, em: Instant) {
        estadoRegras.value = estadoRegras.value.copy(falhaEm = em, motivoFalha = motivo)
    }

    override suspend fun registrarAlertaDeCaptura(referencia: Instant, em: Instant) {
        alerta.value = AlertaDeCaptura(referencia = referencia, enviadoEm = em)
    }

    override suspend fun clear() {
        estado.value = EstadoListenerPersistido(EstadoListener.PARADO)
        leituras.value = emptyMap()
        contadores.value = emptyMap()
        estadoRegras.value = EstadoAtualizacaoRegras()
        observando.value = null
        alerta.value = null
    }
}
