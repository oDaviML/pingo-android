package com.pingo.core.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Assinatura
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.JanelaDeAvisoDeAssinatura
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal data class TrabalhoDeAvisoAgendado(val id: UUID, val tags: Set<String>)

internal interface FilaDeAvisosWorkManager {
    suspend fun trabalhosAtivos(): List<TrabalhoDeAvisoAgendado>

    fun agendarUnico(nomeUnico: String, tags: Set<String>, atrasoInicial: Duration, dados: Data)

    fun cancelarPorNomeUnico(nomeUnico: String)

    fun cancelarPorTag(tag: String)
}

internal class WorkManagerFilaDeAvisos(private val context: Context) : FilaDeAvisosWorkManager {
    private fun workManager(): WorkManager = WorkManager.getInstance(context)

    override suspend fun trabalhosAtivos(): List<TrabalhoDeAvisoAgendado> = workManager()
        .getWorkInfosByTag(WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL)
        .get()
        .filter { !it.state.isFinished }
        .map { TrabalhoDeAvisoAgendado(id = it.id, tags = it.tags) }

    override fun agendarUnico(nomeUnico: String, tags: Set<String>, atrasoInicial: Duration, dados: Data) {
        val construtor = OneTimeWorkRequestBuilder<AvisoDeAssinaturaWorker>()
            .setInitialDelay(atrasoInicial.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(dados)
        tags.forEach { construtor.addTag(it) }
        workManager()
            .enqueueUniqueWork(nomeUnico, ExistingWorkPolicy.REPLACE, construtor.build())
            .result
            .get()
    }

    override fun cancelarPorNomeUnico(nomeUnico: String) {
        workManager().cancelUniqueWork(nomeUnico).result.get()
    }

    override fun cancelarPorTag(tag: String) {
        workManager().cancelAllWorkByTag(tag).result.get()
    }
}

@Singleton
class WorkManagerAgendadorDeAvisosDeAssinatura internal constructor(
    private val fila: FilaDeAvisosWorkManager,
    private val preferences: UserPreferencesDataSource,
    private val assinaturaRepository: Provider<AssinaturaRepository>,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : AgendadorDeAvisosDeAssinatura {
    internal constructor(
        fila: FilaDeAvisosWorkManager,
        preferences: UserPreferencesDataSource,
        assinaturaRepository: AssinaturaRepository,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        fila = fila,
        preferences = preferences,
        assinaturaRepository = Provider { assinaturaRepository },
        clock = clock,
        dispatchers = dispatchers,
    )

    @Inject
    constructor(
        @ApplicationContext context: Context,
        preferences: UserPreferencesDataSource,
        assinaturaRepository: Provider<AssinaturaRepository>,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        fila = WorkManagerFilaDeAvisos(context),
        preferences = preferences,
        assinaturaRepository = assinaturaRepository,
        clock = clock,
        dispatchers = dispatchers,
    )

    constructor(
        context: Context,
        preferences: UserPreferencesDataSource,
        assinaturaRepository: AssinaturaRepository,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        fila = WorkManagerFilaDeAvisos(context),
        preferences = preferences,
        assinaturaRepository = Provider { assinaturaRepository },
        clock = clock,
        dispatchers = dispatchers,
    )

    private val mutex = Mutex()

    override suspend fun reconciliarTodas() {
        mutex.withLock {
            withContext(dispatchers.io) {
                if (!preferences.avisoAssinaturasAtivo.first()) {
                    fila.cancelarPorTag(TAG_GERAL)
                    return@withContext
                }

                val agora = clock.instant()
                val desejados = assinaturasElegiveis(agora)
                cancelarTrabalhosObsoletos(desejados.map { it.nomeUnico }.toSet())
                agendarDesejados(desejados, agora)
            }
        }
    }

    override suspend fun cancelarParaAssinatura(assinaturaPublicId: String) {
        mutex.withLock {
            withContext(dispatchers.io) {
                fila.cancelarPorTag(tagDaAssinatura(assinaturaPublicId))
            }
        }
    }

    override suspend fun cancelarTodos() {
        mutex.withLock {
            withContext(dispatchers.io) {
                fila.cancelarPorTag(TAG_GERAL)
            }
        }
    }

    private suspend fun assinaturasElegiveis(agora: Instant): List<AvisoDesejado> = assinaturaRepository
        .get()
        .observeConfirmadas()
        .first()
        .asSequence()
        .filter { it.excluidoEm == null && it.status == StatusAssinatura.CONFIRMADA }
        .mapNotNull { assinatura -> criarAvisoDesejado(assinatura, agora) }
        .toList()

    private fun criarAvisoDesejado(assinatura: Assinatura, agora: Instant): AvisoDesejado? {
        val proximaData = assinatura.proximaCobrancaEstimada ?: return null
        val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(proximaData)
        if (!JanelaDeAvisoDeAssinatura.deveAgendar(agora, alvo)) {
            return null
        }
        return AvisoDesejado(
            assinatura = assinatura,
            proximaData = proximaData,
            alvo = alvo,
            nomeUnico = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico(assinatura.publicId, proximaData),
        )
    }

    private suspend fun cancelarTrabalhosObsoletos(nomesDesejados: Set<String>) {
        fila.trabalhosAtivos()
            .asSequence()
            .mapNotNull { trabalho -> extrairNomeUnico(trabalho.tags) }
            .filter { nomeUnico -> nomeUnico !in nomesDesejados }
            .distinct()
            .forEach { nomeObsoleto -> fila.cancelarPorNomeUnico(nomeObsoleto) }
    }

    private fun agendarDesejados(desejados: List<AvisoDesejado>, agora: Instant) {
        for (item in desejados) {
            val atraso = Duration.ofMillis(JanelaDeAvisoDeAssinatura.calcularAtrasoInicialMilis(agora, item.alvo))
            val dados = AvisoDeAssinaturaWorker.dadosDeEntrada(
                assinaturaPublicId = item.assinatura.publicId,
                proximaCobrancaEstimada = item.proximaData,
                alvo = item.alvo,
            )
            val tags = setOf(
                TAG_GERAL,
                tagDaAssinatura(item.assinatura.publicId),
                tagDoTrabalhoUnico(item.nomeUnico),
            )
            fila.agendarUnico(
                nomeUnico = item.nomeUnico,
                tags = tags,
                atrasoInicial = atraso,
                dados = dados,
            )
        }
    }

    private fun extrairNomeUnico(tags: Set<String>): String? = tags
        .firstOrNull { it.startsWith(PREFIXO_TAG_UNICO) }
        ?.removePrefix(PREFIXO_TAG_UNICO)

    private data class AvisoDesejado(val assinatura: Assinatura, val proximaData: LocalDate, val alvo: Instant, val nomeUnico: String)

    companion object {
        const val TAG_GERAL = "pingo_avisos_assinaturas"
        private const val PREFIXO_TAG_ASSINATURA = "aviso-assinatura-"
        private const val PREFIXO_TAG_UNICO = "aviso-unico-"

        fun tagDaAssinatura(assinaturaPublicId: String): String = "$PREFIXO_TAG_ASSINATURA$assinaturaPublicId"

        fun tagDoTrabalhoUnico(nomeUnico: String): String = "$PREFIXO_TAG_UNICO$nomeUnico"
    }
}
