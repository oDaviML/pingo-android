package com.pingo.feature.onboarding.impl

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.descricaoDeExibicao
import com.pingo.core.ui.contas.instituicoesFinanceiras
import com.pingo.core.ui.contas.letrasDeExibicao
import com.pingo.core.ui.contas.nomeDeExibicao

internal data class BancoOferecido(val instituicao: InstituicaoFinanceira) {
    val nome: String
        get() = instituicao.nomeDeExibicao()

    val descricao: String
        get() = instituicao.descricaoDeExibicao()

    val letras: String
        get() = instituicao.letrasDeExibicao()

    fun escolhaInicial(): ContaFormulario = ContaFormulario.nova(instituicao)

    companion object {
        val todos: List<BancoOferecido> = instituicoesFinanceiras.map(::BancoOferecido)

        fun de(instituicao: InstituicaoFinanceira): BancoOferecido = todos.first { it.instituicao == instituicao }
    }
}
