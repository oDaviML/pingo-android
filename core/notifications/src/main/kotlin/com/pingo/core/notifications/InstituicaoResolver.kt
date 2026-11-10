package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira

interface InstituicaoResolver {
    fun resolver(pacote: String, tag: String?): InstituicaoFinanceira?
}
