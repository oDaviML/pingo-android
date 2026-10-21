package com.pingo.core.data

import com.pingo.core.model.Lancamento
import kotlinx.coroutines.flow.Flow

interface LancamentoRepository {
    fun observeLancamentos(): Flow<List<Lancamento>>
}
