package com.pingo.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingoDatabaseMigrationTest {
    init {
        System.loadLibrary("sqlcipher")
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PingoDatabase::class.java,
        emptyList(),
        SupportOpenHelperFactory(PASSWORD.toByteArray()),
    )

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun schemaAtualConfereComOSchemaExportado() {
        val database = helper.createDatabase(DATABASE_NAME, PingoDatabase.VERSION)

        assertTrue(database.isOpen)
        database.close()
    }

    @Test
    fun migracaoDaVersao1Para2PreservaOsLancamentos() {
        helper.createDatabase(DATABASE_NAME, 1).use { database ->
            database.execSQL(INSERCAO_NA_VERSAO_1)
        }

        val migrado = helper.runMigrationsAndValidate(DATABASE_NAME, PingoDatabase.VERSION, true, PingoMigrations.MIGRACAO_1_2)

        migrado.query("SELECT publicId, valorCentavos, textoBruto FROM lancamentos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("lancamento-1", cursor.getString(0))
            assertEquals(3790L, cursor.getLong(1))
            assertTrue(cursor.isNull(2))
        }
        migrado.close()
    }

    private companion object {
        const val DATABASE_NAME = "pingo-migration-test.db"
        const val PASSWORD = "pingo-test-password"
        const val INSERCAO_NA_VERSAO_1 = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, confianca, motivoPendencia, hashDuplicata, observacao,
                parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm, excluidoEm
            ) VALUES (
                'lancamento-1', 'instrumento-1', NULL, NULL, NULL,
                NULL, NULL, NULL, 3790, 1762788000000, 'DESPESA',
                'LOJA', 'CONFIRMADO', 'NOTIFICACAO', 1.0, NULL, 'hash', NULL,
                1, 1, NULL, 1762788000000, 1762788000000, NULL
            )
        """
    }
}
