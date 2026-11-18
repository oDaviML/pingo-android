package com.pingo.core.data.impl

import com.pingo.core.data.AssinaturaRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.model.Assinatura
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.assinatura.ChaveAssinatura
import com.pingo.core.model.assinatura.DetectorDeAssinaturas
import com.pingo.core.model.assinatura.SugestaoDeAssinatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class AssinaturaRepositoryImpl @Inject constructor(
    private val assinaturaDao: AssinaturaDao,
    private val lancamentoDao: LancamentoDao,
    private val categoriaDao: CategoriaDao,
    private val transacaoLocal: TransacaoLocal,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
    private val clock: Clock,
) : AssinaturaRepository {
    override fun observeAssinaturas(): Flow<List<Assinatura>> =
        assinaturaDao.observeAtivas().map { assinaturas -> assinaturas.map { it.toModel() } }

    override fun observeConfirmadas(): Flow<List<Assinatura>> =
        assinaturaDao.observePorStatus(StatusAssinatura.CONFIRMADA).map { lista -> lista.map { it.toModel() } }

    override fun observeSugestoes(): Flow<List<SugestaoDeAssinatura>> = combine(
        lancamentoDao.observeElegiveisParaDeteccao(),
        assinaturaDao.observeAtivas(),
    ) { lancamentos, assinaturas ->
        val chavesIgnoradas = assinaturas
            .asSequence()
            .filter { it.excluidoEm == null && it.instrumentoPublicId != null && it.status != StatusAssinatura.SUGERIDA }
            .map { ChaveAssinatura(requireNotNull(it.instrumentoPublicId), it.estabelecimentoNormalizado) }
            .toSet()
        DetectorDeAssinaturas.detectarSugestoes(lancamentos.map { it.toModel() }, chavesIgnoradas)
    }

    override fun observeCobrancasDaAssinatura(assinaturaPublicId: String): Flow<List<Lancamento>> =
        lancamentoDao.observePorAssinatura(assinaturaPublicId).map { lista -> lista.map { it.toModel() } }

    override suspend fun buscar(publicId: String): Assinatura? = assinaturaDao.buscar(publicId)?.toModel()

    override suspend fun salvar(assinatura: Assinatura) {
        assinaturaDao.upsert(assinatura.toEntity())
    }

    override suspend fun salvarTodas(assinaturas: List<Assinatura>) {
        assinaturaDao.upsertAll(assinaturas.map { it.toEntity() })
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        transacaoLocal.executar {
            assinaturaDao.marcarExcluida(publicId, quando)
            agendadorDeAvisos.cancelarParaAssinatura(publicId)
        }
    }

    override suspend fun confirmarSugestao(sugestao: SugestaoDeAssinatura): Assinatura {
        val resultado = transacaoLocal.executar {
            val agora = clock.instant()
            val existente = assinaturaDao.buscarPorChaveComInstrumento(
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
            )
            val entidade = AssinaturaEntity(
                publicId = existente?.publicId ?: UUID.randomUUID().toString(),
                estabelecimento = sugestao.estabelecimentoExibicao,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
                valorReferenciaCentavos = sugestao.valorReferenciaCentavos,
                ultimaCobranca = sugestao.ultimaCobranca,
                proximaCobrancaEstimada = sugestao.proximaCobrancaEstimada,
                status = StatusAssinatura.CONFIRMADA,
                criadoEm = existente?.criadoEm ?: agora,
                atualizadoEm = agora,
                excluidoEm = null,
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
            )
            assinaturaDao.upsert(entidade)
            vincularQualificadores(sugestao, entidade.publicId, agora)
            entidade.toModel()
        }
        agendadorDeAvisos.reconciliarTodas()
        return resultado
    }

    override suspend fun recusarSugestao(sugestao: SugestaoDeAssinatura): Assinatura {
        val resultado = transacaoLocal.executar {
            val agora = clock.instant()
            val existente = assinaturaDao.buscarPorChaveComInstrumento(
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
            )
            val entidade = AssinaturaEntity(
                publicId = existente?.publicId ?: UUID.randomUUID().toString(),
                estabelecimento = sugestao.estabelecimentoExibicao,
                estabelecimentoNormalizado = sugestao.chave.estabelecimentoNormalizado,
                valorReferenciaCentavos = sugestao.valorReferenciaCentavos,
                ultimaCobranca = sugestao.ultimaCobranca,
                proximaCobrancaEstimada = null,
                status = StatusAssinatura.RECUSADA,
                criadoEm = existente?.criadoEm ?: agora,
                atualizadoEm = agora,
                excluidoEm = null,
                instrumentoPublicId = sugestao.chave.instrumentoPublicId,
            )
            assinaturaDao.upsert(entidade)
            entidade.toModel()
        }
        agendadorDeAvisos.cancelarParaAssinatura(resultado.publicId)
        return resultado
    }

    override suspend fun cancelarAssinatura(assinaturaPublicId: String) {
        transacaoLocal.executar {
            val atual = assinaturaDao.buscar(assinaturaPublicId)?.takeIf { it.excluidoEm == null } ?: return@executar
            assinaturaDao.upsert(
                atual.copy(
                    status = StatusAssinatura.CANCELADA,
                    atualizadoEm = clock.instant(),
                ),
            )
        }
        agendadorDeAvisos.cancelarParaAssinatura(assinaturaPublicId)
    }

    private suspend fun vincularQualificadores(sugestao: SugestaoDeAssinatura, assinaturaPublicId: String, agora: Instant) {
        val categoriaAssinaturasId = categoriaPorNome(NOME_CATEGORIA_ASSINATURAS)
        val categoriaOutrosId = categoriaPorNome(NOME_CATEGORIA_OUTROS)
        for (lancId in sugestao.lancamentosQualificadoresPublicIds) {
            val lanc = lancamentoDao.buscar(lancId)?.takeIf { it.excluidoEm == null } ?: continue
            val novaCategoria = if (categoriaAssinaturasId != null &&
                (lanc.categoriaPublicId == null || lanc.categoriaPublicId == categoriaOutrosId)
            ) {
                categoriaAssinaturasId
            } else {
                lanc.categoriaPublicId
            }
            if (lanc.assinaturaPublicId != assinaturaPublicId || lanc.categoriaPublicId != novaCategoria) {
                lancamentoDao.upsert(
                    lanc.copy(
                        assinaturaPublicId = assinaturaPublicId,
                        categoriaPublicId = novaCategoria,
                        atualizadoEm = agora,
                    ),
                )
            }
        }
    }

    private suspend fun categoriaPorNome(nome: String): String? = categoriaDao.listarAtivas()
        .firstOrNull { it.tipo == TipoCategoria.DESPESA && it.nome.equals(nome, ignoreCase = true) }
        ?.publicId

    private companion object {
        const val NOME_CATEGORIA_ASSINATURAS = "Assinaturas"
        const val NOME_CATEGORIA_OUTROS = "Outros"
    }
}
