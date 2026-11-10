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
        ).forEach { tabela ->
            assertTrue("Tabela ausente: $tabela", tabela in tables)
        }

        val colunas = mutableListOf<String>()
        database.openHelper.readableDatabase.query("PRAGMA table_info(lancamentos)").use { cursor ->
            while (cursor.moveToNext()) {
                colunas.add(cursor.getString(1))
            }
        }
        assertTrue("Coluna textoBruto ausente", "textoBruto" in colunas)

        database.close()
    }
}
