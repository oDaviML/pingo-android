package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface InstrumentoDao {
    @Query("SELECT * FROM instrumentos WHERE excluidoEm IS NULL ORDER BY apelido")
    fun observeNaoExcluidos(): Flow<List<InstrumentoEntity>>

    @Query(
        """
        SELECT instrumentos.* FROM instrumentos
        INNER JOIN contas ON contas.publicId = instrumentos.contaPublicId
        WHERE contas.arquivada = 0
        AND contas.excluidoEm IS NULL
        AND instrumentos.excluidoEm IS NULL
        AND instrumentos.arquivado = 0
        ORDER BY instrumentos.apelido
        """,
    )
    fun observeAtivos(): Flow<List<InstrumentoEntity>>

    @Query("SELECT * FROM instrumentos WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): InstrumentoEntity?

    @Query("SELECT * FROM instrumentos WHERE contaPublicId = :contaPublicId AND excluidoEm IS NULL ORDER BY apelido")
    fun observePorConta(contaPublicId: String): Flow<List<InstrumentoEntity>>

    @Query(
        """
        SELECT instrumentos.* FROM instrumentos
        INNER JOIN contas ON contas.publicId = instrumentos.contaPublicId
        WHERE contas.instituicao = :instituicao
        AND contas.excluidoEm IS NULL
        AND contas.arquivada = 0
        AND instrumentos.excluidoEm IS NULL
        AND instrumentos.arquivado = 0
        ORDER BY instrumentos.apelido
        """,
    )
    suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<InstrumentoEntity>

    @Query("SELECT * FROM instrumentos WHERE contaPublicId = :contaPublicId AND excluidoEm IS NULL ORDER BY apelido")
    suspend fun listarPorConta(contaPublicId: String): List<InstrumentoEntity>

    @Query("SELECT * FROM instrumentos WHERE atualizadoEm >= :desde")
    suspend fun atualizadosDesde(desde: Instant): List<InstrumentoEntity>

    @Upsert
    suspend fun upsert(instrumento: InstrumentoEntity)

    @Upsert
    suspend fun upsertAll(instrumentos: List<InstrumentoEntity>)

    @Query("UPDATE instrumentos SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluido(publicId: String, quando: Instant)

    @Query("UPDATE instrumentos SET arquivado = 1, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarArquivado(publicId: String, quando: Instant)

    @Query("UPDATE instrumentos SET arquivado = 0, arquivadoPorCascata = 0, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarReativado(publicId: String, quando: Instant)

    @Query(
        """
        UPDATE instrumentos
        SET arquivado = 1, arquivadoPorCascata = 1, atualizadoEm = :quando
        WHERE contaPublicId = :contaPublicId AND excluidoEm IS NULL AND arquivado = 0
        """,
    )
    suspend fun arquivarAtivosPorCascata(contaPublicId: String, quando: Instant)

    @Query(
        """
        UPDATE instrumentos
        SET arquivado = 0, arquivadoPorCascata = 0, atualizadoEm = :quando
        WHERE contaPublicId = :contaPublicId AND excluidoEm IS NULL AND arquivado = 1 AND arquivadoPorCascata = 1
        """,
    )
    suspend fun reativarArquivadosPorCascata(contaPublicId: String, quando: Instant)
}
