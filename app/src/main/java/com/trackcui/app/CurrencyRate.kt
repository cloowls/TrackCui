package com.trackcui.app

data class CurrencyRate(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateInIdr: Double
)