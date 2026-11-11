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
}
