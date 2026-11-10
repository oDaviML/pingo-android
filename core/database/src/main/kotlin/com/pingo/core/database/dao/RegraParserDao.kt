package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pingo.core.database.entity.RegraParserEntity
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class RegraParserDao {
    @Query("SELECT * FROM regras_parser WHERE ativa = 1 ORDER BY instituicao")
    abstract fun observeAtivas(): Flow<List<RegraParserEntity>>

    @Query("SELECT * FROM regras_parser WHERE publicId = :publicId")
    abstract suspend fun buscar(publicId: String): RegraParserEntity?

    @Query("SELECT * FROM regras_parser WHERE instituicao = :instituicao AND ativa = 1 ORDER BY versao DESC LIMIT 1")
    abstract suspend fun buscarAtiva(instituicao: InstituicaoFinanceira): RegraParserEntity?

    @Query("SELECT * FROM regras_parser WHERE atualizadoEm >= :desde")
    abstract suspend fun atualizadasDesde(desde: Instant): List<RegraParserEntity>

    @Query("SELECT MAX(versao) FROM regras_parser WHERE ativa = 1")
    abstract suspend fun versaoAtivaMaxima(): Int?

    @Query("SELECT * FROM regras_parser WHERE ativa = 1")
    abstract suspend fun ativas(): List<RegraParserEntity>

    @Query("UPDATE regras_parser SET ativa = 0 WHERE ativa = 1 AND instituicao IN (:instituicoes)")
    abstract suspend fun desativarDas(instituicoes: List<InstituicaoFinanceira>)

    @Upsert
    abstract suspend fun upsert(regra: RegraParserEntity)

    @Upsert
    abstract suspend fun upsertAll(regras: List<RegraParserEntity>)

    @Transaction
    open suspend fun substituirVigentes(regras: List<RegraParserEntity>) {
        if (regras.isEmpty()) {
            return
        }

        desativarDas(regras.map(RegraParserEntity::instituicao).distinct())
        upsertAll(regras)
    }
}
