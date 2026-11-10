package com.pingo.core.notifications

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.parser.RegrasVigentes
import com.pingo.core.parser.VersaoRegras
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegrasParaCaptura @Inject constructor(private val regraParserRepository: RegraParserRepository) {
    private val mutex = Mutex()
    private var compiladas: Map<InstituicaoFinanceira, VersaoRegras>? = null

    suspend fun versaoDe(instituicao: InstituicaoFinanceira): VersaoRegras? = selecionar()[instituicao]

    suspend fun atualizar() {
        mutex.withLock { compiladas = null }
    }

    private suspend fun selecionar(): Map<InstituicaoFinanceira, VersaoRegras> {
        compiladas?.let { return it }

        return mutex.withLock {
            compiladas ?: regraParserRepository.observeRegrasAtivas()
                .first()
                .let(RegrasVigentes::selecionar)
                .also { compiladas = it }
        }
    }
}
