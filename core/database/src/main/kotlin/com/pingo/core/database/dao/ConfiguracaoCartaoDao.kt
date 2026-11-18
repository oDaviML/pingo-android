package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ConfiguracaoCartaoDao {
    @Query("SELECT * FROM configuracoes_cartao WHERE excluidoEm IS NULL ORDER BY instrumentoPublicId, vigenciaInicio ASC, publicId ASC")
    fun observeAtivas(): Flow<List<ConfiguracaoCartaoEntity>>

    @Query(
        """
        SELECT * FROM configuracoes_cartao
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY vigenciaInicio ASC, publicId ASC
        """,
    )
    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<ConfiguracaoCartaoEntity>>

    @Query(
        """
        SELECT * FROM configuracoes_cartao
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY vigenciaInicio ASC, publicId ASC
        """,
    )
    suspend fun listarPorInstrumento(instrumentoPublicId: String): List<ConfiguracaoCartaoEntity>

    @Query("SELECT * FROM configuracoes_cartao WHERE excluidoEm IS NULL ORDER BY instrumentoPublicId, vigenciaInicio ASC, publicId ASC")
    suspend fun listarAtivas(): List<ConfiguracaoCartaoEntity>

    @Query("SELECT * FROM configuracoes_cartao WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): ConfiguracaoCartaoEntity?

    @Query("SELECT * FROM configuracoes_cartao WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<ConfiguracaoCartaoEntity>

    @Upsert
    suspend fun upsert(configuracao: ConfiguracaoCartaoEntity)

    @Upsert
    suspend fun upsertAll(configuracoes: List<ConfiguracaoCartaoEntity>)

    @Query("UPDATE configuracoes_cartao SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}
