package com.pingo.core.data.revisao

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

data class ResultadoDaConfirmacao(val confirmados: Int, val mantidos: Int)

class ConfirmarPendencias @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val clock: Clock,
) {
    suspend fun confirmar(publicIds: Collection<String>): ResultadoDaConfirmacao {
        val pendentes = lancamentoRepository.observeLancamentos().first()
            .filter { it.publicId in publicIds && it.status == StatusLancamento.PENDENTE }

        return confirmarPendentes(pendentes)
    }

    suspend fun confirmarTodos(): ResultadoDaConfirmacao = confirmarPendentes(
        lancamentoRepository.observeLancamentos().first().filter { it.status == StatusLancamento.PENDENTE },
    )

    private suspend fun confirmarPendentes(pendentes: List<Lancamento>): ResultadoDaConfirmacao {
        val elegiveis = pendentes.filter { instrumentoValido(it.instrumentoPublicId) }
        val agora = clock.instant()
        val confirmados = elegiveis.map {
            it.copy(status = StatusLancamento.CONFIRMADO, motivoPendencia = null, atualizadoEm = agora)
        }

        if (confirmados.isNotEmpty()) {
            lancamentoRepository.salvarTodos(confirmados)
        }

        return ResultadoDaConfirmacao(confirmados = confirmados.size, mantidos = pendentes.size - confirmados.size)
    }

    private suspend fun instrumentoValido(publicId: String?): Boolean {
        val id = publicId ?: return false
        val instrumento = instrumentoRepository.buscar(id) ?: return false

        return instrumento.excluidoEm == null
    }
}
