package com.pingo.core.data.detalhe

import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditarLancamentoParceladoTest {
    @Test
    fun `converte compra capturada automaticamente no credito em compra parcelada preservando origem na primeira`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Nubank", InstituicaoFinanceira.NUBANK))
        val categoria = ambiente.categoria("Compras", TipoCategoria.DESPESA)
        val original = lancamentoCapturado(
            ambiente = ambiente,
            publicId = "capturado-1",
            instrumentoPublicId = credito.publicId,
            valorCentavos = 10000L,
            estabelecimento = "Amazon",
            status = StatusLancamento.PENDENTE,
        )
        ambiente.lancamentos.salvar(original)

        val resultado = criarEditorComConfiguracao(ambiente).editar(
            edicaoDe(original, credito.publicId, categoria.publicId, totalParcelas = 3, modo = ModoParcelamento.VALOR_TOTAL),
        )

        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(original.publicId, salvo.publicId)
        assertEquals(OrigemLancamento.NOTIFICACAO, salvo.origem)
        assertEquals("hash-amazon-100", salvo.hashDuplicata)
        assertEquals(StatusLancamento.CONFIRMADO, salvo.status)
        assertNull(salvo.motivoPendencia)

        val parcelas = ambiente.lancamentos.listarParcelasDaCompra(original.publicId)
        assertEquals(listOf(3334L, 3333L, 3333L), parcelas.map { it.valorCentavos })
        assertEquals(10000L, parcelas.first().valorTotalCompraCentavos)
        assertFilhasConfirmadas(original.publicId, parcelas.drop(1))
    }

    @Test
    fun `converte compra capturada informando valor da parcela replicando para todas as parcelas`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val categoria = ambiente.categoria("Eletrônicos", TipoCategoria.DESPESA)
        val original = lancamentoCapturado(
            ambiente = ambiente,
            publicId = "capturado-parcela",
            instrumentoPublicId = credito.publicId,
            categoriaPublicId = categoria.publicId,
            valorCentavos = 15000L,
            estabelecimento = "Kabum",
        )
        ambiente.lancamentos.salvar(original)

        val resultado = criarEditorComConfiguracao(ambiente).editar(
            edicaoDe(original, credito.publicId, categoria.publicId, totalParcelas = 4, modo = ModoParcelamento.VALOR_PARCELA),
        )

        assertTrue(resultado is ResultadoDaEdicao.Salvo)
        val parcelas = ambiente.lancamentos.listarParcelasDaCompra(original.publicId)
        assertEquals(4, parcelas.size)
        assertEquals(listOf(15000L, 15000L, 15000L, 15000L), parcelas.map { it.valorCentavos })
        assertEquals(60000L, parcelas.first().valorTotalCompraCentavos)
        assertNotNull(parcelas.last().dataHora)
    }

    @Test
    fun `recusa parcelar na edicao quando instrumento nao for credito`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val conta = ambiente.conta("Nubank", InstituicaoFinanceira.NUBANK)
        val debito =
            Instrumento(
                "debito-1",
                conta.publicId,
                TipoInstrumento.DEBITO,
                "Débito",
                criadoEm = ambiente.agora,
                atualizadoEm = ambiente.agora,
            )
        ambiente.instrumentos.salvar(debito)
        val original =
            lancamentoCapturado(ambiente, "capturado-debito", debito.publicId, valorCentavos = 5000L, estabelecimento = "Mercado")
        ambiente.lancamentos.salvar(original)

        val resultado = ambiente.editar.editar(
            edicaoDe(original, debito.publicId, categoriaId = null, totalParcelas = 2, modo = ModoParcelamento.VALOR_TOTAL),
        )

        val invalido = resultado as ResultadoDaEdicao.Invalido
        assertTrue(CampoDoDetalhe.PARCELAS in invalido.campos)
    }

    @Test
    fun `aumenta parcelas de compra ja parcelada preservando ids existentes e criando novas`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Itaú", InstituicaoFinanceira.ITAU))
        val categoria = ambiente.categoria("Compras", TipoCategoria.DESPESA)
        val editor = criarEditorComConfiguracao(ambiente)
        val p1 = lancamentoCapturado(ambiente, "p1", credito.publicId, 10000L, "O Boticário", categoria.publicId)
        ambiente.lancamentos.salvar(p1)
        editor.editar(edicaoDe(p1, credito.publicId, categoria.publicId, totalParcelas = 2, modo = ModoParcelamento.VALOR_PARCELA))
        val iniciais = ambiente.lancamentos.listarParcelasDaCompra("p1")
        val idP2 = iniciais[1].publicId

        val resultado = editor.editar(
            edicaoDe(
                iniciais[1],
                credito.publicId,
                categoria.publicId,
                totalParcelas = 4,
                modo = ModoParcelamento.VALOR_PARCELA,
                valorCentavos = 10000L,
            ),
        )

        assertTrue(resultado is ResultadoDaEdicao.Salvo)
        val atualizadas = ambiente.lancamentos.listarParcelasDaCompra("p1")
        assertEquals(4, atualizadas.size)
        assertEquals("p1", atualizadas[0].publicId)
        assertEquals(idP2, atualizadas[1].publicId)
        assertEquals(40000L, atualizadas[0].valorTotalCompraCentavos)
        assertEquals(listOf(10000L, 10000L, 10000L, 10000L), atualizadas.map { it.valorCentavos })
    }

    @Test
    fun `reduz parcelas de compra ja parcelada e exclui excedentes`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Itaú", InstituicaoFinanceira.ITAU))
        val categoria = ambiente.categoria("Compras", TipoCategoria.DESPESA)
        val editor = criarEditorComConfiguracao(ambiente)
        val p1 = lancamentoCapturado(ambiente, "p1", credito.publicId, 30000L, "Loja", categoria.publicId)
        ambiente.lancamentos.salvar(p1)
        editor.editar(edicaoDe(p1, credito.publicId, categoria.publicId, totalParcelas = 4, modo = ModoParcelamento.VALOR_TOTAL))

        val resultado = editor.editar(
            edicaoDe(
                ambiente.lancamentos.buscar("p1")!!,
                credito.publicId,
                categoria.publicId,
                totalParcelas = 2,
                modo = ModoParcelamento.VALOR_TOTAL,
                valorCentavos = 30000L,
            ),
        )

        assertTrue(resultado is ResultadoDaEdicao.Salvo)
        val atualizadas = ambiente.lancamentos.listarParcelasDaCompra("p1")
        assertEquals(2, atualizadas.size)
        assertEquals(listOf(15000L, 15000L), atualizadas.map { it.valorCentavos })
        assertEquals(2, ambiente.lancamentos.todos().count { it.excluidoEm != null })
    }

    @Test
    fun `converte compra ja parcelada de volta para a vista excluindo parcelas filhas`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Nubank", InstituicaoFinanceira.NUBANK))
        val categoria = ambiente.categoria("Compras", TipoCategoria.DESPESA)
        val editor = criarEditorComConfiguracao(ambiente)
        val p1 = lancamentoCapturado(ambiente, "p1", credito.publicId, 30000L, "Loja", categoria.publicId)
        ambiente.lancamentos.salvar(p1)
        editor.editar(edicaoDe(p1, credito.publicId, categoria.publicId, totalParcelas = 3, modo = ModoParcelamento.VALOR_TOTAL))

        val resultado = editor.editar(
            edicaoDe(
                ambiente.lancamentos.buscar("p1")!!,
                credito.publicId,
                categoria.publicId,
                totalParcelas = 1,
                modo = ModoParcelamento.VALOR_TOTAL,
                valorCentavos = 25000L,
            ),
        )

        val salvo = (resultado as ResultadoDaEdicao.Salvo).lancamento
        assertEquals(1, salvo.totalParcelas)
        assertEquals(25000L, salvo.valorCentavos)
        assertNull(salvo.valorTotalCompraCentavos)
        assertEquals(1, ambiente.lancamentos.listarParcelasDaCompra("p1").size)
    }

    @Test
    fun `editar apenas categoria em compra parcelada nao altera centavos das parcelas`() = runBlocking {
        val ambiente = AmbienteDoDetalhe()
        val credito = ambiente.instrumento(ambiente.conta("Itaú", InstituicaoFinanceira.ITAU))
        val cat1 = ambiente.categoria("Compras", TipoCategoria.DESPESA)
        val cat2 = ambiente.categoria("Presentes", TipoCategoria.DESPESA)
        val editor = criarEditorComConfiguracao(ambiente)
        val p1 = lancamentoCapturado(ambiente, "p1", credito.publicId, 10000L, "O Boticário", cat1.publicId)
        ambiente.lancamentos.salvar(p1)
        editor.editar(edicaoDe(p1, credito.publicId, cat1.publicId, totalParcelas = 3, modo = ModoParcelamento.VALOR_TOTAL))
        val parcelasAntes = ambiente.lancamentos.listarParcelasDaCompra("p1")

        editor.editar(
            edicaoDe(
                parcelasAntes[1],
                credito.publicId,
                cat2.publicId,
                totalParcelas = 3,
                modo = ModoParcelamento.VALOR_PARCELA,
                valorCentavos = parcelasAntes[1].valorCentavos,
            ),
        )

        val parcelasDepois = ambiente.lancamentos.listarParcelasDaCompra("p1")
        assertEquals(listOf(3334L, 3333L, 3333L), parcelasDepois.map { it.valorCentavos })
        assertEquals(10000L, parcelasDepois.first().valorTotalCompraCentavos)
        assertTrue(parcelasDepois.all { it.categoriaPublicId == cat2.publicId })
    }

    private fun assertFilhasConfirmadas(paiId: String, filhas: List<Lancamento>) {
        assertTrue(filhas.isNotEmpty())
        filhas.forEach { filha ->
            assertEquals(paiId, filha.compraPaiPublicId)
            assertEquals(StatusLancamento.CONFIRMADO, filha.status)
            assertEquals(OrigemLancamento.MANUAL, filha.origem)
            assertNull(filha.hashDuplicata)
            assertNull(filha.valorTotalCompraCentavos)
        }
    }

    @Suppress("LongParameterList")
    private fun lancamentoCapturado(
        ambiente: AmbienteDoDetalhe,
        publicId: String,
        instrumentoPublicId: String,
        valorCentavos: Long,
        estabelecimento: String,
        categoriaPublicId: String? = null,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        valorCentavos = valorCentavos,
        dataHora = Instant.parse("2026-10-10T14:00:00Z"),
        tipo = TipoLancamento.DESPESA,
        estabelecimento = estabelecimento,
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        confianca = 0.85,
        hashDuplicata = "hash-amazon-100",
        textoBruto = "Compra aprovada $estabelecimento",
        criadoEm = ambiente.agora,
        atualizadoEm = ambiente.agora,
    )

    private fun edicaoDe(
        original: Lancamento,
        instrumentoId: String,
        categoriaId: String?,
        totalParcelas: Int,
        modo: ModoParcelamento,
        valorCentavos: Long = original.valorCentavos,
    ): EdicaoDoLancamento = EdicaoDoLancamento(
        publicId = original.publicId,
        tipo = TipoLancamento.DESPESA,
        valorCentavos = valorCentavos,
        instrumentoPublicId = instrumentoId,
        categoriaPublicId = categoriaId,
        dataHora = original.dataHora,
        estabelecimento = original.estabelecimento,
        totalParcelas = totalParcelas,
        modoParcelamento = modo,
    )

    private fun criarEditorComConfiguracao(ambiente: AmbienteDoDetalhe): EditarLancamento = EditarLancamento(
        lancamentoRepository = ambiente.lancamentos,
        instrumentoRepository = ambiente.instrumentos,
        categoriaRepository = ambiente.categorias,
        categoriaAprendida = ambiente.aprendida,
        confirmadorDeAssinatura = object : ConfirmadorDeAssinatura {
            override suspend fun prepararAoSalvarManual(lancamento: Lancamento, categoria: Categoria?): Lancamento = lancamento

            override suspend fun aoEditarLancamento(original: Lancamento, atualizado: Lancamento, novaCategoria: Categoria?): Lancamento =
                atualizado.copy(assinaturaPublicId = null)
        },
        configuracaoCartaoRepository = object : ConfiguracaoCartaoRepository {
            override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> = emptyFlow()

            override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> = emptyList()

            override suspend fun listarAtivas(): List<VigenciaDeCartao> = emptyList()
        },
        transacaoLocal = ambiente.transacoes,
        clock = ambiente.clock,
    )
}
