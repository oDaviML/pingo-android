package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import java.time.Instant

internal const val PADRAO_VALIDO =
    """[{"id":"compra","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra"],"extracoes":{"valor":"R$ {valor}"}}]"""

internal fun regraParser(instituicao: InstituicaoFinanceira, versao: Int, padroes: String, ativa: Boolean = true): RegraParser =
    RegraParser(
        publicId = "regra-${instituicao.name.lowercase()}-$versao",
        instituicao = instituicao,
        versao = versao,
        padroes = padroes,
        ativa = ativa,
        publicadaEm = Instant.EPOCH,
        criadoEm = Instant.EPOCH,
        atualizadoEm = Instant.EPOCH,
    )

internal fun versaoDe(padroes: String, instituicao: InstituicaoFinanceira = InstituicaoFinanceira.ITAU): VersaoRegras =
    VersaoRegras.compilar(regraParser(instituicao, 1, padroes))
