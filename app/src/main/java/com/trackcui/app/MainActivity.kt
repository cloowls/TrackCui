package com.trackcui.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trackcui.app.databinding.ActivityMainBinding
import com.trackcui.app.databinding.DialogTransactionDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TransactionRepository
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("trackcui_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).transactionDao()
        repository = TransactionRepository(dao)

        adapter = TransactionAdapter(
            items = emptyList(),
            onItemClick = { transaction ->
                showTransactionDetailDialog(transaction)
            },
            onEditClick = { transactionToEdit ->
                openEditTransaction(transactionToEdit)
            },
            onDeleteClick = { transactionToDelete ->
                confirmDelete(transactionToDelete)
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        // Amati perubahan daftar transaksi secara otomatis
        repository.allTransactions.observe(this) { list ->
            adapter.updateList(list)
        }

        // Amati perubahan saldo secara otomatis
        repository.balance.observe(this) { balance ->
            val total = balance ?: 0.0
            binding.tvBalance.text = formatRupiah(total)
            if (total < 0) {
                binding.tvBalance.setTextColor(Color.parseColor("#FCA5A5")) // Warna merah minus
            } else {
                binding.tvBalance.setTextColor(Color.WHITE)
            }
        }

        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.btnCurrency.setOnClickListener {
            startActivity(Intent(this, CurrencyActivity::class.java))
        }

        binding.btnChangePin.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra("IS_CHANGE_PIN", true)
            }
            startActivity(intent)
        }

        binding.btnToggleTheme.setOnClickListener {
            val currentDarkMode = prefs.getBoolean("is_dark_mode", false)
            val newDarkMode = !currentDarkMode
            prefs.edit().putBoolean("is_dark_mode", newDarkMode).apply()

            if (newDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun showTransactionDetailDialog(transaction: Transaction) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogTransactionDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(transaction.date))

        val isIncome = transaction.type == "INCOME"
        val typeText = if (isIncome) "Pemasukan (Income)" else "Pengeluaran (Expense)"
        val amountFormatted = formatRupiah(transaction.amount)

        dialogBinding.tvDetailType.text = typeText
        dialogBinding.tvDetailCategory.text = transaction.category
        dialogBinding.tvDetailAmount.text = "${if (isIncome) "+ " else "- "}$amountFormatted"

        if (isIncome) {
            dialogBinding.tvDetailAmount.setTextColor(Color.parseColor("#10B981"))
        } else {
            dialogBinding.tvDetailAmount.setTextColor(Color.parseColor("#EF4444"))
        }

        if (transaction.currencyCode != "IDR" && transaction.originalAmount > 0) {
            val symbol = when (transaction.currencyCode) {
                "USD" -> "$"
                "EUR" -> "€"
                "SGD" -> "S$"
                "MYR" -> "RM"
                "JPY" -> "¥"
                "GBP" -> "£"
                "AUD" -> "A$"
                "CNY" -> "¥"
                "KRW" -> "₩"
                "SAR" -> "SR"
                else -> transaction.currencyCode
            }
            val origFormatted = String.format(Locale.US, "%,.2f", transaction.originalAmount)
            val rateFormatted = formatRupiah(transaction.exchangeRate)
            dialogBinding.tvDetailForeign.text = "Valas: $symbol $origFormatted (${transaction.currencyCode}) • Kurs: $rateFormatted"
            dialogBinding.tvDetailForeign.visibility = View.VISIBLE
        } else {
            dialogBinding.tvDetailForeign.visibility = View.GONE
        }

        dialogBinding.tvDetailNote.text = if (transaction.note.isBlank()) "-" else transaction.note
        dialogBinding.tvDetailDate.text = dateStr

        dialogBinding.btnDetailEdit.setOnClickListener {
            dialog.dismiss()
            openEditTransaction(transaction)
        }

        dialogBinding.btnDetailDelete.setOnClickListener {
            dialog.dismiss()
            confirmDelete(transaction)
        }

        dialog.show()
    }

    private fun openEditTransaction(transaction: Transaction) {
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            putExtra("EXTRA_IS_EDIT", true)
            putExtra("EXTRA_TRANSACTION_ID", transaction.id)
            putExtra("EXTRA_TRANSACTION_TYPE", transaction.type)
            putExtra("EXTRA_TRANSACTION_AMOUNT", transaction.amount)
            putExtra("EXTRA_TRANSACTION_CATEGORY", transaction.category)
            putExtra("EXTRA_TRANSACTION_NOTE", transaction.note)
            putExtra("EXTRA_TRANSACTION_DATE", transaction.date)
            putExtra("EXTRA_TRANSACTION_CURRENCY_CODE", transaction.currencyCode)
            putExtra("EXTRA_TRANSACTION_ORIGINAL_AMOUNT", transaction.originalAmount)
            putExtra("EXTRA_TRANSACTION_EXCHANGE_RATE", transaction.exchangeRate)
        }
        startActivity(intent)
    }

    private fun confirmDelete(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin ingin menghapus transaksi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    repository.delete(transaction)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}