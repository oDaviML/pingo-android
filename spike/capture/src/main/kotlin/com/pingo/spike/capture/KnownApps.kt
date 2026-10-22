package com.pingo.spike.capture

object KnownApps {
    private val names =
        mapOf(
            "com.nu.production" to "Nubank",
            "br.com.intermedium" to "Inter",
            "com.itau" to "Itaú",
            "br.com.itau.internet" to "Itaú",
            "com.mercadopago.wallet" to "Mercado Pago",
            "com.caju.employeeApp" to "Caju",
        )

    val packages: Set<String> = names.keys

    fun isKnown(packageName: String): Boolean = packageName in names

    fun displayName(packageName: String): String = names[packageName] ?: packageName
}
