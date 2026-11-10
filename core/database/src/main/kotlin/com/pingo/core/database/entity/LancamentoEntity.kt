package com.pingo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.Instant

@Entity(
    tableName = "lancamentos",
    foreignKeys = [
        ForeignKey(
            entity = InstrumentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["instrumentoPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["categoriaPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = FaturaEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["faturaPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AssinaturaEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["assinaturaPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = LancamentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["compraPaiPublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = LancamentoEntity::class,
            parentColumns = ["publicId"],
            childColumns = ["estornoDePublicId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("instrumentoPublicId"),
        Index("categoriaPublicId"),
        Index("faturaPublicId"),
        Index("assinaturaPublicId"),
        Index("compraPaiPublicId"),
        Index("estornoDePublicId"),
        Index("dataHora"),
        Index("atualizadoEm"),
        Index("excluidoEm"),
        Index("hashDuplicata"),
    ],
)
data class LancamentoEntity(
    @PrimaryKey val publicId: String,
    val instrumentoPublicId: String,
    val categoriaPublicId: String?,
    val faturaPublicId: String?,
    val assinaturaPublicId: String?,
    val compraPaiPublicId: String?,
    val estornoDePublicId: String?,
    val transferenciaId: String?,
    val valorCentavos: Long,
    val dataHora: Instant,
    val tipo: TipoLancamento,
    val estabelecimento: String?,
    val status: StatusLancamento,
    val origem: OrigemLancamento,
    val confianca: Double,
    val motivoPendencia: String?,
    val hashDuplicata: String?,
    val observacao: String?,
    val parcelaNumero: Int,
    val totalParcelas: Int,
    val valorTotalCompraCentavos: Long?,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant?,
    val textoBruto: String?,
)
