package com.pingo.core.data.impl

import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.assinatura.DetectorDeAssinaturas
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmadorDeAssinaturaPorCategoria @Inject constructor(
    private val assinaturaDao: AssinaturaDao,
    private val lancamentoDao: LancamentoDao,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
    private val clock: Clock,
) : ConfirmadorDeAssinatura {
    override suspend fun prepararAoSalvarManual(lancamento: Lancamento, categoria: Categoria?): Lancamento =
        if (ehCategoriaDeAssinaturas(categoria)) {
            confirmarEVincular(lancamento)
        } else {
            lancamento
        }

    override suspend fun aoEditarLancamento(original: Lancamento, atualizado: Lancamento, novaCategoria: Categoria?): Lancamento {
        if (!DetectorDeAssinaturas.elegivelParaDeteccao(atualizado)) {
            val assinaturaId = original.assinaturaPublicId
            if (assinaturaId != null) {
                removerSeOrfa(assinaturaId, atualizado.publicId)
            }
            return atualizado.copy(assinaturaPublicId = null)
        }
        return when {
            ehCategoriaDeAssinaturas(novaCategoria) -> confirmarEVincular(atualizado)
            original.assinaturaPublicId != null && deveDesvincular(novaCategoria) -> {
                val assinaturaId = requireNotNull(original.assinaturaPublicId)
                removerSeOrfa(assinaturaId, atualizado.publicId)
                atualizado.copy(assinaturaPublicId = null)
            }
            else -> atualizado
        }
    }

    private suspend fun confirmarEVincular(lancamento: Lancamento): Lancamento {
        val dados = extrairDadosElegiveis(lancamento) ?: return lancamento
        val existente = assinaturaDao.buscarPorChaveComInstrumento(dados.instrumentoId, dados.normalizado)
        val agora = clock.instant()
        val entidade = when {
            existente == null -> criarNovaConfirmada(dados, agora)
            existente.status == StatusAssinatura.SUGERIDA -> promoverExistente(existente, dados, agora)
            existente.status == StatusAssinatura.CONFIRMADA -> existente
            else -> null
        } ?: return lancamento

        if (existente == null || existente.status == StatusAssinatura.SUGERIDA) {
            assinaturaDao.upsert(entidade)
        }
        return lancamento.copy(assinaturaPublicId = entidade.publicId)
    }

    private suspend fun removerSeOrfa(assinaturaPublicId: String, lancamentoEditadoId: String) {
        val restantes = lancamentoDao.listarPorAssinatura(assinaturaPublicId)
            .filter { it.publicId != lancamentoEditadoId && it.excluidoEm == null }
        if (restantes.isEmpty()) {
            assinaturaDao.marcarExcluida(assinaturaPublicId, clock.instant())
            agendadorDeAvisos.cancelarParaAssinatura(assinaturaPublicId)
        }
    }

    private fun criarNovaConfirmada(dados: DadosAssinatura, agora: java.time.Instant): AssinaturaEntity = AssinaturaEntity(
        publicId = UUID.randomUUID().toString(),
        estabelecimento = dados.estabelecimento.take(LIMITE_ESTABELECIMENTO),
        estabelecimentoNormalizado = dados.normalizado.take(LIMITE_ESTABELECIMENTO),
        valorReferenciaCentavos = dados.valorCentavos,
        ultimaCobranca = dados.dataLocal,
        proximaCobrancaEstimada = DetectorDeAssinaturas.calcularProximaCobranca(dados.dataLocal),
        status = StatusAssinatura.CONFIRMADA,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
        instrumentoPublicId = dados.instrumentoId,
    )

    private fun promoverExistente(existente: AssinaturaEntity, dados: DadosAssinatura, agora: java.time.Instant): AssinaturaEntity {
        val ultima = maxOfNotNull(existente.ultimaCobranca, dados.dataLocal)
        return existente.copy(
            estabelecimento = dados.estabelecimento.take(LIMITE_ESTABELECIMENTO),
            valorReferenciaCentavos = dados.valorCentavos,
            ultimaCobranca = ultima,
            proximaCobrancaEstimada = DetectorDeAssinaturas.calcularProximaCobranca(ultima),
            status = StatusAssinatura.CONFIRMADA,
            atualizadoEm = agora,
        )
    }

    private fun extrairDadosElegiveis(lancamento: Lancamento): DadosAssinatura? {
        val instrumentoId = lancamento.instrumentoPublicId
        val estabelecimento = lancamento.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val normalizado = estabelecimento?.let(NormalizadorDeAssinatura::normalizar)
        val valido = DetectorDeAssinaturas.elegivelParaDeteccao(lancamento) &&
            instrumentoId != null &&
            estabelecimento != null &&
            normalizado != null

        return if (valido) {
            DadosAssinatura(
                instrumentoId = instrumentoId!!,
                estabelecimento = estabelecimento!!,
                normalizado = normalizado!!,
                valorCentavos = lancamento.valorCentavos,
                dataLocal = ZonedDateTime.ofInstant(lancamento.dataHora, DetectorDeAssinaturas.ZONA_SAO_PAULO).toLocalDate(),
            )
        } else {
            null
        }
    }

    private fun ehCategoriaDeAssinaturas(categoria: Categoria?): Boolean = categoria != null &&
        categoria.tipo == TipoCategoria.DESPESA &&
        categoria.nome.equals(NOME_ASSINATURAS, ignoreCase = true)

    private fun deveDesvincular(novaCategoria: Categoria?): Boolean = novaCategoria != null &&
        !ehCategoriaDeAssinaturas(novaCategoria) &&
        !novaCategoria.nome.equals(NOME_OUTROS, ignoreCase = true)

    private fun maxOfNotNull(atual: java.time.LocalDate?, candidato: java.time.LocalDate): java.time.LocalDate =
        if (atual != null && atual.isAfter(candidato)) atual else candidato

    private data class DadosAssinatura(
        val instrumentoId: String,
        val estabelecimento: String,
        val normalizado: String,
        val valorCentavos: Long,
        val dataLocal: java.time.LocalDate,
    )

    private companion object {
        const val NOME_ASSINATURAS = "Assinaturas"
        const val NOME_OUTROS = "Outros"
        const val LIMITE_ESTABELECIMENTO = 200
    }
}
