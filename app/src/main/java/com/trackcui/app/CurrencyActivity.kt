package com.trackcui.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.trackcui.app.databinding.ActivityCurrencyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class CurrencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencyBinding
    private lateinit var adapter: CurrencyAdapter
    private var currencyList = ArrayList<CurrencyRate>()

    private var currentInputAmount = 100.0
    private var isIdrToForeignMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CurrencyAdapter(emptyList(), 100.0, false)
        binding.rvCurrencies.layoutManager = LinearLayoutManager(this)
        binding.rvCurrencies.adapter = adapter

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            fetchRatesFromFrankfurter()
        }

        binding.toggleGroupDirection.check(R.id.btnForeignToIdr)

        binding.toggleGroupDirection.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnIdrToForeign) {
                    isIdrToForeignMode = true
                    binding.tilAmount.hint = "Nominal Rupiah (IDR)"
                    binding.tilAmount.prefixText = "Rp. "
                    if (binding.etAmount.text.toString().trim() == "100" || binding.etAmount.text.toString().trim() == "1") {
                        binding.etAmount.setText("100000")
                    }
                } else {
                    isIdrToForeignMode = false
                    binding.tilAmount.hint = "Nominal Asing"
                    binding.tilAmount.prefixText = ""
                    if (binding.etAmount.text.toString().trim() == "100000") {
                        binding.etAmount.setText("100")
                    }
                }
                updateConversion()
            }
        }

        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().replace(".", "").replace(",", "").trim()
                currentInputAmount = input.toDoubleOrNull() ?: 1.0
                updateConversion()
            }
        })

        fetchRatesFromFrankfurter()
    }

    private fun updateConversion() {
        adapter.updateData(currencyList, currentInputAmount, isIdrToForeignMode)
    }

    private fun fetchRatesFromFrankfurter() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlStr = "https://api.frankfurter.app/latest"
                val jsonText = URL(urlStr).readText()
                val jsonObject = JSONObject(jsonText)

                val dateStr = jsonObject.optString("date", "")
                val ratesObj = jsonObject.getJSONObject("rates")
                val idrRateForEur = ratesObj.optDouble("IDR", 17000.0)

                val targets = listOf(
                    Triple("USD", "Dolar Amerika Serikat", Pair("$", "🇺🇸")),
                    Triple("EUR", "Euro", Pair("€", "🇪🇺")),
                    Triple("SGD", "Dolar Singapura", Pair("S$", "🇸🇬")),
                    Triple("MYR", "Ringgit Malaysia", Pair("RM", "🇲🇾")),
                    Triple("JPY", "Yen Jepang", Pair("¥", "🇯🇵"))
                )

                val newList = ArrayList<CurrencyRate>()

                for (target in targets) {
                    val code = target.first
                    val name = target.second
                    val symbol = target.third.first
                    val flag = target.third.second

                    val rateInIdr = if (code == "EUR") {
                        idrRateForEur
                    } else {
                        val ratePerEur = ratesObj.optDouble(code, 0.0)
                        if (ratePerEur > 0) idrRateForEur / ratePerEur else 0.0
                    }

                    if (rateInIdr > 0) {
                        newList.add(CurrencyRate(code, name, symbol, flag, rateInIdr))
                    }
                }

                withContext(Dispatchers.Main) {
                    currencyList = newList
                    updateConversion()
                    binding.progressBar.visibility = View.GONE
                    binding.tvSourceApi.text = "Sumber: Frankfurter API ($dateStr)"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@CurrencyActivity,
                        "Gagal mengambil data kurs. Periksa koneksi internet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}