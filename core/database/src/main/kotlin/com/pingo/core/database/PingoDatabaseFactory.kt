package com.pingo.core.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object PingoDatabaseFactory {
    fun create(context: Context, keyProvider: DatabaseKeyProvider): PingoDatabase {
        System.loadLibrary("sqlcipher")

        return Room.databaseBuilder(context, PingoDatabase::class.java, PingoDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyProvider.loadOrCreate()))
            .build()
    }
}
