package com.trackcui.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trackcui.app.databinding.ItemTransactionBinding
import java.util.Locale

class TransactionAdapter(
    private var items: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = items[position]

        holder.binding.tvCategory.text = transaction.category
        holder.binding.tvNote.text = if (transaction.note.isBlank()) "-" else transaction.note

        val amountText = formatRupiah(transaction.amount)
        if (transaction.type == "INCOME") {
            holder.binding.tvAmount.text = "+ $amountText"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#10B981"))
        } else {
            holder.binding.tvAmount.text = "- $amountText"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#EF4444"))
        }

        if (transaction.currencyCode != "IDR" && transaction.originalAmount > 0) {
            val symbol = getCurrencySymbol(transaction.currencyCode)
            val formattedOriginal = if (transaction.currencyCode == "JPY" || transaction.currencyCode == "KRW") {
                String.format(Locale.US, "%,d", transaction.originalAmount.toLong())
            } else {
                String.format(Locale.US, "%,.2f", transaction.originalAmount)
            }
            val formattedRate = formatRupiah(transaction.exchangeRate)
            holder.binding.tvOriginalCurrency.text = "($symbol $formattedOriginal • Kurs $formattedRate)"
            holder.binding.tvOriginalCurrency.visibility = View.VISIBLE
        } else {
            holder.binding.tvOriginalCurrency.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            onItemClick(transaction)
        }

        holder.binding.btnEdit.setOnClickListener {
            onEditClick(transaction)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    private fun getCurrencySymbol(code: String): String {
        return when (code) {
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
            else -> code
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Transaction>) {
        items = newList
        notifyDataSetChanged()
    }
}