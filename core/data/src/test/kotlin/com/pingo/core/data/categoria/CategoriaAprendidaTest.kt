package com.pingo.core.data.categoria

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoriaAprendidaTest {
    @Test
    fun `aprender grava a regra com o estabelecimento normalizado e a categoria escolhida`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val transporte = ambiente.idDe("Transporte")

        val regra = ambiente.aprendida.aprender("DL *UberRides", transporte, AGORA)

        assertEquals("dl*uberrides", regra?.estabelecimentoNormalizado)
        assertEquals(transporte, regra?.categoriaPublicId)
    }

    @Test
    fun `proximo lancamento de qualquer variante do estabelecimento recebe a categoria aprendida`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        ambiente.aprendida.aprender("DL *UberRides", ambiente.idDe("Transporte"), AGORA)

        assertEquals("Transporte", ambiente.aprendida.resolver("DL*UberRides")?.nome)
        assertEquals("Transporte", ambiente.aprendida.resolver("  dl   *UBERRIDES ")?.nome)
    }

    @Test
    fun `identificador final do estabelecimento nao separa a regra`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        ambiente.aprendida.aprender("CLARO *31987905690", ambiente.idDe("Assinaturas"), AGORA)

        assertEquals("Assinaturas", ambiente.aprendida.resolver("CLARO *1234")?.nome)
    }

    @Test
    fun `estabelecimentos diferentes nao compartilham regra`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        ambiente.aprendida.aprender("DL *UberRides", ambiente.idDe("Transporte"), AGORA)

        assertNull(ambiente.aprendida.resolver("DL*99 RIDE"))
        assertNull(ambiente.aprendida.resolver("DL*99 99"))
    }

    @Test
    fun `aprender de novo troca a categoria sem duplicar a regra`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        ambiente.aprendida.aprender("DL *UberRides", ambiente.idDe("Transporte"), AGORA)
        val depois = AGORA.plusSeconds(3600)

        ambiente.aprendida.aprender("DL*UberRides", ambiente.idDe("Lazer"), depois)

        assertEquals("Lazer", ambiente.aprendida.resolver("DL *UberRides")?.nome)
        val regras = ambiente.repositorioRegras.observeRegras().first()
        assertEquals(1, regras.size)
        assertEquals(AGORA, regras.single().criadoEm)
        assertEquals(depois, regras.single().atualizadoEm)
    }

    @Test
    fun `categoria arquivada nao e aprendida`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")
        ambiente.repositorio.arquivar(lazer, AGORA)

        assertNull(ambiente.aprendida.aprender("Spotify", lazer, AGORA))
    }

    @Test
    fun `regra cuja categoria foi arquivada depois nao e aplicada`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val lazer = ambiente.idDe("Lazer")
        ambiente.aprendida.aprender("Spotify", lazer, AGORA)

        ambiente.repositorio.arquivar(lazer, AGORA.plusSeconds(60))

        assertNull(ambiente.aprendida.resolver("SPOTIFY"))
    }

    @Test
    fun `regra excluida nao e aplicada e aprender de novo a reativa`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()
        val transporte = ambiente.idDe("Transporte")
        ambiente.aprendida.aprender("DL *UberRides", transporte, AGORA)
        val regra = ambiente.repositorioRegras.observeRegras().first().single()

        ambiente.repositorioRegras.excluir(regra.publicId, AGORA.plusSeconds(60))
        assertNull(ambiente.aprendida.resolver("DL*UberRides"))

        ambiente.aprendida.aprender("DL *UberRides", transporte, AGORA.plusSeconds(120))
        assertEquals("Transporte", ambiente.aprendida.resolver("DL*UberRides")?.nome)
    }

    @Test
    fun `estabelecimento vazio nao e aprendido nem aplicado`() = runBlocking {
        val ambiente = AmbienteCategorias().comPadroes()

        assertNull(ambiente.aprendida.aprender("   ", ambiente.idDe("Lazer"), AGORA))
        assertNull(ambiente.aprendida.resolver(null))
    }

    @Test
    fun `sem usuario logado nao aprende regra`() = runBlocking {
        val ambiente = AmbienteCategorias(usuarioSub = null)
        val lazer = ambiente.criar("Lazer")

        assertNull(ambiente.aprendida.aprender("Spotify", lazer.publicId, AGORA))
    }
}
