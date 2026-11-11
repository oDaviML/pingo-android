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

        val migrado = helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, PingoMigrations.MIGRACAO_1_2)

        migrado.query("SELECT publicId, valorCentavos, textoBruto FROM lancamentos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("lancamento-1", cursor.getString(0))
            assertEquals(3790L, cursor.getLong(1))
            assertTrue(cursor.isNull(2))
        }
        migrado.close()
    }

    @Test
    fun migracaoDaVersao2Para3PreservaDadosEVinculos() {
        helper.createDatabase(DATABASE_NAME, 2).use { database ->
            database.execSQL(INSERCAO_NA_VERSAO_2_PAI)
            database.execSQL(INSERCAO_NA_VERSAO_2_FILHO)
        }

        val migrado = helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, PingoMigrations.MIGRACAO_2_3)

        migrado
            .query(
                "SELECT publicId, instrumentoPublicId, compraPaiPublicId, textoBruto, excluidoEm, instituicaoOrigem " +
                    "FROM lancamentos ORDER BY publicId",
            )
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("lancamento-1", cursor.getString(0))
                assertEquals("instrumento-1", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertEquals("texto do pai", cursor.getString(3))
                assertTrue(cursor.isNull(4))
                assertTrue(cursor.isNull(5))

                assertTrue(cursor.moveToNext())
                assertEquals("lancamento-2", cursor.getString(0))
                assertEquals("instrumento-1", cursor.getString(1))
                assertEquals("lancamento-1", cursor.getString(2))
                assertTrue(cursor.isNull(3))
                assertTrue(!cursor.isNull(4))
            }

        val indices = mutableListOf<String>()
        migrado.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'lancamentos'").use { cursor ->
            while (cursor.moveToNext()) {
                indices.add(cursor.getString(0))
            }
        }

        assertTrue(indices.containsAll(INDICES_DE_LANCAMENTOS))

        migrado.close()
    }

    @Test
    fun migracaoDaVersao3Para4PreservaTransferenciasELegadosSemInventarDirecao() {
        helper.createDatabase(DATABASE_NAME, 3).use { database ->
            database.execSQL(INSERCAO_NA_VERSAO_3_TRANSFERENCIA)
            database.execSQL(INSERCAO_NA_VERSAO_3_LEGADO)
        }

        val migrado = helper.runMigrationsAndValidate(DATABASE_NAME, 4, true, PingoMigrations.MIGRACAO_3_4)

        migrado
            .query("SELECT publicId, tipo, transferenciaId, tipoSemTransferencia FROM lancamentos ORDER BY publicId")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("lancamento-legado", cursor.getString(0))
                assertEquals("DESPESA", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))

                assertTrue(cursor.moveToNext())
                assertEquals("lancamento-transferencia", cursor.getString(0))
                assertEquals("TRANSFERENCIA", cursor.getString(1))
                assertEquals("transferencia-1", cursor.getString(2))
                assertTrue(cursor.isNull(3))
            }

        migrado.close()
    }

    @Test
    fun cadeiaDaVersao1Para4PreservaDadosEVinculos() {
        helper.createDatabase(DATABASE_NAME, 1).use { database ->
            database.execSQL(INSERCAO_NA_VERSAO_1)
            database.execSQL(INSERCAO_NA_VERSAO_1_FILHO)
        }

        val migrado = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            PingoDatabase.VERSION,
            true,
            PingoMigrations.MIGRACAO_1_2,
            PingoMigrations.MIGRACAO_2_3,
            PingoMigrations.MIGRACAO_3_4,
        )

        migrado
            .query(
                "SELECT publicId, instrumentoPublicId, compraPaiPublicId, textoBruto, tipoSemTransferencia " +
                    "FROM lancamentos ORDER BY publicId",
            )
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("lancamento-1", cursor.getString(0))
                assertEquals("instrumento-1", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))

                assertTrue(cursor.moveToNext())
                assertEquals("lancamento-3", cursor.getString(0))
                assertEquals("lancamento-1", cursor.getString(2))
                assertTrue(cursor.isNull(4))
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
        const val INSERCAO_NA_VERSAO_1_FILHO = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, confianca, motivoPendencia, hashDuplicata, observacao,
                parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm, excluidoEm
            ) VALUES (
                'lancamento-3', 'instrumento-1', NULL, NULL, NULL,
                'lancamento-1', NULL, NULL, 1795, 1762788000000, 'DESPESA',
                'LOJA', 'CONFIRMADO', 'NOTIFICACAO', 1.0, NULL, NULL, NULL,
                2, 2, NULL, 1762788000000, 1762788000000, NULL
            )
        """
        const val INSERCAO_NA_VERSAO_2_PAI = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, confianca, motivoPendencia, hashDuplicata, observacao,
                parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm, excluidoEm,
                textoBruto
            ) VALUES (
                'lancamento-1', 'instrumento-1', NULL, NULL, NULL,
                NULL, NULL, NULL, 3790, 1762788000000, 'DESPESA',
                'LOJA', 'CONFIRMADO', 'NOTIFICACAO', 1.0, NULL, 'hash', NULL,
                1, 1, NULL, 1762788000000, 1762788000000, NULL,
                'texto do pai'
            )
        """
        const val INSERCAO_NA_VERSAO_2_FILHO = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, confianca, motivoPendencia, hashDuplicata, observacao,
                parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm, excluidoEm,
                textoBruto
            ) VALUES (
                'lancamento-2', 'instrumento-1', NULL, NULL, NULL,
                'lancamento-1', NULL, NULL, 1795, 1762788000000, 'DESPESA',
                'LOJA', 'PENDENTE', 'NOTIFICACAO', 0.8, 'INSTRUMENTO_AMBIGUO', NULL, NULL,
                2, 2, NULL, 1762788000000, 1762788000000, 1762788000000,
                NULL
            )
        """
        const val INSERCAO_NA_VERSAO_3_TRANSFERENCIA = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, instituicaoOrigem, confianca, motivoPendencia, hashDuplicata,
                observacao, parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm,
                excluidoEm, textoBruto
            ) VALUES (
                'lancamento-transferencia', 'instrumento-1', NULL, NULL, NULL,
                NULL, NULL, 'transferencia-1', 5000, 1762788000000, 'TRANSFERENCIA',
                NULL, 'CONFIRMADO', 'MANUAL', NULL, 1.0, NULL, NULL,
                NULL, 1, 1, NULL, 1762788000000, 1762788000000,
                NULL, NULL
            )
        """
        const val INSERCAO_NA_VERSAO_3_LEGADO = """
            INSERT INTO lancamentos (
                publicId, instrumentoPublicId, categoriaPublicId, faturaPublicId, assinaturaPublicId,
                compraPaiPublicId, estornoDePublicId, transferenciaId, valorCentavos, dataHora, tipo,
                estabelecimento, status, origem, instituicaoOrigem, confianca, motivoPendencia, hashDuplicata,
                observacao, parcelaNumero, totalParcelas, valorTotalCompraCentavos, criadoEm, atualizadoEm,
                excluidoEm, textoBruto
            ) VALUES (
                'lancamento-legado', 'instrumento-1', NULL, NULL, NULL,
                NULL, NULL, NULL, 3790, 1762788000000, 'DESPESA',
                'LOJA', 'CONFIRMADO', 'NOTIFICACAO', 'ITAU', 1.0, NULL, 'hash-legado',
                NULL, 1, 1, NULL, 1762788000000, 1762788000000,
                NULL, 'texto local'
            )
        """
        val INDICES_DE_LANCAMENTOS = listOf(
            "index_lancamentos_instrumentoPublicId",
            "index_lancamentos_categoriaPublicId",
            "index_lancamentos_faturaPublicId",
            "index_lancamentos_assinaturaPublicId",
            "index_lancamentos_compraPaiPublicId",
            "index_lancamentos_estornoDePublicId",
            "index_lancamentos_dataHora",
            "index_lancamentos_atualizadoEm",
            "index_lancamentos_excluidoEm",
            "index_lancamentos_hashDuplicata",
        )
    }
}
