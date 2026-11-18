package com.pingo.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria

@Immutable
data class CategoriasERegrasUiState(
    val abaSelecionada: Int = 0,
    val categorias: List<CategoriaComContagem> = emptyList(),
    val regras: List<RegraCategoriaComUso> = emptyList(),
    val carregando: Boolean = true,
    val dialogoCriar: DialogoNovaCategoria? = null,
    val dialogoRenomear: DialogoRenomearCategoria? = null,
    val dialogoExcluirCategoria: DialogoExcluirCategoria? = null,
    val dialogoExcluirRegra: RegraCategoriaComUso? = null,
)

@Immutable
data class DialogoNovaCategoria(
    val nome: String = "",
    val tipo: TipoCategoria = TipoCategoria.DESPESA,
    val erro: String? = null,
    val salvando: Boolean = false,
)

@Immutable
data class DialogoRenomearCategoria(val categoria: Categoria, val nome: String, val erro: String? = null, val salvando: Boolean = false)

@Immutable
data class DialogoExcluirCategoria(
    val item: CategoriaComContagem,
    val destinosDisponiveis: List<Categoria>,
    val destinoSelecionadoId: String? = null,
    val erro: String? = null,
    val excluindo: Boolean = false,
)
