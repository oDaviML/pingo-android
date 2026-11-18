package com.pingo

import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Suppress("LongParameterList")
class InicializacaoDoApp internal constructor(
    private val observador: Provider<IniciadorDeObservacaoDePendencias>,
    private val agendadorDeSync: Provider<AgendadorDeSync>,
    private val aoAutenticar: (suspend () -> Unit)?,
    private val aoDeslogar: (suspend () -> Unit)?,
    private val authRepository: Provider<AuthRepository>?,
    private val preferences: Provider<UserPreferencesDataSource>?,
    private val analytics: Provider<AnalyticsFacade>?,
    private val dispatchers: AppDispatchers,
) {
    @Inject
    constructor(
        observador: Provider<IniciadorDeObservacaoDePendencias>,
        agendadorDeSync: Provider<AgendadorDeSync>,
        materializarRecargasVencidas: Provider<MaterializarRecargasVencidasUseCase>,
        agendadorDeAvisos: Provider<AgendadorDeAvisosOpcional>,
        authRepository: Provider<AuthRepository>,
        preferences: Provider<UserPreferencesDataSource>,
        analytics: Provider<AnalyticsFacade>,
        dispatchers: AppDispatchers,
    ) : this(
        observador = observador,
        agendadorDeSync = agendadorDeSync,
        aoAutenticar = {
            executarComSeguranca { materializarRecargasVencidas.get().invoke() }
            executarComSeguranca { agendadorDeAvisos.get().reconciliarTodas() }
        },
        aoDeslogar = {
            executarComSeguranca { agendadorDeAvisos.get().cancelarTodos() }
        },
        authRepository = authRepository,
        preferences = preferences,
        analytics = analytics,
        dispatchers = dispatchers,
    )

    constructor(
        observador: Provider<IniciadorDeObservacaoDePendencias>,
        agendadorDeSync: AgendadorDeSync,
        dispatchers: AppDispatchers,
    ) : this(
        observador = observador,
        agendadorDeSync = Provider { agendadorDeSync },
        aoAutenticar = null,
        aoDeslogar = null,
        authRepository = null,
        preferences = null,
        analytics = null,
        dispatchers = dispatchers,
    )

    internal constructor(
        observador: Provider<IniciadorDeObservacaoDePendencias>,
        agendadorDeSync: AgendadorDeSync,
        aoAutenticar: (suspend () -> Unit)?,
        aoDeslogar: (suspend () -> Unit)?,
        authRepository: Provider<AuthRepository>?,
        dispatchers: AppDispatchers,
    ) : this(
        observador = observador,
        agendadorDeSync = Provider { agendadorDeSync },
        aoAutenticar = aoAutenticar,
        aoDeslogar = aoDeslogar,
        authRepository = authRepository,
        preferences = null,
        analytics = null,
        dispatchers = dispatchers,
    )

    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val iniciado = AtomicBoolean(false)

    fun iniciar() {
        if (!iniciado.compareAndSet(false, true)) {
            return
        }

        escopo.launch {
            observador.get().iniciar()
            agendadorDeSync.get().agendar()
            launch { observarTelemetria() }
            observarSessao()
        }
    }

    private suspend fun observarTelemetria() {
        val prefs = preferences ?: return
        val facade = analytics ?: return
        prefs.get().telemetriaOptIn.collect { ativo ->
            facade.get().setAnalyticsEnabled(ativo)
        }
    }

    private suspend fun observarSessao() {
        val repositorio = authRepository ?: return
        repositorio.get().session.collect { sessao ->
            when (sessao) {
                SessionState.LoggedIn -> aoAutenticar?.invoke()
                SessionState.LoggedOut -> aoDeslogar?.invoke()
                SessionState.Loading -> Unit
            }
        }
    }

    private companion object {
        suspend fun executarComSeguranca(bloco: suspend () -> Unit) {
            runCatching { bloco() }.onFailure { erro ->
                if (erro is CancellationException) {
                    throw erro
                }
            }
        }
    }
}
