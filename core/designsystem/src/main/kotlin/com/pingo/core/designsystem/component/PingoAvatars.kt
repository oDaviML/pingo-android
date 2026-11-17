package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoTheme

enum class PingoInstitution(val label: String, val monogram: String, val containerColor: Color?) {
    Nubank(label = "Nubank", monogram = "N", containerColor = Color(0xFF820AD1)),
    Inter(label = "Inter", monogram = "i", containerColor = Color(0xFFFF7A00)),
    Itau(label = "Itaú", monogram = "it", containerColor = Color(0xFFEC7000)),
    MercadoPago(label = "Mercado Pago", monogram = "mp", containerColor = Color(0xFF009EE3)),
    Caju(label = "Caju", monogram = "c", containerColor = Color(0xFFFF5A5F)),
    Outra(label = "Outra", monogram = "?", containerColor = null),
    ;

    companion object {
        fun fromKey(key: String?): PingoInstitution = when (
            key
                ?.trim()
                ?.lowercase()
                ?.replace("ú", "u")
                ?.replace(" ", "_")
        ) {
            "nubank" -> Nubank
            "inter" -> Inter
            "itau" -> Itau
            "mercado_pago", "mercadopago" -> MercadoPago
            "caju" -> Caju
            else -> Outra
        }
    }
}

enum class PingoCategoryTone {
    Default,
    Income,
    Transfer,
}

@Composable
fun InstitutionAvatar(
    institution: PingoInstitution,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = institution.label,
) {
    val colors = PingoTheme.colors
    val container = institution.containerColor ?: colors.surfaceVariant

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color = container, shape = CircleShape)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (institution.containerColor == null) {
            PingoIcon(
                imageVector = PingoIcons.Landmark,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = colors.onPrimaryContainer,
            )
        } else {
            val fontSize = (size.value * 0.35f).sp
            PingoText(
                text = institution.monogram,
                style = PingoTheme.typography.label.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                ),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun CategoryAvatar(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tone: PingoCategoryTone = PingoCategoryTone.Default,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = PingoRadius.md,
) {
    val colors = PingoTheme.colors
    val (container, tint) = when (tone) {
        PingoCategoryTone.Default -> colors.primaryContainer to colors.onPrimaryContainer
        PingoCategoryTone.Income -> colors.incomeContainer to colors.income
        PingoCategoryTone.Transfer -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = container, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}
