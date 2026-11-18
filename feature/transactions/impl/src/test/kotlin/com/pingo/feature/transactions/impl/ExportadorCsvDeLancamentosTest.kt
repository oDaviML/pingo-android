package com.pingo.feature.transactions.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExportadorCsvDeLancamentosTest {
    private val agora = Instant.parse("2026-11-10T14:32:00Z")

    @Test
    fun `exporta csv rfc4180 ordenado em sao paulo protegendo formulas e sem texto bruto rnf11`() {
        val conta = contaNubank()
        val instrumento = instrumentoCartao(conta.publicId)
        val categoria = categoriaAlimentacao()
        val despesaComFormula = despesaComFormula(instrumento.publicId, categoria.publicId)
        val receitaAnterior = receitaPendente()

        val resultado = ExportadorCsvDeLancamentos.exportar(
            lancamentos = listOf(receitaAnterior, despesaComFormula),
            categoriasPorId = mapOf(categoria.publicId to categoria),
            instrumentosPorId = mapOf(instrumento.publicId to instrumento),
            contasPorId = mapOf(conta.publicId to conta),
        ) as ResultadoExportacaoCsv.Sucesso

        assertEquals(2, resultado.quantidade)
        assertFalse(resultado.conteudoCsv.contains("SIGILO_TEXTO_BRUTO_NAO_EXPORTAR"))
        assertFalse(resultado.conteudoCsv.contains("OUTRO_TEXTO_BRUTO"))

        val linhas = resultado.conteudoCsv.split("\r\n")
        assertEquals(3, linhas.size)
        assertEquals("Data,Hora,Tipo,Estabelecimento,Categoria,Conta,Instrumento,Valor,Status,Origem", linhas[0])
        val linhaDespesaEsperada = "10/11/2026,11:32,Despesa,\"'=Padaria \"\"Zé\"\", Centro\"," +
            "Alimentação,\"Nubank, Principal\",\"Cartão \"\"Black\"\" 4321\",\"-42,90\",Confirmado,Notificação"
        assertEquals(linhaDespesaEsperada, linhas[1])
        assertEquals("08/11/2026,23:10,Receita,Reembolso,,,,\"1500,00\",Pendente,Manual", linhas[2])
    }

    @Test
    fun `retorna vazio quando lista filtrada nao tem lancamentos`() {
        val resultado = ExportadorCsvDeLancamentos.exportar(
            lancamentos = emptyList(),
            categoriasPorId = emptyMap(),
            instrumentosPorId = emptyMap(),
            contasPorId = emptyMap(),
        )

        assertTrue(resultado is ResultadoExportacaoCsv.Vazio)
        assertEquals(ExportadorCsvDeLancamentos.MENSAGEM_VAZIO, (resultado as ResultadoExportacaoCsv.Vazio).mensagem)
    }

    private fun contaNubank(): Conta = Conta(
        publicId = "conta-1",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank, Principal",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun instrumentoCartao(contaPublicId: String): Instrumento = Instrumento(
        publicId = "inst-1",
        contaPublicId = contaPublicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão \"Black\"",
        ultimosDigitos = "4321",
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun categoriaAlimentacao(): Categoria = Categoria(
        publicId = "cat-1",
        nome = "Alimentação",
        tipo = TipoCategoria.DESPESA,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun despesaComFormula(instrumentoPublicId: String, categoriaPublicId: String): Lancamento = Lancamento(
        publicId = "l1",
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        valorCentavos = 4290L,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "=Padaria \"Zé\", Centro",
        textoBruto = "SIGILO_TEXTO_BRUTO_NAO_EXPORTAR",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun receitaPendente(): Lancamento = Lancamento(
        publicId = "l2",
        instrumentoPublicId = null,
        categoriaPublicId = null,
        valorCentavos = 150000L,
        dataHora = Instant.parse("2026-11-09T02:10:00Z"),
        tipo = TipoLancamento.RECEITA,
        estabelecimento = "Reembolso",
        textoBruto = "OUTRO_TEXTO_BRUTO",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
