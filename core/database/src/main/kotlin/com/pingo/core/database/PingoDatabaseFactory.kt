package com.pingo.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object PingoDatabaseFactory {
    fun create(context: Context, keyProvider: DatabaseKeyProvider): PingoDatabase =
        build(context, PingoDatabase.NAME, keyProvider.loadOrCreate())

    internal fun create(context: Context, name: String, passphrase: ByteArray): PingoDatabase = build(context, name, passphrase)

    private fun build(context: Context, name: String, passphrase: ByteArray): PingoDatabase {
        System.loadLibrary("sqlcipher")

        return Room.databaseBuilder(context, PingoDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(PingoMigrations.MIGRACAO_1_2, PingoMigrations.MIGRACAO_2_3, PingoMigrations.MIGRACAO_3_4)
            .addCallback(ForeignKeysCallback)
            .build()
    }

    private object ForeignKeysCallback : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }
}
