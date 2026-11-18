package com.pingo.core.network.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val jsonDeSync: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    encodeDefaults = true
}

@Serializable
data class PushChangesPayload(
    val contas: List<ContaChangeDto> = emptyList(),
    val instrumentos: List<InstrumentoChangeDto> = emptyList(),
    val categorias: List<CategoriaChangeDto> = emptyList(),
    val regrasCategoria: List<RegraCategoriaChangeDto> = emptyList(),
    val assinaturas: List<AssinaturaChangeDto> = emptyList(),
    val faturas: List<FaturaChangeDto> = emptyList(),
    val lancamentos: List<LancamentoChangeDto> = emptyList(),
    val configuracoesCartao: List<ConfiguracaoCartaoChangeDto> = emptyList(),
    val referenciasSaldoVoucher: List<ReferenciaSaldoVoucherChangeDto> = emptyList(),
    val recargasProgramadasVoucher: List<RecargaProgramadaVoucherChangeDto> = emptyList(),
) {
    val isEmpty: Boolean
        get() = contas.isEmpty() &&
            instrumentos.isEmpty() &&
            configuracoesCartao.isEmpty() &&
            categorias.isEmpty() &&
            regrasCategoria.isEmpty() &&
            assinaturas.isEmpty() &&
            faturas.isEmpty() &&
            lancamentos.isEmpty() &&
            referenciasSaldoVoucher.isEmpty() &&
            recargasProgramadasVoucher.isEmpty()
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
    val arquivadoPorCascata: Boolean? = null,
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
    val instrumentoPublicId: String? = null,
)

@Serializable
data class ConfiguracaoCartaoChangeDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val vigenciaInicio: String,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val excluidoEm: String? = null,
)

@Serializable
data class ReferenciaSaldoVoucherChangeDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val tipoReferencia: String,
    val saldoCentavos: Long,
    val deltaCentavos: Long? = null,
    val instanteReferencia: String,
    val lancamentoPublicId: String? = null,
    val excluidoEm: String? = null,
)

@Serializable
data class RecargaProgramadaVoucherChangeDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val beneficio: String? = null,
    val competenciaMes: String,
    val dataPrevista: String,
    val valorPrevistoCentavos: Long,
    val status: String,
    val lancamentoPublicId: String? = null,
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
    val estabelecimentoNormalizado: String? = null,
    val beneficio: String? = null,
    val saldoObservadoCentavos: Long? = null,
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
    val configuracoesCartao: Int = 0,
    val referenciasSaldoVoucher: Int = 0,
    val recargasProgramadasVoucher: Int = 0,
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
    val configuracoesCartao: List<ConfiguracaoCartaoSnapshotDto> = emptyList(),
    val referenciasSaldoVoucher: List<ReferenciaSaldoVoucherSnapshotDto> = emptyList(),
    val recargasProgramadasVoucher: List<RecargaProgramadaVoucherSnapshotDto> = emptyList(),
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
    val ultimosDigitos: CampoPresenca<String> = CampoPresenca.ausente(),
    val arquivado: Boolean,
    val fechamentoDia: CampoPresenca<Int> = CampoPresenca.ausente(),
    val vencimentoDia: CampoPresenca<Int> = CampoPresenca.ausente(),
    val beneficio: CampoPresenca<String> = CampoPresenca.ausente(),
    val saldo: CampoPresenca<Double> = CampoPresenca.ausente(),
    val diaRecarga: CampoPresenca<Int> = CampoPresenca.ausente(),
    val valorRecarga: CampoPresenca<Double> = CampoPresenca.ausente(),
    val limiteCredito: CampoPresenca<Double> = CampoPresenca.ausente(),
    val arquivadoPorCascata: CampoPresenca<Boolean> = CampoPresenca.ausente(),
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
    val instrumentoPublicId: CampoPresenca<String> = CampoPresenca.ausente(),
)

@Serializable
data class ConfiguracaoCartaoSnapshotDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val vigenciaInicio: String,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class ReferenciaSaldoVoucherSnapshotDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val tipoReferencia: String,
    val saldoCentavos: Long,
    val deltaCentavos: Long? = null,
    val instanteReferencia: String,
    val lancamentoPublicId: String? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val excluidoEm: String? = null,
)

@Serializable
data class RecargaProgramadaVoucherSnapshotDto(
    val publicId: String,
    val instrumentoPublicId: String,
    val beneficio: CampoPresenca<String> = CampoPresenca.ausente(),
    val competenciaMes: String,
    val dataPrevista: String,
    val valorPrevistoCentavos: Long,
    val status: String,
    val lancamentoPublicId: String? = null,
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
    val estabelecimentoNormalizado: CampoPresenca<String> = CampoPresenca.ausente(),
    val beneficio: CampoPresenca<String> = CampoPresenca.ausente(),
    val saldoObservadoCentavos: CampoPresenca<Long> = CampoPresenca.ausente(),
)
