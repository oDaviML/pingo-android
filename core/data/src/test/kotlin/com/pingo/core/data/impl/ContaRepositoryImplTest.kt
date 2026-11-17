package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.InstrumentoEditado
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContaRepositoryImplTest {
    private val contaDao = FakeContaDao()
    private val instrumentoDao = FakeInstrumentoDao()
    private val transacoes = FakeTransacaoLocal()
    private val repositorio = ContaRepositoryImpl(contaDao, instrumentoDao, transacoes)

    @Test
    fun `criar grava conta e instrumentos ligados a ela com o instante informado`() = runBlocking {
        val novaConta = NovaConta(
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "  Nubank  ",
            tipo = TipoConta.CORRENTE,
            instrumentos = listOf(NovoInstrumento(TipoInstrumento.CREDITO, " Cartão ", "1234", fechamentoDia = 5, vencimentoDia = 12)),
        )

        repositorio.criarComInstrumentos(listOf(novaConta), INSTANTE_DA_CRIACAO)

        val conta = contaDao.gravadas().single().toModel()
        val instrumento = instrumentoDao.gravados().single().toModel()
        assertEquals(InstituicaoFinanceira.NUBANK, conta.instituicao)
        assertEquals("Nubank", conta.apelido)
        assertEquals(TipoConta.CORRENTE, conta.tipo)
        assertEquals(INSTANTE_DA_CRIACAO, conta.criadoEm)
        assertEquals(INSTANTE_DA_CRIACAO, conta.atualizadoEm)
        assertEquals(conta.publicId, instrumento.contaPublicId)
        assertEquals(TipoInstrumento.CREDITO, instrumento.tipo)
        assertEquals("Cartão", instrumento.apelido)
        assertEquals("1234", instrumento.ultimosDigitos)
        assertEquals(INSTANTE_DA_CRIACAO, instrumento.criadoEm)
        assertEquals(INSTANTE_DA_CRIACAO, instrumento.atualizadoEm)
    }

    @Test
    fun `cada conta e cada instrumento recebe identificador proprio`() = runBlocking {
        val instrumentos = listOf(
            NovoInstrumento(TipoInstrumento.DEBITO, "Débito"),
            NovoInstrumento(TipoInstrumento.VOUCHER, "VR", beneficio = BeneficioVoucher.VR, saldoCentavos = 10000),
        )

        repositorio.criarComInstrumentos(
            listOf(
                novaConta(instituicao = InstituicaoFinanceira.ITAU, instrumentos = instrumentos),
                novaConta(instituicao = InstituicaoFinanceira.CAJU, instrumentos = instrumentos),
            ),
            INSTANTE_DA_CRIACAO,
        )

        val identificadores = contaDao.gravadas().map(ContaEntity::publicId) + instrumentoDao.gravados().map(InstrumentoEntity::publicId)
        assertEquals(6, identificadores.size)
        assertEquals(identificadores.size, identificadores.toSet().size)
        assertTrue(identificadores.none(String::isBlank))
    }

    @Test
    fun `criar recusa conta sem instrumentos e nao grava nada`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repositorio.criarComInstrumentos(listOf(novaConta(instrumentos = emptyList())), INSTANTE_DA_CRIACAO) }
        }

        assertTrue(contaDao.gravadas().isEmpty())
        assertTrue(instrumentoDao.gravados().isEmpty())
    }

    @Test
    fun `criar recusa o lote inteiro quando uma conta e invalida`() {
        val valida = novaConta(instituicao = InstituicaoFinanceira.NUBANK)
        val invalida = novaConta(instituicao = InstituicaoFinanceira.INTER, apelido = " ")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repositorio.criarComInstrumentos(listOf(valida, invalida), INSTANTE_DA_CRIACAO) }
        }

        assertTrue(contaDao.gravadas().isEmpty())
        assertTrue(instrumentoDao.gravados().isEmpty())
        assertEquals(0, transacoes.execucoes)
    }

    @Test
    fun `criar grava o lote em uma unica transacao`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(novaConta(instituicao = InstituicaoFinanceira.NUBANK), novaConta(instituicao = InstituicaoFinanceira.INTER)),
            INSTANTE_DA_CRIACAO,
        )

        assertEquals(1, transacoes.execucoes)
    }

    @Test
    fun `contas criadas aparecem no fluxo de contas ativas`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(novaConta(instituicao = InstituicaoFinanceira.INTER, apelido = "Inter")),
            INSTANTE_DA_CRIACAO,
        )

        assertEquals(listOf("Inter"), repositorio.observeContas().first().map { it.apelido })
    }

    @Test
    fun `criar grava a configuracao de credito e voucher de cada instrumento`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(
                novaConta(
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Cartão",
                            ultimosDigitos = "1234",
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "VR",
                            beneficio = BeneficioVoucher.VR,
                            saldoCentavos = 25000,
                            diaRecarga = 1,
                            valorRecargaCentavos = 50000,
                        ),
                    ),
                ),
            ),
            INSTANTE_DA_CRIACAO,
        )

        val (credito, voucher) = instrumentoDao.gravados().map { it.toModel() }.sortedBy { it.tipo }
        assertEquals(5, credito.fechamentoDia)
        assertEquals(12, credito.vencimentoDia)
        assertEquals(BeneficioVoucher.VR, voucher.beneficio)
        assertEquals(25000L, voucher.saldoCentavos)
        assertEquals(1, voucher.diaRecarga)
        assertEquals(50000L, voucher.valorRecargaCentavos)
        assertEquals(false, voucher.arquivadoPorCascata)
    }

    @Test
    fun `criar grava dois creditos da mesma conta com dados independentes`() = runBlocking {
        repositorio.criarComInstrumentos(
            listOf(
                novaConta(
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Principal", "1111", fechamentoDia = 5, vencimentoDia = 12),
                        NovoInstrumento(TipoInstrumento.CREDITO, "Adicional", "2222", fechamentoDia = 20, vencimentoDia = 28),
                    ),
                ),
            ),
            INSTANTE_DA_CRIACAO,
        )

        val instrumentos = instrumentoDao.gravados().map { it.toModel() }.sortedBy { it.apelido }
        assertEquals(listOf("Adicional", "Principal"), instrumentos.map { it.apelido })
        assertEquals(2, instrumentos.map { it.publicId }.toSet().size)
        assertEquals(listOf(20, 5), instrumentos.map { it.fechamentoDia })
        assertEquals(listOf(28, 12), instrumentos.map { it.vencimentoDia })
    }

    @Test
    fun `editar preserva identificadores e criadoEm e atualiza atualizadoEm`() = runBlocking {
        criarContaComCredito()
        val contaOriginal = contaDao.gravadas().single().toModel()
        val instrumentoOriginal = instrumentoDao.gravados().single().toModel()

        val salvo = repositorio.salvarCadastro(
            ContaEditada(
                publicId = contaOriginal.publicId,
                apelido = "  Roxinho  ",
                tipo = TipoConta.BENEFICIO,
                instrumentos = listOf(
                    InstrumentoEditado(
                        tipo = TipoInstrumento.CREDITO,
                        apelido = " Cartão novo ",
                        publicId = instrumentoOriginal.publicId,
                        ultimosDigitos = "9999",
                        fechamentoDia = 3,
                        vencimentoDia = 10,
                    ),
                ),
            ),
            INSTANTE_DA_EDICAO,
        )

        val conta = contaDao.gravadas().single().toModel()
        val instrumento = instrumentoDao.gravados().single().toModel()

        assertEquals(contaOriginal.publicId, conta.publicId)
        assertEquals(contaOriginal.criadoEm, conta.criadoEm)
        assertEquals(INSTANTE_DA_EDICAO, conta.atualizadoEm)
        assertEquals("Roxinho", conta.apelido)
        assertEquals(TipoConta.BENEFICIO, conta.tipo)
        assertEquals(instrumentoOriginal.publicId, instrumento.publicId)
        assertEquals(instrumentoOriginal.criadoEm, instrumento.criadoEm)
        assertEquals(INSTANTE_DA_EDICAO, instrumento.atualizadoEm)
        assertEquals("Cartão novo", instrumento.apelido)
        assertEquals("9999", instrumento.ultimosDigitos)
        assertEquals(3, instrumento.fechamentoDia)
        assertEquals(10, instrumento.vencimentoDia)
        assertEquals(conta.publicId, salvo.conta.publicId)
        assertEquals(instrumento.publicId, salvo.instrumentos.single().publicId)
    }

    @Test
    fun `editar adiciona instrumento novo sem substituir os existentes`() = runBlocking {
        criarContaComCredito()
        val conta = contaDao.gravadas().single().toModel()
        val credito = instrumentoDao.gravados().single().toModel()

        repositorio.salvarCadastro(
            ContaEditada(
                publicId = conta.publicId,
                apelido = conta.apelido,
                tipo = conta.tipo,
                instrumentos = listOf(
                    InstrumentoEditado(
                        tipo = TipoInstrumento.CREDITO,
                        apelido = credito.apelido,
                        publicId = credito.publicId,
                        ultimosDigitos = credito.ultimosDigitos,
                        fechamentoDia = credito.fechamentoDia,
                        vencimentoDia = credito.vencimentoDia,
                    ),
                    InstrumentoEditado(
                        tipo = TipoInstrumento.VOUCHER,
                        apelido = "VR",
                        beneficio = BeneficioVoucher.VR,
                        saldoCentavos = 10000,
                    ),
                ),
            ),
            INSTANTE_DA_EDICAO,
        )

        val instrumentos = instrumentoDao.gravados().map { it.toModel() }.sortedBy { it.tipo }
        assertEquals(2, instrumentos.size)
        assertEquals(credito.publicId, instrumentos.first { it.tipo == TipoInstrumento.CREDITO }.publicId)
        assertEquals(BeneficioVoucher.VR, instrumentos.first { it.tipo == TipoInstrumento.VOUCHER }.beneficio)
    }

    @Test
    fun `editar recusa instrumento de outra conta sem alterar nada`() = runBlocking {
        criarContaComCredito()
        val outra = novaConta(instituicao = InstituicaoFinanceira.INTER, apelido = "Inter")
        repositorio.criarComInstrumentos(listOf(outra), INSTANTE_DA_CRIACAO)

        val conta = contaDao.gravadas().first { it.toModel().instituicao == InstituicaoFinanceira.NUBANK }.toModel()
        val instrumentoDeOutraConta = instrumentoDao.gravados()
            .first { it.toModel().contaPublicId != conta.publicId }
            .toModel()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repositorio.salvarCadastro(
                    ContaEditada(
                        publicId = conta.publicId,
                        apelido = "Alterada",
                        tipo = conta.tipo,
                        instrumentos = listOf(
                            InstrumentoEditado(
                                tipo = TipoInstrumento.CREDITO,
                                apelido = "Cartão",
                                publicId = instrumentoDeOutraConta.publicId,
                                fechamentoDia = 5,
                                vencimentoDia = 12,
                            ),
                        ),
                    ),
                    INSTANTE_DA_EDICAO,
                )
            }
        }

        assertEquals(conta.apelido, contaDao.buscar(conta.publicId)?.apelido)
        assertEquals(instrumentoDeOutraConta.apelido, instrumentoDao.buscar(instrumentoDeOutraConta.publicId)?.apelido)
    }

    @Test
    fun `editar grava o cadastro em uma unica transacao`() = runBlocking {
        criarContaComCredito()
        val conta = contaDao.gravadas().single().toModel()
        val credito = instrumentoDao.gravados().single().toModel()

        repositorio.salvarCadastro(
            ContaEditada(
                publicId = conta.publicId,
                apelido = "Roxinho",
                tipo = conta.tipo,
                instrumentos = listOf(
                    InstrumentoEditado(
                        tipo = TipoInstrumento.CREDITO,
                        apelido = credito.apelido,
                        publicId = credito.publicId,
                        fechamentoDia = 5,
                        vencimentoDia = 12,
                    ),
                ),
            ),
            INSTANTE_DA_EDICAO,
        )

        assertEquals(2, transacoes.execucoes)
    }

    @Test
    fun `editar recusa cadastro invalido antes de gravar`() {
        runBlocking { criarContaComCredito() }
        val conta = contaDao.gravadas().single().toModel()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repositorio.salvarCadastro(
                    ContaEditada(
                        publicId = conta.publicId,
                        apelido = conta.apelido,
                        tipo = conta.tipo,
                        instrumentos = listOf(
                            InstrumentoEditado(
                                tipo = TipoInstrumento.CREDITO,
                                apelido = "Cartão",
                                fechamentoDia = 0,
                                vencimentoDia = 32,
                            ),
                        ),
                    ),
                    INSTANTE_DA_EDICAO,
                )
            }
        }

        assertEquals(conta, runBlocking { contaDao.buscar(conta.publicId)?.toModel() })
    }

    @Test
    fun `arquivar conta arquiva apenas os instrumentos ativos e marca a cascata`() = runBlocking {
        criarContaComCredito()
        val conta = contaDao.gravadas().single().toModel()
        seedInstrumento(conta.publicId, publicId = "instrumento-arquivado", arquivado = true)

        repositorio.arquivar(conta.publicId, INSTANTE_DA_EDICAO)

        val instrumentos = instrumentoDao.gravados().map { it.toModel() }.sortedBy { it.apelido }
        val ativo = instrumentos.first { it.apelido == "Cartão de crédito" }
        val preArquivado = instrumentos.first { it.apelido == "Cartão antigo" }
        assertTrue(ativo.arquivado)
        assertTrue(ativo.arquivadoPorCascata)
        assertTrue(preArquivado.arquivado)
        assertFalse(preArquivado.arquivadoPorCascata)
        assertTrue(contaDao.buscar(conta.publicId)?.arquivada == true)
    }

    @Test
    fun `reativar conta reativa somente os instrumentos arquivados pela cascata`() = runBlocking {
        criarContaComCredito()
        val conta = contaDao.gravadas().single().toModel()
        seedInstrumento(conta.publicId, publicId = "instrumento-arquivado", arquivado = true)
        repositorio.arquivar(conta.publicId, INSTANTE_DA_EDICAO)

        repositorio.reativar(conta.publicId, INSTANTE_DA_EDICAO.plusSeconds(60))

        val instrumentos = instrumentoDao.gravados().map { it.toModel() }.sortedBy { it.apelido }
        val cascateado = instrumentos.first { it.apelido == "Cartão de crédito" }
        val preArquivado = instrumentos.first { it.apelido == "Cartão antigo" }
        assertFalse(cascateado.arquivado)
        assertFalse(cascateado.arquivadoPorCascata)
        assertTrue(preArquivado.arquivado)
        assertFalse(preArquivado.arquivadoPorCascata)
        assertFalse(contaDao.buscar(conta.publicId)?.arquivada == true)
    }

    @Test
    fun `repetir arquivamento e reativacao mantem a distincao da cascata`() = runBlocking {
        criarContaComCredito()
        val conta = contaDao.gravadas().single().toModel()
        seedInstrumento(conta.publicId, publicId = "instrumento-arquivado", arquivado = true)

        repeat(2) {
            repositorio.arquivar(conta.publicId, INSTANTE_DA_EDICAO.plusSeconds(it.toLong()))
        }
        repositorio.reativar(conta.publicId, INSTANTE_DA_EDICAO.plusSeconds(10))
        repositorio.reativar(conta.publicId, INSTANTE_DA_EDICAO.plusSeconds(11))

        val instrumentos = instrumentoDao.gravados().map { it.toModel() }
        assertEquals(2, instrumentos.size)
        assertFalse(instrumentos.first { it.apelido == "Cartão de crédito" }.arquivado)
        assertTrue(instrumentos.first { it.apelido == "Cartão antigo" }.arquivado)
    }

    private suspend fun criarContaComCredito() {
        repositorio.criarComInstrumentos(
            listOf(
                novaConta(
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Cartão de crédito", "1234", fechamentoDia = 5, vencimentoDia = 12),
                    ),
                ),
            ),
            INSTANTE_DA_CRIACAO,
        )
    }

    private suspend fun seedInstrumento(contaPublicId: String, publicId: String, arquivado: Boolean) {
        instrumentoDao.upsertAll(
            instrumentoDao.gravados() + InstrumentoEntity(
                publicId = publicId,
                contaPublicId = contaPublicId,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão antigo",
                ultimosDigitos = null,
                arquivado = arquivado,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = INSTANTE_DA_CRIACAO,
                atualizadoEm = INSTANTE_DA_CRIACAO,
                excluidoEm = null,
            ),
        )
    }

    private fun novaConta(
        instituicao: InstituicaoFinanceira = InstituicaoFinanceira.NUBANK,
        apelido: String = "Nubank",
        instrumentos: List<NovoInstrumento> = listOf(
            NovoInstrumento(TipoInstrumento.CREDITO, "Cartão de crédito", "1234", fechamentoDia = 5, vencimentoDia = 12),
        ),
    ) = NovaConta(instituicao = instituicao, apelido = apelido, tipo = TipoConta.CORRENTE, instrumentos = instrumentos)
}
