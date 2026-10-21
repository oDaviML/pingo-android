package com.pingo.core.data

import com.pingo.core.model.Conta
import kotlinx.coroutines.flow.Flow

interface ContaRepository {
    fun observeContas(): Flow<List<Conta>>
}
