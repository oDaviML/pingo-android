package com.pingo.feature.transactions.impl

import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.RegraCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeRegraCategoriaRepository(iniciais: List<RegraCategoria> = emptyList()) : RegraCategoriaRepository {
    private val regras = MutableStateFlow(iniciais)

    override fun observeRegras(): Flow<List<RegraCategoria>> = regras

    override suspend fun buscar(publicId: String): RegraCategoria? = regras.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoria? = regras.value.firstOrNull { regra ->
        regra.estabelecimentoNormalizado == estabelecimento && regra.excluidoEm == null
    }

    override suspend fun salvar(regra: RegraCategoria) {
        regras.update { atuais -> atuais.filterNot { it.publicId == regra.publicId } + regra }
    }

    override suspend fun salvarTodas(regras: List<RegraCategoria>) {
        regras.forEach { salvar(it) }
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        regras.update { atuais -> atuais.map { if (it.publicId == publicId) it.copy(excluidoEm = quando) else it } }
    }
}

class FakeTransacaoLocal : TransacaoLocal {
    var execucoes: Int = 0

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++

        return bloco()
    }
}
