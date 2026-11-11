package com.pingo.feature.review.impl

sealed interface ReviewEvent {
    data object ConfirmarTodos : ReviewEvent

    data class Confirmar(val publicId: String) : ReviewEvent

    data class Descartar(val publicId: String) : ReviewEvent

    data class AbrirTrocaDeCategoria(val publicId: String) : ReviewEvent

    data class SelecionarCategoria(val publicId: String) : ReviewEvent

    data object AlternarAplicarATodos : ReviewEvent

    data object AplicarCategoria : ReviewEvent

    data object FecharTrocaDeCategoria : ReviewEvent

    data class AbrirMarcarTransferencia(val publicId: String) : ReviewEvent

    data class SelecionarDestino(val instrumentoPublicId: String) : ReviewEvent

    data object FecharMarcarTransferencia : ReviewEvent

    data object DispensarMensagem : ReviewEvent
}
