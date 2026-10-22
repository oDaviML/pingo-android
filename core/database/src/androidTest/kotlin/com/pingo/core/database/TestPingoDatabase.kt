package com.pingo.core.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

internal object TestPingoDatabase {
    const val NAME = "pingo-test.db"

    private const val PASSWORD = "pingo-test-password"

    fun open(context: Context, name: String = NAME, password: String = PASSWORD): PingoDatabase {
        System.loadLibrary("sqlcipher")

        return Room.databaseBuilder(context, PingoDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(password.toByteArray()))
            .build()
    }

    fun delete(context: Context, name: String = NAME) {
        context.deleteDatabase(name)
    }
}
