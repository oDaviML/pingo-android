package com.pingo.core.data.categoria

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.model.Categoria
import com.pingo.core.model.CategoriasPadrao
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class CategorizadorAutomaticoTest {
    private val agora: Instant = Instant.parse("2026-11-18T12:00:00Z")

    private val categorias = CategoriasPadrao.todas.mapIndexed { indice, padrao ->
        Categoria("cat-$indice", padrao.nome, padrao.tipo, padrao = true, criadoEm = agora, atualizadoEm = agora)
    }

    private val categorizador = CategorizadorAutomatico(FakeCategorias(categorias))

    @Test
    fun `reconhece estabelecimentos conhecidos para todas as categorias de despesa`() = runBlocking {
        val casos = mapOf(
            "NETFLIX ENTRETENIMENTO" to "Assinaturas",
            "SPOTIFY P2637832" to "Assinaturas",
            "IFD*BURGER KING" to "Alimentação",
            "PADARIA SAO BENTO" to "Alimentação",
            "MERCADOLIVRE*LOJA" to "Compras",
            "SHOPEE *BR" to "Compras",
            "SUPERMERCADO CARREFOUR" to "Mercado",
            "DROGASIL FILIAL 12" to "Saúde",
            "UBER *TRIP" to "Transporte",
            "POSTO IPIRANGA CENTRO" to "Transporte",
            "CLARO *31987905690" to "Contas da casa",
            "LIVRARIA CULTURA" to "Educação",
            "CONDOMINIO EDIFICIO SOL" to "Moradia",
            "CINEMARK SHOPPING" to "Lazer",
        )

        casos.forEach { (estabelecimento, esperado) ->
            val categoria = categorizador.sugerir(estabelecimento, TipoCategoria.DESPESA)
            assertEquals("Esperado '$esperado' para '$estabelecimento'", esperado, categoria?.nome)
        }
    }

    @Test
    fun `reconhece padroes de receita`() = runBlocking {
        assertEquals("Salário", categorizador.sugerir("PAGAMENTO SALARIO EMPRESA", TipoCategoria.RECEITA)?.nome)
        assertEquals("Reembolso", categorizador.sugerir("ESTORNO COMPRA CARTAO", TipoCategoria.RECEITA)?.nome)
        assertEquals("Benefício", categorizador.sugerir("RECARGA CAJU BENEFICIOS", TipoCategoria.RECEITA)?.nome)
    }

    @Test
    fun `remove prefixos de adquirente sem remover ifd`() = runBlocking {
        assertEquals("Transporte", categorizador.sugerir("PAG*Uber *Viagem", TipoCategoria.DESPESA)?.nome)
        assertEquals("Assinaturas", categorizador.sugerir("EBN*SPOTIFY BR", TipoCategoria.DESPESA)?.nome)
        assertEquals("Alimentação", categorizador.sugerir("IFD*RESTAURANTE BOM PRATO", TipoCategoria.DESPESA)?.nome)
    }

    @Test
    fun `respeita fronteira de palavras para evitar falsos positivos`() = runBlocking {
        assertEquals("Mercado", categorizador.sugerir("BARBOSA SUPERMERCADOS LTDA", TipoCategoria.DESPESA)?.nome)
        assertNull(categorizador.sugerir("MAXIMO ELETRONICA LTDA", TipoCategoria.DESPESA))
        assertNull(categorizador.sugerir("OICEL TELECOM", TipoCategoria.DESPESA))
    }

    @Test
    fun `usa dica de beneficio quando estabelecimento eh desconhecido`() = runBlocking {
        assertEquals(
            "Alimentação",
            categorizador.sugerir("ESTABELECIMENTO XYZ 42", TipoCategoria.DESPESA, beneficioBruto = "REFEICAO")?.nome,
        )
        assertEquals(
            "Transporte",
            categorizador.sugerir("ESTABELECIMENTO XYZ 42", TipoCategoria.DESPESA, beneficioBruto = "MOBILIDADE")?.nome,
        )
        assertEquals(
            "Lazer",
            categorizador.sugerir("ESTABELECIMENTO XYZ 42", TipoCategoria.DESPESA, beneficioBruto = "CULTURA")?.nome,
        )
    }

    @Test
    fun `ignora categoria arquivada e retorna null se nao houver correspondencia`() = runBlocking {
        val semAlimentacao = categorias.map { if (it.nome == "Alimentação") it.copy(arquivada = true) else it }
        val filtrandoArquivadas = CategorizadorAutomatico(FakeCategorias(semAlimentacao))

        assertNull(filtrandoArquivadas.sugerir("PADARIA DO JOAO", TipoCategoria.DESPESA))
        assertNull(categorizador.sugerir("COMERCIO GEN XYZ 999", TipoCategoria.DESPESA))
    }

    private class FakeCategorias(iniciais: List<Categoria>) : CategoriaRepository {
        private val fluxo = MutableStateFlow(iniciais.filter { it.podeSerEscolhida })

        override fun observeCategorias(): Flow<List<Categoria>> = fluxo

        override fun observeEscolhiveis(): Flow<List<Categoria>> = fluxo

        override suspend fun buscar(publicId: String): Categoria? = fluxo.value.firstOrNull { it.publicId == publicId }

        override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = ResultadoCategoria.NomeVazio

        override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria =
            ResultadoCategoria.NaoEncontrada

        override suspend fun arquivar(publicId: String, quando: Instant) = Unit

        override suspend fun salvar(categoria: Categoria) = Unit

        override suspend fun salvarTodas(categorias: List<Categoria>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
    }
}
