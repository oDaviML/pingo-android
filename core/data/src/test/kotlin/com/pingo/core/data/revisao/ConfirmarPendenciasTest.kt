package com.pingo.core.data.revisao

import com.pingo.core.data.captura.FakeContas
import com.pingo.core.data.captura.FakeInstrumentos
import com.pingo.core.data.captura.FakeLancamentos
import com.pingo.core.data.captura.MutableClock
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ConfirmarPendenciasTest {
    private val agora: Instant = Instant.parse("2026-11-10T15:00:00Z")
    private val clock = MutableClock(agora, ZoneId.of("America/Sao_Paulo"))
    private val contas = FakeContas()
    private val instrumentos = FakeInstrumentos(contas)
    private val lancamentos = FakeLancamentos(instrumentos)
    private val confirmar = ConfirmarPendencias(lancamentos, instrumentos, clock)

    @Test
    fun `confirma pendente com instrumento valido`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val pendente = pendente("padaria", instrumento.publicId, MotivoPendencia.BAIXA_CONFIANCA)
        lancamentos.salvar(pendente)

        val resultado = confirmar.confirmar(listOf(pendente.publicId))

        assertEquals(1, resultado.confirmados)
        assertEquals(0, resultado.mantidos)
        val persistido = lancamentos.buscar(pendente.publicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)
        assertNull(persistido?.motivoPendencia)
        assertEquals(agora, persistido?.atualizadoEm)
    }

    @Test
    fun `mantem pendente de moeda estrangeira confirmavel com instrumento valido`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.ITAU)
        val pendente = pendente("loja-usa", instrumento.publicId, MotivoPendencia.MOEDA_ESTRANGEIRA)
        lancamentos.salvar(pendente)

        val resultado = confirmar.confirmar(listOf(pendente.publicId))

        assertEquals(1, resultado.confirmados)
        val persistido = lancamentos.buscar(pendente.publicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)
        assertNull(persistido?.motivoPendencia)
    }

    @Test
    fun `mantem pendente sem instrumento`() = runTest {
        val pendente = pendente("padaria", null, MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        lancamentos.salvar(pendente)

        val resultado = confirmar.confirmar(listOf(pendente.publicId))

        assertEquals(0, resultado.confirmados)
        assertEquals(1, resultado.mantidos)
        val persistido = lancamentos.buscar(pendente.publicId)
        assertEquals(StatusLancamento.PENDENTE, persistido?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, persistido?.motivoPendencia)
    }

    @Test
    fun `mantem pendente com instrumento inexistente`() = runTest {
        val pendente = pendente("padaria", "instrumento-fantasma", MotivoPendencia.INSTRUMENTO_AMBIGUO)
        lancamentos.salvar(pendente)

        val resultado = confirmar.confirmar(listOf(pendente.publicId))

        assertEquals(0, resultado.confirmados)
        assertEquals(1, resultado.mantidos)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar(pendente.publicId)?.status)
    }

    @Test
    fun `mantem pendente com instrumento excluido`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.INTER)
        instrumentos.excluir(instrumento.publicId, agora)
        val pendente = pendente("padaria", instrumento.publicId, MotivoPendencia.BAIXA_CONFIANCA)
        lancamentos.salvar(pendente)

        val resultado = confirmar.confirmar(listOf(pendente.publicId))

        assertEquals(0, resultado.confirmados)
        assertEquals(1, resultado.mantidos)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar(pendente.publicId)?.status)
    }

    @Test
    fun `nao contabiliza lancamento ja confirmado`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val confirmado = pendente("padaria", instrumento.publicId, null).copy(status = StatusLancamento.CONFIRMADO)
        lancamentos.salvar(confirmado)

        val resultado = confirmar.confirmar(listOf(confirmado.publicId))

        assertEquals(0, resultado.confirmados)
        assertEquals(0, resultado.mantidos)
    }

    @Test
    fun `lote misto confirma elegiveis e mantem irresolvidos`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val elegivel = pendente("elegivel", instrumento.publicId, MotivoPendencia.CATEGORIA_DESCONHECIDA)
        val outroElegivel = pendente("outro-elegivel", instrumento.publicId, MotivoPendencia.POSSIVEL_DUPLICATA)
        val irresolvido = pendente("irresolvido", null, MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        lancamentos.salvarTodos(listOf(elegivel, outroElegivel, irresolvido))

        val resultado = confirmar.confirmarTodos()

        assertEquals(2, resultado.confirmados)
        assertEquals(1, resultado.mantidos)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar(elegivel.publicId)?.status)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar(outroElegivel.publicId)?.status)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar(irresolvido.publicId)?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamentos.buscar(irresolvido.publicId)?.motivoPendencia)
    }

    @Test
    fun `lote somente de irresolvidos nao confirma nenhum`() = runTest {
        val semInstrumento = pendente("sem-instrumento", null, MotivoPendencia.INSTRUMENTO_AMBIGUO)
        val fantasma = pendente("fantasma", "instrumento-fantasma", MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        lancamentos.salvarTodos(listOf(semInstrumento, fantasma))

        val resultado = confirmar.confirmarTodos()

        assertEquals(0, resultado.confirmados)
        assertEquals(2, resultado.mantidos)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar(semInstrumento.publicId)?.status)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar(fantasma.publicId)?.status)
    }

    @Test
    fun `lote totalmente elegivel confirma todos`() = runTest {
        val instrumento = instrumentos.criar(InstituicaoFinanceira.CAJU)
        val primeiro = pendente("primeiro", instrumento.publicId, MotivoPendencia.BAIXA_CONFIANCA)
        val segundo = pendente("segundo", instrumento.publicId, MotivoPendencia.TRANSFERENCIA_AMBIGUA)
        lancamentos.salvarTodos(listOf(primeiro, segundo))

        val resultado = confirmar.confirmarTodos()

        assertEquals(2, resultado.confirmados)
        assertEquals(0, resultado.mantidos)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar(primeiro.publicId)?.status)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar(segundo.publicId)?.status)
    }

    @Test
    fun `lote vazio nao confirma nada`() = runTest {
        val resultado = confirmar.confirmarTodos()

        assertEquals(0, resultado.confirmados)
        assertEquals(0, resultado.mantidos)
    }

    private fun pendente(publicId: String, instrumentoPublicId: String?, motivo: MotivoPendencia?): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        valorCentavos = 4290,
        dataHora = agora.minusSeconds(3600),
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Padaria do Zé",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        confianca = 0.62,
        motivoPendencia = motivo?.name,
        criadoEm = agora.minusSeconds(3600),
        atualizadoEm = agora.minusSeconds(3600),
        textoBruto = "Compra aprovada R$ 42,90 PADARIA DO ZE",
    )
}
