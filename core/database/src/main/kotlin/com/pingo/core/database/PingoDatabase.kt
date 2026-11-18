package com.pingo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.database.entity.RegraCategoriaEntity
import com.pingo.core.database.entity.RegraParserEntity
import com.pingo.core.database.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        ContaEntity::class,
        InstrumentoEntity::class,
        ConfiguracaoCartaoEntity::class,
        LancamentoEntity::class,
        CategoriaEntity::class,
        RegraCategoriaEntity::class,
        FaturaEntity::class,
        AssinaturaEntity::class,
        RegraParserEntity::class,
        ReferenciaSaldoVoucherEntity::class,
        RecargaProgramadaVoucherEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(PingoTypeConverters::class)
abstract class PingoDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao

    abstract fun contaDao(): ContaDao

    abstract fun instrumentoDao(): InstrumentoDao

    abstract fun configuracaoCartaoDao(): ConfiguracaoCartaoDao

    abstract fun lancamentoDao(): LancamentoDao

    abstract fun categoriaDao(): CategoriaDao

    abstract fun regraCategoriaDao(): RegraCategoriaDao

    abstract fun faturaDao(): FaturaDao

    abstract fun assinaturaDao(): AssinaturaDao

    abstract fun regraParserDao(): RegraParserDao

    abstract fun referenciaSaldoVoucherDao(): ReferenciaSaldoVoucherDao

    abstract fun recargaProgramadaVoucherDao(): RecargaProgramadaVoucherDao

    abstract fun gestaoDeCategoriasDao(): GestaoDeCategoriasDao

    companion object {
        const val NAME = "pingo.db"
        const val VERSION = 6
    }
}
