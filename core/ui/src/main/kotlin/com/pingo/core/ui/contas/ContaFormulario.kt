package com.pingo.core.ui.contas

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.apelidoValido

@Immutable
data class ContaFormulario(
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val tipoConta: TipoConta,
    val instrumentos: List<InstrumentoFormulario>,
    val publicId: String? = null,
) {
    val editando: Boolean
        get() = publicId != null

    fun apelidoValido(): Boolean = apelidoValido(apelido)

    fun valida(): Boolean = apelidoValido() && instrumentos.isNotEmpty() && instrumentos.all(InstrumentoFormulario::valido)

    fun podeRemoverInstrumento(): Boolean = instrumentos.size > 1

    fun adicionarInstrumento(tipo: TipoInstrumento): ContaFormulario = copy(instrumentos = instrumentos + InstrumentoFormulario.novo(tipo))

    fun removerInstrumento(chave: String): ContaFormulario =
        if (podeRemoverInstrumento()) copy(instrumentos = instrumentos.filterNot { it.chave == chave }) else this

    fun alterarInstrumento(chave: String, transformar: (InstrumentoFormulario) -> InstrumentoFormulario): ContaFormulario =
        copy(instrumentos = instrumentos.map { if (it.chave == chave) transformar(it) else it })

    fun numeroDoTipo(indice: Int): Int = instrumentos.take(indice + 1).count { it.tipo == instrumentos[indice].tipo }

    fun paraNovaConta(): NovaConta = NovaConta(
        instituicao = instituicao,
        apelido = apelido.trim(),
        tipo = tipoConta,
        instrumentos = instrumentos.map(InstrumentoFormulario::paraNovoInstrumento),
    )

    fun paraEdicao(): ContaEditada {
        val contaPublicId = requireNotNull(publicId) { "Conta sem identidade para edição" }

        return ContaEditada(
            publicId = contaPublicId,
            apelido = apelido.trim(),
            tipo = tipoConta,
            instrumentos = instrumentos.map(InstrumentoFormulario::paraEdicao),
        )
    }

    companion object {
        fun nova(instituicao: InstituicaoFinanceira = InstituicaoFinanceira.NUBANK): ContaFormulario = ContaFormulario(
            instituicao = instituicao,
            apelido = instituicao.nomeDeExibicao(),
            tipoConta = instituicao.tipoDeContaPadrao(),
            instrumentos = listOf(InstrumentoFormulario.novo(instituicao.tipoDeInstrumentoPadrao())),
        )

        fun de(conta: Conta, instrumentos: List<Instrumento>): ContaFormulario = ContaFormulario(
            instituicao = conta.instituicao,
            apelido = conta.apelido,
            tipoConta = conta.tipo,
            instrumentos = instrumentos.map(InstrumentoFormulario::de),
            publicId = conta.publicId,
        )
    }
}
