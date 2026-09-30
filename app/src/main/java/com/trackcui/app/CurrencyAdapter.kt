package com.trackcui.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trackcui.app.databinding.ItemCurrencyBinding
import java.util.Locale

class CurrencyAdapter(
    private var items: List<CurrencyRate>,
    private var inputAmount: Double = 1.0,
    private var isIdrToForeign: Boolean = false
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    inner class CurrencyViewHolder(val binding: ItemCurrencyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvFlag.text = item.flag
        holder.binding.tvCodeName.text = "${item.code} - ${item.name}"
        holder.binding.tvUnitRate.text = "1 ${item.code} = ${formatRupiah(item.rateInIdr)}"

        if (isIdrToForeign) {
            // Mode: IDR -> Foreign Currency
            val foreignValue = if (item.rateInIdr > 0) inputAmount / item.rateInIdr else 0.0
            val formattedForeign = if (item.code == "JPY" || item.code == "KRW") {
                String.format(Locale.US, "%s %,d", item.symbol, foreignValue.toLong())
            } else {
                String.format(Locale.US, "%s %,.2f", item.symbol, foreignValue)
            }
            holder.binding.tvConvertedValue.text = formattedForeign
        } else {
            // Mode: Foreign Currency -> IDR
            val totalIdr = item.rateInIdr * inputAmount
            holder.binding.tvConvertedValue.text = formatRupiah(totalIdr)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CurrencyRate>, newInputAmount: Double, newIsIdrToForeign: Boolean) {
        items = newItems
        inputAmount = newInputAmount
        isIdrToForeign = newIsIdrToForeign
        notifyDataSetChanged()
    }
}