package com.pingo.core.model.relatorio

import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth

class AgregadorDeRelatorioMensalTest {
    private fun instanteSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int = 12, minuto: Int = 0, segundo: Int = 0): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto, segundo)
            .atZone(AgregadorDeRelatorioMensal.ZONA_SAO_PAULO)
            .toInstant()

    private fun lancamento(
        publicId: String,
        valorCentavos: Long,
        dataHora: Instant,
        tipo: TipoLancamento,
        categoriaPublicId: String? = "cat-1",
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        excluidoEm: Instant? = null,
    ) = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "inst-arquivado-ou-ativo",
        categoriaPublicId = categoriaPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        status = status,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        excluidoEm = excluidoEm,
    )

    @Test
    fun `fronteira mensal em Sao Paulo separa virada do ano e exclui pendentes transferencias ajustes e excluidos`() {
        val fimDezembro = instanteSaoPaulo(2025, 12, 31, 23, 59, 59)
        val inicioJaneiro = instanteSaoPaulo(2026, 1, 1, 0, 0, 0)
        val meioJaneiro = instanteSaoPaulo(2026, 1, 15, 10, 0, 0)

        val itens = listOf(
            lancamento("d-dez", 12_000L, fimDezembro, TipoLancamento.DESPESA, "cat-alimentos"),
            lancamento("r-jan", 500_000L, inicioJaneiro, TipoLancamento.RECEITA, "cat-salario"),
            lancamento("d-jan", 100_000L, meioJaneiro, TipoLancamento.DESPESA, "cat-alimentos"),
            lancamento("pendente", 50_000L, meioJaneiro, TipoLancamento.DESPESA, "cat-alimentos", status = StatusLancamento.PENDENTE),
            lancamento("transf", 80_000L, meioJaneiro, TipoLancamento.TRANSFERENCIA, null),
            lancamento("ajuste", 25_000L, meioJaneiro, TipoLancamento.AJUSTE, null),
            lancamento("excluido", 40_000L, meioJaneiro, TipoLancamento.DESPESA, "cat-alimentos", excluidoEm = meioJaneiro),
        )

        val relatorioJan = AgregadorDeRelatorioMensal.agregar(YearMonth.of(2026, 1), itens)
        assertEquals(500_000L, relatorioJan.totalReceitasCentavos)
        assertEquals(100_000L, relatorioJan.totalDespesasCentavos)
        assertEquals(400_000L, relatorioJan.saldoMensalCentavos)

        val relatorioDez = AgregadorDeRelatorioMensal.agregar(YearMonth.of(2025, 12), itens)
        assertEquals(0L, relatorioDez.totalReceitasCentavos)
        assertEquals(12_000L, relatorioDez.totalDespesasCentavos)
    }

    @Test
    fun `rosca calcula valores percentuais exatos e comparacao com mes anterior incluindo sem base`() {
        val dez = YearMonth.of(2025, 12)
        val jan = YearMonth.of(2026, 1)
        val itens = listOf(
            lancamento("a-dez", 20_000L, instanteSaoPaulo(2025, 12, 10), TipoLancamento.DESPESA, "cat-a"),
            lancamento("a-jan", 30_000L, instanteSaoPaulo(2026, 1, 10), TipoLancamento.DESPESA, "cat-a"),
            lancamento("b-jan", 10_000L, instanteSaoPaulo(2026, 1, 11), TipoLancamento.DESPESA, "cat-b"),
        )

        val relatorio = AgregadorDeRelatorioMensal.agregar(jan, itens)
        assertEquals(40_000L, relatorio.totalDespesasCentavos)
        assertEquals(2, relatorio.categoriasDespesa.size)

        val catA = relatorio.categoriasDespesa[0]
        assertEquals("cat-a", catA.categoriaPublicId)
        assertEquals(30_000L, catA.valorCentavos)
        assertEquals(75, catA.percentualInteiro)
        assertEquals(7_500, catA.pontosBase)
        assertEquals(VariacaoCategoria.Percentual(50), catA.variacaoMesAnterior)

        val catB = relatorio.categoriasDespesa[1]
        assertEquals("cat-b", catB.categoriaPublicId)
        assertEquals(10_000L, catB.valorCentavos)
        assertEquals(25, catB.percentualInteiro)
        assertEquals(2_500, catB.pontosBase)
        assertEquals(VariacaoCategoria.SemBase, catB.variacaoMesAnterior)

        assertEquals(VariacaoCategoria.Percentual(-25), AgregadorDeRelatorioMensal.calcularVariacao(15_000L, 20_000L))
        assertEquals(VariacaoCategoria.SemBase, AgregadorDeRelatorioMensal.calcularVariacao(15_000L, 0L))
        assertEquals(dez.plusMonths(1), jan)
    }

    @Test
    fun `distribuicao inteira garante soma exata de 100 por cento e 10000 pontos base`() {
        val percentuais = AgregadorDeRelatorioMensal.distribuirPartesInteiras(listOf(10_000L, 10_000L, 10_000L), 100)
        assertEquals(100, percentuais.sum())
        assertEquals(listOf(34, 33, 33), percentuais)

        val pontos = AgregadorDeRelatorioMensal.distribuirPartesInteiras(listOf(10_000L, 10_000L, 10_000L), 10_000)
        assertEquals(10_000, pontos.sum())
        assertEquals(listOf(3_334, 3_333, 3_333), pontos)
    }

    @Test
    fun `serie de seis meses encerra no mes escolhido com meses vazios zerados atravessando virada do ano`() {
        val itens = listOf(
            lancamento("r-set", 100_000L, instanteSaoPaulo(2025, 9, 5), TipoLancamento.RECEITA),
            lancamento("d-dez", 25_000L, instanteSaoPaulo(2025, 12, 20), TipoLancamento.DESPESA),
            lancamento("r-fev", 200_000L, instanteSaoPaulo(2026, 2, 1), TipoLancamento.RECEITA),
            lancamento("d-mar-futuro", 99_000L, instanteSaoPaulo(2026, 3, 1), TipoLancamento.DESPESA),
        )

        val relatorio = AgregadorDeRelatorioMensal.agregar(YearMonth.of(2026, 2), itens)
        assertEquals(6, relatorio.serieSeisMeses.size)
        assertEquals(
            listOf(
                PontoSerieMensal(YearMonth.of(2025, 9), 100_000L, 0L),
                PontoSerieMensal(YearMonth.of(2025, 10), 0L, 0L),
                PontoSerieMensal(YearMonth.of(2025, 11), 0L, 0L),
                PontoSerieMensal(YearMonth.of(2025, 12), 0L, 25_000L),
                PontoSerieMensal(YearMonth.of(2026, 1), 0L, 0L),
                PontoSerieMensal(YearMonth.of(2026, 2), 200_000L, 0L),
            ),
            relatorio.serieSeisMeses,
        )
    }
}
