package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingoDatabaseSchemaTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        TestPingoDatabase.delete(context)
    }

    @After
    fun tearDown() {
        TestPingoDatabase.delete(context)
    }

    @Test
    fun schemaV1ContainsTheModelTables() = runBlocking {
        val database = TestPingoDatabase.open(context)
        val tables = mutableListOf<String>()

        database.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table'")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }

        assertEquals(PingoDatabase.VERSION, database.openHelper.readableDatabase.version)

        listOf(
            "usuarios",
            "contas",
            "instrumentos",
            "lancamentos",
            "categorias",
            "regras_categoria",
            "faturas",
            "assinaturas",
            "regras_parser",
            "configuracoes_cartao",
            "referencias_saldo_voucher",
            "recargas_programadas_voucher",
        ).forEach { tabela ->
            assertTrue("Tabela ausente: $tabela", tabela in tables)
        }

        val colunas = colunasDe(database, "lancamentos")
        assertTrue("Coluna textoBruto ausente", colunas.any { coluna -> coluna.nome == "textoBruto" })
        assertTrue(
            "instrumentoPublicId deveria aceitar nulo",
            colunas.single { coluna -> coluna.nome == "instrumentoPublicId" }.aceitaNulo,
        )
        assertTrue(
            "instituicaoOrigem deveria existir e aceitar nulo",
            colunas.single { coluna -> coluna.nome == "instituicaoOrigem" }.aceitaNulo,
        )
        assertTrue(
            "tipoSemTransferencia deveria existir e aceitar nulo",
            colunas.single { coluna -> coluna.nome == "tipoSemTransferencia" }.aceitaNulo,
        )
        assertTrue(
            "estabelecimentoNormalizado deveria existir e aceitar nulo",
            colunas.single { coluna -> coluna.nome == "estabelecimentoNormalizado" }.aceitaNulo,
        )
        assertTrue(
            "beneficio deveria existir e aceitar nulo",
            colunas.single { coluna -> coluna.nome == "beneficio" }.aceitaNulo,
        )
        assertTrue(
            "saldoObservadoCentavos deveria existir e aceitar nulo",
            colunas.single { coluna -> coluna.nome == "saldoObservadoCentavos" }.aceitaNulo,
        )

        val colunasDaAssinatura = colunasDe(database, "assinaturas")
        assertTrue(
            "instrumentoPublicId da assinatura deveria existir e aceitar nulo",
            colunasDaAssinatura.single { coluna -> coluna.nome == "instrumentoPublicId" }.aceitaNulo,
        )

        val colunasDaFatura = colunasDe(database, "faturas")
        assertTrue(
            "instrumentoPublicId da fatura continua obrigatório",
            !colunasDaFatura.single { coluna -> coluna.nome == "instrumentoPublicId" }.aceitaNulo,
        )

        database.close()
    }

    private fun colunasDe(database: PingoDatabase, tabela: String): List<Coluna> {
        val colunas = mutableListOf<Coluna>()
        database.openHelper.readableDatabase.query("PRAGMA table_info($tabela)").use { cursor ->
            while (cursor.moveToNext()) {
                colunas.add(Coluna(cursor.getString(1), cursor.getInt(3) == 0))
            }
        }
        return colunas
    }

    private data class Coluna(val nome: String, val aceitaNulo: Boolean)
}
