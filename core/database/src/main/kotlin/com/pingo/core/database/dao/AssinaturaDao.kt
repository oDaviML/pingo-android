package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.AssinaturaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AssinaturaDao {
    @Query("SELECT * FROM assinaturas WHERE excluidoEm IS NULL ORDER BY estabelecimento")
    fun observeAtivas(): Flow<List<AssinaturaEntity>>

    @Query("SELECT * FROM assinaturas WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): AssinaturaEntity?

    @Query("SELECT * FROM assinaturas WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<AssinaturaEntity>

    @Upsert
    suspend fun upsert(assinatura: AssinaturaEntity)

    @Upsert
    suspend fun upsertAll(assinaturas: List<AssinaturaEntity>)

    @Query("UPDATE assinaturas SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}
