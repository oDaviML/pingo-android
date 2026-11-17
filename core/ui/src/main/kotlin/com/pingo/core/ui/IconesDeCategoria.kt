package com.pingo.core.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.pingo.core.designsystem.icon.PingoIcons

private val iconesPorNome = mapOf(
    "alimentação" to PingoIcons.Utensils,
    "mercado" to PingoIcons.ShoppingCart,
    "transporte" to PingoIcons.CarFront,
    "moradia" to PingoIcons.Home,
    "contas da casa" to PingoIcons.HousePlug,
    "saúde" to PingoIcons.HeartPulse,
    "lazer" to PingoIcons.Gamepad,
    "compras" to PingoIcons.ShoppingBag,
    "assinaturas" to PingoIcons.Repeat,
    "educação" to PingoIcons.GraduationCap,
    "salário" to PingoIcons.Banknote,
    "benefício" to PingoIcons.Gift,
    "reembolso" to PingoIcons.RotateCcw,
    "outros" to PingoIcons.Tag,
)

fun iconeDaCategoria(nome: String?): ImageVector = iconesPorNome[nome?.lowercase()?.trim()] ?: PingoIcons.Tag
