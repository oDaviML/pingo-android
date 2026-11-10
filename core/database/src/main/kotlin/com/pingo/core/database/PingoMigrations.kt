package com.pingo.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PingoMigrations {
    val MIGRACAO_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN textoBruto TEXT")
        }
    }
}
