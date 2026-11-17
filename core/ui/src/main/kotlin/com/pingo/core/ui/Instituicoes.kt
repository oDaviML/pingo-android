package com.pingo.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.InstitutionAvatar
import com.pingo.core.designsystem.component.PingoInstitution
import com.pingo.core.model.InstituicaoFinanceira

@Composable
fun AvatarDaInstituicao(
    instituicao: InstituicaoFinanceira?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    InstitutionAvatar(
        institution = instituicao.paraDesignSystem(),
        modifier = modifier,
        size = size,
        contentDescription = contentDescription,
    )
}

private fun InstituicaoFinanceira?.paraDesignSystem(): PingoInstitution = when (this) {
    InstituicaoFinanceira.NUBANK -> PingoInstitution.Nubank
    InstituicaoFinanceira.INTER -> PingoInstitution.Inter
    InstituicaoFinanceira.ITAU -> PingoInstitution.Itau
    InstituicaoFinanceira.MERCADO_PAGO -> PingoInstitution.MercadoPago
    InstituicaoFinanceira.CAJU -> PingoInstitution.Caju
    InstituicaoFinanceira.OUTRA, null -> PingoInstitution.Outra
}
