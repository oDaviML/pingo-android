package com.pingo.core.notifications

import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ObservadorDePendenciasTest {
    private val agora = Instant.parse("2026-11-11T12:00:00Z")

    @Test
    fun `posta uma notificacao por pendente com dados estruturados`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)
        val alimentacao = categoria("alimentacao", "Alimentação")

        observador.sincronizar(
            listOf(
                pendencia("pendencia-1"),
                pendencia("pendencia-2", estabelecimento = null, categoria = null, valorCentavos = 1990),
                pendencia("confirmada", status = StatusLancamento.CONFIRMADO),
            ),
            listOf(alimentacao),
        )

        assertEquals(2, notificacoes.postagens.size)
        val primeira = notificacoes.postagens.first().pendencia
        assertEquals("pendencia-1", primeira.publicId)
        assertEquals("Padaria do Zé", primeira.estabelecimento)
        assertEquals(4290, primeira.valorCentavos)
        assertEquals("Alimentação", primeira.categoria)
        assertFalse(notificacoes.postagens.first().precisaCorrigir)
        assertNull(notificacoes.postagens.last().pendencia.categoria)
        assertEquals(setOf("pendencia-1", "pendencia-2"), notificacoes.ausentes.last())
    }

    @Test
    fun `nao reposta a mesma pendencia em emissoes repetidas`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)
        val pendente = pendencia("pendencia-1")

        observador.sincronizar(listOf(pendente), emptyList())
        observador.sincronizar(listOf(pendente), emptyList())

        assertEquals(1, notificacoes.postagens.size)
        assertTrue(notificacoes.canceladas.isEmpty())
    }

    @Test
    fun `cancela a notificacao quando a pendencia e resolvida por outra via`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)

        observador.sincronizar(listOf(pendencia("pendencia-1")), emptyList())
        observador.sincronizar(listOf(pendencia("pendencia-1", status = StatusLancamento.CONFIRMADO)), emptyList())

        assertEquals(listOf("pendencia-1"), notificacoes.canceladas)
        assertEquals(emptySet<String>(), notificacoes.ausentes.last())
    }

    @Test
    fun `cancela a notificacao quando a pendencia e descartada`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)

        observador.sincronizar(listOf(pendencia("pendencia-1")), emptyList())
        observador.sincronizar(emptyList(), emptyList())

        assertEquals(listOf("pendencia-1"), notificacoes.canceladas)
        assertEquals(emptySet<String>(), notificacoes.ausentes.last())
    }

    @Test
    fun `sem permissao nao posta e continua sem quebrar`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes(habilitada = false)
        val observador = observador(notificacoes)

        observador.sincronizar(listOf(pendencia("pendencia-1")), emptyList())

        assertTrue(notificacoes.postagens.isEmpty())
        assertEquals(setOf("pendencia-1"), notificacoes.ausentes.last())
    }

    @Test
    fun `limpa notificacoes orfas mesmo sem permissao`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes(habilitada = false)
        val observador = observador(notificacoes)

        observador.sincronizar(emptyList(), emptyList())

        assertEquals(emptySet<String>(), notificacoes.ausentes.last())
    }

    @Test
    fun `volta a postar quando a permissao e concedida depois`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes(habilitada = false)
        val observador = observador(notificacoes)
        val pendente = pendencia("pendencia-1")

        observador.sincronizar(listOf(pendente), emptyList())
        notificacoes.habilitada = true
        observador.sincronizar(listOf(pendente), emptyList())

        assertEquals(1, notificacoes.postagens.size)
    }

    @Test
    fun `reposta quando o sistema recusa a notificacao`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes().apply { aceitaPostagem = false }
        val observador = observador(notificacoes)
        val pendente = pendencia("pendencia-1")

        observador.sincronizar(listOf(pendente), emptyList())
        notificacoes.aceitaPostagem = true
        observador.sincronizar(listOf(pendente), emptyList())

        assertEquals(2, notificacoes.postagens.size)
    }

    @Test
    fun `nunca transporta o texto bruto da notificacao capturada`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)
        val bruto = "Compra aprovada R$ 42,90 PADARIA DO ZE no cartao final 1234"
        val pendente = pendencia("pendencia-1", textoBruto = bruto)

        observador.sincronizar(listOf(pendente), emptyList())

        val postada = notificacoes.postagens.single().pendencia
        assertFalse(postada.toString().contains("PADARIA DO ZE"))
        assertFalse(postada.estabelecimento.orEmpty().contains("Compra aprovada"))
        assertEquals("Padaria do Zé", postada.estabelecimento)
    }

    @Test
    fun `nao posta para lancamento confirmado`() = runTest {
        val notificacoes = FakeRevisaoNotificacoes()
        val observador = observador(notificacoes)

        observador.sincronizar(listOf(pendencia("confirmada", status = StatusLancamento.CONFIRMADO)), emptyList())

        assertTrue(notificacoes.postagens.isEmpty())
    }

    private fun observador(notificacoes: FakeRevisaoNotificacoes): ObservadorDePendencias = ObservadorDePendencias(
        lancamentoRepository = FakeLancamentoRepository(),
        categoriaRepository = FakeCategoriaRepository(),
        notificacoes = notificacoes,
        dispatchers = DefaultAppDispatchers,
    )

    private fun categoria(publicId: String, nome: String): Categoria = Categoria(
        publicId = publicId,
        nome = nome,
        tipo = TipoCategoria.DESPESA,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun pendencia(
        publicId: String,
        status: StatusLancamento = StatusLancamento.PENDENTE,
        estabelecimento: String? = "Padaria do Zé",
        categoria: String? = "alimentacao",
        valorCentavos: Long = 4290,
        textoBruto: String? = null,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "instrumento-1",
        categoriaPublicId = categoria,
        valorCentavos = valorCentavos,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = estabelecimento,
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.ITAU,
        confianca = 0.9,
        motivoPendencia = if (status == StatusLancamento.PENDENTE) MotivoPendencia.BAIXA_CONFIANCA.name else null,
        criadoEm = agora,
        atualizadoEm = agora,
        textoBruto = textoBruto,
    )
}
