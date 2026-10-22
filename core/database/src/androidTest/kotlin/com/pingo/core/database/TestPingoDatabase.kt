package com.pingo.core.database

import android.content.Context

internal object TestPingoDatabase {
    const val NAME = "pingo-test.db"

    private const val PASSWORD = "pingo-test-password"

    fun open(context: Context, name: String = NAME, password: String = PASSWORD): PingoDatabase =
        PingoDatabaseFactory.create(context, name, password.toByteArray())

    fun delete(context: Context, name: String = NAME) {
        context.deleteDatabase(name)
    }
}
