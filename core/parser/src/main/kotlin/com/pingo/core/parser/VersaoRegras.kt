package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser

class VersaoRegras private constructor(
    val instituicao: InstituicaoFinanceira,
    val versao: Int,
    internal val padroes: List<PadraoCompilado>,
) {
    companion object {
        fun compilar(regra: RegraParser): VersaoRegras {
            val padroes = PadroesRegraCodec.decodificar(regra.padroes).map(CompiladorPadrao::compilar)
            require(padroes.isNotEmpty()) { "versão ${regra.versao} de ${regra.instituicao} sem padrões" }
            return VersaoRegras(regra.instituicao, regra.versao, padroes)
        }
    }
}
