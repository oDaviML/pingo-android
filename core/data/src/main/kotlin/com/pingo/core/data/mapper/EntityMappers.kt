package com.pingo.core.data.mapper

import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RegraCategoriaEntity
import com.pingo.core.database.entity.RegraParserEntity
import com.pingo.core.database.entity.UsuarioEntity
import com.pingo.core.model.Assinatura
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.RegraParser
import com.pingo.core.model.Usuario

fun UsuarioEntity.toModel(): Usuario = Usuario(
    publicId = publicId,
    sub = sub,
    email = email,
    nome = nome,
    consentimentoLgpdEm = consentimentoLgpdEm,
    consentimentoLgpdVersao = consentimentoLgpdVersao,
    telemetriaOptIn = telemetriaOptIn,
    telemetriaOptInEm = telemetriaOptInEm,
    aparelhoAtivoId = aparelhoAtivoId,
    aparelhoAtivoNome = aparelhoAtivoNome,
    aparelhoAtivoEm = aparelhoAtivoEm,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Usuario.toEntity(): UsuarioEntity = UsuarioEntity(
    publicId = publicId,
    sub = sub,
    email = email,
    nome = nome,
    consentimentoLgpdEm = consentimentoLgpdEm,
    consentimentoLgpdVersao = consentimentoLgpdVersao,
    telemetriaOptIn = telemetriaOptIn,
    telemetriaOptInEm = telemetriaOptInEm,
    aparelhoAtivoId = aparelhoAtivoId,
    aparelhoAtivoNome = aparelhoAtivoNome,
    aparelhoAtivoEm = aparelhoAtivoEm,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun ContaEntity.toModel(): Conta = Conta(
    publicId = publicId,
    instituicao = instituicao,
    apelido = apelido,
    tipo = tipo,
    arquivada = arquivada,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Conta.toEntity(): ContaEntity = ContaEntity(
    publicId = publicId,
    instituicao = instituicao,
    apelido = apelido,
    tipo = tipo,
    arquivada = arquivada,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun InstrumentoEntity.toModel(): Instrumento = Instrumento(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = tipo,
    apelido = apelido,
    ultimosDigitos = ultimosDigitos,
    arquivado = arquivado,
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    beneficio = beneficio,
    saldoCentavos = saldoCentavos,
    diaRecarga = diaRecarga,
    valorRecargaCentavos = valorRecargaCentavos,
    limiteCreditoCentavos = limiteCreditoCentavos,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Instrumento.toEntity(): InstrumentoEntity = InstrumentoEntity(
    publicId = publicId,
    contaPublicId = contaPublicId,
    tipo = tipo,
    apelido = apelido,
    ultimosDigitos = ultimosDigitos,
    arquivado = arquivado,
    fechamentoDia = fechamentoDia,
    vencimentoDia = vencimentoDia,
    beneficio = beneficio,
    saldoCentavos = saldoCentavos,
    diaRecarga = diaRecarga,
    valorRecargaCentavos = valorRecargaCentavos,
    limiteCreditoCentavos = limiteCreditoCentavos,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun LancamentoEntity.toModel(): Lancamento = Lancamento(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    faturaPublicId = faturaPublicId,
    assinaturaPublicId = assinaturaPublicId,
    compraPaiPublicId = compraPaiPublicId,
    estornoDePublicId = estornoDePublicId,
    transferenciaId = transferenciaId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = origem,
    confianca = confianca,
    motivoPendencia = motivoPendencia,
    hashDuplicata = hashDuplicata,
    observacao = observacao,
    parcelaNumero = parcelaNumero,
    totalParcelas = totalParcelas,
    valorTotalCompraCentavos = valorTotalCompraCentavos,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
    textoBruto = textoBruto,
)

fun Lancamento.toEntity(): LancamentoEntity = LancamentoEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    faturaPublicId = faturaPublicId,
    assinaturaPublicId = assinaturaPublicId,
    compraPaiPublicId = compraPaiPublicId,
    estornoDePublicId = estornoDePublicId,
    transferenciaId = transferenciaId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = origem,
    confianca = confianca,
    motivoPendencia = motivoPendencia,
    hashDuplicata = hashDuplicata,
    observacao = observacao,
    parcelaNumero = parcelaNumero,
    totalParcelas = totalParcelas,
    valorTotalCompraCentavos = valorTotalCompraCentavos,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
    textoBruto = textoBruto,
)

fun CategoriaEntity.toModel(): Categoria = Categoria(
    publicId = publicId,
    nome = nome,
    tipo = tipo,
    padrao = padrao,
    arquivada = arquivada,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Categoria.toEntity(): CategoriaEntity = CategoriaEntity(
    publicId = publicId,
    nome = nome,
    tipo = tipo,
    padrao = padrao,
    arquivada = arquivada,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun RegraCategoriaEntity.toModel(): RegraCategoria = RegraCategoria(
    publicId = publicId,
    categoriaPublicId = categoriaPublicId,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun RegraCategoria.toEntity(): RegraCategoriaEntity = RegraCategoriaEntity(
    publicId = publicId,
    categoriaPublicId = categoriaPublicId,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun FaturaEntity.toModel(): Fatura = Fatura(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    competencia = competencia,
    dataFechamento = dataFechamento,
    dataVencimento = dataVencimento,
    totalCentavos = totalCentavos,
    totalPagoCentavos = totalPagoCentavos,
    status = status,
    dataPagamento = dataPagamento,
    pagamentoLancamentoPublicId = pagamentoLancamentoPublicId,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Fatura.toEntity(): FaturaEntity = FaturaEntity(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    competencia = competencia,
    dataFechamento = dataFechamento,
    dataVencimento = dataVencimento,
    totalCentavos = totalCentavos,
    totalPagoCentavos = totalPagoCentavos,
    status = status,
    dataPagamento = dataPagamento,
    pagamentoLancamentoPublicId = pagamentoLancamentoPublicId,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun AssinaturaEntity.toModel(): Assinatura = Assinatura(
    publicId = publicId,
    estabelecimento = estabelecimento,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    valorReferenciaCentavos = valorReferenciaCentavos,
    ultimaCobranca = ultimaCobranca,
    proximaCobrancaEstimada = proximaCobrancaEstimada,
    status = status,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun Assinatura.toEntity(): AssinaturaEntity = AssinaturaEntity(
    publicId = publicId,
    estabelecimento = estabelecimento,
    estabelecimentoNormalizado = estabelecimentoNormalizado,
    valorReferenciaCentavos = valorReferenciaCentavos,
    ultimaCobranca = ultimaCobranca,
    proximaCobrancaEstimada = proximaCobrancaEstimada,
    status = status,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
    excluidoEm = excluidoEm,
)

fun RegraParserEntity.toModel(): RegraParser = RegraParser(
    publicId = publicId,
    instituicao = instituicao,
    versao = versao,
    padroes = padroes,
    ativa = ativa,
    publicadaEm = publicadaEm,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
)

fun RegraParser.toEntity(): RegraParserEntity = RegraParserEntity(
    publicId = publicId,
    instituicao = instituicao,
    versao = versao,
    padroes = padroes,
    ativa = ativa,
    publicadaEm = publicadaEm,
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
)
