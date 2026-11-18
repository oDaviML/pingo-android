package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservadorDePendencias @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val notificacoes: RevisaoNotificacoes,
    private val preferences: UserPreferencesDataSource,
    private val dispatchers: AppDispatchers,
) : IniciadorDeObservacaoDePendencias {
    constructor(
        lancamentoRepository: LancamentoRepository,
        categoriaRepository: CategoriaRepository,
        notificacoes: RevisaoNotificacoes,
        dispatchers: AppDispatchers,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        categoriaRepository = categoriaRepository,
        notificacoes = notificacoes,
        preferences = PreferenciasPadrao,
        dispatchers = dispatchers,
    )

    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val notificadas = mutableSetOf<String>()
    private var iniciado = false

    override fun iniciar() {
        if (iniciado) {
            return
        }

        iniciado = true
        escopo.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                categoriaRepository.observeCategorias(),
                preferences.revisarPelaNotificacaoAtivo,
            ) { lancamentos, categorias, ativo -> Triple(lancamentos, categorias, ativo) }
                .collect { (lancamentos, categorias, ativo) ->
                    runCatching { sincronizar(lancamentos, categorias, ativo) }
                }
        }
    }

    internal suspend fun sincronizar(lancamentos: List<Lancamento>, categorias: List<Categoria>, ativo: Boolean = true) {
        if (!ativo) {
            notificadas.clear()
            notificacoes.cancelarAusentes(emptySet())
            return
        }

        val pendentes = lancamentos.filter { it.status == StatusLancamento.PENDENTE && it.excluidoEm == null }
        val pendentesIds = pendentes.map { it.publicId }.toSet()
        val nomesDasCategorias = categorias.associate { it.publicId to it.nome }
        val novas = pendentes.filter { it.publicId !in notificadas }

        if (novas.isNotEmpty() && notificacoes.habilitada()) {
            novas.forEach { lancamento ->
                if (notificacoes.postar(lancamento.paraNotificar(nomesDasCategorias), precisaCorrigir = false)) {
                    notificadas += lancamento.publicId
                }
            }
        }

        notificadas.filterNot { it in pendentesIds }.toList().forEach { publicId ->
            notificacoes.cancelar(publicId)
            notificadas -= publicId
        }

        notificacoes.cancelarAusentes(pendentesIds)
    }

    private fun Lancamento.paraNotificar(nomesDasCategorias: Map<String, String>): PendenciaParaNotificar = PendenciaParaNotificar(
        publicId = publicId,
        estabelecimento = estabelecimento,
        valorCentavos = valorCentavos,
        categoria = categoriaPublicId?.let { nomesDasCategorias[it] },
    )

    private object PreferenciasPadrao : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = MutableStateFlow(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = ""

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
