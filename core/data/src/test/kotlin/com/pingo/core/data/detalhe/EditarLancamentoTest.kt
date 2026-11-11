package com.pingo.core.data.detalhe

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditarLancamentoTest {
    private val ambiente = AmbienteDoDetalhe()

    @Test
    fun `resolve pendente sem instrumento com escolha explicita e confirma`() = runTest {
        val instrumento = ambiente.instrumento()
        val categoria = ambiente.categoria("Alimentação")
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "pendente",
                instrumentoPublicId = null,
                valorCentavos = 4290,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
            ),
        )

        val resultado = ambiente.editar.editar(
            edicao(publicId = "pendente", instrumento = instrumento.publicId, categoria = categoria.publicId),
        )
        assertTrue(resultado is ResultadoDaEdicao.Salvo)
        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(StatusLancamento.CONFIRMADO, salvo.status)
        assertNull(salvo.motivoPendencia)
        assertEquals(instrumento.publicId, salvo.instrumentoPublicId)
        assertEquals(categoria.publicId, salvo.categoriaPublicId)
        assertEquals(InstituicaoFinanceira.NUBANK, salvo.instituicaoOrigem)
        assertEquals(salvo, ambiente.lancamentos.buscar("pendente"))
    }

    @Test
    fun `pendente sem instrumento permanece pendente quando nada e escolhido`() = runTest {
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "pendente",
                instrumentoPublicId = null,
                valorCentavos = 1890,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.INSTRUMENTO_AMBIGUO,
            ),
        )

        val resultado = ambiente.editar.editar(edicao(publicId = "pendente", valorCentavos = 1990))

        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(StatusLancamento.PENDENTE, salvo.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_AMBIGUO.name, salvo.motivoPendencia)
        assertNull(salvo.instrumentoPublicId)
        assertEquals(1990L, salvo.valorCentavos)
    }

    @Test
    fun `manual sem instrumento e invalido`() = runTest {
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "manual",
                instrumentoPublicId = null,
                valorCentavos = 5000,
                origem = OrigemLancamento.MANUAL,
                instituicaoOrigem = null,
            ),
        )

        val resultado = ambiente.editar.editar(edicao(publicId = "manual"))

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.INSTRUMENTO)), resultado)
    }

    @Test
    fun `capturado confirmado sem instrumento e invalido`() = runTest {
        ambiente.lancamentos.salvar(lancamentoDeTeste(publicId = "confirmado", instrumentoPublicId = null, valorCentavos = 5000))

        val resultado = ambiente.editar.editar(edicao(publicId = "confirmado"))

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.INSTRUMENTO)), resultado)
    }

    @Test
    fun `valor zerado e invalido`() = runTest {
        val instrumento = ambiente.instrumento()
        ambiente.lancamentos.salvar(lancamentoDeTeste(publicId = "lanc", instrumentoPublicId = instrumento.publicId, valorCentavos = 5000))

        val resultado = ambiente.editar.editar(edicao(valorCentavos = 0, instrumento = instrumento.publicId))

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.VALOR)), resultado)
    }

    @Test
    fun `instrumento informado inexistente nao vira nulo`() = runTest {
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "lanc",
                instrumentoPublicId = null,
                valorCentavos = 5000,
                status = StatusLancamento.PENDENTE,
            ),
        )

        val resultado = ambiente.editar.editar(edicao(instrumento = "instrumento-que-sumiu"))

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.INSTRUMENTO)), resultado)
    }

    @Test
    fun `aplicar a todos grava regra do estabelecimento normalizado`() = runTest {
        val instrumento = ambiente.instrumento()
        val categoria = ambiente.categoria("Alimentação")
        ambiente.lancamentos.salvar(lancamentoDeTeste(publicId = "lanc", instrumentoPublicId = instrumento.publicId, valorCentavos = 4290))

        val resultado = ambiente.editar.editar(
            edicao(instrumento = instrumento.publicId, categoria = categoria.publicId, aplicarATodos = true),
        )

        assertTrue(resultado is ResultadoDaEdicao.Salvo)
        val regra = ambiente.regras.buscarPorEstabelecimento("padaria do zé")
        assertNotNull(regra)
        assertEquals(categoria.publicId, regra?.categoriaPublicId)
        assertEquals(categoria.publicId, ambiente.aprendida.resolver("PADARIA DO ZÉ")?.publicId)
    }

    @Test
    fun `sem aplicar nao grava regra`() = runTest {
        val instrumento = ambiente.instrumento()
        val categoria = ambiente.categoria("Alimentação")
        ambiente.lancamentos.salvar(lancamentoDeTeste(publicId = "lanc", instrumentoPublicId = instrumento.publicId, valorCentavos = 4290))

        ambiente.editar.editar(edicao(instrumento = instrumento.publicId, categoria = categoria.publicId))

        assertNull(ambiente.regras.buscarPorEstabelecimento("padaria do zé"))
    }

    @Test
    fun `categoria de tipo incompativel e invalida`() = runTest {
        val instrumento = ambiente.instrumento()
        val receita = ambiente.categoria("Salário", TipoCategoria.RECEITA)
        ambiente.lancamentos.salvar(lancamentoDeTeste(publicId = "lanc", instrumentoPublicId = instrumento.publicId, valorCentavos = 4290))

        val resultado = ambiente.editar.editar(edicao(instrumento = instrumento.publicId, categoria = receita.publicId))

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.CATEGORIA)), resultado)
    }

    @Test
    fun `pendente de categoria desconhecida confirma ao escolher categoria`() = runTest {
        val instrumento = ambiente.instrumento()
        val categoria = ambiente.categoria("Alimentação")
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "lanc",
                instrumentoPublicId = instrumento.publicId,
                valorCentavos = 4290,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.CATEGORIA_DESCONHECIDA,
            ),
        )

        val resultado = ambiente.editar.editar(edicao(instrumento = instrumento.publicId, categoria = categoria.publicId))

        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(StatusLancamento.CONFIRMADO, salvo.status)
        assertNull(salvo.motivoPendencia)
    }

    @Test
    fun `pendencia de moeda estrangeira permanece ao salvar`() = runTest {
        val instrumento = ambiente.instrumento()
        val categoria = ambiente.categoria("Compras")
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "lanc",
                instrumentoPublicId = instrumento.publicId,
                valorCentavos = 4290,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.MOEDA_ESTRANGEIRA,
            ),
        )

        val resultado = ambiente.editar.editar(edicao(instrumento = instrumento.publicId, categoria = categoria.publicId))

        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(StatusLancamento.PENDENTE, salvo.status)
        assertEquals(MotivoPendencia.MOEDA_ESTRANGEIRA.name, salvo.motivoPendencia)
    }

    @Test
    fun `editar transferencia atualiza valor e instrumentos do par`() = runTest {
        val origem = ambiente.instrumento(ambiente.conta("Nubank"), digitos = "1234")
        val destino = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val saida = lancamentoDeTeste(
            publicId = "saida",
            instrumentoPublicId = origem.publicId,
            valorCentavos = 50000,
            tipo = TipoLancamento.TRANSFERENCIA,
            transferenciaId = "transferencia-1",
            instituicaoOrigem = null,
        )
        val entrada = lancamentoDeTeste(
            publicId = "entrada",
            instrumentoPublicId = destino.publicId,
            valorCentavos = 50000,
            tipo = TipoLancamento.TRANSFERENCIA,
            transferenciaId = "transferencia-1",
            instituicaoOrigem = null,
        )
        ambiente.lancamentos.salvarTodos(listOf(saida, entrada))

        val resultado = ambiente.editar.editar(
            edicao(
                publicId = "saida",
                tipo = TipoLancamento.TRANSFERENCIA,
                valorCentavos = 60000,
                instrumento = origem.publicId,
                destino = destino.publicId,
            ),
        )

        val salvo = resultado as ResultadoDaEdicao.Salvo
        assertEquals(60000L, salvo.lancamento.valorCentavos)
        assertEquals(60000L, salvo.par?.valorCentavos)
        assertEquals(origem.publicId, salvo.lancamento.instrumentoPublicId)
        assertEquals(destino.publicId, salvo.par?.instrumentoPublicId)
        assertEquals("transferencia-1", salvo.par?.transferenciaId)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `transferencia com destino na mesma conta e invalida`() = runTest {
        val origem = ambiente.instrumento(ambiente.conta("Nubank"))
        val destino = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                publicId = "lanc",
                instrumentoPublicId = origem.publicId,
                valorCentavos = 50000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = "transferencia-1",
            ),
        )

        val resultado = ambiente.editar.editar(
            edicao(
                tipo = TipoLancamento.TRANSFERENCIA,
                valorCentavos = 50000,
                instrumento = origem.publicId,
                destino = origem.publicId,
            ),
        )

        assertEquals(ResultadoDaEdicao.Invalido(setOf(CampoDoDetalhe.DESTINO)), resultado)
        assertNotNull(ambiente.instrumentos.buscar(destino.publicId))
    }

    private fun edicao(
        publicId: String = "lanc",
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        valorCentavos: Long = 4290,
        instrumento: String? = null,
        destino: String? = null,
        categoria: String? = null,
        aplicarATodos: Boolean = false,
    ): EdicaoDoLancamento = EdicaoDoLancamento(
        publicId = publicId,
        tipo = tipo,
        valorCentavos = valorCentavos,
        instrumentoPublicId = instrumento,
        destinoPublicId = destino,
        categoriaPublicId = categoria,
        dataHora = ambiente.agora,
        estabelecimento = "Padaria do Zé",
        aplicarCategoriaATodos = aplicarATodos,
    )
}
