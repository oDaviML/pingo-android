package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoConta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PingoDatabaseEncryptionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        TestPingoDatabase.delete(context, DATABASE_NAME)
    }

    @After
    fun tearDown() {
        TestPingoDatabase.delete(context, DATABASE_NAME)
    }

    @Test
    fun fileIsNotReadableAsPlainSqlite() = runBlocking {
        val database = TestPingoDatabase.open(context, DATABASE_NAME)
        database.contaDao().upsert(conta("Conta Secreta"))
        database.close()

        val arquivo = context.getDatabasePath(DATABASE_NAME)
        val bytes = arquivo.readBytes()

        assertFalse(bytes.contem("SQLite format 3".toByteArray()))
        assertFalse(bytes.contem("Conta Secreta".toByteArray()))
    }

    @Test
    fun wrongPasswordCannotOpenTheDatabase() = runBlocking {
        val database = TestPingoDatabase.open(context, DATABASE_NAME)
        database.contaDao().upsert(conta("Conta"))
        database.close()

        val comSenhaErrada = TestPingoDatabase.open(context, DATABASE_NAME, "senha-errada")
        val falhou = runCatching { comSenhaErrada.contaDao().observeAtivas().first() }.isFailure

        comSenhaErrada.close()

        assertTrue(falhou)
    }

    private fun ByteArray.contem(alvo: ByteArray): Boolean {
        if (alvo.isEmpty() || alvo.size > size) {
            return false
        }

        return (0..(size - alvo.size)).any { inicio ->
            alvo.indices.all { deslocamento -> this[inicio + deslocamento] == alvo[deslocamento] }
        }
    }

    private fun conta(apelido: String) = ContaEntity(
        publicId = "conta-1",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private companion object {
        const val DATABASE_NAME = "pingo-encryption-test.db"
        val INSTANTE: Instant = Instant.parse("2026-10-22T12:00:00Z")
    }
}
