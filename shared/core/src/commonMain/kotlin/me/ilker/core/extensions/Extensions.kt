package me.ilker.core.extensions

import kotlin.math.round

fun Double.round(decimals: Int): Double {
    var multiplier = 1.toDouble()
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun Double.hasDecimals() = this % 1.toDouble() != 0.toDouble()

fun Double.toHumanReadableValue() = if (this.hasDecimals()) {
    this.toString()
} else {
    this.toInt().toString()
}
