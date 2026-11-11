package com.pingo.core.notifications

enum class FabricanteDoSistema {
    SAMSUNG,
    XIAOMI,
    MOTOROLA,
    OUTRO,
    ;

    companion object {
        fun de(fabricante: String?): FabricanteDoSistema = when {
            fabricante.isNullOrBlank() -> OUTRO
            fabricante.contains("samsung", ignoreCase = true) -> SAMSUNG
            fabricante.contains("xiaomi", ignoreCase = true) -> XIAOMI
            fabricante.contains("motorola", ignoreCase = true) -> MOTOROLA
            else -> OUTRO
        }
    }
}
