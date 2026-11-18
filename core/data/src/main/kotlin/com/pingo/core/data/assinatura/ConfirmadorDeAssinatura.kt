package com.pingo.core.data.assinatura

import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento

interface ConfirmadorDeAssinatura {
    suspend fun prepararAoSalvarManual(lancamento: Lancamento, categoria: Categoria?): Lancamento

    suspend fun aoEditarLancamento(original: Lancamento, atualizado: Lancamento, novaCategoria: Categoria?): Lancamento

    object NoOp : ConfirmadorDeAssinatura {
        override suspend fun prepararAoSalvarManual(lancamento: Lancamento, categoria: Categoria?): Lancamento = lancamento

        override suspend fun aoEditarLancamento(original: Lancamento, atualizado: Lancamento, novaCategoria: Categoria?): Lancamento =
            atualizado
    }
}
