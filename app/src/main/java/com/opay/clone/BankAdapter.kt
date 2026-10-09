package com.opay.clone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BankAdapter(
    private var banks: List<Bank>,
    private val onClick: (Bank) -> Unit
) : RecyclerView.Adapter<BankAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivLogo: ImageView = v.findViewById(R.id.ivLogo)
        val tvInitials: TextView = v.findViewById(R.id.tvInitials)
        val tvBankName: TextView = v.findViewById(R.id.tvBankName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bank = banks[position]
        holder.tvBankName.text = bank.name

        val logoRes = bank.logoRes
        if (logoRes != null) {
            holder.ivLogo.visibility = View.VISIBLE
            holder.tvInitials.visibility = View.GONE
            holder.ivLogo.setImageResource(logoRes)
        } else {
            holder.ivLogo.visibility = View.GONE
            holder.tvInitials.visibility = View.VISIBLE
            holder.tvInitials.text = initials(bank.name)
        }

        holder.itemView.setOnClickListener { onClick(bank) }
    }

    override fun getItemCount(): Int = banks.size

    fun filter(query: String) {
        banks = if (query.isBlank()) {
            BankList.all
        } else {
            BankList.all.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    private fun initials(name: String): String {
        val words = name.split(" ").filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> (words[0].first().toString() + words[1].first().toString()).uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "BK"
        }
    }
}
