package com.pingo.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object PingoDatabaseFactory {
    fun create(context: Context, keyProvider: DatabaseKeyProvider): PingoDatabase {
        System.loadLibrary("sqlcipher")

        return Room.databaseBuilder(context, PingoDatabase::class.java, PingoDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyProvider.loadOrCreate()))
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
    }
}
