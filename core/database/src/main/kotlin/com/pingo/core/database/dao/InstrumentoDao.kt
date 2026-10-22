package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.InstrumentoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface InstrumentoDao {
    @Query("SELECT * FROM instrumentos WHERE excluidoEm IS NULL ORDER BY apelido")
    fun observeAtivos(): Flow<List<InstrumentoEntity>>

    @Query("SELECT * FROM instrumentos WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): InstrumentoEntity?

    @Query("SELECT * FROM instrumentos WHERE contaPublicId = :contaPublicId AND excluidoEm IS NULL ORDER BY apelido")
    fun observePorConta(contaPublicId: String): Flow<List<InstrumentoEntity>>

    @Query("SELECT * FROM instrumentos WHERE atualizadoEm >= :desde")
    suspend fun atualizadosDesde(desde: Instant): List<InstrumentoEntity>

    @Upsert
    suspend fun upsert(instrumento: InstrumentoEntity)

    @Upsert
    suspend fun upsertAll(instrumentos: List<InstrumentoEntity>)

    @Query("UPDATE instrumentos SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluido(publicId: String, quando: Instant)

    @Query("DELETE FROM instrumentos")
    suspend fun limpar()
}
