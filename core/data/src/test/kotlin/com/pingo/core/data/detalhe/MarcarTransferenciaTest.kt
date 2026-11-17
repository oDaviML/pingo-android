package com.pingo.core.data.detalhe

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

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
    fun `nao marca quando o instrumento do candidato real esta arquivado`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val contaInter = ambiente.conta("Inter", InstituicaoFinanceira.INTER)
        val destino = ambiente.instrumento(contaInter)
        val candidato = ambiente.instrumento(contaInter)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", candidato.publicId, 5000, tipo = TipoLancamento.RECEITA))
        ambiente.instrumentos.salvar(candidato.copy(arquivado = true, atualizadoEm = ambiente.agora))

        val resultado = ambiente.transferir.marcar("saida", destino.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO), resultado)
        assertNull(ambiente.lancamentos.buscar("saida")?.transferenciaId)
        assertNull(ambiente.lancamentos.buscar("entrada")?.transferenciaId)
        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("entrada")?.tipo)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `nao marca quando o instrumento do candidato real foi excluido`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val contaInter = ambiente.conta("Inter", InstituicaoFinanceira.INTER)
        val destino = ambiente.instrumento(contaInter)
        val candidato = ambiente.instrumento(contaInter)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", candidato.publicId, 5000, tipo = TipoLancamento.RECEITA))
        ambiente.instrumentos.excluir(candidato.publicId, ambiente.agora)

        val resultado = ambiente.transferir.marcar("saida", destino.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO), resultado)
        assertNull(ambiente.lancamentos.buscar("saida")?.transferenciaId)
        assertNull(ambiente.lancamentos.buscar("entrada")?.transferenciaId)
    }

    @Test
    fun `nao marca quando o instrumento do candidato nao existe mais`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", "instrumento-sumido", 5000, tipo = TipoLancamento.RECEITA))

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.PAR_NAO_RESOLVIDO), resultado)
        assertNull(ambiente.lancamentos.buscar("saida")?.transferenciaId)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `candidato arquivado de outra conta nao bloqueia a marcacao`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val outro = ambiente.instrumento(ambiente.conta("Itaú", InstituicaoFinanceira.ITAU))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", outro.publicId, 5000, tipo = TipoLancamento.RECEITA))
        ambiente.instrumentos.salvar(outro.copy(arquivado = true, atualizadoEm = ambiente.agora))

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        val marcada = resultado as ResultadoDaTransferencia.Marcada
        assertEquals(inter.publicId, marcada.par?.instrumentoPublicId)
        assertEquals(3, ambiente.lancamentos.todos().size)
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
    fun `desfazer receita sem categoria restaurando pela propria linha preserva a receita`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("receita", nubank.publicId, 5000, tipo = TipoLancamento.RECEITA))

        ambiente.transferir.marcar("receita", inter.publicId)
        ambiente.transferir.desfazer("receita")

        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("receita")?.tipo)
    }

    @Test
    fun `marcar confirma pendentes com motivo diferente de transferencia ambigua sem elevar a confianca`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.BAIXA_CONFIANCA,
            ),
        )
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada",
                inter.publicId,
                5000,
                tipo = TipoLancamento.RECEITA,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.CATEGORIA_DESCONHECIDA,
            ),
        )

        ambiente.transferir.marcar("saida", inter.publicId)

        val saida = ambiente.lancamentos.buscar("saida")
        val entrada = ambiente.lancamentos.buscar("entrada")
        assertEquals(StatusLancamento.CONFIRMADO, saida?.status)
        assertNull(saida?.motivoPendencia)
        assertEquals(0.8, saida?.confianca ?: 0.0, 0.0001)
        assertEquals(StatusLancamento.CONFIRMADO, entrada?.status)
        assertNull(entrada?.motivoPendencia)
        assertEquals(0.8, entrada?.confianca ?: 0.0, 0.0001)
    }

    @Test
    fun `desfazer sem par apenas remove a marca`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = "transferencia-orfa",
                tipoSemTransferencia = TipoLancamento.DESPESA,
            ),
        )

        val resultado = ambiente.transferir.desfazer("saida")

        assertTrue(resultado is ResultadoDaTransferencia.Desfeita)
        val desfeito = ambiente.lancamentos.buscar("saida")
        assertNull(desfeito?.transferenciaId)
        assertNull(desfeito?.tipoSemTransferencia)
        assertEquals(TipoLancamento.DESPESA, desfeito?.tipo)
        assertEquals(1, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `desfazer pelo destino de transferencia manual restaura cada linha pelo metadado`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val transferenciaId = "transferencia-manual"
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = transferenciaId,
                tipoSemTransferencia = TipoLancamento.DESPESA,
            ),
        )
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada",
                inter.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                dataHora = Instant.parse("2026-12-01T15:00:00Z"),
                transferenciaId = transferenciaId,
                tipoSemTransferencia = TipoLancamento.RECEITA,
            ),
        )

        val resultado = ambiente.transferir.desfazer("entrada")

        assertTrue(resultado is ResultadoDaTransferencia.Desfeita)
        assertEquals(TipoLancamento.DESPESA, ambiente.lancamentos.buscar("saida")?.tipo)
        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("entrada")?.tipo)
        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null && it.tipoSemTransferencia == null })
        assertEquals(setOf("saida", "entrada"), ambiente.lancamentos.todos().map { it.publicId }.toSet())
    }

    @Test
    fun `desfazer par legado sem direcao recusa sem escrita parcial`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val transferenciaId = "transferencia-legada"
        ambiente.lancamentos.salvar(
            lancamentoDeTeste("saida", nubank.publicId, 5000, tipo = TipoLancamento.TRANSFERENCIA, transferenciaId = transferenciaId),
        )
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada",
                inter.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = transferenciaId,
                tipoSemTransferencia = TipoLancamento.RECEITA,
            ),
        )

        val resultado = ambiente.transferir.desfazer("saida")

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.DIRECAO_DESCONHECIDA), resultado)
        assertEquals(TipoLancamento.TRANSFERENCIA, ambiente.lancamentos.buscar("saida")?.tipo)
        assertEquals(TipoLancamento.TRANSFERENCIA, ambiente.lancamentos.buscar("entrada")?.tipo)
        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == transferenciaId })
    }

    @Test
    fun `desfazer par com direcoes iguais recusa sem escrita parcial`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        val transferenciaId = "transferencia-inconsistente"
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "saida",
                nubank.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = transferenciaId,
                tipoSemTransferencia = TipoLancamento.DESPESA,
            ),
        )
        ambiente.lancamentos.salvar(
            lancamentoDeTeste(
                "entrada",
                inter.publicId,
                5000,
                tipo = TipoLancamento.TRANSFERENCIA,
                transferenciaId = transferenciaId,
                tipoSemTransferencia = TipoLancamento.DESPESA,
            ),
        )

        val resultado = ambiente.transferir.desfazer("entrada")

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.PAR_INCONSISTENTE), resultado)
        assertEquals(TipoLancamento.TRANSFERENCIA, ambiente.lancamentos.buscar("saida")?.tipo)
        assertEquals(TipoLancamento.TRANSFERENCIA, ambiente.lancamentos.buscar("entrada")?.tipo)
    }

    @Test
    fun `remarcar depois de desfazer usa o tipo atual`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("receita", nubank.publicId, 5000, tipo = TipoLancamento.RECEITA))

        ambiente.transferir.marcar("receita", inter.publicId)
        ambiente.transferir.desfazer("receita")
        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("receita")?.tipo)

        val remarcada = ambiente.transferir.marcar("receita", inter.publicId) as ResultadoDaTransferencia.Marcada

        assertEquals(TipoLancamento.RECEITA, remarcada.lancamento.tipoSemTransferencia)
        assertEquals(TipoLancamento.DESPESA, remarcada.par?.tipoSemTransferencia)
    }

    @Test
    fun `marcar preserva o tipo proprio de cada lado inclusive com categorias ausentes`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val inter = ambiente.instrumento(ambiente.conta("Inter", InstituicaoFinanceira.INTER))
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000, tipo = TipoLancamento.DESPESA))
        ambiente.lancamentos.salvar(lancamentoDeTeste("entrada", inter.publicId, 5000, tipo = TipoLancamento.RECEITA))

        ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(TipoLancamento.DESPESA, ambiente.lancamentos.buscar("saida")?.tipoSemTransferencia)
        assertEquals(TipoLancamento.RECEITA, ambiente.lancamentos.buscar("entrada")?.tipoSemTransferencia)
    }

    @Test
    fun `nao marca quando conta pai do destino esta arquivada`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val contaInter = ambiente.conta("Inter", InstituicaoFinanceira.INTER)
        val inter = ambiente.instrumento(contaInter)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))
        ambiente.contas.arquivar(contaInter.publicId, ambiente.agora)

        val resultado = ambiente.transferir.marcar("saida", inter.publicId)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.DESTINO_NAO_ENCONTRADO), resultado)
        assertNull(ambiente.lancamentos.buscar("saida")?.transferenciaId)
        assertEquals(1, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `corrida com arquivamento antes da marcacao recusa transferencia sem criar par`() = runTest {
        val nubank = ambiente.instrumento(ambiente.conta("Nubank"))
        val contaInter = ambiente.conta("Inter", InstituicaoFinanceira.INTER)
        val inter = ambiente.instrumento(contaInter)
        ambiente.lancamentos.salvar(lancamentoDeTeste("saida", nubank.publicId, 5000))

        val entrouNaBarreira = CompletableDeferred<Unit>()
        val liberarBarreira = CompletableDeferred<Unit>()
        ambiente.transacoes.antesDeIniciar = {
            entrouNaBarreira.complete(Unit)
            liberarBarreira.await()
        }

        val tarefa = async { ambiente.transferir.marcar("saida", inter.publicId) }
        entrouNaBarreira.await()
        ambiente.contas.arquivar(contaInter.publicId, ambiente.agora)
        liberarBarreira.complete(Unit)

        assertEquals(ResultadoDaTransferencia.Recusada(MotivoDaRecusa.DESTINO_NAO_ENCONTRADO), tarefa.await())
        assertEquals(1, ambiente.lancamentos.todos().size)
    }
}
