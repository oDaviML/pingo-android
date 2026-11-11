package com.pingo.core.network.sync

import kotlinx.serialization.Serializable

@Serializable
data class PushChangesPayload(
    val contas: List<ContaChangeDto> = emptyList(),
    val instrumentos: List<InstrumentoChangeDto> = emptyList(),
    val categorias: List<CategoriaChangeDto> = emptyList(),
    val regrasCategoria: List<RegraCategoriaChangeDto> = emptyList(),
    val assinaturas: List<AssinaturaChangeDto> = emptyList(),
    val faturas: List<FaturaChangeDto> = emptyList(),
    val lancamentos: List<LancamentoChangeDto> = emptyList(),
) {
    val isEmpty: Boolean
        get() = contas.isEmpty() &&
            instrumentos.isEmpty() &&
            categorias.isEmpty() &&
            regrasCategoria.isEmpty() &&
            assinaturas.isEmpty() &&
            faturas.isEmpty() &&
            lancamentos.isEmpty()
}

@Serializable
data class ContaChangeDto(
    val publicId: String,
    val instituicao: String,
    val apelido: String,
    val tipo: String,
    val arquivada: Boolean,
    val excluidoEm: String? = null,
)

@Serializable
data class InstrumentoChangeDto(
    val publicId: String,
    val contaPublicId: String,
    val tipo: String,
    val apelido: String,
    val ultimosDigitos: String? = null,
    val arquivado: Boolean,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: String? = null,
    val saldo: Double? = null,
    val diaRecarga: Int? = null,
    val valorRecarga: Double? = null,
    val limiteCredito: Double? = null,
    val excluidoEm: String? = null,
)

@Serializable
data class CategoriaChangeDto(
    val publicId: String,
    val nome: String,
    val tipo: String,
    val padrao: Boolean,
    val arquivada: Boolean,
    val excluidoEm: String? = null,
)

@Serializable
data class RegraCategoriaChangeDto(
    val publicId: String,
    val categoriaPublicId: String,
    val estabelecimentoNormalizado: String,
    val excluidoEm: String? = null,
)

@Serializable
data class AssinaturaChangeDto(
    val publicId: String,
    val estabelecimento: String,
    val estabelecimentoNormalizado: String,
    val valorReferencia: Double,
    val ultimaCobranca: String? = null,
    val proximaCobrancaEstimada: String? = null,
    val status: String,
    val excluidoEm: String? = null,
)

@Serializable
data class FaturaChangeDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val competencia: String,
    val dataFechamento: String,
    val dataVencimento: String,
    val total: Double,
    val totalPago: Double,
    val status: String,
    val dataPagamento: String? = null,
    val pagamentoLancamentoPublicId: String? = null,
    val excluidoEm: String? = null,
)

@Serializable
data class LancamentoChangeDto(
    val publicId: String,
    val instrumentoPublicId: String? = null,
    val categoriaPublicId: String? = null,
    val faturaPublicId: String? = null,
    val assinaturaPublicId: String? = null,
    val compraPaiPublicId: String? = null,
    val estornoDePublicId: String? = null,
    val transferenciaId: String? = null,
    val valor: Double,
    val dataHora: String,
    val tipo: String,
    val tipoSemTransferencia: String? = null,
    val estabelecimento: String? = null,
    val status: String,
    val origem: String,
    val instituicaoOrigem: String? = null,
    val confianca: Double,
    val motivoPendencia: String? = null,
    val hashDuplicata: String? = null,
    val observacao: String? = null,
    val parcelaNumero: Int = 1,
    val totalParcelas: Int = 1,
    val valorTotalCompra: Double? = null,
    val excluidoEm: String? = null,
)

@Serializable
data class PushChangesResult(
    val contas: Int = 0,
    val instrumentos: Int = 0,
    val categorias: Int = 0,
    val regrasCategoria: Int = 0,
    val assinaturas: Int = 0,
    val faturas: Int = 0,
    val lancamentos: Int = 0,
    val ignorados: Int = 0,
    val aliases: List<PublicIdAliasDto> = emptyList(),
)

@Serializable
data class PublicIdAliasDto(val publicId: String, val ownerPublicId: String)

@Serializable
data class PullChangesResult(
    val proximoCursor: String? = null,
    val servidorEm: String,
    val contas: List<ContaSnapshotDto> = emptyList(),
    val instrumentos: List<InstrumentoSnapshotDto> = emptyList(),
    val categorias: List<CategoriaSnapshotDto> = emptyList(),
    val regrasCategoria: List<RegraCategoriaSnapshotDto> = emptyList(),
    val assinaturas: List<AssinaturaSnapshotDto> = emptyList(),
    val faturas: List<FaturaSnapshotDto> = emptyList(),
    val lancamentos: List<LancamentoSnapshotDto> = emptyList(),
)

@Serializable
data class ContaSnapshotDto(
    val publicId: String,
    val instituicao: String,
    val apelido: String,
    val tipo: String,
    val arquivada: Boolean,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class InstrumentoSnapshotDto(
    val publicId: String,
    val contaPublicId: String,
    val tipo: String,
    val apelido: String,
    val ultimosDigitos: String? = null,
    val arquivado: Boolean,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val beneficio: String? = null,
    val saldo: Double? = null,
    val diaRecarga: Int? = null,
    val valorRecarga: Double? = null,
    val limiteCredito: Double? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class CategoriaSnapshotDto(
    val publicId: String,
    val nome: String,
    val tipo: String,
    val padrao: Boolean,
    val arquivada: Boolean,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class RegraCategoriaSnapshotDto(
    val publicId: String,
    val categoriaPublicId: String,
    val estabelecimentoNormalizado: String,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class AssinaturaSnapshotDto(
    val publicId: String,
    val estabelecimento: String,
    val estabelecimentoNormalizado: String,
    val valorReferencia: Double,
    val ultimaCobranca: String? = null,
    val proximaCobrancaEstimada: String? = null,
    val status: String,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class FaturaSnapshotDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val competencia: String,
    val dataFechamento: String,
    val dataVencimento: String,
    val total: Double,
    val totalPago: Double,
    val status: String,
    val dataPagamento: String? = null,
    val pagamentoLancamentoPublicId: String? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class LancamentoSnapshotDto(
    val publicId: String,
    val instrumentoPublicId: String? = null,
    val categoriaPublicId: String? = null,
    val faturaPublicId: String? = null,
    val assinaturaPublicId: String? = null,
    val compraPaiPublicId: String? = null,
    val estornoDePublicId: String? = null,
    val transferenciaId: String? = null,
    val valor: Double,
    val dataHora: String,
    val tipo: String,
    val tipoSemTransferencia: String? = null,
    val estabelecimento: String? = null,
    val status: String,
    val origem: String,
    val instituicaoOrigem: String? = null,
    val confianca: Double,
    val motivoPendencia: String? = null,
    val hashDuplicata: String? = null,
    val observacao: String? = null,
    val parcelaNumero: Int = 1,
    val totalParcelas: Int = 1,
    val valorTotalCompra: Double? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)
