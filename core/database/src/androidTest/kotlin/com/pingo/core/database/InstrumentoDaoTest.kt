package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class InstrumentoDaoTest {
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
    fun selecionaveisExigemContaEInstrumentoAtivos() = runBlocking {
        database.contaDao().upsert(conta("ativa", arquivada = false))
        database.contaDao().upsert(conta("arquivada", arquivada = true))
        database.contaDao().upsert(conta("apagada", arquivada = false, excluidoEm = AGORA))
        database.instrumentoDao().upsert(instrumento("ativo", "ativa"))
        database.instrumentoDao().upsert(instrumento("arquivado", "ativa", arquivado = true))
        database.instrumentoDao().upsert(instrumento("apagado", "ativa", excluidoEm = AGORA))
        database.instrumentoDao().upsert(instrumento("de-conta-arquivada", "arquivada"))
        database.instrumentoDao().upsert(instrumento("de-conta-apagada", "apagada"))

        val ativos = database.instrumentoDao().observeAtivos().first()

        assertEquals(listOf("ativo"), ativos.map(InstrumentoEntity::publicId))
    }

    @Test
    fun consultaHistoricaMantemArquivadosEDeContaArquivada() = runBlocking {
        database.contaDao().upsert(conta("ativa", arquivada = false))
        database.contaDao().upsert(conta("arquivada", arquivada = true))
        database.instrumentoDao().upsert(instrumento("arquivado", "ativa", arquivado = true))
        database.instrumentoDao().upsert(instrumento("de-conta-arquivada", "arquivada"))
        database.instrumentoDao().upsert(instrumento("apagado", "ativa", excluidoEm = AGORA))

        val historicos = database.instrumentoDao().observeNaoExcluidos().first()

        assertEquals(listOf("arquivado", "de-conta-arquivada"), historicos.map(InstrumentoEntity::publicId))
    }

    @Test
    fun buscarAtivoExigeContaEInstrumentoAtivos() = runBlocking {
        database.contaDao().upsert(conta("ativa", arquivada = false))
        database.contaDao().upsert(conta("arquivada", arquivada = true))
        database.contaDao().upsert(conta("apagada", arquivada = false, excluidoEm = AGORA))
        database.instrumentoDao().upsert(instrumento("ativo", "ativa"))
        database.instrumentoDao().upsert(instrumento("arquivado", "ativa", arquivado = true))
        database.instrumentoDao().upsert(instrumento("apagado", "ativa", excluidoEm = AGORA))
        database.instrumentoDao().upsert(instrumento("de-conta-arquivada", "arquivada"))
        database.instrumentoDao().upsert(instrumento("de-conta-apagada", "apagada"))

        val dao = database.instrumentoDao()

        assertNotNull(dao.buscarAtivo("ativo"))
        assertNull(dao.buscarAtivo("arquivado"))
        assertNull(dao.buscarAtivo("apagado"))
        assertNull(dao.buscarAtivo("de-conta-arquivada"))
        assertNull(dao.buscarAtivo("de-conta-apagada"))
    }

    private fun conta(publicId: String, arquivada: Boolean, excluidoEm: Instant? = null) = ContaEntity(
        publicId = publicId,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = publicId,
        tipo = TipoConta.CORRENTE,
        arquivada = arquivada,
        criadoEm = AGORA,
        atualizadoEm = AGORA,
        excluidoEm = excluidoEm,
    )

    private fun instrumento(publicId: String, contaPublicId: String, arquivado: Boolean = false, excluidoEm: Instant? = null) =
        InstrumentoEntity(
            publicId = publicId,
            contaPublicId = contaPublicId,
            tipo = TipoInstrumento.DEBITO,
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

    private companion object {
        val AGORA: Instant = Instant.parse("2026-11-10T13:54:48Z")
    }
}
