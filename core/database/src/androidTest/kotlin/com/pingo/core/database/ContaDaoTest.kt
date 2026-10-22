package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ContaDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PingoDatabase
    private lateinit var dao: ContaDao

    @Before
    fun setUp() {
        TestPingoDatabase.delete(context)
        database = TestPingoDatabase.open(context)
        dao = database.contaDao()
    }

    @After
    fun tearDown() {
        database.close()
        TestPingoDatabase.delete(context)
    }

    @Test
    fun upsertAndFindByPublicId() = runBlocking {
        dao.upsert(conta("Conta principal"))

        val encontrada = dao.buscar(PUBLIC_ID)

        assertNotNull(encontrada)
        assertEquals("Conta principal", encontrada?.apelido)
    }

    @Test
    fun observeEmitsWhenDataChanges() = runBlocking {
        val valores = mutableListOf<List<ContaEntity>>()
        val job = launch { dao.observeAtivas().collect { valores.add(it) } }

        waitUntil { valores.isNotEmpty() }
        assertEquals(0, valores.last().size)

        dao.upsert(conta("Conta nova"))

        waitUntil { valores.last().isNotEmpty() }
        assertEquals(1, valores.last().size)

        job.cancel()
    }

    @Test
    fun softDeleteHidesFromActiveQueries() = runBlocking {
        dao.upsert(conta("Conta apagada"))

        val quando = Instant.parse("2026-10-22T12:00:00Z")
        dao.marcarExcluida(PUBLIC_ID, quando)

        val salva = dao.buscar(PUBLIC_ID)

        assertNotNull(salva)
        assertEquals(quando, salva?.excluidoEm)
        assertTrue(dao.observeAtivas().first().isEmpty())
    }

    private suspend fun waitUntil(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) {
                delay(10)
            }
        }
    }

    private fun conta(apelido: String) = ContaEntity(
        publicId = PUBLIC_ID,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private companion object {
        const val PUBLIC_ID = "conta-1"
        val INSTANTE: Instant = Instant.parse("2026-10-22T12:00:00Z")
    }
}
