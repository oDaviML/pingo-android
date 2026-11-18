package com.pingo.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.fatura.CompletarDatasDoCartaoUseCase
import com.pingo.core.data.impl.AssinaturaRepositoryImpl
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.ContasFinanceiroProjectionImpl
import com.pingo.core.data.impl.ContextoDeCaptura
import com.pingo.core.data.impl.CoordenadorDeContaFinanceiroImpl
import com.pingo.core.data.impl.FontesContasProjection
import com.pingo.core.data.impl.InicioFinanceiroProjectionImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.ProcessadorDeCapturaImpl
import com.pingo.core.data.impl.ReconciliadorDeAssinaturasLocal
import com.pingo.core.data.impl.ReconciliadorDeFaturasLocal
import com.pingo.core.data.impl.ReconciliadorDeVouchersLocal
import com.pingo.core.data.impl.ReconciliadorFinanceiroImpl
import com.pingo.core.data.impl.ReconciliadorVoucherCapturaImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RelatoriosProjectionImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.data.voucher.AjustarSaldoVoucherUseCase
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.data.voucher.ResolverRecargaVoucherUseCase
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class RelogioDeTeste(var instante: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("America/Sao_Paulo")

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = instante
}

internal class BancoRoomDeTeste(private val context: Context, nomeArquivo: String) {
    val agora: Instant = Instant.parse("2026-11-10T12:00:00Z")
    val relogio = RelogioDeTeste(agora)
    val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val queryExecutor: ExecutorService = Executors.newFixedThreadPool(4)
    private val transactionExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    private val corridaExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    private val corridaDispatcher = corridaExecutor.asCoroutineDispatcher()
    private val dbPath = context.getDatabasePath(nomeArquivo)

    val database: PingoDatabase = run {
        dbPath.delete()
        Room.databaseBuilder(context, PingoDatabase::class.java, nomeArquivo)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryExecutor(queryExecutor)
            .setTransactionExecutor(transactionExecutor)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
    }

    val roomTx: TransacaoLocal = RoomTransacaoLocal(database)
    val instrumentoRepository = InstrumentoRepositoryImpl(database.instrumentoDao())
    val lancamentoRepository = LancamentoRepositoryImpl(database.lancamentoDao())
    private val categoriaRepository = CategoriaRepositoryImpl(database.categoriaDao())
    private val regraRepository = RegraCategoriaRepositoryImpl(database.regraCategoriaDao())
    private val tokenStore = object : TokenStore {
        override suspend fun read(): StoredTokens? = null

        override suspend fun write(tokens: StoredTokens) = Unit

        override suspend fun clear() = Unit
    }
    private val categoriaAprendida = CategoriaAprendida(
        categoriaRepository = categoriaRepository,
        regraRepository = regraRepository,
        usuario = UsuarioAutenticado(tokenStore),
    )
    private val dispatchers = object : AppDispatchers {
        override val io: CoroutineDispatcher = corridaDispatcher
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }

    fun fechar() {
        database.close()
        corridaDispatcher.close()
        corridaExecutor.shutdownNow()
        transactionExecutor.shutdownNow()
        queryExecutor.shutdownNow()
        dbPath.delete()
    }

    fun contas(transacao: TransacaoLocal = roomTx): ContaRepositoryImpl =
        ContaRepositoryImpl(database.contaDao(), database.instrumentoDao(), transacao)

    fun processador(transacao: TransacaoLocal = roomTx): ProcessadorDeCapturaImpl = ProcessadorDeCapturaImpl(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        transacaoLocal = transacao,
        clock = clock,
        dispatchers = dispatchers,
    )

    fun manual(transacao: TransacaoLocal = roomTx): RegistrarLancamentoManual = RegistrarLancamentoManual(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        transacaoLocal = transacao,
        clock = clock,
    )

    fun editor(transacao: TransacaoLocal = roomTx): EditarLancamento = EditarLancamento(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        transacaoLocal = transacao,
        clock = clock,
    )

    fun marcador(transacao: TransacaoLocal = roomTx): MarcarTransferencia = MarcarTransferencia(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        transacaoLocal = transacao,
        clock = clock,
    )

    val coordenadorConta = CoordenadorDeContaFinanceiroImpl(
        configuracaoCartaoDao = database.configuracaoCartaoDao(),
        referenciaDao = database.referenciaSaldoVoucherDao(),
    )

    val reconciliadorDeFaturas = ReconciliadorDeFaturasLocal(
        instrumentoDao = database.instrumentoDao(),
        configuracaoCartaoDao = database.configuracaoCartaoDao(),
        faturaDao = database.faturaDao(),
        lancamentoDao = database.lancamentoDao(),
        clock = relogio,
    )

    val reconciliadorDeVouchers = ReconciliadorDeVouchersLocal(
        instrumentoDao = database.instrumentoDao(),
        referenciaDao = database.referenciaSaldoVoucherDao(),
        recargaDao = database.recargaProgramadaVoucherDao(),
        clock = relogio,
    )

