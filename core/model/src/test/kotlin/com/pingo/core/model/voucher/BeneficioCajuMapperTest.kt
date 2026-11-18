package com.pingo.core.model.voucher

import com.pingo.core.model.BeneficioVoucher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeneficioCajuMapperTest {
    @Test
    fun `mapeia nomes conhecidos de vr va e vt ignorando caixa e acentos`() {
        assertEquals(BeneficioVoucher.VR, BeneficioCajuMapper.mapear("VR"))
        assertEquals(BeneficioVoucher.VR, BeneficioCajuMapper.mapear("Auxílio Refeição"))
        assertEquals(BeneficioVoucher.VR, BeneficioCajuMapper.mapear("REFEIÇÃO"))
        assertEquals(BeneficioVoucher.VA, BeneficioCajuMapper.mapear("VA"))
        assertEquals(BeneficioVoucher.VA, BeneficioCajuMapper.mapear("AUXÍLIO ALIMENTAÇÃO"))
        assertEquals(BeneficioVoucher.VA, BeneficioCajuMapper.mapear("vale-alimentação"))
        assertEquals(BeneficioVoucher.VT, BeneficioCajuMapper.mapear("VT"))
        assertEquals(BeneficioVoucher.VT, BeneficioCajuMapper.mapear("MOBILIDADE"))
        assertEquals(BeneficioVoucher.VT, BeneficioCajuMapper.mapear("Auxílio Transporte"))
    }

    @Test
    fun `retorna nulo para saldo livre desconhecido ou vazio`() {
        assertNull(BeneficioCajuMapper.mapear("SALDO LIVRE"))
        assertNull(BeneficioCajuMapper.mapear("CULTURA"))
        assertNull(BeneficioCajuMapper.mapear("  "))
        assertNull(BeneficioCajuMapper.mapear(null))
    }
}
