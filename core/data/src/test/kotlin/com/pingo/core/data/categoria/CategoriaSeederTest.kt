package com.pingo.core.data.categoria

import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriaSeederTest {
    private val despesasRn10 = listOf(
        "Alimentação",
        "Mercado",
        "Transporte",
        "Moradia",
        "Contas da casa",
        "Saúde",
        "Lazer",
        "Compras",
        "Assinaturas",
        "Educação",
        "Outros",
    )
    private val receitasRn10 = listOf("Salário", "Benefício", "Reembolso", "Outros")

    @Test
    fun `semeia onze despesas e quatro receitas marcadas como padrao`() = runBlocking {
        val ambiente = AmbienteCategorias()

        ambiente.seeder.garantirPadroes(AGORA)

        val categorias = ambiente.repositorio.observeCategorias().first()
        assertEquals(despesasRn10.sorted(), categorias.filter { it.tipo == TipoCategoria.DESPESA }.map { it.nome }.sorted())
        assertEquals(receitasRn10.sorted(), categorias.filter { it.tipo == TipoCategoria.RECEITA }.map { it.nome }.sorted())
        assertTrue(categorias.all { it.padrao && !it.arquivada && it.excluidoEm == null })
    }

    @Test
    fun `cada categoria padrao recebe identificador proprio mesmo com nome repetido em outro tipo`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        assertEquals(15, idsDe(ambiente).size)
    }

    @Test
    fun `executar de novo nao duplica as categorias padrao`() = runBlocking {
        val ambiente = AmbienteCategorias()

        ambiente.seeder.garantirPadroes(AGORA)
        ambiente.seeder.garantirPadroes(AGORA.plusSeconds(60))

        assertEquals(15, ambiente.repositorio.observeCategorias().first().size)
    }

    @Test
    fun `categoria padrao arquivada nao volta quando o padrao e semeado de novo`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")

        ambiente.repositorio.arquivar(lazer, AGORA.plusSeconds(60))
        ambiente.seeder.garantirPadroes(AGORA.plusSeconds(120))

        val categorias = ambiente.repositorio.observeCategorias().first()
        assertEquals(15, categorias.size)
        assertEquals(listOf(true), categorias.filter { it.nome == "Lazer" }.map { it.arquivada })
    }

    @Test
    fun `categoria padrao excluida nao volta quando o padrao e semeado de novo`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")

        ambiente.repositorio.excluir(lazer, AGORA.plusSeconds(60))
        ambiente.seeder.garantirPadroes(AGORA.plusSeconds(120))

        val categorias = ambiente.repositorio.observeCategorias().first()
        assertEquals(14, categorias.size)
        assertTrue(categorias.none { it.nome == "Lazer" })
    }

    @Test
    fun `categoria padrao renomeada mantem o identificador e o nome original nao volta`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")

        ambiente.repositorio.renomear(lazer, "Diversão", AGORA.plusSeconds(60))
        ambiente.seeder.garantirPadroes(AGORA.plusSeconds(120))

        val categorias = ambiente.repositorio.observeCategorias().first()
        assertEquals(15, categorias.size)
        assertTrue(categorias.none { it.nome == "Lazer" })
        assertEquals("Diversão", ambiente.repositorio.buscar(lazer)?.nome)
    }

    @Test
    fun `nome que o usuario ja usa no mesmo tipo nao ganha duplicata padrao`() = runBlocking {
        val ambiente = AmbienteCategorias()
        val lazerDoUsuario = ambiente.criar("Lazer")

        ambiente.seeder.garantirPadroes(AGORA)

        val lazeres = ambiente.repositorio.observeCategorias().first().filter { it.nome == "Lazer" }
        assertEquals(listOf(lazerDoUsuario.publicId), lazeres.map { it.publicId })
        assertEquals(15, ambiente.repositorio.observeCategorias().first().size)
    }

    @Test
    fun `sem usuario logado nao semeia nada`() = runBlocking {
        val ambiente = AmbienteCategorias(usuarioSub = null)

        ambiente.seeder.garantirPadroes(AGORA)

        assertTrue(ambiente.repositorio.observeCategorias().first().isEmpty())
    }

    @Test
    fun `o mesmo usuario em outro aparelho gera os mesmos identificadores`() = runBlocking {
        val primeiroAparelho = AmbienteCategorias(usuarioSub = "auth0|lucas").comPadroes()
        val segundoAparelho = AmbienteCategorias(usuarioSub = "auth0|lucas").comPadroes()

        assertEquals(idsDe(primeiroAparelho), idsDe(segundoAparelho))
    }

    @Test
    fun `usuarios diferentes recebem identificadores diferentes`() = runBlocking {
        val lucas = AmbienteCategorias(usuarioSub = "auth0|lucas").comPadroes()
        val ana = AmbienteCategorias(usuarioSub = "auth0|ana").comPadroes()

        assertTrue(idsDe(lucas).intersect(idsDe(ana)).isEmpty())
    }

    private suspend fun idsDe(ambiente: AmbienteCategorias): Set<String> =
        ambiente.repositorio.observeCategorias().first().map { it.publicId }.toSet()
}