    val reconciliadorDeAssinaturas = ReconciliadorDeAssinaturasLocal(
        assinaturaDao = database.assinaturaDao(),
        lancamentoDao = database.lancamentoDao(),
        categoriaDao = database.categoriaDao(),
        agendadorDeAvisos = AgendadorDeAvisosOpcional(),
        clock = relogio,
    )

    val reconciliadorFinanceiro = ReconciliadorFinanceiroImpl(
        faturas = reconciliadorDeFaturas,
        vouchers = reconciliadorDeVouchers,
        assinaturas = reconciliadorDeAssinaturas,
    )

    val lancamentosF2 = LancamentoRepositoryImpl(
        lancamentoDao = database.lancamentoDao(),
        transacaoLocal = roomTx,
        reconciliador = reconciliadorFinanceiro,
    )

    val reconciliadorVoucherCaptura = ReconciliadorVoucherCapturaImpl(
        recargaDao = database.recargaProgramadaVoucherDao(),
        lancamentoDao = database.lancamentoDao(),
    )

    fun contasF2(): ContaRepositoryImpl = ContaRepositoryImpl(
        contaDao = database.contaDao(),
        instrumentoDao = database.instrumentoDao(),
        transacao = roomTx,
        financeiro = coordenadorConta,
    )

    fun processadorF2(): ProcessadorDeCapturaImpl = ProcessadorDeCapturaImpl(
        lancamentoRepository = lancamentosF2,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        contexto = ContextoDeCaptura(roomTx, relogio, dispatchers, reconciliadorVoucherCaptura),
    )

    fun manualF2(): RegistrarLancamentoManual = RegistrarLancamentoManual(
        lancamentoRepository = lancamentosF2,
        instrumentoRepository = instrumentoRepository,
        transacaoLocal = roomTx,
        clock = relogio,
    )

    fun editorF2(): EditarLancamento = EditarLancamento(
        lancamentoRepository = lancamentosF2,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        transacaoLocal = roomTx,
        clock = relogio,
    )

    fun marcadorF2(): MarcarTransferencia = MarcarTransferencia(
        lancamentoRepository = lancamentosF2,
        instrumentoRepository = instrumentoRepository,
        transacaoLocal = roomTx,
        clock = relogio,
    )

    fun confirmarF2(): ConfirmarPendencias = ConfirmarPendencias(
        lancamentoRepository = lancamentosF2,
        instrumentoRepository = instrumentoRepository,
        clock = relogio,
        transacaoLocal = roomTx,
    )

    fun completarDatasF2(): CompletarDatasDoCartaoUseCase = CompletarDatasDoCartaoUseCase(
        transacaoLocal = roomTx,
        instrumentoDao = database.instrumentoDao(),
        configuracaoCartaoDao = database.configuracaoCartaoDao(),
        reconciliadorDeFaturas = reconciliadorDeFaturas,
        clock = relogio,
    )

    fun ajustarSaldoF2(): AjustarSaldoVoucherUseCase = AjustarSaldoVoucherUseCase(
        transacaoLocal = roomTx,
        instrumentoDao = database.instrumentoDao(),
        referenciaDao = database.referenciaSaldoVoucherDao(),
        lancamentoDao = database.lancamentoDao(),
        clock = relogio,
    )

    fun materializarRecargasF2(): MaterializarRecargasVencidasUseCase = MaterializarRecargasVencidasUseCase(
        transacaoLocal = roomTx,
        instrumentoDao = database.instrumentoDao(),
        recargaDao = database.recargaProgramadaVoucherDao(),
        lancamentoDao = database.lancamentoDao(),
        categoriaDao = database.categoriaDao(),
        clock = relogio,
    )

    fun resolverRecargaF2(): ResolverRecargaVoucherUseCase = ResolverRecargaVoucherUseCase(
        transacaoLocal = roomTx,
        lancamentoDao = database.lancamentoDao(),
        instrumentoDao = database.instrumentoDao(),
        recargaDao = database.recargaProgramadaVoucherDao(),
        categoriaDao = database.categoriaDao(),
        reconciliador = reconciliadorFinanceiro,
        clock = relogio,
    )

    fun assinaturasF2(): AssinaturaRepositoryImpl = AssinaturaRepositoryImpl(
        assinaturaDao = database.assinaturaDao(),
        lancamentoDao = database.lancamentoDao(),
        categoriaDao = database.categoriaDao(),
        transacaoLocal = roomTx,
        agendadorDeAvisos = AgendadorDeAvisosOpcional(),
        clock = relogio,
    )

    fun fontesContasProjection(): FontesContasProjection = FontesContasProjection(
        contaDao = database.contaDao(),
        instrumentoDao = database.instrumentoDao(),
        faturaDao = database.faturaDao(),
        configuracaoCartaoDao = database.configuracaoCartaoDao(),
        referenciaDao = database.referenciaSaldoVoucherDao(),
        recargaDao = database.recargaProgramadaVoucherDao(),
        lancamentoDao = database.lancamentoDao(),
    )

