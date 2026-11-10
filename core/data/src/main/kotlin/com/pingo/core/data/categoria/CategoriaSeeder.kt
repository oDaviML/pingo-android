package com.pingo.core.data.categoria

import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.model.Categoria
import com.pingo.core.model.CategoriaPadrao
import com.pingo.core.model.CategoriasPadrao
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class CategoriaSeeder @Inject constructor(private val categoriaRepository: CategoriaRepository, private val usuario: UsuarioAutenticado) {
    suspend fun garantirPadroes(agora: Instant) {
        val identificador = usuario.identificador() ?: return
        val categoriasDoUsuario = categoriaRepository.observeCategorias().first()

        val ausentes = CategoriasPadrao.todas
            .map { it.paraCategoria(identificador, agora) }
            .filter { categoriaRepository.buscar(it.publicId) == null }
            .filter { padrao -> categoriasDoUsuario.none { it.ocupaNome(padrao.nome, padrao.tipo) } }

        if (ausentes.isNotEmpty()) {
            categoriaRepository.salvarTodas(ausentes)
        }
    }

    private fun CategoriaPadrao.paraCategoria(identificador: String, agora: Instant): Categoria = Categoria(
        publicId = identificadorDeterministico(CATEGORIA, identificador, tipo.name, nome),
        nome = nome,
        tipo = tipo,
        padrao = true,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private companion object {
        const val CATEGORIA = "categoria"
    }
}
