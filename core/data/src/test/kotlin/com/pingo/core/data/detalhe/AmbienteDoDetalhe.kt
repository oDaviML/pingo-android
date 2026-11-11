package com.pingo.core.data.detalhe

import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.captura.FakeContas
import com.pingo.core.data.captura.FakeInstrumentos
import com.pingo.core.data.captura.FakeLancamentos
import com.pingo.core.data.captura.MutableClock
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.categoria.FakeCategoriaDao
import com.pingo.core.data.categoria.FakeRegraCategoriaDao
import com.pingo.core.data.categoria.FakeTokenStore
import com.pingo.core.data.categoria.tokensDoUsuario
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.FakeTransacaoLocal
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import java.time.Instant
import java.time.ZoneId

internal class AmbienteDoDetalhe {
    val agora: Instant = Instant.parse("2026-11-10T15:00:00Z")
    val clock = MutableClock(agora, ZoneId.of("America/Sao_Paulo"))
    val contas = FakeContas()
    val instrumentos = FakeInstrumentos(contas)
    val lancamentos = FakeLancamentos(instrumentos)
    val categorias = CategoriaRepositoryImpl(FakeCategoriaDao())
    val regras = RegraCategoriaRepositoryImpl(FakeRegraCategoriaDao())
    val transacoes = FakeTransacaoLocal()
    private val usuario = UsuarioAutenticado(FakeTokenStore(tokensDoUsuario("auth0|lucas")))
    val aprendida = CategoriaAprendida(categorias, regras, usuario)
    val editar = EditarLancamento(lancamentos, instrumentos, categorias, aprendida, transacoes, clock)
    val transferir = MarcarTransferencia(lancamentos, instrumentos, categorias, transacoes, clock)

    suspend fun categoria(nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): Categoria =
        when (val resultado = categorias.criar(nome, tipo, agora)) {
            is ResultadoCategoria.Sucesso -> resultado.categoria
            else -> error("categoria não criada: $resultado")
        }

    fun conta(apelido: String = "Nubank", instituicao: InstituicaoFinanceira = InstituicaoFinanceira.NUBANK): Conta =
        instanciaConta(apelido, instituicao)

    fun instrumento(conta: Conta, digitos: String? = null): Instrumento =
        instrumentos.criarNaConta(conta, digitos = digitos, apelido = "Cartão ${conta.apelido}")

    fun instrumento(instituicao: InstituicaoFinanceira = InstituicaoFinanceira.NUBANK): Instrumento =
        instrumentos.criar(instituicao, apelido = "Cartão ${instituicao.name}")

    private var contadorContas = 0

    private fun instanciaConta(apelido: String, instituicao: InstituicaoFinanceira): Conta {
        contadorContas += 1
        return Conta(
            publicId = "conta-$contadorContas",
            instituicao = instituicao,
            apelido = apelido,
            tipo = com.pingo.core.model.TipoConta.CORRENTE,
            criadoEm = agora,
            atualizadoEm = agora,
        )
    }
}

@Suppress("LongParameterList")
internal fun lancamentoDeTeste(
    publicId: String,
    instrumentoPublicId: String?,
    valorCentavos: Long,
    tipo: TipoLancamento = TipoLancamento.DESPESA,
    status: StatusLancamento = StatusLancamento.CONFIRMADO,
    origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
    motivoPendencia: MotivoPendencia? = null,
    categoriaPublicId: String? = null,
    estabelecimento: String? = "Padaria do Zé",
    dataHora: Instant = Instant.parse("2026-11-10T15:00:00Z"),
    instituicaoOrigem: InstituicaoFinanceira? = InstituicaoFinanceira.NUBANK,
    textoBruto: String? = null,
    transferenciaId: String? = null,
): Lancamento = Lancamento(
    publicId = publicId,
    instrumentoPublicId = instrumentoPublicId,
    categoriaPublicId = categoriaPublicId,
    transferenciaId = transferenciaId,
    valorCentavos = valorCentavos,
    dataHora = dataHora,
    tipo = tipo,
    estabelecimento = estabelecimento,
    status = status,
    origem = origem,
    instituicaoOrigem = instituicaoOrigem,
    confianca = if (origem == OrigemLancamento.MANUAL) 1.0 else 0.8,
    motivoPendencia = motivoPendencia?.name,
    criadoEm = dataHora,
    atualizadoEm = dataHora,
    textoBruto = textoBruto,
)
