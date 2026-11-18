package com.pingo.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PingoMigrations {
    val MIGRACAO_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN textoBruto TEXT")
        }
    }

    val MIGRACAO_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_LANCAMENTOS_V3)
            db.execSQL(COPIA_LANCAMENTOS_V2)
            db.execSQL("DROP TABLE lancamentos")
            db.execSQL("ALTER TABLE lancamentos_novo RENAME TO lancamentos")
            INDICES_LANCAMENTOS.forEach(db::execSQL)
        }
    }

    val MIGRACAO_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN tipoSemTransferencia TEXT")
        }
    }

    val MIGRACAO_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE instrumentos ADD COLUMN arquivadoPorCascata INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRACAO_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_CONFIGURACOES_CARTAO_V6)
            INDICES_CONFIGURACOES_CARTAO.forEach(db::execSQL)

            db.execSQL(CREATE_REFERENCIAS_SALDO_VOUCHER_V6)
            INDICES_REFERENCIAS_SALDO_VOUCHER.forEach(db::execSQL)

            db.execSQL(CREATE_RECARGAS_PROGRAMADAS_VOUCHER_V6)
            INDICES_RECARGAS_PROGRAMADAS_VOUCHER.forEach(db::execSQL)

            db.execSQL(
                "ALTER TABLE assinaturas ADD COLUMN instrumentoPublicId TEXT " +
                    "REFERENCES instrumentos(publicId) ON UPDATE NO ACTION ON DELETE NO ACTION",
            )
            INDICES_ASSINATURAS_V6.forEach(db::execSQL)

            db.execSQL("ALTER TABLE lancamentos ADD COLUMN estabelecimentoNormalizado TEXT")
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN beneficio TEXT")
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN saldoObservadoCentavos INTEGER")
            INDICES_LANCAMENTOS_V6.forEach(db::execSQL)
        }
    }

    private const val CREATE_LANCAMENTOS_V3 = """
        CREATE TABLE IF NOT EXISTS `lancamentos_novo` (
            `publicId` TEXT NOT NULL,
            `instrumentoPublicId` TEXT,
            `categoriaPublicId` TEXT,
            `faturaPublicId` TEXT,
            `assinaturaPublicId` TEXT,
            `compraPaiPublicId` TEXT,
            `estornoDePublicId` TEXT,
            `transferenciaId` TEXT,
            `valorCentavos` INTEGER NOT NULL,
            `dataHora` INTEGER NOT NULL,
            `tipo` TEXT NOT NULL,
            `estabelecimento` TEXT,
            `status` TEXT NOT NULL,
            `origem` TEXT NOT NULL,
            `instituicaoOrigem` TEXT,
            `confianca` REAL NOT NULL,
            `motivoPendencia` TEXT,
            `hashDuplicata` TEXT,
            `observacao` TEXT,
            `parcelaNumero` INTEGER NOT NULL,
            `totalParcelas` INTEGER NOT NULL,
            `valorTotalCompraCentavos` INTEGER,
            `criadoEm` INTEGER NOT NULL,
            `atualizadoEm` INTEGER NOT NULL,
            `excluidoEm` INTEGER,
            `textoBruto` TEXT,
            PRIMARY KEY(`publicId`),
            FOREIGN KEY(`instrumentoPublicId`) REFERENCES `instrumentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`categoriaPublicId`) REFERENCES `categorias`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`faturaPublicId`) REFERENCES `faturas`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`assinaturaPublicId`) REFERENCES `assinaturas`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`compraPaiPublicId`) REFERENCES `lancamentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`estornoDePublicId`) REFERENCES `lancamentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    """

    private const val COPIA_LANCAMENTOS_V2 = """
        INSERT INTO `lancamentos_novo` (
            `publicId`, `instrumentoPublicId`, `categoriaPublicId`, `faturaPublicId`, `assinaturaPublicId`,
            `compraPaiPublicId`, `estornoDePublicId`, `transferenciaId`, `valorCentavos`, `dataHora`, `tipo`,
            `estabelecimento`, `status`, `origem`, `confianca`, `motivoPendencia`, `hashDuplicata`, `observacao`,
            `parcelaNumero`, `totalParcelas`, `valorTotalCompraCentavos`, `criadoEm`, `atualizadoEm`, `excluidoEm`,
            `textoBruto`
        )
        SELECT
            `publicId`, `instrumentoPublicId`, `categoriaPublicId`, `faturaPublicId`, `assinaturaPublicId`,
            `compraPaiPublicId`, `estornoDePublicId`, `transferenciaId`, `valorCentavos`, `dataHora`, `tipo`,
            `estabelecimento`, `status`, `origem`, `confianca`, `motivoPendencia`, `hashDuplicata`, `observacao`,
            `parcelaNumero`, `totalParcelas`, `valorTotalCompraCentavos`, `criadoEm`, `atualizadoEm`, `excluidoEm`,
            `textoBruto`
        FROM `lancamentos`
    """

    private val INDICES_LANCAMENTOS = listOf(
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_instrumentoPublicId` ON `lancamentos` (`instrumentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_categoriaPublicId` ON `lancamentos` (`categoriaPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_faturaPublicId` ON `lancamentos` (`faturaPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_assinaturaPublicId` ON `lancamentos` (`assinaturaPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_compraPaiPublicId` ON `lancamentos` (`compraPaiPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_estornoDePublicId` ON `lancamentos` (`estornoDePublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_dataHora` ON `lancamentos` (`dataHora`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_atualizadoEm` ON `lancamentos` (`atualizadoEm`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_excluidoEm` ON `lancamentos` (`excluidoEm`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_hashDuplicata` ON `lancamentos` (`hashDuplicata`)",
    )

    private const val CREATE_CONFIGURACOES_CARTAO_V6 = """
        CREATE TABLE IF NOT EXISTS `configuracoes_cartao` (
            `publicId` TEXT NOT NULL,
            `instrumentoPublicId` TEXT NOT NULL,
            `vigenciaInicio` INTEGER NOT NULL,
            `fechamentoDia` INTEGER,
            `vencimentoDia` INTEGER,
            `criadoEm` INTEGER NOT NULL,
            `atualizadoEm` INTEGER NOT NULL,
            `excluidoEm` INTEGER,
            PRIMARY KEY(`publicId`),
            FOREIGN KEY(`instrumentoPublicId`) REFERENCES `instrumentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    """

    private val INDICES_CONFIGURACOES_CARTAO = listOf(
        "CREATE INDEX IF NOT EXISTS `index_configuracoes_cartao_instrumentoPublicId` ON `configuracoes_cartao` (`instrumentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_configuracoes_cartao_instrumentoPublicId_vigenciaInicio` " +
            "ON `configuracoes_cartao` (`instrumentoPublicId`, `vigenciaInicio`)",
        "CREATE INDEX IF NOT EXISTS `index_configuracoes_cartao_atualizadoEm` ON `configuracoes_cartao` (`atualizadoEm`)",
        "CREATE INDEX IF NOT EXISTS `index_configuracoes_cartao_excluidoEm` ON `configuracoes_cartao` (`excluidoEm`)",
    )

    private const val CREATE_REFERENCIAS_SALDO_VOUCHER_V6 = """
        CREATE TABLE IF NOT EXISTS `referencias_saldo_voucher` (
            `publicId` TEXT NOT NULL,
            `instrumentoPublicId` TEXT NOT NULL,
            `tipoReferencia` TEXT NOT NULL,
            `saldoCentavos` INTEGER NOT NULL,
            `deltaCentavos` INTEGER,
            `instanteReferencia` INTEGER NOT NULL,
            `lancamentoPublicId` TEXT,
            `criadoEm` INTEGER NOT NULL,
            `atualizadoEm` INTEGER NOT NULL,
            `excluidoEm` INTEGER,
            PRIMARY KEY(`publicId`),
            FOREIGN KEY(`instrumentoPublicId`) REFERENCES `instrumentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`lancamentoPublicId`) REFERENCES `lancamentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    """

    private val INDICES_REFERENCIAS_SALDO_VOUCHER = listOf(
        "CREATE INDEX IF NOT EXISTS `index_referencias_saldo_voucher_instrumentoPublicId` " +
            "ON `referencias_saldo_voucher` (`instrumentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_referencias_saldo_voucher_lancamentoPublicId` " +
            "ON `referencias_saldo_voucher` (`lancamentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_referencias_saldo_voucher_instrumentoPublicId_instanteReferencia` " +
            "ON `referencias_saldo_voucher` (`instrumentoPublicId`, `instanteReferencia`)",
        "CREATE INDEX IF NOT EXISTS `index_referencias_saldo_voucher_atualizadoEm` ON `referencias_saldo_voucher` (`atualizadoEm`)",
        "CREATE INDEX IF NOT EXISTS `index_referencias_saldo_voucher_excluidoEm` ON `referencias_saldo_voucher` (`excluidoEm`)",
    )

    private const val CREATE_RECARGAS_PROGRAMADAS_VOUCHER_V6 = """
        CREATE TABLE IF NOT EXISTS `recargas_programadas_voucher` (
            `publicId` TEXT NOT NULL,
            `instrumentoPublicId` TEXT NOT NULL,
            `beneficio` TEXT,
            `competenciaMes` INTEGER NOT NULL,
            `dataPrevista` INTEGER NOT NULL,
            `valorPrevistoCentavos` INTEGER NOT NULL,
            `status` TEXT NOT NULL,
            `lancamentoPublicId` TEXT,
            `criadoEm` INTEGER NOT NULL,
            `atualizadoEm` INTEGER NOT NULL,
            `excluidoEm` INTEGER,
            PRIMARY KEY(`publicId`),
            FOREIGN KEY(`instrumentoPublicId`) REFERENCES `instrumentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`lancamentoPublicId`) REFERENCES `lancamentos`(`publicId`) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    """

    private val INDICES_RECARGAS_PROGRAMADAS_VOUCHER = listOf(
        "CREATE INDEX IF NOT EXISTS `index_recargas_programadas_voucher_instrumentoPublicId` " +
            "ON `recargas_programadas_voucher` (`instrumentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_recargas_programadas_voucher_lancamentoPublicId` " +
            "ON `recargas_programadas_voucher` (`lancamentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_recargas_programadas_voucher_instrumentoPublicId_competenciaMes` " +
            "ON `recargas_programadas_voucher` (`instrumentoPublicId`, `competenciaMes`)",
        "CREATE INDEX IF NOT EXISTS `index_recargas_programadas_voucher_atualizadoEm` ON `recargas_programadas_voucher` (`atualizadoEm`)",
        "CREATE INDEX IF NOT EXISTS `index_recargas_programadas_voucher_excluidoEm` ON `recargas_programadas_voucher` (`excluidoEm`)",
    )

    private val INDICES_ASSINATURAS_V6 = listOf(
        "CREATE INDEX IF NOT EXISTS `index_assinaturas_instrumentoPublicId` ON `assinaturas` (`instrumentoPublicId`)",
        "CREATE INDEX IF NOT EXISTS `index_assinaturas_instrumentoPublicId_estabelecimentoNormalizado` " +
            "ON `assinaturas` (`instrumentoPublicId`, `estabelecimentoNormalizado`)",
    )

    private val INDICES_LANCAMENTOS_V6 = listOf(
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_excluidoEm_status_tipo_dataHora` " +
            "ON `lancamentos` (`excluidoEm`, `status`, `tipo`, `dataHora`)",
        "CREATE INDEX IF NOT EXISTS `index_lancamentos_estabelecimentoNormalizado` ON `lancamentos` (`estabelecimentoNormalizado`)",
    )
}
