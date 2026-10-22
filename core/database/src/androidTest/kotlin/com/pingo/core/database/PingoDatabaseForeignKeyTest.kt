package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PingoDatabaseForeignKeyTest {
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
    fun referenceToMissingParentIsRejected() = runBlocking {
        val falhou = runCatching {
            database.instrumentoDao().upsert(instrumento())
        }.isFailure

        assertTrue(falhou)
    }

    @Test
    fun referenceToExistingParentIsAccepted() = runBlocking {
        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())

        assertNotNull(database.instrumentoDao().buscar(INSTRUMENTO_ID))
    }

    private fun conta() = ContaEntity(
        publicId = CONTA_ID,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Conta",
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private fun instrumento() = InstrumentoEntity(
        publicId = INSTRUMENTO_ID,
        contaPublicId = CONTA_ID,
        tipo = TipoInstrumento.DEBITO,
        apelido = "Débito",
        ultimosDigitos = null,
        arquivado = false,
        fechamentoDia = null,
        vencimentoDia = null,
        beneficio = null,
        saldoCentavos = null,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private companion object {
        const val CONTA_ID = "conta-1"
        const val INSTRUMENTO_ID = "instrumento-1"
        val INSTANTE: Instant = Instant.parse("2026-10-22T12:00:00Z")
    }
}
