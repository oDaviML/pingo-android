package com.pingo.sync

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.roundToLong

internal fun Long.toApiReais(): Double = this / 100.0

internal fun Double.toCentavos(): Long = (this * 100).roundToLong()

internal fun Instant.toApiValue(): String = toString()

internal fun String.toInstantValue(): Instant = OffsetDateTime.parse(this).toInstant()

internal fun LocalDate.toApiValue(): String = toString()
