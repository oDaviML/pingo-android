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
    ): EdicaoDoLancamento = EdicaoDoLancamento(
        publicId = original.publicId,
        tipo = TipoLancamento.DESPESA,
        valorCentavos = original.valorCentavos,
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
