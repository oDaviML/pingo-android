package com.pingo.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.GestaoDeCategoriasRepository
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.ResultadoExclusaoCategoria
import com.pingo.core.model.TipoCategoria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class CategoriasERegrasViewModel @Inject constructor(private val repository: GestaoDeCategoriasRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CategoriasERegrasUiState())
    val uiState: StateFlow<CategoriasERegrasUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeCategoriasComContagem(),
                repository.observeRegrasComUso(),
            ) { categorias, regras -> categorias to regras }
                .collect { (categorias, regras) ->
                    mutableUiState.update { it.copy(categorias = categorias, regras = regras, carregando = false) }
                }
        }
    }

    fun selecionarAba(indice: Int) {
        mutableUiState.update { it.copy(abaSelecionada = indice.coerceIn(0, 1)) }
    }

    fun abrirDialogoCriar() {
        mutableUiState.update { it.copy(dialogoCriar = DialogoNovaCategoria()) }
    }

    fun atualizarNovaCategoria(nome: String? = null, tipo: TipoCategoria? = null) {
        mutableUiState.update { state ->
            val dialogo = state.dialogoCriar ?: return@update state
            state.copy(
                dialogoCriar = dialogo.copy(
                    nome = nome ?: dialogo.nome,
                    tipo = tipo ?: dialogo.tipo,
                    erro = null,
                ),
            )
        }
    }

    fun fecharDialogoCriar() {
        mutableUiState.update { it.copy(dialogoCriar = null) }
    }

    fun confirmarCriarCategoria() {
        val dialogo = mutableUiState.value.dialogoCriar ?: return
        if (dialogo.salvando) return

        viewModelScope.launch {
            mutableUiState.update { it.copy(dialogoCriar = dialogo.copy(salvando = true, erro = null)) }
            when (repository.criarCategoria(dialogo.nome, dialogo.tipo)) {
                is ResultadoCategoria.Sucesso -> mutableUiState.update { it.copy(dialogoCriar = null) }
                ResultadoCategoria.NomeVazio -> mutableUiState.update {
                    it.copy(dialogoCriar = dialogo.copy(salvando = false, erro = "Informe um nome para a categoria."))
                }
                ResultadoCategoria.NomeEmUso -> mutableUiState.update {
                    it.copy(dialogoCriar = dialogo.copy(salvando = false, erro = "Já existe uma categoria com esse nome."))
                }
                ResultadoCategoria.NaoEncontrada -> mutableUiState.update { it.copy(dialogoCriar = null) }
            }
        }
    }

    fun abrirDialogoRenomear(item: CategoriaComContagem) {
        mutableUiState.update {
            it.copy(dialogoRenomear = DialogoRenomearCategoria(categoria = item.categoria, nome = item.categoria.nome))
        }
    }

    fun atualizarNomeRenomeacao(nome: String) {
        mutableUiState.update { state ->
            val dialogo = state.dialogoRenomear ?: return@update state
            state.copy(dialogoRenomear = dialogo.copy(nome = nome, erro = null))
        }
    }

    fun fecharDialogoRenomear() {
        mutableUiState.update { it.copy(dialogoRenomear = null) }
    }

    fun confirmarRenomear() {
        val dialogo = mutableUiState.value.dialogoRenomear ?: return
        if (dialogo.salvando) return

        viewModelScope.launch {
            mutableUiState.update { it.copy(dialogoRenomear = dialogo.copy(salvando = true, erro = null)) }
            when (repository.renomearCategoria(dialogo.categoria.publicId, dialogo.nome)) {
                is ResultadoCategoria.Sucesso -> mutableUiState.update { it.copy(dialogoRenomear = null) }
                ResultadoCategoria.NomeVazio -> mutableUiState.update {
                    it.copy(dialogoRenomear = dialogo.copy(salvando = false, erro = "Informe um nome válido."))
                }
                ResultadoCategoria.NomeEmUso -> mutableUiState.update {
                    it.copy(dialogoRenomear = dialogo.copy(salvando = false, erro = "Já existe outra categoria com esse nome."))
                }
                ResultadoCategoria.NaoEncontrada -> mutableUiState.update { it.copy(dialogoRenomear = null) }
            }
        }
    }

    fun alternarArquivo(item: CategoriaComContagem) {
        viewModelScope.launch {
            repository.alternarArquivoCategoria(item.categoria.publicId, !item.categoria.arquivada)
        }
    }

    fun abrirDialogoExcluirCategoria(item: CategoriaComContagem) {
        val destinos = mutableUiState.value.categorias
            .map { it.categoria }
            .filter {
                it.publicId != item.categoria.publicId &&
                    it.podeSerEscolhida &&
                    it.tipo == item.categoria.tipo
            }
        mutableUiState.update {
            it.copy(
                dialogoExcluirCategoria = DialogoExcluirCategoria(
                    item = item,
                    destinosDisponiveis = destinos,
                ),
            )
        }
    }

    fun selecionarDestinoExclusao(categoriaPublicId: String) {
        mutableUiState.update { state ->
            val dialogo = state.dialogoExcluirCategoria ?: return@update state
            state.copy(dialogoExcluirCategoria = dialogo.copy(destinoSelecionadoId = categoriaPublicId, erro = null))
        }
    }

    fun fecharDialogoExcluirCategoria() {
        mutableUiState.update { it.copy(dialogoExcluirCategoria = null) }
    }

    fun confirmarExcluirCategoria() {
        val dialogo = mutableUiState.value.dialogoExcluirCategoria ?: return
        if (dialogo.excluindo) return

        viewModelScope.launch {
            mutableUiState.update { it.copy(dialogoExcluirCategoria = dialogo.copy(excluindo = true, erro = null)) }
            when (repository.excluirCategoria(dialogo.item.categoria.publicId, dialogo.destinoSelecionadoId)) {
                ResultadoExclusaoCategoria.Excluida,
                ResultadoExclusaoCategoria.NaoEncontrada,
                -> mutableUiState.update { it.copy(dialogoExcluirCategoria = null) }
                ResultadoExclusaoCategoria.DestinoObrigatorio -> mutableUiState.update {
                    it.copy(
                        dialogoExcluirCategoria = dialogo.copy(
                            excluindo = false,
                            erro = "Escolha para qual categoria mover os lançamentos antes de excluir.",
                        ),
                    )
                }
                ResultadoExclusaoCategoria.DestinoInvalido -> mutableUiState.update {
                    it.copy(
                        dialogoExcluirCategoria = dialogo.copy(
                            excluindo = false,
                            erro = "Selecione uma categoria ativa do mesmo tipo.",
                        ),
                    )
                }
            }
        }
    }

    fun solicitarExcluirRegra(item: RegraCategoriaComUso) {
        mutableUiState.update { it.copy(dialogoExcluirRegra = item) }
    }

    fun fecharDialogoExcluirRegra() {
        mutableUiState.update { it.copy(dialogoExcluirRegra = null) }
    }

    fun confirmarExcluirRegra() {
        val alvo = mutableUiState.value.dialogoExcluirRegra ?: return
        viewModelScope.launch {
            repository.excluirRegra(alvo.regra.publicId)
            mutableUiState.update { it.copy(dialogoExcluirRegra = null) }
        }
    }
}
