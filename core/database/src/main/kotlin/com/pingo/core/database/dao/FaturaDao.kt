package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.FaturaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FaturaDao {
    @Query("SELECT * FROM faturas WHERE excluidoEm IS NULL ORDER BY competencia DESC")
    fun observeAtivas(): Flow<List<FaturaEntity>>

    @Query("SELECT * FROM faturas WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): FaturaEntity?

    @Query("SELECT * FROM faturas WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL ORDER BY competencia DESC")
    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<FaturaEntity>>

    @Query("SELECT * FROM faturas WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<FaturaEntity>

    @Upsert
    suspend fun upsert(fatura: FaturaEntity)

    @Upsert
    suspend fun upsertAll(faturas: List<FaturaEntity>)

    @Query("UPDATE faturas SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}
