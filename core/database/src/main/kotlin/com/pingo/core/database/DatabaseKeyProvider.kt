package com.pingo.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseKeyProvider @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun loadOrCreate(): ByteArray {
        val cifrada = preferences.getString(PREFERENCE_PASSPHRASE, null)
        val chave = obterOuCriarChave()

        if (cifrada != null) {
            val passphrase = runCatching { decifrar(chave, cifrada) }.getOrNull()

            if (passphrase != null) {
                return passphrase
            }

            apagarBanco()
        }

        return criarPassphrase(chave)
    }

    private val preferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun obterOuCriarChave(): SecretKey = try {
        carregarOuGerarChave()
    } catch (exception: GeneralSecurityException) {
        reiniciarChave()
        carregarOuGerarChave()
    }

    private fun carregarOuGerarChave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existente = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry

        if (existente != null) {
            return existente.secretKey
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

    private fun reiniciarChave() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (keyStore.containsAlias(ALIAS)) {
            keyStore.deleteEntry(ALIAS)
        }
    }

    private fun criarPassphrase(chave: SecretKey): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cifrada = cifrar(chave, passphrase)

        preferences.edit().putString(PREFERENCE_PASSPHRASE, cifrada).apply()

        return passphrase
    }

    private fun cifrar(chave: SecretKey, valor: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, chave)
        val pacote = cipher.iv + cipher.doFinal(valor)

        return Base64.getEncoder().encodeToString(pacote)
    }

    private fun decifrar(chave: SecretKey, valor: String): ByteArray {
        val pacote = Base64.getDecoder().decode(valor)
        require(pacote.size > GCM_IV_BYTES)

        val iv = pacote.copyOfRange(0, GCM_IV_BYTES)
        val cifrado = pacote.copyOfRange(GCM_IV_BYTES, pacote.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, chave, GCMParameterSpec(GCM_TAG_BITS, iv))

        return cipher.doFinal(cifrado)
    }

    private fun apagarBanco() {
        context.deleteDatabase(PingoDatabase.NAME)
        preferences.edit().remove(PREFERENCE_PASSPHRASE).apply()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "pingo_database_key"
        const val PREFERENCES_NAME = "pingo_database_key"
        const val PREFERENCE_PASSPHRASE = "passphrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BITS = 256
        const val PASSPHRASE_BYTES = 32
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
