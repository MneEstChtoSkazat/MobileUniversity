package com.example.wallet.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private fun format(pattern: String, value: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' }
    return DecimalFormat(pattern, symbols).format(value)
}

/** 12 345.6 -> «12 345.6 ₽» */
fun formatRub(value: Double): String = format("#,##0.##", value) + " ₽"

/** Количество актива: до 8 знаков после точки без лишних нулей. */
fun formatQty(value: Double): String = format("#,##0.########", value)

/** Курс за единицу: у дешёвых монет нужны знаки после запятой. */
fun formatRate(value: Double): String = format("#,##0.########", value) + " ₽"
