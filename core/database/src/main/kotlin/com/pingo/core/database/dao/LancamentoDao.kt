package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class LancamentoDao {
    @Query("SELECT * FROM lancamentos WHERE excluidoEm IS NULL ORDER BY dataHora DESC")
    abstract fun observeAtivos(): Flow<List<LancamentoEntity>>

    @Query("SELECT * FROM lancamentos WHERE excluidoEm IS NULL AND dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    abstract fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<LancamentoEntity>>

    @Query("SELECT * FROM lancamentos WHERE publicId = :publicId")
    abstract suspend fun buscar(publicId: String): LancamentoEntity?

    @Query("SELECT * FROM lancamentos WHERE hashDuplicata = :hash AND excluidoEm IS NULL LIMIT 1")
    abstract suspend fun buscarPorHash(hash: String): LancamentoEntity?

    @Query(
        """
        SELECT instrumentoPublicId FROM lancamentos
        WHERE excluidoEm IS NULL AND instrumentoPublicId IN (:instrumentos)
        ORDER BY dataHora DESC
        LIMIT 1
        """,
    )
    abstract suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String?

    @Query(
        """
        SELECT * FROM lancamentos
        WHERE excluidoEm IS NULL
        AND origem = :origem
        AND instrumentoPublicId = :instrumentoPublicId
        AND valorCentavos = :valorCentavos
        AND dataHora BETWEEN :inicio AND :fim
        ORDER BY dataHora
        """,
    )
    abstract suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        origem: OrigemLancamento,
    ): List<LancamentoEntity>

    @Query(
        """
        SELECT lancamentos.* FROM lancamentos
        INNER JOIN instrumentos ON instrumentos.publicId = lancamentos.instrumentoPublicId
        INNER JOIN contas ON contas.publicId = instrumentos.contaPublicId
        WHERE lancamentos.excluidoEm IS NULL
        AND lancamentos.transferenciaId IS NULL
        AND lancamentos.origem = :origem
        AND lancamentos.tipo = :tipo
        AND lancamentos.valorCentavos = :valorCentavos
        AND lancamentos.dataHora BETWEEN :inicio AND :fim
        AND contas.publicId <> :contaPublicId
        ORDER BY lancamentos.dataHora
        """,
    )
    abstract suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<LancamentoEntity>

    @Query("SELECT * FROM lancamentos WHERE atualizadoEm >= :desde")
    abstract suspend fun atualizadosDesde(desde: Instant): List<LancamentoEntity>

    @Upsert
    abstract suspend fun upsert(lancamento: LancamentoEntity)

    @Upsert
    abstract suspend fun upsertAll(lancamentos: List<LancamentoEntity>)

    @Query("UPDATE lancamentos SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    abstract suspend fun marcarExcluido(publicId: String, quando: Instant)

    @Query("UPDATE lancamentos SET textoBruto = :textoBruto WHERE publicId = :publicId")
    abstract suspend fun definirTextoBruto(publicId: String, textoBruto: String)

    @Query("UPDATE lancamentos SET compraPaiPublicId = :para WHERE compraPaiPublicId = :de")
    abstract suspend fun repointarCompraPai(de: String, para: String)

    @Query("UPDATE lancamentos SET estornoDePublicId = :para WHERE estornoDePublicId = :de")
    abstract suspend fun repointarEstorno(de: String, para: String)

    @Query("DELETE FROM lancamentos WHERE publicId = :publicId")
    abstract suspend fun apagar(publicId: String)

    @Transaction
    open suspend fun upsertEmDuasPassadas(semEstorno: List<LancamentoEntity>, comEstorno: List<LancamentoEntity>) {
        upsertAll(semEstorno)

        if (comEstorno.isNotEmpty()) {
            upsertAll(comEstorno)
        }
    }
}
