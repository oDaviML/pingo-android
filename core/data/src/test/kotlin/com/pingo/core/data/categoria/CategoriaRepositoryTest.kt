package com.pingo.core.data.categoria

import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriaRepositoryTest {
    @Test
    fun `criar devolve categoria ativa com o nome sem espacos sobrando`() = runBlocking {
        val ambiente = AmbienteCategorias()

        val categoria = ambiente.criar("  Pets   e  bichos ")

        assertEquals("Pets e bichos", categoria.nome)
        assertFalse(categoria.padrao)
        assertTrue(categoria.podeSerEscolhida)
        assertEquals(listOf(categoria.publicId), ambiente.repositorio.observeEscolhiveis().first().map { it.publicId })
    }

    @Test
    fun `criar recusa nome vazio`() = runBlocking {
        val ambiente = AmbienteCategorias()

        assertEquals(ResultadoCategoria.NomeVazio, ambiente.repositorio.criar("   ", TipoCategoria.DESPESA, AGORA))
    }

    @Test
    fun `criar recusa nome que outra categoria do mesmo tipo ja usa sem diferenciar maiusculas`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        assertEquals(ResultadoCategoria.NomeEmUso, ambiente.repositorio.criar("LAZER", TipoCategoria.DESPESA, AGORA))
    }

    @Test
    fun `o mesmo nome em outro tipo e aceito`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        val resultado = ambiente.repositorio.criar("Lazer", TipoCategoria.RECEITA, AGORA)

        assertTrue(resultado is ResultadoCategoria.Sucesso)
    }

    @Test
    fun `renomear troca o nome e mantem o identificador`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")

        val resultado = ambiente.repositorio.renomear(lazer, "Diversão", AGORA.plusSeconds(60))

        assertEquals("Diversão", (resultado as ResultadoCategoria.Sucesso).categoria.nome)
        assertEquals("Diversão", ambiente.repositorio.buscar(lazer)?.nome)
    }

    @Test
    fun `renomear recusa nome que outra categoria do mesmo tipo ja usa`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        val resultado = ambiente.repositorio.renomear(ambiente.idDe("Lazer"), "mercado", AGORA.plusSeconds(60))

        assertEquals(ResultadoCategoria.NomeEmUso, resultado)
    }

    @Test
    fun `renomear a propria categoria so trocando maiusculas nao e conflito`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        val resultado = ambiente.repositorio.renomear(ambiente.idDe("Lazer"), "LAZER", AGORA.plusSeconds(60))

        assertTrue(resultado is ResultadoCategoria.Sucesso)
    }

    @Test
    fun `renomear categoria inexistente devolve nao encontrada`() = runBlocking {
        val ambiente = AmbienteCategorias()

        assertEquals(ResultadoCategoria.NaoEncontrada, ambiente.repositorio.renomear("inexistente", "Pets", AGORA))
    }

    @Test
    fun `arquivar tira a categoria das escolhas mas ela continua no historico`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")

        ambiente.repositorio.arquivar(lazer, AGORA.plusSeconds(60))

        assertFalse(ambiente.repositorio.observeEscolhiveis().first().any { it.publicId == lazer })
        assertTrue(ambiente.repositorio.observeCategorias().first().any { it.publicId == lazer })
        val arquivada = ambiente.repositorio.buscar(lazer)
        assertTrue(arquivada?.arquivada == true)
        assertNull(arquivada?.excluidoEm)
    }

    @Test
    fun `categoria excluida nao aparece no historico nem nas escolhas`() = runBlocking {
        val repositorio = CategoriaRepositoryImpl(
            FakeCategoriaDao(listOf(categoriaEntidade(publicId = "antiga", nome = "Antiga", excluidoEm = AGORA))),
        )

        assertTrue(repositorio.observeCategorias().first().isEmpty())
        assertTrue(repositorio.observeEscolhiveis().first().isEmpty())
    }
}
