package com.pingo.feature.capturestatus.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.SituacaoDaCaptura
import java.time.Instant

enum class ItemDePermissao {
    ACESSO_A_NOTIFICACOES,
    NOTIFICACOES_DO_PINGO,
    BATERIA,
}

@Immutable
data class PermissaoDaCaptura(val item: ItemDePermissao, val concedida: Boolean)

@Immutable
data class LeituraPorApp(val instituicao: InstituicaoFinanceira, val em: Instant?)

@Immutable
data class ContadoresDaCaptura(val reconhecidas: Int = 0, val paraRevisao: Int = 0, val ignoradas: Int = 0, val naoEntendidas: Int = 0)

@Immutable
data class CaptureStatusUiState(
    val carregando: Boolean = true,
    val situacao: SituacaoDaCaptura = SituacaoDaCaptura.ATIVA,
    val motivo: MotivoParada? = null,
    val ultimaLeituraEm: Instant? = null,
    val ultimaLeituraInstituicao: InstituicaoFinanceira? = null,
    val semLeituraDesde: Instant? = null,
    val agora: Instant = Instant.EPOCH,
    val contadores: ContadoresDaCaptura = ContadoresDaCaptura(),
    val leiturasPorApp: List<LeituraPorApp> = emptyList(),
    val permissoes: List<PermissaoDaCaptura> = emptyList(),
    val versaoDasRegras: Int? = null,
    val regrasAtualizadasEm: Instant? = null,
    val falhaAoAtualizarRegras: String? = null,
    val atualizandoRegras: Boolean = false,
    val reconectando: Boolean = false,
    val mensagem: String? = null,
) {
    val parada: Boolean
        get() = situacao != SituacaoDaCaptura.ATIVA

    val ajustePrioritario: ItemDePermissao?
        get() = permissoes.firstOrNull { permissao -> !permissao.concedida }?.item
}
