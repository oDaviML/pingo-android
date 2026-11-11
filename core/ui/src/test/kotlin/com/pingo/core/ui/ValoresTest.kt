package com.pingo.core.ui

import com.pingo.core.designsystem.theme.PingoLightColors
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ValoresTest {
    @Test
    fun `formata reais com separador de milhar e centavos`() {
        assertEquals("R$ 0,00", Valores.formatar(0))
        assertEquals("R$ 0,05", Valores.formatar(5))
        assertEquals("R$ 42,90", Valores.formatar(4290))
        assertEquals("R$ 1.234,56", Valores.formatar(123456))
        assertEquals("R$ 1.000.000,00", Valores.formatar(100000000))
    }

    @Test
    fun `assina despesa com menos e receita com mais`() {
        assertEquals("- R$ 42,90", Valores.assinado(4290, TipoLancamento.DESPESA))
        assertEquals("+ R$ 150,00", Valores.assinado(15000, TipoLancamento.RECEITA))
        assertEquals("R$ 500,00", Valores.assinado(50000, TipoLancamento.TRANSFERENCIA))
    }

    @Test
    fun `total do dia usa sinal conforme o saldo`() {
        assertEquals("- R$ 60,00", Valores.total(-6000))
        assertEquals("+ R$ 150,00", Valores.total(15000))
        assertEquals("R$ 0,00", Valores.total(0))
    }

    @Test
    fun `cores seguem o tipo do lancamento`() {
        assertEquals(PingoLightColors.expense, Valores.corDoTipo(TipoLancamento.DESPESA, PingoLightColors))
        assertEquals(PingoLightColors.income, Valores.corDoTipo(TipoLancamento.RECEITA, PingoLightColors))
        assertEquals(PingoLightColors.transfer, Valores.corDoTipo(TipoLancamento.TRANSFERENCIA, PingoLightColors))
    }

    @Test
    fun `horario usa o fuso informado`() {
        assertEquals("14:32", Valores.horario(Instant.parse("2026-10-22T14:32:00Z"), ZoneOffset.UTC))
        assertEquals("11:32", Valores.horario(Instant.parse("2026-10-22T14:32:00Z"), ZoneOffset.ofHours(-3)))
    }
}
