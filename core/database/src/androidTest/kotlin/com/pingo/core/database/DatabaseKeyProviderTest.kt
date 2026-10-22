package com.pingo.core.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context.deleteDatabase(PingoDatabase.NAME)
        preferences.edit().clear().commit()
        deleteKeystoreKey()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(PingoDatabase.NAME)
        preferences.edit().clear().commit()
        deleteKeystoreKey()
    }

    @Test
    fun passphraseIsStable() {
        val provider = DatabaseKeyProvider(context)

        assertArrayEquals(provider.loadOrCreate(), provider.loadOrCreate())
    }

    @Test
    fun corruptedPassphraseDeletesTheDatabase() {
        val provider = DatabaseKeyProvider(context)
        provider.loadOrCreate()

        context.openOrCreateDatabase(PingoDatabase.NAME, Context.MODE_PRIVATE, null).close()
        assertTrue(context.getDatabasePath(PingoDatabase.NAME).exists())

        preferences.edit().putString(PASSPHRASE_KEY, "corrompida").commit()

        val novaPassphrase = provider.loadOrCreate()

        assertFalse(context.getDatabasePath(PingoDatabase.NAME).exists())
        assertTrue(novaPassphrase.isNotEmpty())
    }

    private fun deleteKeystoreKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (keyStore.containsAlias(ALIAS)) {
            keyStore.deleteEntry(ALIAS)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "pingo_database_key"
        const val PREFERENCES_NAME = "pingo_database_key"
        const val PASSPHRASE_KEY = "passphrase"
    }
}
