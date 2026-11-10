package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RegrasParaCapturaTest {
    @Test
    fun compilaAsRegrasUmaUnicaVez() = runTest {
        val repositorio = FakeRegraParserRepository()
        val regras = RegrasParaCaptura(repositorio)

        assertNotNull(regras.versaoDe(InstituicaoFinanceira.ITAU))
        assertNotNull(regras.versaoDe(InstituicaoFinanceira.NUBANK))

        assertEquals(1, repositorio.coletas)
    }

    @Test
    fun atualizarDescartaOCacheEAceitaNovaVersao() = runTest {
        val repositorio = FakeRegraParserRepository()
        val regras = RegrasParaCaptura(repositorio)

        regras.versaoDe(InstituicaoFinanceira.ITAU)
        regras.atualizar()
        regras.versaoDe(InstituicaoFinanceira.ITAU)

        assertEquals(2, repositorio.coletas)
    }

    @Test
    fun instituicaoSemRegraNaoTemVersao() = runTest {
        val regras = RegrasParaCaptura(FakeRegraParserRepository(RegrasDeTeste.todas()))

        assertEquals(null, regras.versaoDe(InstituicaoFinanceira.CAJU))
    }
}
