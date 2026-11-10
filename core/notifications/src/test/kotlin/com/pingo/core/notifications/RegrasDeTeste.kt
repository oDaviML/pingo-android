package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.PadraoRegra
import com.pingo.core.parser.PadroesRegraCodec
import com.pingo.core.parser.TipoLancamentoPadrao
import java.time.Instant

internal object RegrasDeTeste {
    const val TEXTO_COMPRA_ITAU =
        "Olá! Compra aprovada de R$ 37,90 em CLARO *31987905690 no dia 10/11 às 00:55 no seu cartão Itau. Boas compras."
    const val TEXTO_RUIDO_ITAU = "Mais controle para gerenciar seu limite"
    const val TEXTO_DESCONHECIDO = "Seu extrato está disponível no aplicativo"

    fun todas(): List<RegraParser> = listOf(itau(), nubank())

    fun itau(): RegraParser = regra(
        instituicao = InstituicaoFinanceira.ITAU,
        padroes = listOf(
            PadraoRegra(
                id = "itau-compra-aprovada",
                tipoLancamento = TipoLancamentoPadrao.COMPRA,
                direcao = DirecaoPadrao.DESPESA,
                ancoras = listOf("Compra aprovada de R$"),
                extracoes = mapOf(
                    "valor" to "Compra aprovada de R$ {valor} em",
                    "estabelecimento" to "em {estabelecimento} no dia",
                    "dataHora" to "no dia {dataHora} no seu cartão",
                ),
            ),
            PadraoRegra(id = "itau-ruido-limite-conta", ruido = true, ancoras = listOf("Mais controle para gerenciar seu limite")),
        ),
    )

    fun nubank(): RegraParser = regra(
        instituicao = InstituicaoFinanceira.NUBANK,
        padroes = listOf(
            PadraoRegra(id = "nubank-ruido-limite-evolui", ruido = true, ancoras = listOf("Seu limite evolui com você")),
        ),
    )

    private fun regra(instituicao: InstituicaoFinanceira, padroes: List<PadraoRegra>): RegraParser = RegraParser(
        publicId = "teste-${instituicao.name.lowercase()}",
        instituicao = instituicao,
        versao = 1,
        padroes = PadroesRegraCodec.codificar(padroes),
        ativa = true,
        publicadaEm = Instant.EPOCH,
        criadoEm = Instant.EPOCH,
        atualizadoEm = Instant.EPOCH,
    )
}
