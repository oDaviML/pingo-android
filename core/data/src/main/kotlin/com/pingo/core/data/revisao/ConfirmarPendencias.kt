package com.pingo.core.data.revisao

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

data class ResultadoDaConfirmacao(val confirmados: Int, val mantidos: Int)

class ConfirmarPendencias @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val clock: Clock,
    private val transacaoLocal: TransacaoLocal = TransacaoLocal.Direta,
) {
    suspend fun confirmar(publicIds: Collection<String>): ResultadoDaConfirmacao = transacaoLocal.executar {
        val pendentes = lancamentoRepository.observeLancamentos().first()
            .filter { it.publicId in publicIds && it.status == StatusLancamento.PENDENTE }

        confirmarPendentes(pendentes)
    }

    suspend fun confirmarTodos(): ResultadoDaConfirmacao = transacaoLocal.executar {
        confirmarPendentes(
            lancamentoRepository.observeLancamentos().first().filter { it.status == StatusLancamento.PENDENTE },
        )
    }

    private suspend fun confirmarPendentes(pendentes: List<Lancamento>): ResultadoDaConfirmacao {
        val elegiveis = pendentes.filter { !exigeRevisaoVoucher(it.motivoPendencia) && instrumentoValido(it.instrumentoPublicId) }
        val agora = clock.instant()
        val confirmados = elegiveis.map {
            it.copy(status = StatusLancamento.CONFIRMADO, motivoPendencia = null, atualizadoEm = agora)
        }

        if (confirmados.isNotEmpty()) {
            lancamentoRepository.salvarTodos(confirmados)
        }

        return ResultadoDaConfirmacao(confirmados = confirmados.size, mantidos = pendentes.size - confirmados.size)
    }

    private fun exigeRevisaoVoucher(motivo: String?): Boolean = motivo == MotivoPendencia.DIVERGENCIA_RECARGA.name ||
        motivo == MotivoPendencia.BENEFICIO_AMBIGUO.name

    private suspend fun instrumentoValido(publicId: String?): Boolean {
        val id = publicId ?: return false
        val instrumento = instrumentoRepository.buscar(id) ?: return false

        return instrumento.excluidoEm == null
    }
}
