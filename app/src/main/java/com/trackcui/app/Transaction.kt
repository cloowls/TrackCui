package com.trackcui.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,                   // "INCOME" atau "EXPENSE"
    val amount: Double,                 // nominal setara IDR
    val category: String,               // kategori, misal "Makanan", "Gaji"
    val note: String,                   // catatan tambahan
    val date: Long,                     // tanggal transaksi (timestamp)
    val currencyCode: String = "IDR",   // "IDR", "USD", "EUR", "SGD", "MYR", "JPY", dll
    val originalAmount: Double = 0.0,   // nominal dalam valuta asing (misal 10.0)
    val exchangeRate: Double = 1.0      // nilai kurs dalam IDR saat transaksi dicatat (misal 16250.0)
)