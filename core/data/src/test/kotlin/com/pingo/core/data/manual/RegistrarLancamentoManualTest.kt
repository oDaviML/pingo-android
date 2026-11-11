package com.pingo.core.data.manual

import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RegistrarLancamentoManualTest {
    @Test
    fun `despesa valida grava lancamento manual confirmado com categoria e instrumento`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar("Nubank crédito")

        val resultado = ambiente.registrar.registrar(
            entrada(
                valorCentavos = 4290,
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = "cat-alimentacao",
                estabelecimento = "  Padaria do Zé  ",
            ),
        )

        val lancamento = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.single()
        assertEquals(4290, lancamento.valorCentavos)
        assertEquals(TipoLancamento.DESPESA, lancamento.tipo)
        assertEquals(OrigemLancamento.MANUAL, lancamento.origem)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertNull(lancamento.motivoPendencia)
        assertEquals(instrumento.publicId, lancamento.instrumentoPublicId)
        assertEquals("cat-alimentacao", lancamento.categoriaPublicId)
        assertEquals("Padaria do Zé", lancamento.estabelecimento)
        assertEquals(AGORA_MANUAL, lancamento.criadoEm)
        assertEquals(listOf(lancamento), ambiente.lancamentos.todos())
    }

    @Test
    fun `valor zero bloqueia o salvamento com erro no campo valor`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 0, instrumentoPublicId = instrumento.publicId))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.VALOR)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `valor negativo bloqueia o salvamento com erro no campo valor`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = -100, instrumentoPublicId = instrumento.publicId))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.VALOR)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `despesa sem instrumento bloqueia o salvamento`() = runBlocking {
        val ambiente = AmbienteManual()
        ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 1000, instrumentoPublicId = null))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `instrumento arquivado nao vale como escolha manual`() = runBlocking {
        val ambiente = AmbienteManual()
        val arquivado = ambiente.instrumentos.criar("Cartão antigo", arquivado = true)

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 1000, instrumentoPublicId = arquivado.publicId))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `instrumento inexistente bloqueia o salvamento`() = runBlocking {
        val ambiente = AmbienteManual()
        ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 1000, instrumentoPublicId = "instrumento-fantasma"))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `sem nenhum instrumento cadastrado bloqueia sem criar instrumento ficticio`() = runBlocking {
        val ambiente = AmbienteManual()

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 1000, instrumentoPublicId = null))

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.INSTRUMENTO)), resultado)
        assertTrue(ambiente.instrumentos.todos().isEmpty())
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `tipo ajuste nao pode ser criado manualmente`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(
            entrada(valorCentavos = 1000, instrumentoPublicId = instrumento.publicId, tipo = TipoLancamento.AJUSTE),
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.TIPO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `transferencia com origem igual ao destino bloqueia com erro no destino`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(
            entrada(
                tipo = TipoLancamento.TRANSFERENCIA,
                valorCentavos = 50000,
                instrumentoPublicId = instrumento.publicId,
                destinoPublicId = instrumento.publicId,
            ),
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.DESTINO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `transferencia sem destino resolvido bloqueia o salvamento`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()

        val resultado = ambiente.registrar.registrar(
            entrada(
                tipo = TipoLancamento.TRANSFERENCIA,
                valorCentavos = 50000,
                instrumentoPublicId = instrumento.publicId,
                destinoPublicId = null,
            ),
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.DESTINO)), resultado)
        assertTrue(ambiente.lancamentos.todos().isEmpty())
    }

    @Test
    fun `transferencia valida grava o par saida e entrada com o mesmo identificador`() = runBlocking {
        val ambiente = AmbienteManual()
        val origem = ambiente.instrumentos.criar("Inter conta", contaPublicId = "conta-inter")
        val destino = ambiente.instrumentos.criar("Mercado Pago conta", contaPublicId = "conta-mp")

        val resultado = ambiente.registrar.registrar(
            entrada(
                tipo = TipoLancamento.TRANSFERENCIA,
                valorCentavos = 50000,
                instrumentoPublicId = origem.publicId,
                destinoPublicId = destino.publicId,
                estabelecimento = "Apartamento",
            ),
        )

        val par = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        assertEquals(2, par.size)
        assertEquals(2, ambiente.lancamentos.todos().size)
        assertEquals(1, ambiente.transacao.execucoes)
        assertEquals(origem.publicId, par[0].instrumentoPublicId)
        assertEquals(destino.publicId, par[1].instrumentoPublicId)
        assertEquals(par[0].transferenciaId, par[1].transferenciaId)
        assertNotEquals(null, par[0].transferenciaId)
        par.forEach { lancamento ->
            assertEquals(TipoLancamento.TRANSFERENCIA, lancamento.tipo)
            assertEquals(50000, lancamento.valorCentavos)
            assertEquals(OrigemLancamento.MANUAL, lancamento.origem)
            assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
            assertEquals("Apartamento", lancamento.estabelecimento)
        }
        assertEquals(TipoLancamento.DESPESA, par[0].tipoSemTransferencia)
        assertEquals(TipoLancamento.RECEITA, par[1].tipoSemTransferencia)
        assertNotEquals(par[0].publicId, par[1].publicId)
    }

    @Test
    fun `transferencia com capturado so na origem grava apenas a saida pendente`() = runBlocking {
        val ambiente = AmbienteManual()
        val origem = ambiente.instrumentos.criar("Inter conta", contaPublicId = "conta-inter")
        val destino = ambiente.instrumentos.criar("Mercado Pago conta", contaPublicId = "conta-mp")
        ambiente.lancamentos.salvar(
            capturado(origem.publicId, valorCentavos = 50000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1))),
        )

        val resultado = ambiente.registrar.registrar(transferencia(origem.publicId, destino.publicId, valorCentavos = 50000))

        val par = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.associateBy { it.instrumentoPublicId }
        assertEquals(StatusLancamento.PENDENTE, par.getValue(origem.publicId).status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, par.getValue(origem.publicId).motivoPendencia)
        assertEquals(StatusLancamento.CONFIRMADO, par.getValue(destino.publicId).status)
        assertNull(par.getValue(destino.publicId).motivoPendencia)
    }

    @Test
    fun `transferencia com capturado so no destino grava apenas a entrada pendente`() = runBlocking {
        val ambiente = AmbienteManual()
        val origem = ambiente.instrumentos.criar("Inter conta", contaPublicId = "conta-inter")
        val destino = ambiente.instrumentos.criar("Mercado Pago conta", contaPublicId = "conta-mp")
        ambiente.lancamentos.salvar(
            capturado(destino.publicId, valorCentavos = 50000, dataHora = AGORA_MANUAL.plus(Duration.ofHours(2))),
        )

        val resultado = ambiente.registrar.registrar(transferencia(origem.publicId, destino.publicId, valorCentavos = 50000))

        val par = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.associateBy { it.instrumentoPublicId }
        assertEquals(StatusLancamento.CONFIRMADO, par.getValue(origem.publicId).status)
        assertNull(par.getValue(origem.publicId).motivoPendencia)
        assertEquals(StatusLancamento.PENDENTE, par.getValue(destino.publicId).status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, par.getValue(destino.publicId).motivoPendencia)
    }

    @Test
    fun `transferencia com capturado na origem e no destino grava as duas linhas pendentes`() = runBlocking {
        val ambiente = AmbienteManual()
        val origem = ambiente.instrumentos.criar("Inter conta", contaPublicId = "conta-inter")
        val destino = ambiente.instrumentos.criar("Mercado Pago conta", contaPublicId = "conta-mp")
        val capturadoNaOrigem = capturado(origem.publicId, valorCentavos = 50000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1)))
        val capturadoNoDestino = capturado(destino.publicId, valorCentavos = 50000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(2)))
        ambiente.lancamentos.salvar(capturadoNaOrigem)
        ambiente.lancamentos.salvar(capturadoNoDestino)

        val resultado = ambiente.registrar.registrar(transferencia(origem.publicId, destino.publicId, valorCentavos = 50000))

        val par = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        assertEquals(2, par.size)
        assertEquals(1, par.map { it.transferenciaId }.distinct().size)
        assertNotEquals(null, par.first().transferenciaId)
        assertEquals(2, par.map { it.publicId }.distinct().size)
        par.forEach { lancamento ->
            assertEquals(TipoLancamento.TRANSFERENCIA, lancamento.tipo)
            assertNull(lancamento.categoriaPublicId)
            assertEquals(StatusLancamento.PENDENTE, lancamento.status)
            assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, lancamento.motivoPendencia)
        }
        assertEquals(setOf(origem.publicId, destino.publicId), par.map { it.instrumentoPublicId }.toSet())
        assertEquals(capturadoNaOrigem, ambiente.lancamentos.buscar(capturadoNaOrigem.publicId))
        assertEquals(capturadoNoDestino, ambiente.lancamentos.buscar(capturadoNoDestino.publicId))
    }

    @Test
    fun `transferencia sem capturado nos instrumentos do par grava o par confirmado`() = runBlocking {
        val ambiente = AmbienteManual()
        val origem = ambiente.instrumentos.criar("Inter conta", contaPublicId = "conta-inter")
        val destino = ambiente.instrumentos.criar("Mercado Pago conta", contaPublicId = "conta-mp")
        val terceiro = ambiente.instrumentos.criar("Nubank cartão", contaPublicId = "conta-nu")
        ambiente.lancamentos.salvar(
            capturado(terceiro.publicId, valorCentavos = 50000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1))),
        )

        val resultado = ambiente.registrar.registrar(transferencia(origem.publicId, destino.publicId, valorCentavos = 50000))

        val par = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        par.forEach { lancamento ->
            assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
            assertNull(lancamento.motivoPendencia)
        }
        assertEquals(1, ambiente.transacao.execucoes)
    }

    @Test
    fun `manual parecido com capturado em menos de vinte e quatro horas grava pendente possivel duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        val capturado = capturado(instrumento.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.minus(Duration.ofHours(2)))
        ambiente.lancamentos.salvar(capturado)

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        val lancamento = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.single()
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, lancamento.motivoPendencia)
        assertEquals(StatusLancamento.CONFIRMADO, ambiente.lancamentos.buscar(capturado.publicId)?.status)
        assertEquals(OrigemLancamento.NOTIFICACAO, ambiente.lancamentos.buscar(capturado.publicId)?.origem)
        assertEquals(capturado, ambiente.lancamentos.buscar(capturado.publicId))
    }

    @Test
    fun `capturado com mais de vinte e quatro horas nao marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        ambiente.lancamentos.salvar(
            capturado(instrumento.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.minus(Duration.ofHours(25))),
        )

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        val lancamento = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.single()
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertNull(lancamento.motivoPendencia)
    }

    @Test
    fun `capturado exatamente vinte e quatro horas antes marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        ambiente.lancamentos.salvar(
            capturado(instrumento.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.minus(Duration.ofHours(24))),
        )

        val lancamento = salvoUnico(ambiente, entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, lancamento.motivoPendencia)
    }

    @Test
    fun `capturado exatamente vinte e quatro horas depois marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        ambiente.lancamentos.salvar(
            capturado(instrumento.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.plus(Duration.ofHours(24))),
        )

        val lancamento = salvoUnico(ambiente, entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, lancamento.motivoPendencia)
    }

    @Test
    fun `capturado um milissegundo antes do limite nao marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        val foraDaJanela = AGORA_MANUAL.minus(Duration.ofHours(24)).minusMillis(1)
        ambiente.lancamentos.salvar(capturado(instrumento.publicId, valorCentavos = 4290, dataHora = foraDaJanela))

        val lancamento = salvoUnico(ambiente, entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertNull(lancamento.motivoPendencia)
    }

    @Test
    fun `capturado um milissegundo depois do limite nao marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        val foraDaJanela = AGORA_MANUAL.plus(Duration.ofHours(24)).plusMillis(1)
        ambiente.lancamentos.salvar(capturado(instrumento.publicId, valorCentavos = 4290, dataHora = foraDaJanela))

        val lancamento = salvoUnico(ambiente, entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertNull(lancamento.motivoPendencia)
    }

    @Test
    fun `capturado de mesmo valor em outro instrumento nao marca duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val escolhido = ambiente.instrumentos.criar("Cartão A")
        val outro = ambiente.instrumentos.criar("Cartão B")
        ambiente.lancamentos.salvar(
            capturado(outro.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1))),
        )

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 4290, instrumentoPublicId = escolhido.publicId))

        val lancamento = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.single()
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
    }

    @Test
    fun `manual anterior de mesmo valor e instrumento nao conta como duplicata`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar()
        ambiente.lancamentos.salvar(
            capturado(instrumento.publicId, valorCentavos = 4290, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1)))
                .copy(origem = OrigemLancamento.MANUAL),
        )

        val resultado = ambiente.registrar.registrar(entrada(valorCentavos = 4290, instrumentoPublicId = instrumento.publicId))

        val lancamento = (resultado as ResultadoLancamentoManual.Salvo).lancamentos.single()
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
    }

    @Test
    fun `ultimo instrumento usado ignora captura sem instrumento resolvido`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar("Nubank crédito")
        val outro = ambiente.instrumentos.criar("Inter débito")
        ambiente.lancamentos.salvar(
            capturado(instrumento.publicId, valorCentavos = 1000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(5))),
        )
        ambiente.lancamentos.salvar(
            capturado(null, valorCentavos = 2000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1))),
        )
        ambiente.instrumentos.criar("Caju voucher")

        assertEquals(instrumento.publicId, ambiente.registrar.ultimoInstrumentoUsado())
        assertNotEquals(outro.publicId, ambiente.registrar.ultimoInstrumentoUsado())
    }

    @Test
    fun `unico instrumento cadastrado vira default sem historico`() = runBlocking {
        val ambiente = AmbienteManual()
        val instrumento = ambiente.instrumentos.criar("Nubank crédito")

        assertEquals(instrumento.publicId, ambiente.registrar.ultimoInstrumentoUsado())
    }

    @Test
    fun `sem historico e com varios instrumentos nao inventa default`() = runBlocking {
        val ambiente = AmbienteManual()
        ambiente.instrumentos.criar("Nubank crédito")
        ambiente.instrumentos.criar("Inter débito")

        assertNull(ambiente.registrar.ultimoInstrumentoUsado())
    }

    @Test
    fun `ultimo instrumento arquivado cede lugar ao anterior ativo`() = runBlocking {
        val ambiente = AmbienteManual()
        val antigo = ambiente.instrumentos.criar("Nubank crédito")
        val arquivado = ambiente.instrumentos.criar("Inter antigo", arquivado = true)
        ambiente.lancamentos.salvar(capturado(antigo.publicId, valorCentavos = 1000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(6))))
        ambiente.lancamentos.salvar(
            capturado(arquivado.publicId, valorCentavos = 2000, dataHora = AGORA_MANUAL.minus(Duration.ofHours(1))),
        )

        assertEquals(antigo.publicId, ambiente.registrar.ultimoInstrumentoUsado())
    }

    @Test
    fun `sem instrumentos cadastrados nao ha default`() = runBlocking {
        val ambiente = AmbienteManual()

        assertNull(ambiente.registrar.ultimoInstrumentoUsado())
    }

    private fun transferencia(origemPublicId: String, destinoPublicId: String, valorCentavos: Long): NovoLancamentoManual = entrada(
        tipo = TipoLancamento.TRANSFERENCIA,
        valorCentavos = valorCentavos,
        instrumentoPublicId = origemPublicId,
        destinoPublicId = destinoPublicId,
        estabelecimento = "Apartamento",
    )

    private suspend fun salvoUnico(ambiente: AmbienteManual, novo: NovoLancamentoManual): Lancamento =
        (ambiente.registrar.registrar(novo) as ResultadoLancamentoManual.Salvo).lancamentos.single()

    private fun entrada(
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        valorCentavos: Long,
        instrumentoPublicId: String?,
        destinoPublicId: String? = null,
        categoriaPublicId: String? = null,
        estabelecimento: String? = null,
    ): NovoLancamentoManual = NovoLancamentoManual(
        tipo = tipo,
        valorCentavos = valorCentavos,
        instrumentoPublicId = instrumentoPublicId,
        destinoPublicId = destinoPublicId,
        categoriaPublicId = categoriaPublicId,
        dataHora = AGORA_MANUAL,
        estabelecimento = estabelecimento,
    )

    private fun capturado(instrumentoPublicId: String?, valorCentavos: Long, dataHora: Instant): Lancamento = Lancamento(
        publicId = "capturado-$valorCentavos-${dataHora.epochSecond}",
        instrumentoPublicId = instrumentoPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Estabelecimento",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
    )
}
