package com.pingo.core.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreTokenStoreTest {
    private val store = KeystoreTokenStore(
        context = ApplicationProvider.getApplicationContext<Context>(),
        dispatchers = FakeAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun guardaLeEContinuaLimpando() = runTest {
        store.clear()

        val tokens = StoredTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochMillis = 1_700_000_000_000,
        )

        store.write(tokens)

        val restored = store.read()

        assertEquals(tokens.accessToken, restored?.accessToken)
        assertEquals(tokens.refreshToken, restored?.refreshToken)
        assertEquals(tokens.expiresAtEpochMillis, restored?.expiresAtEpochMillis)

        store.clear()

        assertNull(store.read())
    }
}
