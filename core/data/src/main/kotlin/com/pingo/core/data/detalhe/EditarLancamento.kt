package com.pingo.core.data.detalhe

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.data.captura.ConfiancaDaLeitura
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import com.pingo.core.model.direcaoDaTransferencia
import com.pingo.core.model.direcaoOposta
import com.pingo.core.model.fatura.GeradorDeParcelas
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.model.fatura.ParcelaPlanejada
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
class EditarLancamento @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categoriaAprendida: CategoriaAprendida,
    private val confirmadorDeAssinatura: ConfirmadorDeAssinatura,
    private val configuracaoCartaoRepository: ConfiguracaoCartaoRepository,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        categoriaAprendida: CategoriaAprendida,
        confirmadorDeAssinatura: ConfirmadorDeAssinatura,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        confirmadorDeAssinatura = confirmadorDeAssinatura,
        configuracaoCartaoRepository = SemConfiguracaoCartaoNaEdicao,
        transacaoLocal = transacaoLocal,
        clock = clock,
    )

    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        categoriaAprendida: CategoriaAprendida,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        confirmadorDeAssinatura = ConfirmadorDeAssinatura.NoOp,
        configuracaoCartaoRepository = SemConfiguracaoCartaoNaEdicao,
        transacaoLocal = transacaoLocal,
        clock = clock,
    )
    suspend fun editar(edicao: EdicaoDoLancamento): ResultadoDaEdicao = transacaoLocal.executar {
        editarNaTransacao(edicao)
    }

    private suspend fun editarNaTransacao(edicao: EdicaoDoLancamento): ResultadoDaEdicao {
        val original = lancamentoRepository.buscar(edicao.publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoDaEdicao.NaoEncontrado
        val instrumento = resolverInstrumento(edicao.instrumentoPublicId)
        val destino = resolverInstrumento(edicao.destinoPublicId)
        val instrumentoAtivo = edicao.instrumentoPublicId?.let { instrumentoRepository.buscarAtivo(it) }
        val destinoAtivo = edicao.destinoPublicId?.let { instrumentoRepository.buscarAtivo(it) }
        val categoria = resolverCategoria(edicao.categoriaPublicId)
        val destinoOriginal = destinoOriginalDe(original, edicao.destinoPublicId)
        val grupoAtivo = carregarGrupoParcelado(original)
        val paiGrupo = grupoAtivo.firstOrNull { it.parcelaNumero == 1 && it.compraPaiPublicId == null }
        val invalidos = validar(
            ValidacaoDaEdicao(
                edicao = edicao,
                original = original,
                instrumento = instrumento,
                instrumentoAtivo = instrumentoAtivo,
                destino = destino,
                destinoAtivo = destinoAtivo,
                destinoOriginal = destinoOriginal,
                categoria = categoria,
                paiGrupo = paiGrupo,
            ),
        )

        return when {
            invalidos.isNotEmpty() -> ResultadoDaEdicao.Invalido(invalidos)
            edicao.tipo == TipoLancamento.TRANSFERENCIA -> editarTransferencia(original, edicao, instrumento, destino)
            grupoAtivo.isNotEmpty() && paiGrupo != null ->
                editarGrupoParcelado(original, paiGrupo, grupoAtivo, edicao, instrumento, categoria)
            edicao.totalParcelas > 1 -> aplicarEdicaoParcelada(original, edicao, requireNotNull(instrumento), categoria)
            else -> aplicarEdicao(original, edicao, instrumento, categoria)
        }
    }

    private suspend fun carregarGrupoParcelado(original: Lancamento): List<Lancamento> {
        if (original.totalParcelas <= 1 && original.compraPaiPublicId == null) {
            return emptyList()
        }
        val raizPublicId = original.compraPaiPublicId ?: original.publicId
        return lancamentoRepository.listarParcelasDaCompra(raizPublicId)
            .filter { it.excluidoEm == null }
            .sortedBy { it.parcelaNumero }
    }

    private suspend fun resolverInstrumento(publicId: String?): Instrumento? = publicId
        ?.let { instrumentoRepository.buscar(it) }
        ?.takeIf { it.excluidoEm == null }

    private suspend fun destinoOriginalDe(original: Lancamento, destinoPublicId: String?): String? {
        if (destinoPublicId == null) {
            return null
        }

        val transferenciaId = original.transferenciaId ?: return null

        return lancamentoRepository.observeLancamentos().first()
            .firstOrNull { it.transferenciaId == transferenciaId && it.publicId != original.publicId }
            ?.instrumentoPublicId
    }

    private suspend fun resolverCategoria(publicId: String?): Categoria? = publicId
        ?.let { categoriaRepository.buscar(it) }
        ?.takeIf { it.podeSerEscolhida }

    private fun elegivelParaVinculo(escolhido: String, original: String?, ativo: Instrumento?): Boolean =
        ativo != null || escolhido == original

    private fun validar(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> {
        val doValor = if (dados.edicao.valorCentavos <= 0) setOf(CampoDoDetalhe.VALOR) else emptySet()

        return if (dados.edicao.tipo == TipoLancamento.TRANSFERENCIA) {
            doValor + validarTransferencia(dados)
        } else {
            doValor + validarLancamento(dados)
        }
    }

    private fun validarTransferencia(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> = buildSet {
        val edicao = dados.edicao
        if (edicao.instrumentoPublicId == null || dados.instrumento == null) {
            add(CampoDoDetalhe.INSTRUMENTO)
        } else if (!elegivelParaVinculo(edicao.instrumentoPublicId, dados.original.instrumentoPublicId, dados.instrumentoAtivo)) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.destinoPublicId == null || dados.destino == null) {
            add(CampoDoDetalhe.DESTINO)
        } else if (dados.instrumento != null && dados.destino.contaPublicId == dados.instrumento.contaPublicId) {
            add(CampoDoDetalhe.DESTINO)
        } else if (!elegivelParaVinculo(edicao.destinoPublicId, dados.destinoOriginal, dados.destinoAtivo)) {
            add(CampoDoDetalhe.DESTINO)
        }
    }

    private fun validarLancamento(dados: ValidacaoDaEdicao): Set<CampoDoDetalhe> = buildSet {
        val edicao = dados.edicao
        if (edicao.instrumentoPublicId != null && dados.instrumento == null) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.instrumentoPublicId != null && dados.instrumento != null) {
            if (!elegivelParaVinculo(edicao.instrumentoPublicId, dados.original.instrumentoPublicId, dados.instrumentoAtivo)) {
                add(CampoDoDetalhe.INSTRUMENTO)
            }
        }
        if (edicao.instrumentoPublicId == null && exigeInstrumento(dados.original)) {
            add(CampoDoDetalhe.INSTRUMENTO)
        }
        if (edicao.categoriaPublicId != null && dados.categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (dados.categoria != null && !compativel(dados.categoria, edicao.tipo)) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        if (edicao.aplicarCategoriaATodos && dados.categoria == null) {
            add(CampoDoDetalhe.CATEGORIA)
        }
        validarParcelamentoNaEdicao(dados)
    }

    private fun MutableSet<CampoDoDetalhe>.validarParcelamentoNaEdicao(dados: ValidacaoDaEdicao) {
        val edicao = dados.edicao
        if (edicao.totalParcelas !in GeradorDeParcelas.MIN_PARCELAS..GeradorDeParcelas.MAX_PARCELAS) {
            add(CampoDoDetalhe.PARCELAS)
            return
        }
        val jaParcelado = dados.original.totalParcelas > 1 || dados.original.compraPaiPublicId != null
        if (jaParcelado) {
            val alterouParcelamento = edicao.totalParcelas != dados.original.totalParcelas ||
                edicao.valorCentavos != dados.original.valorCentavos ||
                edicao.instrumentoPublicId != dados.original.instrumentoPublicId ||
                edicao.recalcularParcelas
            if (alterouParcelamento && dados.paiGrupo == null) {
                add(CampoDoDetalhe.PARCELAS)
                return
            }
        }
        if (edicao.totalParcelas <= 1) {
            return
        }
        val instrumentoCredito = dados.instrumento?.tipo == TipoInstrumento.CREDITO
        if (edicao.tipo != TipoLancamento.DESPESA || !instrumentoCredito) {
            add(CampoDoDetalhe.PARCELAS)
        } else if (
            edicao.modoParcelamento == ModoParcelamento.VALOR_TOTAL &&
            edicao.valorCentavos in 1 until edicao.totalParcelas
        ) {
            add(CampoDoDetalhe.VALOR)
        }
    }

    private data class ValidacaoDaEdicao(
        val edicao: EdicaoDoLancamento,
        val original: Lancamento,
        val instrumento: Instrumento?,
        val instrumentoAtivo: Instrumento?,
        val destino: Instrumento?,
        val destinoAtivo: Instrumento?,
        val destinoOriginal: String?,
        val categoria: Categoria?,
        val paiGrupo: Lancamento? = null,
    )

    private fun exigeInstrumento(original: Lancamento): Boolean =
        original.origem == OrigemLancamento.MANUAL || original.status == StatusLancamento.CONFIRMADO

    private fun compativel(categoria: Categoria, tipo: TipoLancamento): Boolean = when (tipo) {
        TipoLancamento.DESPESA -> categoria.tipo == TipoCategoria.DESPESA
        TipoLancamento.RECEITA -> categoria.tipo == TipoCategoria.RECEITA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> true
    }

    private suspend fun aplicarEdicao(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val baseAtualizada = construirBaseAtualizada(original, edicao, instrumento, categoria, agora)
        val lancamento = confirmadorDeAssinatura.aoEditarLancamento(original, baseAtualizada, categoria)

        lancamentoRepository.salvar(lancamento)
        aprender(edicao, estabelecimento, categoria, agora)

        return ResultadoDaEdicao.Salvo(lancamento)
    }

    private suspend fun aplicarEdicaoParcelada(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val vigencias = configuracaoCartaoRepository.listarPorInstrumento(instrumento.publicId)
        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = edicao.valorCentavos,
            totalParcelas = edicao.totalParcelas,
            modo = edicao.modoParcelamento,
            dataHoraInicial = edicao.dataHora,
            instrumento = instrumento,
            vigencias = vigencias,
        )
        val primeiraPlanejada = parcelas.first()
        val edicaoPrimeira = edicao.copy(valorCentavos = primeiraPlanejada.valorCentavos, dataHora = primeiraPlanejada.dataHora)
        val primeiraBase = construirBaseAtualizada(original, edicaoPrimeira, instrumento, categoria, agora).copy(
            tipo = TipoLancamento.DESPESA,
            compraPaiPublicId = null,
            parcelaNumero = primeiraPlanejada.numero,
            totalParcelas = primeiraPlanejada.totalParcelas,
            valorTotalCompraCentavos = primeiraPlanejada.valorTotalCompraCentavos,
        )
        val primeiraSalva = confirmadorDeAssinatura.aoEditarLancamento(original, primeiraBase, categoria)
        val restantes = parcelas.drop(1).map { parcela -> montarParcelaFilha(original, primeiraSalva, parcela, agora) }

        lancamentoRepository.salvarTodos(listOf(primeiraSalva) + restantes)
        aprender(edicao, estabelecimento, categoria, agora)

        return ResultadoDaEdicao.Salvo(primeiraSalva)
    }

    @Suppress("LongParameterList")
    private suspend fun editarGrupoParcelado(
        original: Lancamento,
        pai: Lancamento,
        grupoAtivo: List<Lancamento>,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val precisaReplanejar = edicao.totalParcelas != original.totalParcelas ||
            edicao.valorCentavos != original.valorCentavos ||
            edicao.instrumentoPublicId != original.instrumentoPublicId ||
            edicao.recalcularParcelas ||
            (original.publicId == pai.publicId && edicao.dataHora != pai.dataHora)

        return when {
            !precisaReplanejar -> atualizarMetadadosGrupoParcelado(original, pai, grupoAtivo, edicao, instrumento, categoria)
            edicao.totalParcelas == 1 -> converterGrupoParaAVista(original, pai, grupoAtivo, edicao, instrumento, categoria)
            else -> replanejarGrupoParcelado(original, pai, grupoAtivo, edicao, requireNotNull(instrumento), categoria)
        }
    }

    @Suppress("LongParameterList")
    private suspend fun atualizarMetadadosGrupoParcelado(
        original: Lancamento,
        pai: Lancamento,
        grupoAtivo: List<Lancamento>,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val atualizados = grupoAtivo.map { item ->
            val dataItem = if (item.publicId == original.publicId) edicao.dataHora else item.dataHora
            val edicaoItem = edicao.copy(valorCentavos = item.valorCentavos, dataHora = dataItem)
            val base = construirBaseAtualizada(item, edicaoItem, instrumento, categoria, agora).copy(
                compraPaiPublicId = item.compraPaiPublicId,
                parcelaNumero = item.parcelaNumero,
                totalParcelas = item.totalParcelas,
                valorTotalCompraCentavos = item.valorTotalCompraCentavos,
            )
            if (item.publicId == pai.publicId) confirmadorDeAssinatura.aoEditarLancamento(pai, base, categoria) else base
        }
        lancamentoRepository.salvarTodos(atualizados)
        aprender(edicao, estabelecimento, categoria, agora)
        return ResultadoDaEdicao.Salvo(atualizados.firstOrNull { it.publicId == original.publicId } ?: atualizados.first())
    }

    @Suppress("LongParameterList")
    private suspend fun converterGrupoParaAVista(
        original: Lancamento,
        pai: Lancamento,
        grupoAtivo: List<Lancamento>,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val dataUnica = if (original.publicId == pai.publicId) edicao.dataHora else pai.dataHora
        val base = construirBaseAtualizada(pai, edicao.copy(dataHora = dataUnica), instrumento, categoria, agora).copy(
            compraPaiPublicId = null,
            parcelaNumero = 1,
            totalParcelas = 1,
            valorTotalCompraCentavos = null,
        )
        val salvo = confirmadorDeAssinatura.aoEditarLancamento(pai, base, categoria)
        lancamentoRepository.salvar(salvo)
        val filhasIds = grupoAtivo.filter { it.publicId != pai.publicId }.map { it.publicId }
        if (filhasIds.isNotEmpty()) {
            lancamentoRepository.excluirTodos(filhasIds, agora)
        }
        aprender(edicao, estabelecimento, categoria, agora)
        return ResultadoDaEdicao.Salvo(salvo)
    }

    @Suppress("LongParameterList")
    private suspend fun replanejarGrupoParcelado(
        original: Lancamento,
        pai: Lancamento,
        grupoAtivo: List<Lancamento>,
        edicao: EdicaoDoLancamento,
        cartao: Instrumento,
        categoria: Categoria?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val vigencias = configuracaoCartaoRepository.listarPorInstrumento(cartao.publicId)
        val dataHoraBase = if (original.publicId == pai.publicId) edicao.dataHora else pai.dataHora
        val planejadas = GeradorDeParcelas.planejar(
            valorBaseCentavos = edicao.valorCentavos,
            totalParcelas = edicao.totalParcelas,
            modo = edicao.modoParcelamento,
            dataHoraInicial = dataHoraBase,
            instrumento = cartao,
            vigencias = vigencias,
        )
        val primeiraSalva = atualizarParcelaPai(pai, planejadas.first(), edicao, cartao, categoria, agora)
        val porNumero = grupoAtivo.associateBy { it.parcelaNumero }
        val restantes = planejadas.drop(1).map { parcela ->
            atualizarOuCriarParcelaFilha(pai, primeiraSalva, porNumero[parcela.numero], parcela, agora)
        }
        val novasParcelas = listOf(primeiraSalva) + restantes
        lancamentoRepository.salvarTodos(novasParcelas)
        val excedentesIds = grupoAtivo
            .filter { it.publicId != pai.publicId && it.parcelaNumero > edicao.totalParcelas }
            .map { it.publicId }
        if (excedentesIds.isNotEmpty()) {
            lancamentoRepository.excluirTodos(excedentesIds, agora)
        }
        aprender(edicao, estabelecimento, categoria, agora)
        return ResultadoDaEdicao.Salvo(novasParcelas.firstOrNull { it.publicId == original.publicId } ?: primeiraSalva)
    }

    @Suppress("LongParameterList")
    private suspend fun atualizarParcelaPai(
        pai: Lancamento,
        primeiraPlanejada: ParcelaPlanejada,
        edicao: EdicaoDoLancamento,
        cartao: Instrumento,
        categoria: Categoria?,
        agora: Instant,
    ): Lancamento {
        val primeiraBase = construirBaseAtualizada(
            pai,
            edicao.copy(valorCentavos = primeiraPlanejada.valorCentavos, dataHora = primeiraPlanejada.dataHora),
            cartao,
            categoria,
            agora,
        ).copy(
            tipo = TipoLancamento.DESPESA,
            compraPaiPublicId = null,
            parcelaNumero = primeiraPlanejada.numero,
            totalParcelas = primeiraPlanejada.totalParcelas,
            valorTotalCompraCentavos = primeiraPlanejada.valorTotalCompraCentavos,
        )
        return confirmadorDeAssinatura.aoEditarLancamento(pai, primeiraBase, categoria)
    }

    private fun atualizarOuCriarParcelaFilha(
        pai: Lancamento,
        primeiraSalva: Lancamento,
        candidata: Lancamento?,
        parcela: ParcelaPlanejada,
        agora: Instant,
    ): Lancamento {
        val existente = candidata?.takeIf { it.publicId != pai.publicId }
            ?: return montarParcelaFilha(pai, primeiraSalva, parcela, agora)
        return existente.copy(
            instrumentoPublicId = primeiraSalva.instrumentoPublicId,
            categoriaPublicId = primeiraSalva.categoriaPublicId,
            compraPaiPublicId = pai.publicId,
            valorCentavos = parcela.valorCentavos,
            dataHora = parcela.dataHora,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = primeiraSalva.estabelecimento,
            status = StatusLancamento.CONFIRMADO,
            confianca = 1.0,
            motivoPendencia = null,
            parcelaNumero = parcela.numero,
            totalParcelas = parcela.totalParcelas,
            valorTotalCompraCentavos = null,
            atualizadoEm = agora,
            estabelecimentoNormalizado = primeiraSalva.estabelecimentoNormalizado,
        )
    }

    private fun construirBaseAtualizada(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
        agora: Instant,
    ): Lancamento {
        val estabelecimento = edicao.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }
        val motivo = motivoReavaliado(original, instrumento, categoria, estabelecimento)
        val beneficio = if (instrumento?.tipo == TipoInstrumento.VOUCHER && original.beneficio == null) {
            instrumento.beneficio
        } else {
            original.beneficio
        }
        return original.copy(
            tipo = edicao.tipo,
            valorCentavos = edicao.valorCentavos,
            instrumentoPublicId = instrumento?.publicId,
            categoriaPublicId = categoria?.publicId,
            dataHora = edicao.dataHora,
            estabelecimento = estabelecimento,
            status = statusDe(original.status, motivo),
            motivoPendencia = motivo?.name,
            confianca = confiancaDe(original, edicao.valorCentavos, estabelecimento, categoria),
            transferenciaId = null,
            tipoSemTransferencia = null,
            atualizadoEm = agora,
            estabelecimentoNormalizado = NormalizadorDeAssinatura.normalizar(estabelecimento)?.take(LIMITE_NORMALIZADO),
            beneficio = beneficio,
        )
    }

    private fun montarParcelaFilha(original: Lancamento, primeira: Lancamento, parcela: ParcelaPlanejada, agora: Instant): Lancamento =
        Lancamento(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = primeira.instrumentoPublicId,
            categoriaPublicId = primeira.categoriaPublicId,
            compraPaiPublicId = original.publicId,
            valorCentavos = parcela.valorCentavos,
            dataHora = parcela.dataHora,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = primeira.estabelecimento,
            status = StatusLancamento.CONFIRMADO,
            origem = OrigemLancamento.MANUAL,
            instituicaoOrigem = original.instituicaoOrigem,
            confianca = 1.0,
            parcelaNumero = parcela.numero,
            totalParcelas = parcela.totalParcelas,
            valorTotalCompraCentavos = null,
            criadoEm = agora,
            atualizadoEm = agora,
            estabelecimentoNormalizado = primeira.estabelecimentoNormalizado,
        )

    private suspend fun editarTransferencia(
        original: Lancamento,
        edicao: EdicaoDoLancamento,
        instrumento: Instrumento?,
        destino: Instrumento?,
    ): ResultadoDaEdicao {
        val agora = clock.instant()
        val transferenciaId = original.transferenciaId ?: UUID.randomUUID().toString()
        val parceiro = lancamentoRepository.observeLancamentos().first()
            .firstOrNull { it.transferenciaId == transferenciaId && it.publicId != original.publicId }
        val lancamento = original.copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = original.direcaoDaTransferencia(),
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            instrumentoPublicId = instrumento?.publicId,
            transferenciaId = transferenciaId,
            status = transferenciaResolvida(original.status, original.motivoPendencia),
            motivoPendencia = motivoDaTransferencia(original.status, original.motivoPendencia),
            atualizadoEm = agora,
        )
        val par = parceiro?.copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = parceiro.direcaoDaTransferencia(),
            valorCentavos = edicao.valorCentavos,
            dataHora = edicao.dataHora,
            instrumentoPublicId = destino?.publicId,
            transferenciaId = transferenciaId,
            status = transferenciaResolvida(parceiro.status, parceiro.motivoPendencia),
            motivoPendencia = motivoDaTransferencia(parceiro.status, parceiro.motivoPendencia),
            atualizadoEm = agora,
        ) ?: novoPar(edicao, destino, transferenciaId, original.direcaoDaTransferencia()?.direcaoOposta, agora)

        lancamentoRepository.salvar(lancamento)
        lancamentoRepository.salvar(par)

        return ResultadoDaEdicao.Salvo(lancamento = lancamento, par = par)
    }

    private fun novoPar(
        edicao: EdicaoDoLancamento,
        destino: Instrumento?,
        transferenciaId: String,
        direcao: TipoLancamento?,
        agora: Instant,
    ): Lancamento = Lancamento(
        publicId = UUID.randomUUID().toString(),
        instrumentoPublicId = destino?.publicId,
        transferenciaId = transferenciaId,
        valorCentavos = edicao.valorCentavos,
        dataHora = edicao.dataHora,
        tipo = TipoLancamento.TRANSFERENCIA,
        tipoSemTransferencia = direcao,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun Lancamento.direcaoDaTransferencia(): TipoLancamento? = tipoSemTransferencia ?: tipo.direcaoDaTransferencia

    private fun transferenciaResolvida(status: StatusLancamento, motivo: String?): StatusLancamento =
        if (status == StatusLancamento.PENDENTE && motivo == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name) {
            StatusLancamento.CONFIRMADO
        } else {
            status
        }

    private fun motivoDaTransferencia(status: StatusLancamento, motivo: String?): String? =
        if (status == StatusLancamento.PENDENTE && motivo == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name) null else motivo

    private fun motivoReavaliado(
        original: Lancamento,
        instrumento: Instrumento?,
        categoria: Categoria?,
        estabelecimento: String?,
    ): MotivoPendencia? = when {
        original.status == StatusLancamento.CONFIRMADO -> null
        original.motivoPendencia == MotivoPendencia.DIVERGENCIA_RECARGA.name -> MotivoPendencia.DIVERGENCIA_RECARGA
        instrumento == null -> motivoDeInstrumento(original.motivoPendencia)
        original.motivoPendencia == MotivoPendencia.MOEDA_ESTRANGEIRA.name -> MotivoPendencia.MOEDA_ESTRANGEIRA
        original.motivoPendencia == MotivoPendencia.POSSIVEL_DUPLICATA.name -> MotivoPendencia.POSSIVEL_DUPLICATA
        estabelecimento.isNullOrBlank() -> MotivoPendencia.CAMPOS_INCOMPLETOS
        categoria == null -> MotivoPendencia.CATEGORIA_DESCONHECIDA
        else -> null
    }

    private fun motivoDeInstrumento(motivo: String?): MotivoPendencia = when (motivo) {
        MotivoPendencia.INSTRUMENTO_AMBIGUO.name -> MotivoPendencia.INSTRUMENTO_AMBIGUO
        MotivoPendencia.BENEFICIO_AMBIGUO.name -> MotivoPendencia.BENEFICIO_AMBIGUO
        else -> MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO
    }

    private fun statusDe(original: StatusLancamento, motivo: MotivoPendencia?): StatusLancamento =
        if (original == StatusLancamento.CONFIRMADO || motivo == null) StatusLancamento.CONFIRMADO else StatusLancamento.PENDENTE

    private fun confiancaDe(original: Lancamento, valorCentavos: Long, estabelecimento: String?, categoria: Categoria?): Double =
        if (original.origem == OrigemLancamento.NOTIFICACAO) {
            ConfiancaDaLeitura.calcular(valorCentavos, estabelecimento, categoria)
        } else {
            original.confianca
        }

    private suspend fun aprender(edicao: EdicaoDoLancamento, estabelecimento: String?, categoria: Categoria?, agora: Instant) {
        if (!edicao.aplicarCategoriaATodos || estabelecimento == null || categoria == null) {
            return
        }

        categoriaAprendida.aprender(estabelecimento, categoria.publicId, agora)
    }

    private companion object {
        const val LIMITE_NORMALIZADO = 200
    }
}

private object SemConfiguracaoCartaoNaEdicao : ConfiguracaoCartaoRepository {
    override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> = emptyFlow()

    override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> = emptyList()

    override suspend fun listarAtivas(): List<VigenciaDeCartao> = emptyList()
}
