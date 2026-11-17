package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.ui.contas.instituicoesFinanceiras
import com.pingo.core.ui.contas.rotuloDeExibicao

@Immutable
data class GrupoDeContas(val instituicao: InstituicaoFinanceira, val contas: List<ContaDaLista>)

@Immutable
data class ContaDaLista(val conta: Conta, val instrumentos: List<Instrumento>)

@Immutable
data class ContaArquivadaDaLista(val conta: Conta, val instrumentos: List<Instrumento>)

internal fun agruparContas(contas: List<Conta>, instrumentos: List<Instrumento>): List<GrupoDeContas> = contas
    .filter { conta -> !conta.arquivada && conta.excluidoEm == null }
    .sortedBy { conta -> instituicoesFinanceiras.indexOf(conta.instituicao) }
    .groupBy { conta -> conta.instituicao }
    .map { (instituicao, contasDaInstituicao) ->
        GrupoDeContas(
            instituicao = instituicao,
            contas = contasDaInstituicao.sortedBy { conta -> conta.apelido }.map { conta ->
                ContaDaLista(conta = conta, instrumentos = instrumentosDaConta(conta, instrumentos))
            },
        )
    }

private fun instrumentosDaConta(conta: Conta, instrumentos: List<Instrumento>): List<Instrumento> = instrumentos
    .filter { instrumento ->
        instrumento.contaPublicId == conta.publicId && !instrumento.arquivado && instrumento.excluidoEm == null
    }
    .sortedWith(compareBy({ it.tipo }, { it.apelido }))

internal fun contasArquivadas(contas: List<Conta>, instrumentos: List<Instrumento>): List<ContaArquivadaDaLista> = contas
    .filter { conta -> conta.arquivada && conta.excluidoEm == null }
    .sortedWith(compareBy({ conta -> instituicoesFinanceiras.indexOf(conta.instituicao) }, { conta -> conta.apelido }))
    .map { conta ->
        ContaArquivadaDaLista(
            conta = conta,
            instrumentos = instrumentos
                .filter { instrumento -> instrumento.contaPublicId == conta.publicId && instrumento.excluidoEm == null }
                .sortedWith(compareBy({ it.tipo }, { it.apelido })),
        )
    }

internal fun Instrumento.rotuloNaListaDeContas(): String = when (tipo) {
    TipoInstrumento.VOUCHER -> listOfNotNull(beneficio?.rotuloDeExibicao() ?: "Voucher", apelido).joinToString(" · ")
    TipoInstrumento.CREDITO, TipoInstrumento.DEBITO ->
        listOfNotNull(tipo.rotuloDeExibicao(), apelido, ultimosDigitos?.let { "final $it" }).joinToString(" · ")
}
