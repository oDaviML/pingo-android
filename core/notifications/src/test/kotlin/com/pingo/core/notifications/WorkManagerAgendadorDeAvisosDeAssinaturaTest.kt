package com.pingo.core.notifications

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Assinatura
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.JanelaDeAvisoDeAssinatura
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class WorkManagerAgendadorDeAvisosDeAssinaturaTest {
    @Test
    fun `toggle desligado cancela trabalhos e nao agenda novos avisos`() = runTest {
        val fila = FakeFilaDeAvisos()
        val preferences = FakeUserPreferences(avisoAtivo = false)
        val repo = FakeAssinaturaRepo(listOf(assinatura("sub-1", LocalDate.of(2026, 11, 20))))
        val clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC)
        val agendador = criarAgendador(fila, preferences, repo, clock, testScheduler)

        agendador.reconciliarTodas()

        assertEquals(listOf(WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL), fila.tagsCanceladas)
        assertTrue(fila.agendados.isEmpty())
    }

    @Test
    fun `agenda dois dias antes as nove horas de sao paulo sem texto bruto`() = runTest {
        val fila = FakeFilaDeAvisos()
        val preferences = FakeUserPreferences(avisoAtivo = true)
        val proxima = LocalDate.of(2026, 11, 20)
        val repo = FakeAssinaturaRepo(listOf(assinatura("sub-1", proxima)))
        val agora = Instant.parse("2026-11-17T12:00:00Z")
        val clock = Clock.fixed(agora, ZoneOffset.UTC)
        val agendador = criarAgendador(fila, preferences, repo, clock, testScheduler)

        agendador.reconciliarTodas()

        val agendado = fila.agendados.single()
        val alvoEsperado = JanelaDeAvisoDeAssinatura.calcularAlvo(proxima)
        assertEquals(Instant.parse("2026-11-18T12:00:00Z"), alvoEsperado)
        assertEquals(JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", proxima), agendado.nomeUnico)
        assertEquals(Duration.between(agora, alvoEsperado), agendado.atrasoInicial)
        assertTrue(WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL in agendado.tags)
        assertTrue(WorkManagerAgendadorDeAvisosDeAssinatura.tagDaAssinatura("sub-1") in agendado.tags)
        assertEquals("sub-1", agendado.dados.getString(AvisoDeAssinaturaWorker.CHAVE_ASSINATURA_PUBLIC_ID))
        assertEquals("2026-11-20", agendado.dados.getString(AvisoDeAssinaturaWorker.CHAVE_DATA_PREVISTA_ISO))
        assertEquals(alvoEsperado.toEpochMilli(), agendado.dados.getLong(AvisoDeAssinaturaWorker.CHAVE_ALVO_EPOCH_MILLIS, -1L))
        assertFalse(agendado.dados.keyValueMap.keys.any { it.contains("texto", ignoreCase = true) })
    }

    @Test
    fun `nao agenda quando o horario alvo ja passou e cancela obsoletos`() = runTest {
        val proxima = LocalDate.of(2026, 11, 20)
        val nomeUnico = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", proxima)
        val fila = FakeFilaDeAvisos(
            ativosIniciais = listOf(
                TrabalhoDeAvisoAgendado(
                    id = UUID.randomUUID(),
                    tags = setOf(
                        WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL,
                        WorkManagerAgendadorDeAvisosDeAssinatura.tagDoTrabalhoUnico(nomeUnico),
                    ),
                ),
            ),
        )
        val preferences = FakeUserPreferences(avisoAtivo = true)
        val repo = FakeAssinaturaRepo(listOf(assinatura("sub-1", proxima)))
        val depoisDoAlvo = Instant.parse("2026-11-18T12:00:01Z")
        val agendador = criarAgendador(fila, preferences, repo, Clock.fixed(depoisDoAlvo, ZoneOffset.UTC), testScheduler)

        agendador.reconciliarTodas()

        assertTrue(fila.agendados.isEmpty())
        assertEquals(listOf(nomeUnico), fila.nomesCancelados)
    }

    @Test
    fun `reconciliacao cancela trabalho antigo quando proxima cobranca muda ou assinatura e cancelada`() = runTest {
        val dataAntiga = LocalDate.of(2026, 11, 20)
        val dataNova = LocalDate.of(2026, 12, 20)
        val nomeAntigo = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", dataAntiga)
        val nomeRemovida = JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-2", dataAntiga)
        val fila = FakeFilaDeAvisos(
            ativosIniciais = listOf(
                TrabalhoDeAvisoAgendado(
                    id = UUID.randomUUID(),
                    tags = setOf(
                        WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL,
                        WorkManagerAgendadorDeAvisosDeAssinatura.tagDoTrabalhoUnico(nomeAntigo),
                    ),
                ),
                TrabalhoDeAvisoAgendado(
                    id = UUID.randomUUID(),
                    tags = setOf(
                        WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL,
                        WorkManagerAgendadorDeAvisosDeAssinatura.tagDoTrabalhoUnico(nomeRemovida),
                    ),
                ),
            ),
        )
        val preferences = FakeUserPreferences(avisoAtivo = true)
        val repo = FakeAssinaturaRepo(
            listOf(
                assinatura("sub-1", dataNova, StatusAssinatura.CONFIRMADA),
                assinatura("sub-2", dataAntiga, StatusAssinatura.CANCELADA),
                assinatura("sub-3", dataNova, StatusAssinatura.SUGERIDA),
                assinatura("sub-4", dataNova, StatusAssinatura.CONFIRMADA, excluidoEm = Instant.parse("2026-11-10T10:00:00Z")),
            ),
        )
        val agora = Instant.parse("2026-11-17T12:00:00Z")
        val agendador = criarAgendador(fila, preferences, repo, Clock.fixed(agora, ZoneOffset.UTC), testScheduler)

        agendador.reconciliarTodas()

        assertEquals(listOf(nomeAntigo, nomeRemovida), fila.nomesCancelados)
        assertEquals(1, fila.agendados.size)
        assertEquals(JanelaDeAvisoDeAssinatura.nomeTrabalhoUnico("sub-1", dataNova), fila.agendados.single().nomeUnico)
    }

    @Test
    fun `cancelarParaAssinatura e cancelarTodos usam as tags esperadas`() = runTest {
        val fila = FakeFilaDeAvisos()
        val agendador = criarAgendador(
            fila = fila,
            preferences = FakeUserPreferences(true),
            repo = FakeAssinaturaRepo(emptyList()),
            clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC),
            scheduler = testScheduler,
        )

        agendador.cancelarParaAssinatura("sub-99")
        agendador.cancelarTodos()

        assertEquals(
            listOf(
                WorkManagerAgendadorDeAvisosDeAssinatura.tagDaAssinatura("sub-99"),
                WorkManagerAgendadorDeAvisosDeAssinatura.TAG_GERAL,
            ),
            fila.tagsCanceladas,
        )
    }

    private fun criarAgendador(
        fila: FakeFilaDeAvisos,
        preferences: UserPreferencesDataSource,
        repo: AssinaturaRepository,
        clock: Clock,
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ): WorkManagerAgendadorDeAvisosDeAssinatura {
        val dispatcher = StandardTestDispatcher(scheduler)
        return WorkManagerAgendadorDeAvisosDeAssinatura(
            fila = fila,
            preferences = preferences,
            assinaturaRepository = repo,
            clock = clock,
            dispatchers = FakeAppDispatchers(dispatcher),
        )
    }

    private fun assinatura(
        publicId: String,
        proximaCobranca: LocalDate?,
        status: StatusAssinatura = StatusAssinatura.CONFIRMADA,
        excluidoEm: Instant? = null,
    ): Assinatura = Assinatura(
        publicId = publicId,
        estabelecimento = "Netflix",
        estabelecimentoNormalizado = "netflix",
        valorReferenciaCentavos = 5590,
        ultimaCobranca = LocalDate.of(2026, 10, 20),
        proximaCobrancaEstimada = proximaCobranca,
        status = status,
        criadoEm = Instant.parse("2026-10-20T12:00:00Z"),
        atualizadoEm = Instant.parse("2026-10-20T12:00:00Z"),
        excluidoEm = excluidoEm,
    )

    private data class ChamadaAgendamento(
        val nomeUnico: String,
        val tags: Set<String>,
        val atrasoInicial: Duration,
        val dados: androidx.work.Data,
    )

    private class FakeFilaDeAvisos(ativosIniciais: List<TrabalhoDeAvisoAgendado> = emptyList()) : FilaDeAvisosWorkManager {
        private val ativos = ativosIniciais.toMutableList()
        val agendados = mutableListOf<ChamadaAgendamento>()
        val nomesCancelados = mutableListOf<String>()
        val tagsCanceladas = mutableListOf<String>()

        override suspend fun trabalhosAtivos(): List<TrabalhoDeAvisoAgendado> = ativos.toList()

        override fun agendarUnico(nomeUnico: String, tags: Set<String>, atrasoInicial: Duration, dados: androidx.work.Data) {
            agendados += ChamadaAgendamento(nomeUnico, tags, atrasoInicial, dados)
        }

        override fun cancelarPorNomeUnico(nomeUnico: String) {
            nomesCancelados += nomeUnico
        }

        override fun cancelarPorTag(tag: String) {
            tagsCanceladas += tag
        }
    }

    private class FakeUserPreferences(avisoAtivo: Boolean) : UserPreferencesDataSource {
        private val estado = MutableStateFlow(avisoAtivo)
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = estado

        override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
            estado.value = ativo
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev"

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

    private class FakeAssinaturaRepo(private val lista: List<Assinatura>) : AssinaturaRepository {
        override fun observeAssinaturas(): Flow<List<Assinatura>> = flowOf(lista)

        override suspend fun buscar(publicId: String): Assinatura? = lista.firstOrNull { it.publicId == publicId }

        override suspend fun salvar(assinatura: Assinatura) = Unit

        override suspend fun salvarTodas(assinaturas: List<Assinatura>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
    }
}
