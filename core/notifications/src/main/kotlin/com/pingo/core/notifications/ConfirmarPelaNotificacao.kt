package com.pingo.core.notifications

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmarPelaNotificacao @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val confirmarPendencias: ConfirmarPendencias,
    private val notificacoes: RevisaoNotificacoes,
) {
    suspend fun confirmar(publicId: String) {
        val lancamento = lancamentoRepository.buscar(publicId)?.takeIf { it.excluidoEm == null }

        if (lancamento == null || lancamento.status != StatusLancamento.PENDENTE) {
            notificacoes.cancelar(publicId)
            return
        }

        val confirmacao = confirmarPendencias.confirmar(listOf(publicId))
        if (confirmacao.confirmados > 0) {
            notificacoes.cancelar(publicId)
            return
        }

        notificacoes.postar(lancamento.paraNotificar(), precisaCorrigir = true)
    }

    private suspend fun Lancamento.paraNotificar(): PendenciaParaNotificar = PendenciaParaNotificar(
        publicId = publicId,
        estabelecimento = estabelecimento,
        valorCentavos = valorCentavos,
        categoria = categoriaPublicId?.let { categoriaRepository.buscar(it)?.nome },
    )
}
