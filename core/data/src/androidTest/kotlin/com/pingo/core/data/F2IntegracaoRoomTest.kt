package com.pingo.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.data.detalhe.EdicaoDoLancamento
import com.pingo.core.data.fatura.ResultadoCompletarDatasCartao
import com.pingo.core.data.manual.NovoLancamentoManual
import com.pingo.core.data.manual.ResultadoLancamentoManual
import com.pingo.core.data.voucher.ResultadoAjusteVoucher
import com.pingo.core.data.voucher.ValorAjusteSaldoVoucher
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.InstrumentoEditado
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.relatorio.VariacaoCategoria
import com.pingo.core.model.voucher.DecisaoRecargaVoucher
import com.pingo.core.model.voucher.ResultadoResolucaoRecarga
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class F2IntegracaoRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var ambiente: BancoRoomDeTeste

    @Before
    fun setUp() = runBlocking {
        ambiente = BancoRoomDeTeste(context, "f2-integracao-test.db")
        ambiente.semearCategoriasF2()
    }

    @After
    fun tearDown() {
        ambiente.fechar()
    }

    @Test
    fun indicadoresECiclosDeCreditoEDebitoRespeitamCalendarioSaoPaulo() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 2, 10, 12)
        val cadastro = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Roxinho", "1234", fechamentoDia = 31, vencimentoDia = 5),
                        NovoInstrumento(TipoInstrumento.DEBITO, "Débito"),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val creditoId = cadastro.instrumentos.first { it.tipo == TipoInstrumento.CREDITO }.publicId
        val debitoId = cadastro.instrumentos.first { it.tipo == TipoInstrumento.DEBITO }.publicId

        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                instrumentoPublicId = creditoId,
                valorCentavos = 10000L,
                dataHora = saoPaulo(2026, 1, 30, 23, 59),
                estabelecimento = "Mercado Jan",
                categoriaPublicId = "cat-outros-despesa",
            ),
        )
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                instrumentoPublicId = creditoId,
                valorCentavos = 25000L,
                dataHora = saoPaulo(2026, 1, 31, 0, 1),
                estabelecimento = "Farmacia Fev",
                categoriaPublicId = "cat-outros-despesa",
            ),
        )
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                instrumentoPublicId = debitoId,
                valorCentavos = 8000L,
                dataHora = saoPaulo(2026, 2, 5, 10),
                estabelecimento = "Padaria",
                categoriaPublicId = "cat-outros-despesa",
            ),
        )

        val faturas = ambiente.contasProjectionF2().observeFaturasDoCartao(creditoId).first()
        assertNotNull(faturas)
        assertFalse(requireNotNull(faturas).configuracaoIncompleta)
        val fechadaJan = faturas.ciclos.first { it.competencia == LocalDate.of(2026, 1, 1) }
        val abertaFev = faturas.ciclos.first { it.competencia == LocalDate.of(2026, 2, 1) }
        assertEquals(TipoCicloFatura.FECHADA, fechadaJan.tipoCiclo)
        assertEquals(10000L, fechadaJan.totalCentavos)
        assertEquals(TipoCicloFatura.ABERTA, abertaFev.tipoCiclo)
        assertEquals(LocalDate.of(2026, 2, 28), abertaFev.dataFechamento)
        assertEquals(25000L, abertaFev.totalCentavos)

        val resumo = ambiente.contasProjectionF2().observeContas().first()
        val debito = resumo.contasAtivas.single().instrumentos.filterIsInstance<IndicadorDeInstrumento.Debito>().single()
        assertEquals(8000L, debito.gastoDoMesCentavos)
    }

    @Test
    fun editarFechamentoPreservaHistoricoELegadoSemDatasCompletaSemDescartar() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 10, 1, 12)
        val criada = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.ITAU,
                    apelido = "Itau",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Click", "9999", fechamentoDia = 5, vencimentoDia = 12),
                        NovoInstrumento(TipoInstrumento.CREDITO, "Legado", "0001"),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val click = criada.instrumentos.first { it.apelido == "Click" }
        val legado = criada.instrumentos.first { it.apelido == "Legado" }

        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                instrumentoPublicId = click.publicId,
                valorCentavos = 12000L,
                dataHora = saoPaulo(2026, 10, 3, 15),
                estabelecimento = "Restaurante",
                categoriaPublicId = "cat-outros-despesa",
            ),
        )
        ambiente.relogio.instante = saoPaulo(2026, 11, 2, 10)
        ambiente.contasF2().salvarCadastro(
            ContaEditada(
                publicId = criada.conta.publicId,
                apelido = "Itau",
                tipo = TipoConta.CORRENTE,
                instrumentos = listOf(
                    InstrumentoEditado(
                        tipo = TipoInstrumento.CREDITO,
                        apelido = "Click",
                        publicId = click.publicId,
                        ultimosDigitos = "9999",
                        fechamentoDia = 15,
                        vencimentoDia = 22,
                    ),
                    InstrumentoEditado(
                        tipo = TipoInstrumento.CREDITO,
                        apelido = "Legado",
                        publicId = legado.publicId,
                        ultimosDigitos = "0001",
                    ),
                ),
            ),
            ambiente.relogio.instante,
        )
        ambiente.processadorF2().processar(
            CapturaParaProcessar(
                instituicao = InstituicaoFinanceira.ITAU,
                postTime = saoPaulo(2026, 10, 4, 9),
                textoBruto = "Compra R$ 30,00 final 9999",
                transacao = TransacaoExtraida(
                    padraoId = "itau-compra",
                    tipo = TipoLancamentoPadrao.COMPRA,
                    direcao = DirecaoPadrao.DESPESA,
                    valorCentavos = 3000L,
                    estabelecimento = "Cafe",
                    digitos = "9999",
                ),
            ),
        )

        val faturasClick = ambiente.contasProjectionF2().observeFaturasDoCartao(click.publicId).first()
        val faturaOut = requireNotNull(faturasClick).ciclos.first { it.competencia == LocalDate.of(2026, 10, 1) }
        assertEquals(LocalDate.of(2026, 10, 5), faturaOut.dataFechamento)
        assertEquals(15000L, faturaOut.totalCentavos)

        val salvoLegado = (
            ambiente.manualF2().registrar(
                NovoLancamentoManual(
                    tipo = TipoLancamento.DESPESA,
                    instrumentoPublicId = legado.publicId,
                    valorCentavos = 7000L,
                    dataHora = saoPaulo(2026, 11, 1, 10),
                    estabelecimento = "Livro",
                    categoriaPublicId = "cat-outros-despesa",
                ),
            ) as ResultadoLancamentoManual.Salvo
            ).lancamentos.single()
        assertNull(salvoLegado.faturaPublicId)

        val resumoAntes = ambiente.contasProjectionF2().observeContas().first()
        val legadoIndicador = resumoAntes.contasAtivas.single().instrumentos
            .filterIsInstance<IndicadorDeInstrumento.Credito>()
            .first { it.instrumento.publicId == legado.publicId }
        assertTrue(legadoIndicador.estadoFatura is EstadoFaturaCartao.ConfiguracaoIncompleta)

        assertTrue(ambiente.completarDatasF2()(legado.publicId, 10, 17) is ResultadoCompletarDatasCartao.Sucesso)
        val legadoAtualizado = ambiente.lancamentosF2.buscar(salvoLegado.publicId)
        assertNotNull(legadoAtualizado?.faturaPublicId)
    }

    @Test
    fun transferenciaExclusaoEDesfazerReconciliamFaturaSemDuplicar() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 11, 2, 12)
        val conta = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.DEBITO, "Corrente"),
                        NovoInstrumento(TipoInstrumento.CREDITO, "Cartao", "4444", fechamentoDia = 10, vencimentoDia = 18),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val debito = conta.instrumentos.first { it.tipo == TipoInstrumento.DEBITO }
        val credito = conta.instrumentos.first { it.tipo == TipoInstrumento.CREDITO }

        val compra = (
            ambiente.manualF2().registrar(
                NovoLancamentoManual(
                    tipo = TipoLancamento.DESPESA,
                    instrumentoPublicId = credito.publicId,
                    valorCentavos = 12000L,
                    dataHora = ambiente.relogio.instante,
                    estabelecimento = "Mercado",
                    categoriaPublicId = "cat-outros-despesa",
                ),
            ) as ResultadoLancamentoManual.Salvo
            ).lancamentos.single()
        val pagamento = (
            ambiente.manualF2().registrar(
                NovoLancamentoManual(
                    tipo = TipoLancamento.TRANSFERENCIA,
                    instrumentoPublicId = debito.publicId,
                    destinoPublicId = credito.publicId,
                    valorCentavos = 12000L,
                    dataHora = ambiente.relogio.instante.plusSeconds(60),
                    estabelecimento = "Pagamento fatura",
                ),
            ) as ResultadoLancamentoManual.Salvo
            ).lancamentos
        assertEquals(2, pagamento.size)

        val abertaComPagamento = ambiente.contasProjectionF2().observeFaturasDoCartao(credito.publicId).first()
            ?.ciclos?.first { it.tipoCiclo == TipoCicloFatura.ABERTA }
        assertEquals(12000L, abertaComPagamento?.totalCentavos)
        assertEquals(1, abertaComPagamento?.lancamentos?.size)

        ambiente.editorF2().editar(
            EdicaoDoLancamento(
                publicId = compra.publicId,
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 15000L,
                instrumentoPublicId = credito.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = compra.dataHora,
                estabelecimento = "Mercado",
            ),
        )
        assertEquals(15000L, ambiente.database.faturaDao().listarPorInstrumento(credito.publicId).single().totalCentavos)

        ambiente.lancamentosF2.excluir(compra.publicId, ambiente.relogio.instante.plusSeconds(120))
        assertEquals(0L, ambiente.database.faturaDao().listarPorInstrumento(credito.publicId).single().totalCentavos)

        val excluido = requireNotNull(ambiente.lancamentosF2.buscar(compra.publicId))
        ambiente.lancamentosF2.salvar(excluido.copy(excluidoEm = null, atualizadoEm = ambiente.relogio.instante.plusSeconds(180)))
        assertEquals(15000L, ambiente.database.faturaDao().listarPorInstrumento(credito.publicId).single().totalCentavos)

        ambiente.marcadorF2().marcar(compra.publicId, debito.publicId)
        assertEquals(0L, ambiente.database.faturaDao().listarPorInstrumento(credito.publicId).single().totalCentavos)

        ambiente.marcadorF2().desfazer(compra.publicId)
        assertEquals(15000L, ambiente.database.faturaDao().listarPorInstrumento(credito.publicId).single().totalCentavos)
    }

    @Test
    fun cajuSaldoTemporalNotificacaoAtrasadaEPendentePreservado() = runBlocking {
        val t0 = saoPaulo(2026, 11, 1, 8)
        ambiente.relogio.instante = t0
        val caju = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.VOUCHER, "VA", beneficio = BeneficioVoucher.VA, saldoCentavos = 50000L),
                        NovoInstrumento(TipoInstrumento.VOUCHER, "VR", beneficio = BeneficioVoucher.VR, saldoCentavos = 30000L),
                    ),
                ),
            ),
            t0,
        ).single()
        val va = caju.instrumentos.first { it.beneficio == BeneficioVoucher.VA }
        val vr = caju.instrumentos.first { it.beneficio == BeneficioVoucher.VR }

        val t1 = saoPaulo(2026, 11, 2, 12)
        ambiente.relogio.instante = t1
        val cap1 = ambiente.processadorF2().processar(
            cajuCompra("Mercado", 5000L, "AUXÍLIO ALIMENTAÇÃO", 45000L, t1),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.CONFIRMADO, cap1.lancamento.status)

        val t2 = saoPaulo(2026, 11, 3, 12)
        ambiente.relogio.instante = t2
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 2000L,
                instrumentoPublicId = va.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = t2,
                estabelecimento = "Padaria",
            ),
        )
        val tAtrasado = saoPaulo(2026, 11, 1, 20)
        ambiente.processadorF2().processar(
            cajuCompra("Farmacia Antiga", 1000L, "AUXÍLIO ALIMENTAÇÃO", 49000L, tAtrasado),
        )
        val tPendente = saoPaulo(2026, 11, 4, 12)
        val capPend = ambiente.processadorF2().processar(
            cajuCompra("Lanchonete", 3000L, "SALDO LIVRE", 40000L, tPendente),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.PENDENTE, capPend.lancamento.status)
        assertEquals(40000L, capPend.lancamento.saldoObservadoCentavos)

        val saldoAntesResolver = saldoDoInstrumento(va.publicId)
        assertEquals(43000L, saldoAntesResolver)
        assertEquals(30000L, saldoDoInstrumento(vr.publicId))

        ambiente.editorF2().editar(
            EdicaoDoLancamento(
                publicId = capPend.lancamento.publicId,
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 3000L,
                instrumentoPublicId = va.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = tPendente,
                estabelecimento = "Lanchonete",
            ),
        )
        assertEquals(40000L, saldoDoInstrumento(va.publicId))
    }

    @Test
    fun recargaMaterializaComClampESubstituiEmAmbasOrdens() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 1, 15, 9)
        val caju = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            TipoInstrumento.VOUCHER,
                            "VT",
                            beneficio = BeneficioVoucher.VT,
                            saldoCentavos = 0L,
                            diaRecarga = 31,
                            valorRecargaCentavos = 30000L,
                        ),
                        NovoInstrumento(
                            TipoInstrumento.VOUCHER,
                            "VR",
                            beneficio = BeneficioVoucher.VR,
                            saldoCentavos = 0L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 60000L,
                        ),
                        NovoInstrumento(
                            TipoInstrumento.VOUCHER,
                            "VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 0L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 50000L,
                        ),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val vt = caju.instrumentos.first { it.beneficio == BeneficioVoucher.VT }
        val vr = caju.instrumentos.first { it.beneficio == BeneficioVoucher.VR }
        val va = caju.instrumentos.first { it.beneficio == BeneficioVoucher.VA }

        ambiente.relogio.instante = saoPaulo(2026, 1, 30, 12)
        assertEquals(2, ambiente.materializarRecargasF2()())
        assertEquals(0L, saldoDoInstrumento(vt.publicId))

        ambiente.relogio.instante = saoPaulo(2026, 2, 28, 12)
        assertEquals(2, ambiente.materializarRecargasF2()())
        assertEquals(0, ambiente.materializarRecargasF2()())
        assertEquals(60000L, saldoDoInstrumento(vt.publicId))

        ambiente.relogio.instante = saoPaulo(2026, 5, 5, 8)
        val depVa = ambiente.processadorF2().processar(
            cajuDeposito(50000L, "AUXÍLIO ALIMENTAÇÃO", 50000L, saoPaulo(2026, 5, 5, 8)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.CONFIRMADO, depVa.lancamento.status)

        ambiente.materializarRecargasF2()()
        assertEquals(50000L, saldoDoInstrumento(va.publicId))

        val depVr = ambiente.processadorF2().processar(
            cajuDeposito(60000L, "AUXÍLIO REFEIÇÃO", 300000L, saoPaulo(2026, 5, 5, 14)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.CONFIRMADO, depVr.lancamento.status)
        val recVrMai = ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(vr.publicId, LocalDate.of(2026, 5, 1))
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recVrMai?.status)
        assertEquals(depVr.lancamento.publicId, recVrMai?.lancamentoPublicId)
    }

    @Test
    fun revisaoVoucherEAjusteNaoPoluemReceitasDespesas() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 10, 1, 9)
        val caju = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            TipoInstrumento.VOUCHER,
                            "VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 10000L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 50000L,
                        ),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val va = caju.instrumentos.single()

        ambiente.relogio.instante = saoPaulo(2026, 10, 5, 9)
        ambiente.materializarRecargasF2()()
        assertEquals(60000L, saldoDoInstrumento(va.publicId))

        val amb = ambiente.processadorF2().processar(
            cajuDeposito(10000L, "SALDO LIVRE", null, saoPaulo(2026, 10, 5, 10)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.PENDENTE, amb.lancamento.status)
        assertEquals(MotivoPendencia.BENEFICIO_AMBIGUO.name, amb.lancamento.motivoPendencia)
        assertNull(amb.lancamento.instrumentoPublicId)

        val div = ambiente.processadorF2().processar(
            cajuDeposito(55000L, "AUXÍLIO ALIMENTAÇÃO", null, saoPaulo(2026, 10, 5, 11)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.PENDENTE, div.lancamento.status)
        assertEquals(MotivoPendencia.DIVERGENCIA_RECARGA.name, div.lancamento.motivoPendencia)

        val emLote = ambiente.confirmarF2().confirmarTodos()
        assertEquals(0, emLote.confirmados)
        assertEquals(2, emLote.mantidos)

        val candidatos = ambiente.resolverRecargaF2().listarCandidatos(div.lancamento.publicId)
        assertEquals(1, candidatos?.ocorrenciasCandidatas?.size)
        val ocorrenciaId = requireNotNull(candidatos?.ocorrenciasCandidatas?.single()?.publicId)
        val res1 = ambiente.resolverRecargaF2().resolver(
            div.lancamento.publicId,
            va.publicId,
            DecisaoRecargaVoucher.SubstituirPrevisao(ocorrenciaId),
        )
        val res2 = ambiente.resolverRecargaF2().resolver(
            div.lancamento.publicId,
            va.publicId,
            DecisaoRecargaVoucher.SubstituirPrevisao(ocorrenciaId),
        )
        assertTrue(res1 is ResultadoResolucaoRecarga.Resolvido)
        assertTrue(res2 is ResultadoResolucaoRecarga.Resolvido)
        assertEquals(65000L, saldoDoInstrumento(va.publicId))

        ambiente.relogio.instante = saoPaulo(2026, 10, 10, 12)
        val ajuste = ambiente.ajustarSaldoF2()(va.publicId, ValorAjusteSaldoVoucher.SaldoAlvo(40000L), "Correção manual")
        assertTrue(ajuste is ResultadoAjusteVoucher.Aplicado)
        assertEquals(40000L, saldoDoInstrumento(va.publicId))

        val detalhe = ambiente.contasProjectionF2().observeDetalheVoucher(va.publicId).first()
        assertEquals(1, detalhe?.historicoAjustes?.size)
        assertEquals(-25000L, detalhe?.historicoAjustes?.single()?.deltaCentavos)

        val relatorioOut = ambiente.relatoriosProjectionF2().observeRelatorioMensal(YearMonth.of(2026, 10)).first()
        assertEquals(55000L, relatorioOut.relatorio.totalReceitasCentavos)
        assertEquals(0L, relatorioOut.relatorio.totalDespesasCentavos)
    }

    @Test
    fun assinaturasRelatoriosEInicioIsolamInstrumentosEAggregamMes() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 11, 15, 12)
        val cadastro = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Cartao A", "1111", fechamentoDia = 20, vencimentoDia = 28),
                        NovoInstrumento(TipoInstrumento.CREDITO, "Cartao B", "2222", fechamentoDia = 20, vencimentoDia = 28),
                    ),
                ),
            ),
            ambiente.relogio.instante,
        ).single()
        val cartaoA = cadastro.instrumentos.first { it.ultimosDigitos == "1111" }
        val cartaoB = cadastro.instrumentos.first { it.ultimosDigitos == "2222" }

        val m1 = saoPaulo(2026, 9, 10, 10)
        val m2 = saoPaulo(2026, 10, 11, 10)
        val m3 = saoPaulo(2026, 11, 11, 10)
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 2190L,
                instrumentoPublicId = cartaoA.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = m1,
                estabelecimento = "Spotify",
            ),
        )
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 2190L,
                instrumentoPublicId = cartaoA.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = m2,
                estabelecimento = "SPOTIFY *123",
            ),
        )
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 2190L,
                instrumentoPublicId = cartaoB.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = m1,
                estabelecimento = "Spotify",
            ),
        )
        ambiente.manualF2().registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 2190L,
                instrumentoPublicId = cartaoB.publicId,
                categoriaPublicId = "cat-outros-despesa",
                dataHora = m2,
                estabelecimento = "Spotify",
            ),
        )

        val sugestoes = ambiente.assinaturasF2().observeSugestoes().first()
        assertEquals(2, sugestoes.size)

        val confirmadaA = ambiente.assinaturasF2().confirmarSugestao(sugestoes.first { it.chave.instrumentoPublicId == cartaoA.publicId })
        ambiente.assinaturasF2().recusarSugestao(sugestoes.first { it.chave.instrumentoPublicId == cartaoB.publicId })
        assertTrue(ambiente.assinaturasF2().observeSugestoes().first().isEmpty())

        val cobranca3 = (
            ambiente.manualF2().registrar(
                NovoLancamentoManual(
                    tipo = TipoLancamento.DESPESA,
                    valorCentavos = 2190L,
                    instrumentoPublicId = cartaoA.publicId,
                    categoriaPublicId = "cat-outros-despesa",
                    dataHora = m3,
                    estabelecimento = "Spotify",
                ),
            ) as ResultadoLancamentoManual.Salvo
            ).lancamentos.single()
        assertEquals(confirmadaA.publicId, ambiente.lancamentosF2.buscar(cobranca3.publicId)?.assinaturaPublicId)
        assertEquals("cat-assinaturas", ambiente.lancamentosF2.buscar(cobranca3.publicId)?.categoriaPublicId)
        assertEquals(LocalDate.of(2026, 11, 11), ambiente.assinaturasF2().buscar(confirmadaA.publicId)?.ultimaCobranca)

        ambiente.lancamentosF2.excluir(cobranca3.publicId, ambiente.relogio.instante)
        assertEquals(LocalDate.of(2026, 10, 11), ambiente.assinaturasF2().buscar(confirmadaA.publicId)?.ultimaCobranca)

        val inicioFaturas = ambiente.inicioProjectionF2().observeFaturasAbertas().first { it is EstadoBlocoFinanceiro.Sucesso }
            as EstadoBlocoFinanceiro.Sucesso
        assertEquals(2, inicioFaturas.dados.size)

        val relatorioOut = ambiente.relatoriosProjectionF2().observeRelatorioMensal(YearMonth.of(2026, 10)).first()
        assertEquals(2190L, relatorioOut.relatorio.totalDespesasCentavos)
        assertEquals(VariacaoCategoria.Percentual(0), relatorioOut.categorias.single().variacaoMesAnterior)
    }

    @Test
    fun depositoCajuSemBeneficioComUmVoucherConciliaEDivergeMasComDoisFicaAmbiguo() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 11, 5, 9)
        val contaUm = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 10000L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 60000L,
                        ),
                    ),
                ),
            ),
            saoPaulo(2026, 10, 1, 9),
        ).single()
        val va = contaUm.instrumentos.single()

        val unicoExato = ambiente.processadorF2().processar(
            cajuDeposito(valorCentavos = 60000L, beneficio = null, saldoCentavos = 70000L, quando = saoPaulo(2026, 11, 5, 10)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.CONFIRMADO, unicoExato.lancamento.status)
        assertEquals(va.publicId, unicoExato.lancamento.instrumentoPublicId)
        assertEquals(BeneficioVoucher.VA, unicoExato.lancamento.beneficio)
        assertEquals(70000L, saldoDoInstrumento(va.publicId))
        val recargaSubstituida = ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(
            va.publicId,
            LocalDate.of(2026, 11, 1),
        )
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recargaSubstituida?.status)

        val unicoDivergente = ambiente.processadorF2().processar(
            cajuDeposito(valorCentavos = 55000L, beneficio = null, saldoCentavos = 125000L, quando = saoPaulo(2026, 12, 5, 10)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.PENDENTE, unicoDivergente.lancamento.status)
        assertEquals(MotivoPendencia.DIVERGENCIA_RECARGA.name, unicoDivergente.lancamento.motivoPendencia)
        assertEquals(va.publicId, unicoDivergente.lancamento.instrumentoPublicId)

        ambiente.contasF2().salvarCadastro(
            ContaEditada(
                publicId = contaUm.conta.publicId,
                apelido = "Caju",
                tipo = TipoConta.BENEFICIO,
                instrumentos = listOf(
                    InstrumentoEditado(
                        tipo = TipoInstrumento.VOUCHER,
                        apelido = "VA",
                        publicId = va.publicId,
                        beneficio = BeneficioVoucher.VA,
                        saldoCentavos = 10000L,
                        diaRecarga = 5,
                        valorRecargaCentavos = 60000L,
                    ),
                    InstrumentoEditado(
                        tipo = TipoInstrumento.VOUCHER,
                        apelido = "VR",
                        beneficio = BeneficioVoucher.VR,
                        saldoCentavos = 5000L,
                        diaRecarga = 5,
                        valorRecargaCentavos = 40000L,
                    ),
                ),
            ),
            saoPaulo(2026, 11, 6, 9),
        )
        val ambiguo = ambiente.processadorF2().processar(
            cajuDeposito(valorCentavos = 40000L, beneficio = null, saldoCentavos = 45000L, quando = saoPaulo(2026, 11, 6, 10)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(StatusLancamento.PENDENTE, ambiguo.lancamento.status)
        assertEquals(MotivoPendencia.BENEFICIO_AMBIGUO.name, ambiguo.lancamento.motivoPendencia)
        assertNull(ambiguo.lancamento.instrumentoPublicId)
    }

    @Test
    fun excluirMaterializarEDesfazerRecargaReconciliadaNaoDuplicaSaldo() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 11, 5, 9)
        val caju = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 10000L,
                            diaRecarga = 5,
                            valorRecargaCentavos = 60000L,
                        ),
                    ),
                ),
            ),
            saoPaulo(2026, 10, 1, 9),
        ).single()
        val va = caju.instrumentos.single()

        val captura = ambiente.processadorF2().processar(
            cajuDeposito(60000L, "AUXÍLIO ALIMENTAÇÃO", 70000L, saoPaulo(2026, 11, 5, 10)),
        ) as ResultadoProcessamento.Gravado
        assertEquals(70000L, saldoDoInstrumento(va.publicId))

        ambiente.lancamentosF2.excluir(captura.lancamento.publicId, saoPaulo(2026, 11, 5, 11))
        assertEquals(10000L, saldoDoInstrumento(va.publicId))
        val recIgnorada = ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(va.publicId, LocalDate.of(2026, 11, 1))
        assertEquals(StatusRecargaProgramada.IGNORADA, recIgnorada?.status)
        assertEquals(captura.lancamento.publicId, recIgnorada?.lancamentoPublicId)

        assertEquals(0, ambiente.materializarRecargasF2()())
        ambiente.contasProjectionF2().observeContas().first()
        assertEquals(10000L, saldoDoInstrumento(va.publicId))

        val excluido = requireNotNull(ambiente.lancamentosF2.buscar(captura.lancamento.publicId))
        ambiente.lancamentosF2.salvar(excluido.copy(excluidoEm = null, atualizadoEm = saoPaulo(2026, 11, 5, 12)))
        assertEquals(70000L, saldoDoInstrumento(va.publicId))
        val recRestaurada = ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(va.publicId, LocalDate.of(2026, 11, 1))
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recRestaurada?.status)

        ambiente.relogio.instante = saoPaulo(2026, 12, 6, 9)
        assertEquals(1, ambiente.materializarRecargasF2()())
        assertEquals(130000L, saldoDoInstrumento(va.publicId))
        val recDez = requireNotNull(
            ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(va.publicId, LocalDate.of(2026, 12, 1)),
        )
        val lancManualDezId = requireNotNull(recDez.lancamentoPublicId)

        ambiente.lancamentosF2.excluir(lancManualDezId, saoPaulo(2026, 12, 6, 10))
        assertEquals(70000L, saldoDoInstrumento(va.publicId))
        assertEquals(0, ambiente.materializarRecargasF2()())
        val lancDezExcluido = requireNotNull(ambiente.lancamentosF2.buscar(lancManualDezId))
        ambiente.lancamentosF2.salvar(lancDezExcluido.copy(excluidoEm = null, atualizadoEm = saoPaulo(2026, 12, 6, 11)))
        assertEquals(130000L, saldoDoInstrumento(va.publicId))
        assertEquals(
            StatusRecargaProgramada.EFETIVADA,
            ambiente.database.recargaProgramadaVoucherDao().buscarPorCompetencia(va.publicId, LocalDate.of(2026, 12, 1))?.status,
        )
    }

    @Test
    fun excluirEDesfazerAjustePreservaTipoEHistoricoDeAjuste() = runBlocking {
        ambiente.relogio.instante = saoPaulo(2026, 11, 10, 9)
        val caju = ambiente.contasF2().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.VOUCHER, "VA", beneficio = BeneficioVoucher.VA, saldoCentavos = 10000L),
                    ),
                ),
            ),
            saoPaulo(2026, 11, 1, 9),
        ).single()
        val va = caju.instrumentos.single()

        val aplicado = ambiente.ajustarSaldoF2()(va.publicId, ValorAjusteSaldoVoucher.SaldoAlvo(40000L), "Conferência")
            as ResultadoAjusteVoucher.Aplicado
        assertEquals(40000L, saldoDoInstrumento(va.publicId))

        ambiente.lancamentosF2.excluir(aplicado.lancamentoPublicId, saoPaulo(2026, 11, 10, 10))
        assertEquals(10000L, saldoDoInstrumento(va.publicId))
        assertTrue(ambiente.contasProjectionF2().observeDetalheVoucher(va.publicId).first()?.historicoAjustes.orEmpty().isEmpty())

        val ajusteExcluido = requireNotNull(ambiente.lancamentosF2.buscar(aplicado.lancamentoPublicId))
        ambiente.lancamentosF2.salvar(ajusteExcluido.copy(excluidoEm = null, atualizadoEm = saoPaulo(2026, 11, 10, 11)))

        val detalhe = requireNotNull(ambiente.contasProjectionF2().observeDetalheVoucher(va.publicId).first())
        assertEquals(40000L, detalhe.saldoAtualCentavos)
        assertEquals(1, detalhe.historicoAjustes.size)
        assertEquals(30000L, detalhe.historicoAjustes.single().deltaCentavos)
        assertEquals("Conferência", detalhe.historicoAjustes.single().observacao)
    }

    private suspend fun saldoDoInstrumento(instrumentoPublicId: String): Long =
        requireNotNull(ambiente.contasProjectionF2().observeDetalheVoucher(instrumentoPublicId).first()).saldoAtualCentavos

    private fun saoPaulo(ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int = 0): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto).atZone(CalendarioDeFatura.ZONA_SAO_PAULO).toInstant()

    private fun cajuCompra(
        estabelecimento: String,
        valorCentavos: Long,
        beneficio: String?,
        saldoCentavos: Long?,
        quando: Instant,
    ): CapturaParaProcessar = CapturaParaProcessar(
        instituicao = InstituicaoFinanceira.CAJU,
        postTime = quando,
        textoBruto = "Caju compra $estabelecimento",
        transacao = TransacaoExtraida(
            padraoId = "caju-compra",
            tipo = TipoLancamentoPadrao.COMPRA,
            direcao = DirecaoPadrao.DESPESA,
            valorCentavos = valorCentavos,
            estabelecimento = estabelecimento,
            digitos = null,
            saldoCentavos = saldoCentavos,
            beneficio = beneficio,
        ),
    )

    private fun cajuDeposito(valorCentavos: Long, beneficio: String?, saldoCentavos: Long?, quando: Instant): CapturaParaProcessar =
        CapturaParaProcessar(
            instituicao = InstituicaoFinanceira.CAJU,
            postTime = quando,
            textoBruto = "Caju deposito",
            transacao = TransacaoExtraida(
                padraoId = "caju-deposito",
                tipo = TipoLancamentoPadrao.DEPOSITO,
                direcao = DirecaoPadrao.RECEITA,
                valorCentavos = valorCentavos,
                estabelecimento = null,
                digitos = null,
                saldoCentavos = saldoCentavos,
                beneficio = beneficio,
            ),
        )
}
