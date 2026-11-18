package com.pingo.core.notifications

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Assinatura
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.assinatura.JanelaDeAvisoDeAssinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExecutorDeAvisoDeAssinaturaTest {
    private val dataPrevista = LocalDate.of(2026, 11, 20)
    private val alvo = JanelaDeAvisoDeAssinatura.calcularAlvo(dataPrevista)
    private val assinaturaBase = Assinatura(
        publicId = "sub-1",
        estabelecimento = "Spotify Duo",
        estabelecimentoNormalizado = "spotify duo",
        valorReferenciaCentavos = 2790,
        ultimaCobranca = LocalDate.of(2026, 10, 20),
        proximaCobrancaEstimada = dataPrevista,
        status = StatusAssinatura.CONFIRMADA,
        criadoEm = Instant.parse("2026-10-20T12:00:00Z"),
        atualizadoEm = Instant.parse("2026-10-20T12:00:00Z"),
    )

    @Test
    fun `posta exatamente as nove horas do mesmo dia alvo`() = runTest {
        val notificador = FakeNotificadorDeAssinatura(podeNotificar = true)
        val executor = criarExecutor(
            avisoAtivo = true,
            assinatura = assinaturaBase,
            notificador = notificador,
            instante = Instant.parse("2026-11-18T12:00:00Z"),
        )

        val postado = executor.executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli())

        assertTrue(postado)
        assertEquals(1, notificador.postagens.size)
        val conteudo = NotificacoesDeAssinatura.conteudo(notificador.postagens.single().first, notificador.postagens.single().second)
        assertTrue(conteudo.texto.contains("Spotify Duo"))
        assertTrue(conteudo.texto.contains("R$ 27,90"))
        assertTrue(conteudo.texto.contains("20/11/2026"))
    }

    @Test
    fun `posta ate vinte e tres e cinquenta e nove do mesmo dia em sao paulo`() = runTest {
        val notificador = FakeNotificadorDeAssinatura(podeNotificar = true)
        val executor = criarExecutor(
            avisoAtivo = true,
            assinatura = assinaturaBase,
            notificador = notificador,
            instante = Instant.parse("2026-11-19T02:59:59Z"),
        )

        val postado = executor.executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli())

        assertTrue(postado)
        assertEquals(1, notificador.postagens.size)
    }

    @Test
    fun `bloqueia antes das nove horas e bloqueia no dia seguinte`() = runTest {
        val antes = FakeNotificadorDeAssinatura(podeNotificar = true)
        val executorAntes = criarExecutor(
            avisoAtivo = true,
            assinatura = assinaturaBase,
            notificador = antes,
            instante = Instant.parse("2026-11-18T11:59:59Z"),
        )
        assertFalse(executorAntes.executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli()))
        assertTrue(antes.postagens.isEmpty())

        val diaSeguinte = FakeNotificadorDeAssinatura(podeNotificar = true)
        val executorAtrasado = criarExecutor(
            avisoAtivo = true,
            assinatura = assinaturaBase,
            notificador = diaSeguinte,
            instante = Instant.parse("2026-11-19T03:00:00Z"),
        )
        assertFalse(executorAtrasado.executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli()))
        assertTrue(diaSeguinte.postagens.isEmpty())
    }

    @Test
    fun `bloqueia quando toggle desligado ou permissao negada`() = runTest {
        val desligado = FakeNotificadorDeAssinatura(podeNotificar = true)
        assertFalse(
            criarExecutor(
                avisoAtivo = false,
                assinatura = assinaturaBase,
                notificador = desligado,
                instante = alvo,
            ).executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli()),
        )
        assertTrue(desligado.postagens.isEmpty())

        val semPermissao = FakeNotificadorDeAssinatura(podeNotificar = false)
        assertFalse(
            criarExecutor(
                avisoAtivo = true,
                assinatura = assinaturaBase,
                notificador = semPermissao,
                instante = alvo,
            ).executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli()),
        )
        assertTrue(semPermissao.postagens.isEmpty())
    }

    @Test
    fun `bloqueia quando assinatura foi cancelada excluida ou teve data alterada`() = runTest {
        val cancelada = assinaturaBase.copy(status = StatusAssinatura.CANCELADA)
        val excluida = assinaturaBase.copy(excluidoEm = alvo)
        val reagendada = assinaturaBase.copy(proximaCobrancaEstimada = LocalDate.of(2026, 12, 20))

        listOf(cancelada, excluida, reagendada, null).forEach { candidato ->
            val notificador = FakeNotificadorDeAssinatura(podeNotificar = true)
            val resultado = criarExecutor(
                avisoAtivo = true,
                assinatura = candidato,
                notificador = notificador,
                instante = alvo,
            ).executar("sub-1", dataPrevista.toString(), alvo.toEpochMilli())

            assertFalse(resultado)
            assertTrue(notificador.postagens.isEmpty())
        }
    }

    private fun criarExecutor(
        avisoAtivo: Boolean,
        assinatura: Assinatura?,
        notificador: FakeNotificadorDeAssinatura,
        instante: Instant,
    ): ExecutorDeAvisoDeAssinatura = ExecutorDeAvisoDeAssinatura(
        preferences = FakePreferences(avisoAtivo),
        assinaturaRepository = FakeRepo(assinatura),
        notificadorDeAssinatura = notificador,
        clock = Clock.fixed(instante, ZoneOffset.UTC),
    )

    private class FakeNotificadorDeAssinatura(private val podeNotificar: Boolean) : NotificadorDeAssinatura {
        val postagens = mutableListOf<Pair<Assinatura, LocalDate>>()

        override fun podeNotificar(): Boolean = podeNotificar

        override fun postar(assinatura: Assinatura, dataPrevista: LocalDate): Boolean {
            postagens += assinatura to dataPrevista
            return true
        }
    }

    private class FakePreferences(ativo: Boolean) : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = MutableStateFlow(ativo)

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

    private class FakeRepo(private val assinatura: Assinatura?) : AssinaturaRepository {
        override fun observeAssinaturas(): Flow<List<Assinatura>> = flowOf(listOfNotNull(assinatura))

        override suspend fun buscar(publicId: String): Assinatura? = assinatura?.takeIf { it.publicId == publicId }

        override suspend fun salvar(assinatura: Assinatura) = Unit

        override suspend fun salvarTodas(assinaturas: List<Assinatura>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
    }
}
