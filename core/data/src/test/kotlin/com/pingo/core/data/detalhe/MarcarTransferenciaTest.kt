package com.pingo.core.data.detalhe

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarcarTransferenciaTest {
    private val ambiente = AmbienteDoDetalhe()

    @Test
    fun `marca com par candidato sem duplicar`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", inter.publicId, 5000, tipo = TipoLancamento.RECEITA))

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        val marcada = resultado as ResultadoDaTransferencia.Marcada
        assertNotNull(marcada.lancamento.transferenciaId)
        assertEquals(marcada.lancamento.transferenciaId, marcada.par?.transferenciaId)
        assertEquals("entrada", marcada.par?.publicId)
        assertEquals(TipoLancamento.TRANSFERENCIA, marcada.lancamento.tipo)
        assertEquals(TipoLancamento.TRANSFERENCIA, marcada.par?.tipo)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `cria par quando nao existe candidato`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        val marcada = resultado as ResultadoDaTransferencia.Marcada
        val par = marcada.par
        assertNotNull(par)
        assertEquals(inter.publicId, par?.instrumentoPublicId)
        assertEquals(TipoLancamento.TRANSFERENCIA, par?.tipo)
        assertEquals(marcada.lancamento.transferenciaId, par?.transferenciaId)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `nao marca sem instrumento resolvido`() = runTest {
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste("pendente", instrumentoPublicId = null, valorCentavos = 5000, status = StatusLancamento.PENDENTE),
        )

        val resultado = ambiente.transferir.marcar("pendente", inter.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO), resultado)
        val original = ambiente.lancamentos.buscar("pendente")
        assertNull(original?.transferenciaId)
        assertEquals(TipoLancamento.DESPESA, original?.tipo)
    }

    @Test
    fun `nao marca quando o candidato nao tem instrumento`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada-sem-cartao",
                instrumentoPublicId = null,
                valorCentavos = 5000,
                tipo = TipoLancamento.RECEITA,
                status = StatusLancamento.PENDENTE,
            ),
        )

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.PAR_NAO_RESOLVIDO), resultado)
        assertEquals(2, ambiente.lancamentos.todos().size)
        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null })
    }

    @Test
    fun `nao marca com destino na mesma conta`() = runTest {
        val conta = ambiente.conta("Nubank")
        val nubank = ambiente.instrumento(conta)
        val outroNubank = ambiente.instrumento(conta)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))

        val resultado = ambiente.transferir.marcar("saida", outroNubank.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.MESMA_CONTA), resultado)
        assertNull(ambiente.lancamentos.buscar("saida")?.transferenciaId)
    }

    @Test
    fun `nao marca com mais de um candidato`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada-1", inter.publicId, 5000, tipo = TipoLancamento.RECEITA))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada-2", inter.publicId, 5000, tipo = TipoLancamento.RECEITA))

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.PAR_AMBIGUO), resultado)
        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null })
    }

    @Test
    fun `desfazer remove a marca dos dois lados e restaura os tipos`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val despesa = ambiente.categoria("Alimentação")
        val receita = ambiente.categoria("Salário", com.pingo.core.model.TipoCategoria.RECEITA)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000, categoriaPublicId = despesa.publicId))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste("entrada", inter.publicId, 5000, tipo = TipoLancamento.RECEITA, categoriaPublicId = receita.publicId),
        )
        ambiente.transferir.marcar("saida", inter.publicId)

        val resultado = ambiente.transferir.desfazer("saida")

        val desfeita = resultado as ResultadoDaTransferencia.Desfeita
        assertEquals(2, desfeita.lancamentos.size)
        assertEquals(2, ambiente.lancamentos.todos().size)
        assertEquals(2, ambiente.lancamentos.todos().map { it.publicId }.distinct().size)
        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null })
        assertEquals(TipoLancamento.DESPESA, ambiente.lancamentos.buscar("saida")?.tipo)
        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("entrada")?.tipo)
    }

    @Test
    fun `marcar resolve transferencia ambigua dos dois lados`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.TRANSFERENCIA_AMBIGUA,
            ),
        )
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada",
                inter.publicId,
                5000,
                tipo = TipoLancamento.RECEITA,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.TRANSFERENCIA_AMBIGUA,
            ),
        )

        ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(StatusLancamento.CONFIRMADO, ambiente.lancamentos.buscar("saida")?.status)
        assertNull(ambiente.lancamentos.buscar("saida")?.motivoPendencia)
        assertEquals(StatusLancamento.CONFIRMADO, ambiente.lancamentos.buscar("entrada")?.status)
        assertNull(ambiente.lancamentos.buscar("entrada")?.motivoPendencia)
    }

    @Test
    fun `desfazer sem par apenas remove a marca`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val despesa = ambiente.categoria("Alimentação")
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = "transferencia-orfa",
                categoriaPublicId = despesa.publicId,
            ),
        )

        val resultado = ambiente.transferir.desfazer("saida")

        assertTrue(resultado is ResultadoDaTransferencia.Desfeita)
        val desfeito = ambiente.lancamentos.buscar("saida")
        assertNull(desfeito?.transferenciaId)
        assertEquals(TipoLancamento.DESPESA, desfeito?.tipo)
        assertEquals(1, ambiente.lancamentos.todos().size)
    }
}
