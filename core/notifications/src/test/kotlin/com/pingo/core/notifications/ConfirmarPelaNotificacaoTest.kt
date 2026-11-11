package com.pingo.core.notifications

import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConfirmarPelaNotificacaoTest {
    private val agora = Instant.parse("2026-11-11T12:00:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

    @Test
    fun `confirmar pendente elegivel confirma e remove a notificacao`() = runTest {
        val lancamentos = FakeLancamentoRepository(listOf(pendencia("pendencia-1")))
        val instrumentos = FakeInstrumentoRepository(listOf(instrumento()))
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, instrumentos, notificacoes).confirmar("pendencia-1")

        val atual = lancamentos.buscar("pendencia-1")
        assertEquals(StatusLancamento.CONFIRMADO, atual?.status)
        assertNull(atual?.motivoPendencia)
        assertEquals(listOf("pendencia-1"), notificacoes.canceladas)
        assertTrue(notificacoes.postagens.isEmpty())
    }

    @Test
    fun `confirmar pendente sem instrumento mantem pendente e reposta com aviso de correcao`() = runTest {
        val lancamentos = FakeLancamentoRepository(
            listOf(pendencia("pendencia-1", instrumento = null, motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)),
        )
        val categorias = FakeCategoriaRepository(listOf(categoria()))
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, FakeInstrumentoRepository(), notificacoes, categorias).confirmar("pendencia-1")

        val atual = lancamentos.buscar("pendencia-1")
        assertEquals(StatusLancamento.PENDENTE, atual?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, atual?.motivoPendencia)
        assertTrue(notificacoes.canceladas.isEmpty())
        val reposta = notificacoes.postagens.single()
        assertTrue(reposta.precisaCorrigir)
        assertEquals("Alimentação", reposta.pendencia.categoria)
        assertEquals("Padaria do Zé", reposta.pendencia.estabelecimento)
        assertEquals(4290, reposta.pendencia.valorCentavos)
    }

    @Test
    fun `confirmar pendente com instrumento excluido nao confirma`() = runTest {
        val lancamentos = FakeLancamentoRepository(listOf(pendencia("pendencia-1")))
        val instrumentos = FakeInstrumentoRepository(listOf(instrumento(excluidoEm = agora)))
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, instrumentos, notificacoes).confirmar("pendencia-1")

        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar("pendencia-1")?.status)
        assertEquals(1, notificacoes.postagens.size)
        assertTrue(notificacoes.postagens.single().precisaCorrigir)
        assertTrue(notificacoes.canceladas.isEmpty())
    }

    @Test
    fun `acao antiga de pendente ja confirmado nao altera e remove a notificacao`() = runTest {
        val lancamentos = FakeLancamentoRepository(listOf(pendencia("pendencia-1", status = StatusLancamento.CONFIRMADO)))
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, FakeInstrumentoRepository(listOf(instrumento())), notificacoes).confirmar("pendencia-1")

        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar("pendencia-1")?.status)
        assertEquals(listOf("pendencia-1"), notificacoes.canceladas)
        assertTrue(notificacoes.postagens.isEmpty())
    }

    @Test
    fun `publicId desconhecido remove notificacao orfa sem quebrar`() = runTest {
        val lancamentos = FakeLancamentoRepository()
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, FakeInstrumentoRepository(), notificacoes).confirmar("desconhecida")

        assertEquals(listOf("desconhecida"), notificacoes.canceladas)
        assertTrue(notificacoes.postagens.isEmpty())
        assertTrue(lancamentos.todos().isEmpty())
    }

    @Test
    fun `confirma pendente sem categoria quando o instrumento e valido`() = runTest {
        val lancamentos = FakeLancamentoRepository(listOf(pendencia("pendencia-1", categoria = null)))
        val notificacoes = FakeRevisaoNotificacoes()

        caso(lancamentos, FakeInstrumentoRepository(listOf(instrumento())), notificacoes).confirmar("pendencia-1")

        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar("pendencia-1")?.status)
        assertNull(lancamentos.buscar("pendencia-1")?.categoriaPublicId)
        assertEquals(1, notificacoes.canceladas.size)
        assertTrue(notificacoes.postagens.isEmpty())
    }

    private fun caso(
        lancamentos: FakeLancamentoRepository,
        instrumentos: FakeInstrumentoRepository,
        notificacoes: FakeRevisaoNotificacoes,
        categorias: FakeCategoriaRepository = FakeCategoriaRepository(),
    ): ConfirmarPelaNotificacao = ConfirmarPelaNotificacao(
        lancamentoRepository = lancamentos,
        categoriaRepository = categorias,
        confirmarPendencias = ConfirmarPendencias(lancamentos, instrumentos, clock),
        notificacoes = notificacoes,
    )

    private fun instrumento(excluidoEm: Instant? = null): Instrumento = Instrumento(
        publicId = "instrumento-1",
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão Itaú",
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = excluidoEm,
    )

    private fun categoria(): Categoria = Categoria(
        publicId = "alimentacao",
        nome = "Alimentação",
        tipo = TipoCategoria.DESPESA,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun pendencia(
        publicId: String,
        status: StatusLancamento = StatusLancamento.PENDENTE,
        instrumento: String? = "instrumento-1",
        categoria: String? = "alimentacao",
        motivo: MotivoPendencia = MotivoPendencia.BAIXA_CONFIANCA,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumento,
        categoriaPublicId = categoria,
        valorCentavos = 4290,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Padaria do Zé",
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 0.9,
        motivoPendencia = motivo.name,
        criadoEm = agora,
        atualizadoEm = agora,
        textoBruto = "Compra aprovada R$ 42,90 PADARIA DO ZE",
    )
}
