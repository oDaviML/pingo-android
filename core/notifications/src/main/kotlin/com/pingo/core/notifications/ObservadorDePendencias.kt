package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservadorDePendencias @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val notificacoes: RevisaoNotificacoes,
    private val dispatchers: AppDispatchers,
) : IniciadorDeObservacaoDePendencias {
    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val notificadas = mutableSetOf<String>()
    private var iniciado = false

    override fun iniciar() {
        if (iniciado) {
            return
        }

        iniciado = true
        escopo.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                categoriaRepository.observeCategorias(),
            ) { lancamentos, categorias -> lancamentos to categorias }
                .collect { (lancamentos, categorias) -> runCatching { sincronizar(lancamentos, categorias) } }
        }
    }

    internal suspend fun sincronizar(lancamentos: List<Lancamento>, categorias: List<Categoria>) {
        val pendentes = lancamentos.filter { it.status == StatusLancamento.PENDENTE && it.excluidoEm == null }
        val pendentesIds = pendentes.map { it.publicId }.toSet()
        val nomesDasCategorias = categorias.associate { it.publicId to it.nome }
        val novas = pendentes.filter { it.publicId !in notificadas }

        if (novas.isNotEmpty() && notificacoes.habilitada()) {
            novas.forEach { lancamento ->
                if (notificacoes.postar(lancamento.paraNotificar(nomesDasCategorias), precisaCorrigir = false)) {
                    notificadas += lancamento.publicId
                }
            }
        }

        notificadas.filterNot { it in pendentesIds }.toList().forEach { publicId ->
            notificacoes.cancelar(publicId)
            notificadas -= publicId
        }

        notificacoes.cancelarAusentes(pendentesIds)
    }

    private fun Lancamento.paraNotificar(nomesDasCategorias: Map<String, String>): PendenciaParaNotificar = PendenciaParaNotificar(
        publicId = publicId,
        estabelecimento = estabelecimento,
        valorCentavos = valorCentavos,
        categoria = categoriaPublicId?.let { nomesDasCategorias[it] },
    )
}
