package com.pingo.core.database

import android.content.Context
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.dao.FaturaDao
import com.pingo.core.database.dao.GestaoDeCategoriasDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.dao.RegraCategoriaDao
import com.pingo.core.database.dao.RegraParserDao
import com.pingo.core.database.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providePingoDatabase(@ApplicationContext context: Context, keyProvider: DatabaseKeyProvider): PingoDatabase =
        PingoDatabaseFactory.create(context, keyProvider)

    @Provides
    fun provideUsuarioDao(database: PingoDatabase): UsuarioDao = database.usuarioDao()

    @Provides
    fun provideContaDao(database: PingoDatabase): ContaDao = database.contaDao()

    @Provides
    fun provideInstrumentoDao(database: PingoDatabase): InstrumentoDao = database.instrumentoDao()

    @Provides
    fun provideLancamentoDao(database: PingoDatabase): LancamentoDao = database.lancamentoDao()

    @Provides
    fun provideCategoriaDao(database: PingoDatabase): CategoriaDao = database.categoriaDao()

    @Provides
    fun provideRegraCategoriaDao(database: PingoDatabase): RegraCategoriaDao = database.regraCategoriaDao()

    @Provides
    fun provideFaturaDao(database: PingoDatabase): FaturaDao = database.faturaDao()

    @Provides
    fun provideAssinaturaDao(database: PingoDatabase): AssinaturaDao = database.assinaturaDao()

    @Provides
    fun provideRegraParserDao(database: PingoDatabase): RegraParserDao = database.regraParserDao()

    @Provides
    fun provideConfiguracaoCartaoDao(database: PingoDatabase): ConfiguracaoCartaoDao = database.configuracaoCartaoDao()

    @Provides
    fun provideReferenciaSaldoVoucherDao(database: PingoDatabase): ReferenciaSaldoVoucherDao = database.referenciaSaldoVoucherDao()

    @Provides
    fun provideRecargaProgramadaVoucherDao(database: PingoDatabase): RecargaProgramadaVoucherDao = database.recargaProgramadaVoucherDao()

    @Provides
    fun provideGestaoDeCategoriasDao(database: PingoDatabase): GestaoDeCategoriasDao = database.gestaoDeCategoriasDao()
}
