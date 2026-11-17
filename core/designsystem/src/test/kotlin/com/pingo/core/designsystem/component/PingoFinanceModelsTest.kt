package com.pingo.core.designsystem.component

import com.pingo.core.designsystem.component.chart.PingoSubscriptionRowUi
import com.pingo.core.designsystem.component.chart.PingoSubscriptionSuggestionUi
import com.pingo.core.designsystem.component.finance.PingoDebitCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceStatus
import com.pingo.core.designsystem.component.finance.PingoTabItem
import com.pingo.core.designsystem.component.finance.PingoVoucherCardUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PingoFinanceModelsTest {
    @Test
    fun invoiceStatusLabelsMatchPortugueseCopy() {
        assertEquals("Aberta", PingoInvoiceStatus.Aberta.defaultLabel)
        assertEquals("Fechada", PingoInvoiceStatus.Fechada.defaultLabel)
        assertEquals("Próxima", PingoInvoiceStatus.Proxima.defaultLabel)
        assertEquals("Incompleta", PingoInvoiceStatus.Incompleta.defaultLabel)
    }

    @Test
    fun incompleteInvoiceUiDoesNotInventDatesOrAmounts() {
        val incomplete = PingoInvoiceCardUi(
            institution = PingoInstitution.Nubank,
            title = "Nubank · crédito",
            status = PingoInvoiceStatus.Incompleta,
        )
        assertNull(incomplete.amountFormatted)
        assertNull(incomplete.cycleProgress)
        assertNull(incomplete.cyclePeriodText)
        assertNull(incomplete.dueCountdownText)
        assertFalse(incomplete.isOverdue)
        assertEquals("Completar datas", incomplete.completeDatesLabel)
    }

    @Test
    fun financeAndSubscriptionUiModelsPreserveDefaults() {
        val debit = PingoDebitCardUi(
            institution = PingoInstitution.Itau,
            title = "Itaú · débito",
            amountFormatted = "R$ 2.140,00",
        )
        assertEquals("Gasto do mês", debit.subtitle)

        val voucher = PingoVoucherCardUi(
            title = "VR · Refeição",
            subtitle = "Caju · benefício",
            balanceFormatted = "R$ 486,20",
        )
        assertEquals(PingoInstitution.Caju, voucher.institution)
        assertEquals("Ajustar saldo", voucher.adjustLabel)

        val tab = PingoTabItem(id = "aberta", label = "Aberta")
        assertEquals("aberta", tab.id)

        val sub = PingoSubscriptionRowUi("Spotify", "Próxima cobrança ≈ 12/11 · Nubank", "R$ 21,90")
        assertEquals("Spotify", sub.title)

        val suggestion = PingoSubscriptionSuggestionUi("Netflix", "Inter · crédito", "R$ 44,90")
        assertEquals("Parece assinatura", suggestion.tagLabel)
        assertEquals("Confirmar", suggestion.confirmLabel)
        assertEquals("Recusar", suggestion.dismissLabel)
    }
}
