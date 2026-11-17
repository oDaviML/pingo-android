package com.pingo.core.data.captura

import com.pingo.core.data.categoria.AmbienteCategorias
import com.pingo.core.data.impl.FakeTransacaoLocal
import com.pingo.core.data.impl.ProcessadorDeCapturaImpl
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.HashDuplicata
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessadorDeCapturaTest {
    private val ambiente = AmbienteDeCaptura()

    @Test
    fun `compra completa com categoria do dicionario grava confirmado`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val instrumento = ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val captura = captura()

        val resultado = processador(categorias).processar(captura)

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(1, ambiente.lancamentos.todos().size)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        assertNull(lancamento.motivoPendencia)
        assertEquals(OrigemLancamento.NOTIFICACAO, lancamento.origem)
        assertEquals(InstituicaoFinanceira.ITAU, lancamento.instituicaoOrigem)
        assertEquals(instrumento.publicId, lancamento.instrumentoPublicId)
        assertEquals(categorias.idDe("Outros", TipoCategoria.DESPESA), lancamento.categoriaPublicId)
        assertEquals(3790L, lancamento.valorCentavos)
        assertEquals(TipoLancamento.DESPESA, lancamento.tipo)
        assertEquals("CLARO *31987905690", lancamento.estabelecimento)
        assertEquals(captura.postTime, lancamento.dataHora)
        assertEquals(1.0, lancamento.confianca, 0.0001)
        assertEquals("Olá! Compra aprovada", lancamento.textoBruto)
        assertNull(lancamento.transferenciaId)
        assertEquals(HashDuplicata.de(InstituicaoFinanceira.ITAU, 3790, "claro", captura.postTime), lancamento.hashDuplicata)
        assertEquals(captura.postTime, lancamento.criadoEm)
        assertEquals(captura.postTime, lancamento.atualizadoEm)
    }

    @Test
    fun `data e hora embutidas viram a data do lancamento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura(dataHoraTexto = "10/11 às 00:55"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(Instant.parse("2026-11-10T03:55:00Z"), lancamento.dataHora)
    }

    @Test
    fun `captura sem estabelecimento grava pendente por campos incompletos`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura(estabelecimento = null))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.CAMPOS_INCOMPLETOS.name, lancamento.motivoPendencia)
        assertEquals(0.8, lancamento.confianca, 0.0001)
    }

    @Test
    fun `moeda estrangeira grava pendente com o motivo`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura(moeda = "USD"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.MOEDA_ESTRANGEIRA.name, lancamento.motivoPendencia)
    }

    @Test
    fun `categoria ausente grava pendente por categoria desconhecida`() = runTest {
        val categorias = AmbienteCategorias()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura())

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.CATEGORIA_DESCONHECIDA.name, lancamento.motivoPendencia)
        assertNull(lancamento.categoriaPublicId)
    }

    @Test
    fun `um instrumento sem digitos e usado por padrao`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val instrumento = ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura())

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(instrumento.publicId, lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
    }

    @Test
    fun `digitos escolhem o instrumento correspondente`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU, digitos = "1111")
        val segundo = ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU, digitos = "2222")

        val resultado = processador(categorias).processar(captura(digitos = "2222"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(segundo.publicId, lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
    }

    @Test
    fun `multiplos instrumentos sem digitos gravam pendente por instrumento ambiguo`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)

        val resultado = processador(categorias).processar(captura())

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_AMBIGUO.name, lancamento.motivoPendencia)
        assertNull(lancamento.instrumentoPublicId)
    }

    @Test
    fun `match nao unico dos digitos grava pendente ambiguo sem instrumento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU, digitos = "2222")
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU, digitos = "2222")

        val resultado = processador(categorias).processar(captura(digitos = "2222"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_AMBIGUO.name, lancamento.motivoPendencia)
        assertNull(lancamento.instrumentoPublicId)
    }

    @Test
    fun `digitos sem instrumento correspondente gravam pendente sem instrumento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU, digitos = "1111")

        val resultado = processador(categorias).processar(captura(digitos = "2222"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento.motivoPendencia)
        assertNull(lancamento.instrumentoPublicId)
    }

    @Test
    fun `sem instrumento da instituicao grava pendente com instrumento nulo e origem`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()

        val resultado = processador(categorias).processar(captura())

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento.motivoPendencia)
        assertNull(lancamento.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.ITAU, lancamento.instituicaoOrigem)
        assertEquals(1, ambiente.lancamentos.todos().size)
        assertTrue(ambiente.instrumentos.todos().isEmpty())
    }

    @Test
    fun `instrumento irresolvido tem precedencia sobre moeda estrangeira`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()

        val resultado = processador(categorias).processar(captura(moeda = "USD"))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento.motivoPendencia)
        assertNull(lancamento.instrumentoPublicId)
    }

    @Test
    fun `mesmo hash descarta mesmo com texto bruto diferente`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val processador = processador(categorias)
        val primeira = captura(valorCentavos = 43065, estabelecimento = "DAVI")

        val primeiroResultado = processador.processar(primeira)
        val segunda = primeira.copy(textoBruto = "Outro texto da mesma transação")
        val segundoResultado = processador.processar(segunda)

        assertTrue(primeiroResultado is ResultadoProcessamento.Gravado)
        assertEquals(ResultadoProcessamento.Duplicada, segundoResultado)
        assertEquals(1, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `hash com texto bruto nulo tambem descarta`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val processador = processador(categorias)
        val primeira = captura(valorCentavos = 43065, estabelecimento = "DAVI", textoBruto = null)

        val primeiroResultado = processador.processar(primeira)
        val segundoResultado = processador.processar(primeira.copy(textoBruto = "Texto que chegou depois"))

        assertTrue(primeiroResultado is ResultadoProcessamento.Gravado)
        assertEquals(ResultadoProcessamento.Duplicada, segundoResultado)
        assertEquals(1, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `textos iguais em minutos diferentes sao mantidos`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val processador = processador(categorias)
        val primeira = captura(valorCentavos = 43065, estabelecimento = "DAVI", textoBruto = "Compra aprovada")

        val primeiroResultado = processador.processar(primeira)
        val segunda = primeira.copy(postTime = primeira.postTime.plus(Duration.ofMinutes(7)))
        ambiente.clock.agora = segunda.postTime
        val segundoResultado = processador.processar(segunda)

        assertTrue(primeiroResultado is ResultadoProcessamento.Gravado)
        assertTrue(segundoResultado is ResultadoProcessamento.Gravado)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `pagamentos no mesmo segundo com valores diferentes nao sao duplicatas`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.MERCADO_PAGO)
        val processador = processador(categorias)

        val uber = processador.processar(
            captura(instituicao = InstituicaoFinanceira.MERCADO_PAGO, valorCentavos = 489, estabelecimento = "Uber"),
        )
        val uberMaior =
            processador.processar(captura(instituicao = InstituicaoFinanceira.MERCADO_PAGO, valorCentavos = 1295, estabelecimento = "Uber"))

        assertTrue(uber is ResultadoProcessamento.Gravado)
        assertTrue(uberMaior is ResultadoProcessamento.Gravado)
        assertEquals(2, ambiente.lancamentos.todos().size)
    }

    @Test
    fun `despesa e receita de mesmo valor em contas diferentes viram transferencia`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val contaDoPix = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        ambiente.instrumentos.criarNaConta(contaDoPix)
        ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val processador = processador(categorias)

        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
                postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(3)),
            ),
        )

        val lancamentos = ambiente.lancamentos.todos()
        assertEquals(2, lancamentos.size)
        assertEquals(1, lancamentos.mapNotNull(Lancamento::transferenciaId).distinct().size)
        assertNotNull(lancamentos.first().transferenciaId)
        assertTrue(lancamentos.all { it.tipo == TipoLancamento.TRANSFERENCIA })
        assertTrue(lancamentos.all { it.status == StatusLancamento.CONFIRMADO })
        assertEquals(
            TipoLancamento.RECEITA,
            lancamentos.first { it.instituicaoOrigem == InstituicaoFinanceira.NUBANK }.tipoSemTransferencia,
        )
        assertEquals(
            TipoLancamento.DESPESA,
            lancamentos.first { it.instituicaoOrigem == InstituicaoFinanceira.ITAU }.tipoSemTransferencia,
        )
    }

    @Test
    fun `despesa chegando antes da receita tambem preserva as direcoes do par`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val contaDoPix = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        ambiente.instrumentos.criarNaConta(contaDoPix)
        ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val processador = processador(categorias)

        processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
                postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(3)),
            ),
        )

        val lancamentos = ambiente.lancamentos.todos()
        assertEquals(2, lancamentos.size)
        assertTrue(lancamentos.all { it.transferenciaId != null })
        assertEquals(
            TipoLancamento.DESPESA,
            lancamentos.first { it.instituicaoOrigem == InstituicaoFinanceira.ITAU }.tipoSemTransferencia,
        )
        assertEquals(
            TipoLancamento.RECEITA,
            lancamentos.first { it.instituicaoOrigem == InstituicaoFinanceira.NUBANK }.tipoSemTransferencia,
        )
    }

    @Test
    fun `mais de um candidato grava pendente por transferencia ambigua`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        ambiente.instrumentos.criar(InstituicaoFinanceira.INTER)
        val processador = processador(categorias)

        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 8000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.INTER,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.PIX_RECEBIDO,
                valorCentavos = 8000,
                estabelecimento = "DAVI",
            ),
        )
        val resultado = processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 8000,
                estabelecimento = "DAVI",
                postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(1)),
            ),
        )

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.TRANSFERENCIA_AMBIGUA.name, lancamento.motivoPendencia)
        assertNull(lancamento.transferenciaId)
        assertEquals(2, ambiente.lancamentos.todos().count { it.transferenciaId == null && it.tipo == TipoLancamento.RECEITA })
    }

    @Test
    fun `lancamentos fora da janela de quinze minutos nao formam transferencia`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val processador = processador(categorias)

        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
                postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(16)),
            ),
        )

        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null })
    }

    @Test
    fun `captura sem instrumento nao forma transferencia nem altera o parceiro`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val processador = processador(categorias)

        val recebida = processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        val enviada = processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )

        val parceiro = (recebida as ResultadoProcessamento.Gravado).lancamento
        val despesa = (enviada as ResultadoProcessamento.Gravado).lancamento

        assertNull(despesa.instrumentoPublicId)
        assertNull(despesa.transferenciaId)
        assertEquals(InstituicaoFinanceira.ITAU, despesa.instituicaoOrigem)
        assertEquals(StatusLancamento.PENDENTE, despesa.status)

        val parceiroPersistido = ambiente.lancamentos.todos().first { lancamento -> lancamento.publicId == parceiro.publicId }

        assertNull(parceiroPersistido.transferenciaId)
        assertEquals(TipoLancamento.RECEITA, parceiroPersistido.tipo)
        assertEquals(StatusLancamento.CONFIRMADO, parceiroPersistido.status)
    }

    @Test
    fun `candidato sem instrumento nao entra no pareamento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val processador = processador(categorias)

        processador.processar(
            captura(
                instituicao = InstituicaoFinanceira.NUBANK,
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )

        val lancamentos = ambiente.lancamentos.todos()

        assertTrue(lancamentos.all { lancamento -> lancamento.transferenciaId == null })
        assertTrue(lancamentos.none { lancamento -> lancamento.tipo == TipoLancamento.TRANSFERENCIA })
        val semInstrumento = lancamentos.single { lancamento -> lancamento.instituicaoOrigem == InstituicaoFinanceira.NUBANK }
        assertNull(semInstrumento.instrumentoPublicId)
    }

    @Test
    fun `receita e despesa na mesma conta nao formam transferencia`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val conta = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        ambiente.instrumentos.criarNaConta(conta, apelido = "Cartão")
        ambiente.instrumentos.criarNaConta(conta, apelido = "Conta")
        val processador = processador(categorias)

        processador.processar(
            captura(
                direcao = DirecaoPadrao.RECEITA,
                tipo = TipoLancamentoPadrao.PIX_RECEBIDO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )
        processador.processar(
            captura(
                direcao = DirecaoPadrao.DESPESA,
                tipo = TipoLancamentoPadrao.PIX_ENVIADO,
                valorCentavos = 15000,
                estabelecimento = "DAVI",
            ),
        )

        assertTrue(ambiente.lancamentos.todos().all { it.transferenciaId == null })
    }

    @Test
    fun `regra aprendida e aplicada ao proximo lancamento do mesmo estabelecimento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.ITAU)
        val processador = processador(categorias)

        val primeiro = processador.processar(captura(valorCentavos = 4290, estabelecimento = "Padaria do Zé"))
        categorias.aprendida.aprender("Padaria do Zé", categorias.idDe("Alimentação"), AGORA_DA_CAPTURA)
        val segundo = processador.processar(
            captura(valorCentavos = 5190, estabelecimento = "Padaria do Zé", postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(20))),
        )

        val lancamento = (segundo as ResultadoProcessamento.Gravado).lancamento
        assertTrue(primeiro is ResultadoProcessamento.Gravado)
        assertEquals(categorias.idDe("Alimentação"), lancamento.categoriaPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
    }

    private fun TestScope.processador(categorias: AmbienteCategorias): ProcessadorDeCaptura = ProcessadorDeCapturaImpl(
        lancamentoRepository = ambiente.lancamentos,
        instrumentoRepository = ambiente.instrumentos,
        categoriaRepository = categorias.repositorio,
        categoriaAprendida = categorias.aprendida,
        transacaoLocal = ambiente.transacoes,
        clock = ambiente.clock,
        dispatchers = FakeAppDispatchers(StandardTestDispatcher(testScheduler)),
    )

    @Test
    fun `instituicao outra nunca recebe associacao automatica de captura`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        ambiente.instrumentos.criar(InstituicaoFinanceira.OUTRA)

        val resultado = processador(categorias).processar(captura(instituicao = InstituicaoFinanceira.OUTRA))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertNull(lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento.motivoPendencia)
    }

    @Test
    fun `captura com conta pai arquivada mesmo sem flag no instrumento grava pendente sem vinculo`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val conta = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        ambiente.instrumentos.criarNaConta(conta)
        ambiente.contas.arquivar(conta.publicId, AGORA_DA_CAPTURA)

        val resultado = processador(categorias).processar(captura(instituicao = InstituicaoFinanceira.ITAU))

        val lancamento = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertNull(lancamento.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, lancamento.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, lancamento.motivoPendencia)
    }

    @Test
    fun `corrida com arquivamento antes da transacao grava captura pendente sem vincular instrumento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val conta = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        ambiente.instrumentos.criarNaConta(conta)
        val receita = ambiente.instrumentos.criar(InstituicaoFinanceira.NUBANK)
        val processador = processador(categorias)
        processador.processar(capturaPixRecebido())

        val entrouNaBarreira = CompletableDeferred<Unit>()
        val liberarBarreira = CompletableDeferred<Unit>()
        ambiente.transacoes.antesDeIniciar = {
            entrouNaBarreira.complete(Unit)
            liberarBarreira.await()
        }

        val tentativa = async { processador.processar(capturaPixEnviado()) }
        entrouNaBarreira.await()
        ambiente.contas.arquivar(conta.publicId, AGORA_DA_CAPTURA)
        liberarBarreira.complete(Unit)

        val gravado = (tentativa.await() as ResultadoProcessamento.Gravado).lancamento
        assertNull(gravado.instrumentoPublicId)
        assertNull(gravado.transferenciaId)
        assertEquals(StatusLancamento.PENDENTE, gravado.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, gravado.motivoPendencia)
        val parceiro = ambiente.lancamentos.todos().first { it.instrumentoPublicId == receita.publicId }
        assertNull(parceiro.transferenciaId)
    }

    private fun capturaPixRecebido(): CapturaParaProcessar = captura(
        instituicao = InstituicaoFinanceira.NUBANK,
        direcao = DirecaoPadrao.RECEITA,
        tipo = TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA,
        valorCentavos = 15000,
        estabelecimento = "DAVI",
    )

    private fun capturaPixEnviado(): CapturaParaProcessar = captura(
        instituicao = InstituicaoFinanceira.ITAU,
        direcao = DirecaoPadrao.DESPESA,
        tipo = TipoLancamentoPadrao.PIX_ENVIADO,
        valorCentavos = 15000,
        estabelecimento = "DAVI",
        postTime = AGORA_DA_CAPTURA.plus(Duration.ofMinutes(2)),
    )

    @Test
    fun `corrida em que captura comita primeiro preserva vinculo historico apos arquivamento`() = runTest {
        val categorias = AmbienteCategorias().comPadroes()
        val conta = ambiente.contas.criar(InstituicaoFinanceira.ITAU, "Itaú")
        val cartao = ambiente.instrumentos.criarNaConta(conta)
        val gravou = CompletableDeferred<Unit>()
        ambiente.transacoes.depoisDeCommitar = { gravou.complete(Unit) }

        val arquivo = launch {
            gravou.await()
            ambiente.contas.arquivar(conta.publicId, AGORA_DA_CAPTURA.plusSeconds(10))
        }
        val resultado = processador(categorias).processar(captura(instituicao = InstituicaoFinanceira.ITAU))
        arquivo.join()

        val gravado = (resultado as ResultadoProcessamento.Gravado).lancamento
        assertEquals(cartao.publicId, gravado.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, gravado.status)
        assertTrue(ambiente.contas.porId(conta.publicId)?.arquivada == true)
    }

    private class AmbienteDeCaptura {
        val contas = FakeContas()
        val instrumentos = FakeInstrumentos(contas)
        val lancamentos = FakeLancamentos(instrumentos)
        val transacoes = FakeTransacaoLocal()
        val clock = MutableClock(AGORA_DA_CAPTURA, FUSO_DA_CAPTURA)
    }

    @Suppress("LongParameterList")
    private fun captura(
        instituicao: InstituicaoFinanceira = InstituicaoFinanceira.ITAU,
        valorCentavos: Long = 3790,
        tipo: TipoLancamentoPadrao = TipoLancamentoPadrao.COMPRA,
        direcao: DirecaoPadrao = DirecaoPadrao.DESPESA,
        estabelecimento: String? = "CLARO *31987905690",
        digitos: String? = null,
        moeda: String = "BRL",
        dataHoraTexto: String? = null,
        postTime: Instant = AGORA_DA_CAPTURA,
        textoBruto: String? = "Olá! Compra aprovada",
    ): CapturaParaProcessar {
        ambiente.clock.agora = postTime

        return CapturaParaProcessar(
            instituicao = instituicao,
            transacao = TransacaoExtraida(
                padraoId = "padrao-de-teste",
                tipo = tipo,
                direcao = direcao,
                valorCentavos = valorCentavos,
                estabelecimento = estabelecimento,
                digitos = digitos,
                moeda = moeda,
                dataHoraTexto = dataHoraTexto,
            ),
            postTime = postTime,
            textoBruto = textoBruto,
        )
    }
}
