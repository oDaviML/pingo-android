package com.pingo.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.pingo.core.common.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : TokenStore {
    private val preferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun read(): StoredTokens? = withContext(dispatchers.io) {
        val accessToken = decifrarPreferencia(KEY_ACCESS_TOKEN) ?: return@withContext null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)

        StoredTokens(
            accessToken = accessToken,
            refreshToken = decifrarPreferencia(KEY_REFRESH_TOKEN),
            expiresAtEpochMillis = expiresAt,
        )
    }

    override suspend fun write(tokens: StoredTokens) = withContext(dispatchers.io) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, cifrar(tokens.accessToken))
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken?.let(::cifrar))
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtEpochMillis)
            .apply()
    }

    override suspend fun clear() = withContext(dispatchers.io) {
        preferences.edit().clear().apply()
    }

    private fun decifrarPreferencia(key: String): String? {
        val value = preferences.getString(key, null) ?: return null

        return runCatching { decifrar(value) }.getOrElse {
            preferences.edit().remove(key).apply()
            null
        }
    }

    private fun cifrar(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obterOuCriarChave())
        val packageBytes = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(packageBytes, Base64.NO_WRAP)
    }

    private fun decifrar(value: String): String {
        val packageBytes = Base64.decode(value, Base64.NO_WRAP)
        require(packageBytes.size > GCM_IV_BYTES)

        val iv = packageBytes.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = packageBytes.copyOfRange(GCM_IV_BYTES, packageBytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, obterOuCriarChave(), GCMParameterSpec(GCM_TAG_BITS, iv))

        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun obterOuCriarChave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry

        if (existing != null) {
            return existing.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specification = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .build()

        generator.init(specification)

        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "pingo_auth_token_key"
        const val PREFERENCES_NAME = "pingo_auth_tokens"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BITS = 256
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
