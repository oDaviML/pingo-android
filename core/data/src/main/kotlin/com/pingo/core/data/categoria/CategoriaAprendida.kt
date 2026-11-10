package com.pingo.core.data.categoria

import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.model.Categoria
import com.pingo.core.model.EstabelecimentoNormalizado
import com.pingo.core.model.RegraCategoria
import java.time.Instant
import javax.inject.Inject

class CategoriaAprendida @Inject constructor(
    private val categoriaRepository: CategoriaRepository,
    private val regraRepository: RegraCategoriaRepository,
    private val usuario: UsuarioAutenticado,
) {
    suspend fun aprender(estabelecimentoBruto: String?, categoriaPublicId: String, agora: Instant): RegraCategoria? {
        val estabelecimento = EstabelecimentoNormalizado.de(estabelecimentoBruto)
        val identificador = usuario.identificador()
        val categoria = categoriaRepository.buscar(categoriaPublicId)?.takeIf { it.podeSerEscolhida }

        if (estabelecimento == null || identificador == null || categoria == null) {
            return null
        }

        val regra = montarRegra(identificador, estabelecimento, categoria, agora)
        regraRepository.salvar(regra)

        return regra
    }

    suspend fun resolver(estabelecimentoBruto: String?): Categoria? {
        val estabelecimento = EstabelecimentoNormalizado.de(estabelecimentoBruto) ?: return null
        val regra = regraRepository.buscarPorEstabelecimento(estabelecimento) ?: return null

        return categoriaRepository.buscar(regra.categoriaPublicId)?.takeIf { it.podeSerEscolhida }
    }

    private suspend fun montarRegra(identificador: String, estabelecimento: String, categoria: Categoria, agora: Instant): RegraCategoria {
        val publicIdDeterministico = identificadorDeterministico(REGRA, identificador, estabelecimento)
        val existente = regraRepository.buscarPorEstabelecimento(estabelecimento)
            ?: regraRepository.buscar(publicIdDeterministico)

        return RegraCategoria(
            publicId = existente?.publicId ?: publicIdDeterministico,
            categoriaPublicId = categoria.publicId,
            estabelecimentoNormalizado = estabelecimento,
            criadoEm = existente?.criadoEm ?: agora,
            atualizadoEm = agora,
            excluidoEm = null,
        )
    }

    private companion object {
        const val REGRA = "regra"
    }
}
