package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtracaoRegrasV1Test {
    private val entradas = CorpusFixtures.entradas()
    private val regras = CorpusFixtures.regrasV1()

    @Test
    fun `compra aprovada do Itau extrai valor, estabelecimento e tipo COMPRA sem digitos`() {
        val dados = dadosDe("pingo-corpus.txt", 5)

        assertEquals(3790L, dados.valorCentavos)
        assertEquals("CLARO *31987905690", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
        assertEquals(DirecaoPadrao.DESPESA, dados.direcao)
        assertEquals("BRL", dados.moeda)
        assertNull(dados.digitos)
    }

    @Test
    fun `compra em USD do Itau extrai moeda estrangeira e valor na moeda original`() {
        val dados = dadosDe("pingo-corpus.txt", 44)

        assertEquals("USD", dados.moeda)
        assertEquals(2000L, dados.valorCentavos)
        assertEquals("FACTORY AI", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
    }

    @Test
    fun `Pix enviado e recebido do Itau extraem o nome da contraparte sem CPF`() {
        val enviado = dadosDe("pingo-corpus.txt", 7)
        val recebido = dadosDe("pingo-corpus.txt", 17)

        assertEquals(TipoLancamentoPadrao.PIX_ENVIADO, enviado.tipo)
        assertEquals(DirecaoPadrao.DESPESA, enviado.direcao)
        assertEquals(50000L, enviado.valorCentavos)
        assertEquals("DELMIRO", enviado.estabelecimento)
        assertEquals(TipoLancamentoPadrao.PIX_RECEBIDO, recebido.tipo)
        assertEquals(DirecaoPadrao.RECEITA, recebido.direcao)
        assertEquals(43065L, recebido.valorCentavos)
        assertEquals("DAVI", recebido.estabelecimento)
    }

    @Test
    fun `boleto pago do Itau extrai o beneficiario e registra como COMPRA`() {
        val dados = dadosDe("pingo-corpus.txt", 18)

        assertEquals(125174L, dados.valorCentavos)
        assertEquals("SOCIEDADE MINEIRA DE CULT", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
        assertEquals(DirecaoPadrao.DESPESA, dados.direcao)
    }

    @Test
    fun `Pix recebido da Inter com NBSP extrai valor e remetente`() {
        val dados = dadosDe("pingo-corpus.txt", 6)

        assertEquals(45000L, dados.valorCentavos)
        assertEquals("Maria das Dores Martins Lage", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.PIX_RECEBIDO, dados.tipo)
        assertEquals(DirecaoPadrao.RECEITA, dados.direcao)
        assertNull(dados.digitos)
    }

    @Test
    fun `Pix Automatico da Inter extrai valor e destinatario como PIX_ENVIADO`() {
        val dados = dadosDe("pingo-corpus-kaua.txt", 81)

        assertEquals(1290L, dados.valorCentavos)
        assertEquals("Ebanx", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.PIX_ENVIADO, dados.tipo)
        assertEquals(DirecaoPadrao.DESPESA, dados.direcao)
    }

    @Test
    fun `compra no credito da Inter extrai valor, estabelecimento e ultimos 4 digitos`() {
        val dados = dadosDe("pingo-corpus.txt", 92)

        assertEquals(2900L, dados.valorCentavos)
        assertEquals("FLASH UTILIDADES", dados.estabelecimento)
        assertEquals("0809", dados.digitos)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
    }

    @Test
    fun `compra no credito do Nubank extrai os ultimos 4 digitos do cartao com final`() {
        val dados = dadosDe("pingo-corpus-kaua.txt", 34)

        assertEquals(2399L, dados.valorCentavos)
        assertEquals("Google One", dados.estabelecimento)
        assertEquals("9759", dados.digitos)
    }

    @Test
    fun `compra no debito do Nubank sem cartao nao tem digitos`() {
        val dados = dadosDe("pingo-corpus-iasmin.txt", 8)

        assertEquals(4490L, dados.valorCentavos)
        assertEquals("NETFLIX ENTRETENIMENTO", dados.estabelecimento)
        assertNull(dados.digitos)
    }

    @Test
    fun `transferencia recebida do Nubank extrai valor nas tres variacoes`() {
        val semRemetente = dadosDe("pingo-corpus.txt", 95)
        val comRemetente = dadosDe("pingo-corpus-iasmin.txt", 13)
        val comFragmentoDeCpf = dadosDe("pingo-corpus-kaua.txt", 44)

        assertEquals(1L, semRemetente.valorCentavos)
        assertNull(semRemetente.estabelecimento)
        assertEquals(4500L, comRemetente.valorCentavos)
        assertEquals("Wanderson de Oliveira Furtado", comRemetente.estabelecimento)
        assertEquals(45000L, comFragmentoDeCpf.valorCentavos)
        assertEquals("KAUA DOS SANTOS RODRIGUES", comFragmentoDeCpf.estabelecimento)
        assertTrue(
            listOf(semRemetente, comRemetente, comFragmentoDeCpf).all {
                it.tipo == TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA && it.direcao == DirecaoPadrao.RECEITA
            },
        )
    }

    @Test
    fun `pagamento em conta do Mercado Pago extrai valor da linha Debitamos e mercador do titulo`() {
        val dados = dadosDe("pingo-corpus-kaua.txt", 26)

        assertEquals(489L, dados.valorCentavos)
        assertEquals("Uber", dados.estabelecimento)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
        assertEquals(DirecaoPadrao.DESPESA, dados.direcao)
    }

    @Test
    fun `pagamento de cartao do Mercado Pago com valor no titulo extrai valor e estabelecimento`() {
        val comProximaFatura = dadosDe("pingo-corpus.txt", 34)
        val comPara = dadosDe("pingo-corpus-kaua.txt", 8)

        assertEquals(7802L, comProximaFatura.valorCentavos)
        assertEquals("Amazon", comProximaFatura.estabelecimento)
        assertEquals(9999L, comPara.valorCentavos)
        assertEquals("Google", comPara.estabelecimento)
        assertEquals(TipoLancamentoPadrao.COMPRA, comProximaFatura.tipo)
        assertEquals(TipoLancamentoPadrao.COMPRA, comPara.tipo)
    }

    @Test
    fun `compra do Caju extrai valor, estabelecimento, saldo e beneficio do texto`() {
        val dados = dadosDe("pingo-corpus.txt", 14)

        assertEquals(3100L, dados.valorCentavos)
        assertEquals("Pastelaria Capixada", dados.estabelecimento)
        assertEquals(27002L, dados.saldoCentavos)
        assertEquals("AUXÍLIO ALIMENTAÇÃO", dados.beneficio)
        assertEquals(TipoLancamentoPadrao.COMPRA, dados.tipo)
    }

    @Test
    fun `compra do Caju com outro beneficio extrai o saldo e o beneficio daquela compra`() {
        val dados = dadosDe("pingo-corpus-iasmin.txt", 2)

        assertEquals(7500L, dados.valorCentavos)
        assertEquals("Cartao Otimo Otm", dados.estabelecimento)
        assertEquals(20500L, dados.saldoCentavos)
        assertEquals("MOBILIDADE", dados.beneficio)
    }

    @Test
    fun `deposito do Caju vira DEPOSITO de receita sem estabelecimento`() {
        val dados = dadosDe("pingo-corpus.txt", 71)

        assertEquals(72561L, dados.valorCentavos)
        assertEquals(TipoLancamentoPadrao.DEPOSITO, dados.tipo)
        assertEquals(DirecaoPadrao.RECEITA, dados.direcao)
        assertNull(dados.estabelecimento)
    }

    @Test
    fun `cartao final do Itau extrai os ultimos 4 digitos quando presentes`() {
        val notificacao = NotificacaoBruta(
            titulo = "Sua compra foi aprovada 💳",
            texto = "Olá! Compra aprovada de R$ 10,00 em LOJA ALFA no dia 01/11 às 10:00 no cartão final 4640. Boas compras.",
        )

        assertEquals("4640", classificar(InstituicaoFinanceira.ITAU, notificacao).digitos)
    }

    private fun dadosDe(arquivo: String, ordem: Int): TransacaoExtraida {
        val entrada = entradas.single { it.arquivo == arquivo && it.ordem == ordem }
        return classificar(requireNotNull(PacotesBancarios.instituicaoDe(entrada.pacote)), entrada.notificacao())
    }

    private fun classificar(instituicao: InstituicaoFinanceira, notificacao: NotificacaoBruta): TransacaoExtraida {
        val resultado = DefaultParserEngine().classificar(notificacao, regras[instituicao])
        assertTrue("esperada transação, obtido $resultado", resultado is ResultadoParse.Transacao)
        return (resultado as ResultadoParse.Transacao).dados
    }
}
