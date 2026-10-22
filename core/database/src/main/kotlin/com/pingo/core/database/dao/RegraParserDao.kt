package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.RegraParserEntity
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface RegraParserDao {
    @Query("SELECT * FROM regras_parser WHERE ativa = 1 ORDER BY instituicao")
    fun observeAtivas(): Flow<List<RegraParserEntity>>

    @Query("SELECT * FROM regras_parser WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): RegraParserEntity?

    @Query("SELECT * FROM regras_parser WHERE instituicao = :instituicao AND ativa = 1 ORDER BY versao DESC LIMIT 1")
    suspend fun buscarAtiva(instituicao: InstituicaoFinanceira): RegraParserEntity?

    @Query("SELECT * FROM regras_parser WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<RegraParserEntity>

    @Upsert
    suspend fun upsert(regra: RegraParserEntity)

    @Upsert
    suspend fun upsertAll(regras: List<RegraParserEntity>)
}
