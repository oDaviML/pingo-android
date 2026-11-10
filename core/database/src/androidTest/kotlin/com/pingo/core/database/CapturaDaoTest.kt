package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CapturaDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PingoDatabase

    @Before
    fun setUp() {
        TestPingoDatabase.delete(context)
        database = TestPingoDatabase.open(context)
    }

    @After
    fun tearDown() {
        database.close()
        TestPingoDatabase.delete(context)
    }

    @Test
    fun instrumentosDaInstituicaoIgnoramArquivadosApagadosEOutrasInstituicoes() = runBlocking {
        seedConta("itau", InstituicaoFinanceira.ITAU)
        seedConta("nubank", InstituicaoFinanceira.NUBANK)
        database.instrumentoDao().upsert(instrumento("cartao-itau", "itau"))
        database.instrumentoDao().upsert(instrumento("arquivado", "itau", arquivado = true))
        database.instrumentoDao().upsert(instrumento("apagado", "itau", excluidoEm = AGORA))
        database.instrumentoDao().upsert(instrumento("outro", "nubank"))

        val encontrados = database.instrumentoDao().ativosDaInstituicao(InstituicaoFinanceira.ITAU)

        assertEquals(listOf("cartao-itau"), encontrados.map(InstrumentoEntity::publicId))
    }

    @Test
    fun recentesDaInstituicaoFiltramOrigemJanelaEInstituicao() = runBlocking {
        seedConta("itau", InstituicaoFinanceira.ITAU)
        seedConta("nubank", InstituicaoFinanceira.NUBANK)
        seedInstrumento("cartao-itau", "itau")
        seedInstrumento("cartao-nubank", "nubank")
        val dao = database.lancamentoDao()
        dao.upsert(lancamento("recente", "cartao-itau"))
        dao.upsert(lancamento("manual", "cartao-itau", origem = OrigemLancamento.MANUAL))
        dao.upsert(lancamento("antigo", "cartao-itau", criadoEm = AGORA.minus(Duration.ofMinutes(20))))
        dao.upsert(lancamento("nubank", "cartao-nubank"))

        val encontrados = dao.recentesDaInstituicao(
            InstituicaoFinanceira.ITAU,
            OrigemLancamento.NOTIFICACAO,
            AGORA.minus(Duration.ofMinutes(5)),
            AGORA.plus(Duration.ofMinutes(1)),
        )

        assertEquals(listOf("recente"), encontrados.map(LancamentoEntity::publicId))
    }

    @Test
    fun candidatosDeTransferenciaExigemOutraContaValorJanelaETipo() = runBlocking {
        seedConta("itau", InstituicaoFinanceira.ITAU)
        seedConta("nubank", InstituicaoFinanceira.NUBANK)
        seedInstrumento("cartao-itau", "itau")
        seedInstrumento("cartao-nubank", "nubank")
        val dao = database.lancamentoDao()
        dao.upsert(lancamento("receita", "cartao-nubank", tipo = TipoLancamento.RECEITA))
        dao.upsert(lancamento("mesma-conta", "cartao-itau", tipo = TipoLancamento.RECEITA))
        dao.upsert(lancamento("outro-valor", "cartao-nubank", valorCentavos = 14000, tipo = TipoLancamento.RECEITA))
        dao.upsert(
            lancamento(
                "fora-da-janela",
                "cartao-nubank",
                tipo = TipoLancamento.RECEITA,
                dataHora = AGORA.minus(Duration.ofMinutes(20)),
            ),
        )
        dao.upsert(
            lancamento(
                "ja-transferencia",
                "cartao-nubank",
                tipo = TipoLancamento.RECEITA,
                transferenciaId = "transferencia-1",
            ),
        )

        val candidatos = dao.candidatosDeTransferencia(
            TipoLancamento.RECEITA,
            15000,
            AGORA.minus(Duration.ofMinutes(15)),
            AGORA.plus(Duration.ofMinutes(15)),
            "itau",
            OrigemLancamento.NOTIFICACAO,
        )

        assertEquals(listOf("receita"), candidatos.map(LancamentoEntity::publicId))
    }

    private suspend fun seedConta(publicId: String, instituicao: InstituicaoFinanceira) {
        database.contaDao().upsert(conta(publicId, instituicao))
    }

    private suspend fun seedInstrumento(publicId: String, contaPublicId: String) {
        database.instrumentoDao().upsert(instrumento(publicId, contaPublicId))
    }

    private fun conta(publicId: String, instituicao: InstituicaoFinanceira) = ContaEntity(
        publicId = publicId,
        instituicao = instituicao,
        apelido = publicId,
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = AGORA,
        atualizadoEm = AGORA,
        excluidoEm = null,
    )

    private fun instrumento(publicId: String, contaPublicId: String, arquivado: Boolean = false, excluidoEm: Instant? = null) =
        InstrumentoEntity(
            publicId = publicId,
            contaPublicId = contaPublicId,
            tipo = TipoInstrumento.CREDITO,
            apelido = publicId,
            ultimosDigitos = null,
            arquivado = arquivado,
            fechamentoDia = null,
            vencimentoDia = null,
            beneficio = null,
            saldoCentavos = null,
            diaRecarga = null,
            valorRecargaCentavos = null,
            limiteCreditoCentavos = null,
            criadoEm = AGORA,
            atualizadoEm = AGORA,
            excluidoEm = excluidoEm,
        )

    private fun lancamento(
        publicId: String,
        instrumentoPublicId: String,
        valorCentavos: Long = 15000,
        dataHora: Instant = AGORA,
        criadoEm: Instant = AGORA,
        tipo: TipoLancamento = TipoLancamento.RECEITA,
        origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
        transferenciaId: String? = null,
    ) = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = transferenciaId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        estabelecimento = "DAVI",
        status = StatusLancamento.CONFIRMADO,
        origem = origem,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = criadoEm,
        atualizadoEm = AGORA,
        excluidoEm = null,
        textoBruto = null,
    )

    private companion object {
        val AGORA: Instant = Instant.parse("2026-11-10T13:54:48Z")
    }
}
