package com.pingo.core.auth

import com.pingo.core.common.AppDispatchers
import com.pingo.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class EstadoBloqueio {
    Carregando,
    Desbloqueado,
    Bloqueado,
}

@Singleton
class GerenciadorDeBloqueioDoApp internal constructor(
    preferences: UserPreferencesDataSource,
    authRepository: AuthRepository,
    private val clock: Clock,
    scope: CoroutineScope,
) {
    @Inject
    constructor(
        preferences: UserPreferencesDataSource,
        authRepository: AuthRepository,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        preferences = preferences,
        authRepository = authRepository,
        clock = clock,
        scope = CoroutineScope(SupervisorJob() + dispatchers.default),
    )

    private val lock = Any()
    private val mutableEstado = MutableStateFlow(EstadoBloqueio.Carregando)
    private val mutableBloqueado = MutableStateFlow(false)
    private val mutableEsconderEmRecentes = MutableStateFlow(false)

    private var politicaAtual: PoliticaBloqueio? = null
    private var sessaoAnterior: SessionState? = null
    private var desbloqueadoNaSessao = false
    private var segundoPlanoDesde: Instant? = null
    private var autenticacaoEmAndamento = false

    val estado: StateFlow<EstadoBloqueio> = mutableEstado.asStateFlow()
    val bloqueado: StateFlow<Boolean> = mutableBloqueado.asStateFlow()
    val esconderEmRecentes: StateFlow<Boolean> = mutableEsconderEmRecentes.asStateFlow()

    init {
        scope.launch {
            combine(
                preferences.biometriaAtiva,
                preferences.tempoBloqueioSegundos,
                authRepository.session,
            ) { biometriaAtiva, tempoBloqueioSegundos, sessao ->
                PoliticaBloqueio(
                    biometriaAtiva = biometriaAtiva,
                    tempoBloqueioSegundos = tempoBloqueioSegundos,
                    sessao = sessao,
                )
            }.collect(::atualizarPolitica)
        }
    }

    fun marcarDesbloqueado() {
        synchronized(lock) {
            desbloqueadoNaSessao = true
            segundoPlanoDesde = null
            atualizarEstadoBloqueado(false)
        }
    }

    fun marcarAutenticacaoEmAndamento(emAndamento: Boolean) {
        synchronized(lock) {
            autenticacaoEmAndamento = emAndamento
        }
    }

    fun aoIrParaSegundoPlano() {
        synchronized(lock) {
            if (autenticacaoEmAndamento) {
                return
            }
            val politica = politicaAtual ?: return
            val deveMonitorar =
                politica.biometriaAtiva &&
                    politica.sessao == SessionState.LoggedIn &&
                    desbloqueadoNaSessao
            if (deveMonitorar && segundoPlanoDesde == null) {
                segundoPlanoDesde = clock.instant()
            }
        }
    }

    fun aoVoltarParaPrimeiroPlano() {
        synchronized(lock) {
            val inicio = segundoPlanoDesde
            segundoPlanoDesde = null
            val politica = politicaAtual
            if (politica != null && inicio != null && deveBloquearAoRetornar(politica, inicio)) {
                desbloqueadoNaSessao = false
                atualizarEstadoBloqueado(true)
            }
        }
    }

    private fun deveBloquearAoRetornar(politica: PoliticaBloqueio, inicio: Instant): Boolean {
        if (!politica.biometriaAtiva || politica.sessao != SessionState.LoggedIn || !desbloqueadoNaSessao) {
            return false
        }
        return excedeuTempoLimite(inicio, politica.tempoBloqueioSegundos)
    }

    private fun excedeuTempoLimite(inicio: Instant, tempoLimite: Long): Boolean {
        if (tempoLimite < 0L) {
            return false
        }
        val decorrido = Duration.between(inicio, clock.instant()).seconds
        return tempoLimite == 0L || decorrido >= tempoLimite
    }

    private fun atualizarPolitica(nova: PoliticaBloqueio) {
        synchronized(lock) {
            val anterior = sessaoAnterior
            sessaoAnterior = nova.sessao
            politicaAtual = nova

            if (nova.sessao != SessionState.LoggedIn || !nova.biometriaAtiva) {
                if (nova.sessao == SessionState.LoggedIn && anterior == SessionState.LoggedOut) {
                    desbloqueadoNaSessao = true
                } else {
                    desbloqueadoNaSessao = false
                }
                segundoPlanoDesde = null
                atualizarEstadoBloqueado(false)
            } else {
                if (anterior == SessionState.LoggedOut) {
                    desbloqueadoNaSessao = true
                }
                atualizarEstadoBloqueado(!desbloqueadoNaSessao)
            }
        }
    }

    private fun atualizarEstadoBloqueado(deveBloquear: Boolean) {
        mutableBloqueado.value = deveBloquear
        mutableEstado.value = if (deveBloquear) EstadoBloqueio.Bloqueado else EstadoBloqueio.Desbloqueado
        val politica = politicaAtual
        mutableEsconderEmRecentes.value =
            deveBloquear || (politica != null && politica.biometriaAtiva && politica.sessao == SessionState.LoggedIn)
    }

    private data class PoliticaBloqueio(val biometriaAtiva: Boolean, val tempoBloqueioSegundos: Long, val sessao: SessionState)
}
