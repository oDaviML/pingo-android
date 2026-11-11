package com.pingo.feature.transactions.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MapeamentoDoDetalheTest {
    private val agora = Instant.parse("2026-11-10T15:00:00Z")

    @Test
    fun `rotulo do par usa a direcao persistida antes da categoria`() {
        val entrada = lancamento(tipoSemTransferencia = TipoLancamento.RECEITA)
        val saida = lancamento(tipoSemTransferencia = TipoLancamento.DESPESA)
        val categoriaDeDespesa = categoria(TipoCategoria.DESPESA)

        assertEquals(true, entradaDe(entrada, categoriaDeDespesa))
        assertEquals(false, saidaDe(saida, categoria(TipoCategoria.RECEITA)))
    }

    @Test
    fun `par legado sem direcao mantem o rotulo desconhecido sem categoria`() {
        assertNull(entradaDe(lancamento(), categoria = null))
    }

    @Test
    fun `par legado com categoria ainda exibe o rotulo conhecido`() {
        assertEquals(true, entradaDe(lancamento(), categoria(TipoCategoria.RECEITA)))
        assertEquals(false, entradaDe(lancamento(), categoria(TipoCategoria.DESPESA)))
    }

    private fun saidaDe(lancamento: Lancamento, categoria: Categoria?) = entradaDe(lancamento, categoria)

    private fun lancamento(tipoSemTransferencia: TipoLancamento? = null): Lancamento = Lancamento(
        publicId = "lancamento-1",
        instrumentoPublicId = "instrumento-1",
        valorCentavos = 5000,
        dataHora = agora,
        tipo = TipoLancamento.TRANSFERENCIA,
        tipoSemTransferencia = tipoSemTransferencia,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun categoria(tipo: TipoCategoria): Categoria = Categoria(
        publicId = "categoria-${tipo.name}",
        nome = tipo.name,
        tipo = tipo,
        padrao = true,
        arquivada = false,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
