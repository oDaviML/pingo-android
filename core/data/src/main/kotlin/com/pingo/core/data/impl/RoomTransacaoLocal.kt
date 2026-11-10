package com.pingo.core.data.impl

import androidx.room.withTransaction
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.database.PingoDatabase
import javax.inject.Inject

class RoomTransacaoLocal @Inject constructor(private val database: PingoDatabase) : TransacaoLocal {
    override suspend fun <T> executar(bloco: suspend () -> T): T = database.withTransaction(bloco)
}
