package com.pingo.core.designsystem.component.finance

import androidx.compose.runtime.Immutable
import com.pingo.core.designsystem.component.PingoInstitution

enum class PingoInvoiceStatus(val defaultLabel: String) {
    Aberta("Aberta"),
    Fechada("Fechada"),
    Proxima("Próxima"),
    Incompleta("Incompleta"),
}

@Immutable
data class PingoInvoiceCardUi(
    val institution: PingoInstitution,
    val title: String,
    val status: PingoInvoiceStatus,
    val subtitle: String? = null,
    val amountFormatted: String? = null,
    val cycleProgress: Float? = null,
    val isOverdue: Boolean = false,
    val cyclePeriodText: String? = null,
    val dueCountdownText: String? = null,
    val incompleteMessage: String = "Datas de fechamento e vencimento não configuradas.",
    val completeDatesLabel: String = "Completar datas",
)

@Immutable
data class PingoDebitCardUi(
    val institution: PingoInstitution,
    val title: String,
    val amountFormatted: String,
    val subtitle: String = "Gasto do mês",
)

@Immutable
data class PingoVoucherCardUi(
    val title: String,
    val subtitle: String,
    val balanceFormatted: String,
    val institution: PingoInstitution = PingoInstitution.Caju,
    val lastRechargeText: String? = null,
    val adjustLabel: String = "Ajustar saldo",
)

@Immutable
data class PingoTabItem(val id: String, val label: String)
