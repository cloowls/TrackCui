package com.trackcui.app

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

fun formatRupiah(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,###", symbols)
    val absAmount = abs(amount)
    val formatted = formatter.format(absAmount.toLong())
    return if (amount < 0) "- Rp. $formatted" else "Rp. $formatted"
}