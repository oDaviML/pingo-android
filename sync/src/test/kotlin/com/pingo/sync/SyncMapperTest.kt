package com.pingo.sync

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SyncMapperTest {
    @Test
    fun `converte centavos para reais e de volta`() {
        assertEquals(42.9, 4290L.toApiReais(), 0.0001)
        assertEquals(4290L, 42.9.toCentavos())
        assertEquals(0L, 0.0.toCentavos())
        assertEquals(-1250L, (-12.5).toCentavos())
    }

    @Test
    fun `mapeia enums do dominio para os valores da api`() {
        assertEquals("MercadoPago", InstituicaoFinanceira.MERCADO_PAGO.toApiValue())
        assertEquals(InstituicaoFinanceira.MERCADO_PAGO, instituicaoFromApi("MercadoPago"))
        assertEquals("Despesa", TipoLancamento.DESPESA.toApiValue())
        assertEquals(TipoLancamento.TRANSFERENCIA, tipoLancamentoFromApi("Transferencia"))
        assertEquals("Pendente", StatusLancamento.PENDENTE.toApiValue())
        assertEquals(StatusLancamento.PENDENTE, statusLancamentoFromApi("Pendente"))
        assertEquals(InstituicaoFinanceira.OUTRA, instituicaoFromApi("BancoNovo"))
    }

    @Test
    fun `interpreta datas com offset do servidor`() {
        val iso = "2026-10-23T12:34:56.1234567+00:00"

        assertEquals(Instant.parse("2026-10-23T12:34:56.123456700Z"), iso.toInstantValue())
        assertEquals("2026-10-23T12:34:56.123Z", Instant.parse("2026-10-23T12:34:56.123Z").toApiValue())
    }
}
