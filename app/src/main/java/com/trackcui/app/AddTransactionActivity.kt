package com.trackcui.app

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.trackcui.app.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var repository: TransactionRepository
    private var isUpdatingAmount = false
    private var currentBalance: Double = 0.0

    private var isEditMode = false
    private var editTransactionId = 0
    private var editTransactionDate = 0L

    private var selectedCurrencyCode = "IDR"
    private var selectedExchangeRate = 1.0

    private val currencies = listOf(
        Triple("IDR", "🇮🇩 IDR - Rupiah Indonesia", "Rp. "),
        Triple("USD", "🇺🇸 USD - Dolar Amerika Serikat", "$ "),
        Triple("EUR", "🇪🇺 EUR - Euro", "€ "),
        Triple("SGD", "🇸🇬 SGD - Dolar Singapura", "S$ "),
        Triple("MYR", "🇲🇾 MYR - Ringgit Malaysia", "RM "),
        Triple("JPY", "🇯🇵 JPY - Yen Jepang", "¥ ")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).transactionDao()
        repository = TransactionRepository(dao)

        repository.balance.observe(this) { balance ->
            currentBalance = balance ?: 0.0
        }

        setupCurrencyDropdown()

        isEditMode = intent.getBooleanExtra("EXTRA_IS_EDIT", false)

        if (isEditMode) {
            editTransactionId = intent.getIntExtra("EXTRA_TRANSACTION_ID", 0)
            editTransactionDate = intent.getLongExtra("EXTRA_TRANSACTION_DATE", System.currentTimeMillis())
            val type = intent.getStringExtra("EXTRA_TRANSACTION_TYPE") ?: "INCOME"
            val amount = intent.getDoubleExtra("EXTRA_TRANSACTION_AMOUNT", 0.0)
            val category = intent.getStringExtra("EXTRA_TRANSACTION_CATEGORY") ?: ""
            val note = intent.getStringExtra("EXTRA_TRANSACTION_NOTE") ?: ""
            val code = intent.getStringExtra("EXTRA_TRANSACTION_CURRENCY_CODE") ?: "IDR"
            val origAmount = intent.getDoubleExtra("EXTRA_TRANSACTION_ORIGINAL_AMOUNT", 0.0)
            val rate = intent.getDoubleExtra("EXTRA_TRANSACTION_EXCHANGE_RATE", 1.0)

            binding.tvTitle.text = "Edit Transaksi"
            binding.tvSubtitle.text = "Ubah rincian transaksi kamu"
            binding.btnSave.text = "Simpan Perubahan"

            if (type == "INCOME") {
                binding.rbIncome.isChecked = true
            } else {
                binding.rbExpense.isChecked = true
            }

            selectedCurrencyCode = code
            selectedExchangeRate = if (rate > 0) rate else 1.0

            val currIndex = currencies.indexOfFirst { it.first == code }
            if (currIndex >= 0) {
                binding.actCurrency.setText(currencies[currIndex].second, false)
                binding.tilAmount.prefixText = currencies[currIndex].third
            }

            val displayAmount = if (code != "IDR" && origAmount > 0) origAmount else amount
            if (displayAmount > 0) {
                if (code == "IDR") {
                    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
                        groupingSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###", symbols)
                    binding.etAmount.setText(formatter.format(displayAmount.toLong()))
                } else if (code == "JPY") {
                    val symbols = DecimalFormatSymbols(Locale.US).apply {
                        groupingSeparator = ','
                    }
                    val formatter = DecimalFormat("#,###", symbols)
                    binding.etAmount.setText(formatter.format(displayAmount.toLong()))
                } else {
                    binding.etAmount.setText(String.format(Locale.US, "%.2f", displayAmount))
                }
            }

            binding.etCategory.setText(category)
            binding.etNote.setText(note)
            updateCurrencyConversionPreview()
        }

        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingAmount || s == null) return
                isUpdatingAmount = true

                val str = s.toString()

                if (selectedCurrencyCode == "IDR") {
                    val cleanString = str.replace(".", "").replace(",", "").trim()
                    if (cleanString.isEmpty()) {
                        if (str.isNotEmpty()) binding.etAmount.setText("")
                    } else {
                        val doubleVal = cleanString.toDoubleOrNull()
                        if (doubleVal != null) {
                            val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
                                groupingSeparator = '.'
                            }
                            val formatter = DecimalFormat("#,###", symbols)
                            val formatted = formatter.format(doubleVal.toLong())
                            if (formatted != str) {
                                binding.etAmount.setText(formatted)
                                binding.etAmount.setSelection(formatted.length)
                            }
                        }
                    }
                } else if (selectedCurrencyCode == "JPY") {
                    val cleanString = str.replace(",", "").replace(".", "").trim()
                    if (cleanString.isEmpty()) {
                        if (str.isNotEmpty()) binding.etAmount.setText("")
                    } else {
                        val doubleVal = cleanString.toDoubleOrNull()
                        if (doubleVal != null) {
                            val symbols = DecimalFormatSymbols(Locale.US).apply {
                                groupingSeparator = ','
                            }
                            val formatter = DecimalFormat("#,###", symbols)
                            val formatted = formatter.format(doubleVal.toLong())
                            if (formatted != str) {
                                binding.etAmount.setText(formatted)
                                binding.etAmount.setSelection(formatted.length)
                            }
                        }
                    }
                } else {
                    // Foreign Decimals (USD, EUR, SGD, MYR)
                }

                isUpdatingAmount = false
                updateCurrencyConversionPreview()
            }
        })

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun setupCurrencyDropdown() {
        val currencyNames = currencies.map { it.second }
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, currencyNames)
        binding.actCurrency.setAdapter(adapter)

        // Default IDR
        binding.actCurrency.setText(currencies[0].second, false)
        binding.tilAmount.prefixText = currencies[0].third

        binding.actCurrency.setOnItemClickListener { _, _, position, _ ->
            val selected = currencies[position]
            selectedCurrencyCode = selected.first
            binding.tilAmount.prefixText = selected.third

            if (selectedCurrencyCode == "IDR") {
                selectedExchangeRate = 1.0
                binding.layoutCurrencyInfo.visibility = View.GONE
                updateCurrencyConversionPreview()
            } else {
                fetchExchangeRate(selectedCurrencyCode)
            }
        }
    }

    private fun fetchExchangeRate(code: String) {
        binding.layoutCurrencyInfo.visibility = View.VISIBLE
        binding.tvExchangeRateInfo.text = "Mengambil kurs $code dari Frankfurter API..."
        binding.tvConvertedPreview.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlStr = "https://api.frankfurter.app/latest"
                val jsonText = URL(urlStr).readText()
                val jsonObject = JSONObject(jsonText)

                val ratesObj = jsonObject.getJSONObject("rates")
                val idrRateForEur = ratesObj.optDouble("IDR", 17000.0)

                val rateInIdr = if (code == "EUR") {
                    idrRateForEur
                } else {
                    val ratePerEur = ratesObj.optDouble(code, 0.0)
                    if (ratePerEur > 0) idrRateForEur / ratePerEur else 1.0
                }

                withContext(Dispatchers.Main) {
                    selectedExchangeRate = rateInIdr
                    binding.tvExchangeRateInfo.text = "Kurs Terdaftar: 1 $code = ${formatRupiah(rateInIdr)}"
                    updateCurrencyConversionPreview()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvExchangeRateInfo.text = "Gagal mengambil kurs online, menggunakan perkiraan kurs."
                    updateCurrencyConversionPreview()
                }
            }
        }
    }

    private fun updateCurrencyConversionPreview() {
        if (selectedCurrencyCode == "IDR") {
            binding.layoutCurrencyInfo.visibility = View.GONE
            return
        }

        binding.layoutCurrencyInfo.visibility = View.VISIBLE
        val rawText = binding.etAmount.text.toString().trim()

        val originalAmount: Double = if (selectedCurrencyCode == "JPY") {
            rawText.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
        } else {
            rawText.replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        val convertedIdr = originalAmount * selectedExchangeRate

        binding.tvConvertedPreview.text = "Setara dengan: ${formatRupiah(convertedIdr)}"
    }

    private fun saveTransaction() {
        val rawText = binding.etAmount.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val note = binding.etNote.text.toString().trim()

        if (rawText.isBlank()) {
            Toast.makeText(this, "Nominal wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isBlank()) {
            Toast.makeText(this, "Kategori wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val inputAmount: Double? = if (selectedCurrencyCode == "IDR") {
            rawText.replace(".", "").replace(",", "").toDoubleOrNull()
        } else if (selectedCurrencyCode == "JPY") {
            rawText.replace(",", "").replace(".", "").toDoubleOrNull()
        } else {
            rawText.replace(",", ".").toDoubleOrNull()
        }

        if (inputAmount == null || inputAmount <= 0) {
            Toast.makeText(this, "Nominal tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (binding.rbIncome.isChecked) "INCOME" else "EXPENSE"

        val originalAmount: Double
        val finalAmountInIdr: Double

        if (selectedCurrencyCode == "IDR") {
            originalAmount = inputAmount
            finalAmountInIdr = inputAmount
            selectedExchangeRate = 1.0
        } else {
            originalAmount = inputAmount
            finalAmountInIdr = inputAmount * selectedExchangeRate
        }

        if (type == "EXPENSE" && finalAmountInIdr > currentBalance && !isEditMode) {
            Toast.makeText(
                this,
                "Peringatan: Pengeluaran melebihi saldo kamu saat ini (${formatRupiah(currentBalance)})",
                Toast.LENGTH_LONG
            ).show()
        }

        if (isEditMode) {
            val updatedTransaction = Transaction(
                id = editTransactionId,
                type = type,
                amount = finalAmountInIdr,
                category = category,
                note = note,
                date = editTransactionDate,
                currencyCode = selectedCurrencyCode,
                originalAmount = originalAmount,
                exchangeRate = selectedExchangeRate
            )
            lifecycleScope.launch {
                repository.update(updatedTransaction)
                Toast.makeText(this@AddTransactionActivity, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val newTransaction = Transaction(
                type = type,
                amount = finalAmountInIdr,
                category = category,
                note = note,
                date = System.currentTimeMillis(),
                currencyCode = selectedCurrencyCode,
                originalAmount = originalAmount,
                exchangeRate = selectedExchangeRate
            )
            lifecycleScope.launch {
                repository.insert(newTransaction)
                finish()
            }
        }
    }
}