package com.pingo.sync

import androidx.room.withTransaction
import com.pingo.core.database.PingoDatabase
import com.pingo.core.network.sync.AssinaturaSnapshotDto
import com.pingo.core.network.sync.CategoriaSnapshotDto
import com.pingo.core.network.sync.ContaSnapshotDto
import com.pingo.core.network.sync.FaturaSnapshotDto
import com.pingo.core.network.sync.InstrumentoSnapshotDto
import com.pingo.core.network.sync.LancamentoSnapshotDto
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.RegraCategoriaSnapshotDto

internal class SyncApplier(private val database: PingoDatabase) {
    suspend fun apply(result: PullChangesResult) {
        database.withTransaction {
            applyContas(result.contas)
            applyInstrumentos(result.instrumentos)
            applyCategorias(result.categorias)
            applyRegrasCategoria(result.regrasCategoria)
            applyAssinaturas(result.assinaturas)
            applyFaturas(result.faturas)
            applyLancamentos(result.lancamentos)
        }
    }

    private suspend fun applyContas(snapshots: List<ContaSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.contaDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.contaDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyInstrumentos(snapshots: List<InstrumentoSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.instrumentoDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.instrumentoDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyCategorias(snapshots: List<CategoriaSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.categoriaDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.categoriaDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyRegrasCategoria(snapshots: List<RegraCategoriaSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.regraCategoriaDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.regraCategoriaDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyAssinaturas(snapshots: List<AssinaturaSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.assinaturaDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.assinaturaDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyFaturas(snapshots: List<FaturaSnapshotDto>) {
        for (snapshot in snapshots) {
            val local = database.faturaDao().buscar(snapshot.publicId)
            if (local == null || !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())) {
                database.faturaDao().upsert(snapshot.toEntity())
            }
        }
    }

    private suspend fun applyLancamentos(snapshots: List<LancamentoSnapshotDto>) {
        val aplicaveis = snapshots.filter { snapshot -> deveAplicarLancamento(snapshot) }
        val semDependencias = aplicaveis.filter { it.estornoDePublicId == null && it.compraPaiPublicId == null }
        val comCompraPai = aplicaveis.filter { it.estornoDePublicId == null && it.compraPaiPublicId != null }
        val comEstorno = aplicaveis.filter { it.estornoDePublicId != null }

        database.lancamentoDao().upsertAll(semDependencias.map { it.toEntity() })
        database.lancamentoDao().upsertAll(comCompraPai.map { it.toEntity() })
        database.lancamentoDao().upsertAll(comEstorno.map { it.toEntity() })
    }

    private suspend fun deveAplicarLancamento(snapshot: LancamentoSnapshotDto): Boolean {
        val local = database.lancamentoDao().buscar(snapshot.publicId) ?: return true

        return !local.atualizadoEm.isAfter(snapshot.atualizadoEm.toInstantValue())
    }
}
