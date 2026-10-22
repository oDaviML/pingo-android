package com.pingo.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
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
    fun schemaV1MatchesTheExportedSchema() {
        val database = helper.createDatabase(DATABASE_NAME, PingoDatabase.VERSION)

        assertTrue(database.isOpen)
        database.close()
    }

    private companion object {
        const val DATABASE_NAME = "pingo-migration-test.db"
        const val PASSWORD = "pingo-test-password"
    }
}