    fun contasProjectionF2(): ContasFinanceiroProjectionImpl = ContasFinanceiroProjectionImpl(
        fontes = fontesContasProjection(),
        materializarRecargasVencidas = materializarRecargasF2(),
        clock = relogio,
    )

    fun inicioProjectionF2(): InicioFinanceiroProjectionImpl = InicioFinanceiroProjectionImpl(
        fontes = fontesContasProjection(),
        materializarRecargasVencidas = materializarRecargasF2(),
        clock = relogio,
    )

    fun relatoriosProjectionF2(): RelatoriosProjectionImpl = RelatoriosProjectionImpl(
        lancamentoDao = database.lancamentoDao(),
        categoriaDao = database.categoriaDao(),
    )

    suspend fun semearCategoriasF2() {
        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-outros-despesa", "Outros", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-assinaturas", "Assinaturas", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-beneficio-receita", "Benefício", TipoCategoria.RECEITA, true, false, agora, agora, null),
                CategoriaEntity("cat-outros-receita", "Outros", TipoCategoria.RECEITA, true, false, agora, agora, null),
            ),
        )
    }

    suspend fun semearCategoriasPadrao() {
        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-outros-despesa", "Outros", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-outros-receita", "Outros", TipoCategoria.RECEITA, true, false, agora, agora, null),
            ),
        )
    }

    suspend fun semearConta(publicId: String, instituicao: InstituicaoFinanceira, arquivada: Boolean = false) {
        database.contaDao().upsert(
            ContaEntity(
                publicId = publicId,
                instituicao = instituicao,
                apelido = publicId,
                tipo = TipoConta.CORRENTE,
                arquivada = arquivada,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    suspend fun semearInstrumento(
        publicId: String,
        contaPublicId: String,
        tipo: TipoInstrumento = TipoInstrumento.CREDITO,
        digitos: String? = null,
        arquivado: Boolean = false,
    ) {
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = publicId,
                contaPublicId = contaPublicId,
                tipo = tipo,
                apelido = publicId,
                ultimosDigitos = digitos,
                arquivado = arquivado,
                fechamentoDia = if (tipo == TipoInstrumento.CREDITO) 5 else null,
                vencimentoDia = if (tipo == TipoInstrumento.CREDITO) 12 else null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    suspend fun semearLancamento(
        publicId: String,
        instrumentoPublicId: String?,
        valorCentavos: Long = 15000,
        tipo: TipoLancamento = TipoLancamento.RECEITA,
        origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
        dataHora: Instant = agora,
        transferenciaId: String? = null,
    ) {
        database.lancamentoDao().upsert(
            LancamentoEntity(
                publicId = publicId,
                instrumentoPublicId = instrumentoPublicId,
                categoriaPublicId = "cat-outros-despesa",
                faturaPublicId = null,
                assinaturaPublicId = null,
                compraPaiPublicId = null,
                estornoDePublicId = null,
                transferenciaId = transferenciaId,
                valorCentavos = valorCentavos,
                dataHora = dataHora,
                tipo = tipo,
                estabelecimento = "Mercado",
                status = StatusLancamento.CONFIRMADO,
                origem = origem,
                confianca = 1.0,
                motivoPendencia = null,
                hashDuplicata = null,
                observacao = null,
                parcelaNumero = 1,
                totalParcelas = 1,
                valorTotalCompraCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
                textoBruto = null,
            ),
        )
    }

    suspend fun <A, B> executarCorridaPrimeiroComita(
        primeiro: suspend (TransacaoLocal) -> A,
        segundo: suspend (TransacaoLocal) -> B,
    ): Pair<A, B> = coroutineScope {
        val segundoNaPorta = CompletableDeferred<Unit>()
        val primeiroFinalizou = CompletableDeferred<Unit>()
        val txPrimeiro = TransacaoComBarreira(
            base = roomTx,
            antesDeEntrar = {
                withTimeout(TIMEOUT_MS) { segundoNaPorta.await() }
            },
            depoisDeExecutar = {
                primeiroFinalizou.complete(Unit)
            },
        )
        val txSegundo = TransacaoComBarreira(
            base = roomTx,
            antesDeEntrar = {
                segundoNaPorta.complete(Unit)
                withTimeout(TIMEOUT_MS) { primeiroFinalizou.await() }
            },
        )
        val tarefaA = async(corridaDispatcher) { primeiro(txPrimeiro) }
        val tarefaB = async(corridaDispatcher) { segundo(txSegundo) }
        val resultados = awaitAll(tarefaA, tarefaB)
        @Suppress("UNCHECKED_CAST")
        (resultados[0] as A) to (resultados[1] as B)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
    }
}

internal class TransacaoComBarreira(
    private val base: TransacaoLocal,
    private val antesDeEntrar: suspend () -> Unit = {},
    private val depoisDeExecutar: () -> Unit = {},
) : TransacaoLocal {
    override suspend fun <T> executar(bloco: suspend () -> T): T {
        antesDeEntrar()
        return try {
            base.executar {
                bloco()
            }
        } finally {
            depoisDeExecutar()
        }
    }
}
