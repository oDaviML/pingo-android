package com.pingo.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.ProcessadorDeCapturaImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.manual.RegistrarLancamentoManual
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
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class BancoRoomDeTeste(private val context: Context, nomeArquivo: String) {
    val agora: Instant = Instant.parse("2026-11-10T12:00:00Z")
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
