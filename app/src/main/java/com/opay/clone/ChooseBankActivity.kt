package com.opay.clone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChooseBankActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BANK_NAME = "bank_name"
        const val EXTRA_BANK_CODE = "bank_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_bank)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvBanks)
        rv.layoutManager = LinearLayoutManager(this)

        val adapter = BankAdapter(BankList.all) { bank ->
            val data = Intent().apply {
                putExtra(EXTRA_BANK_NAME, bank.name)
                putExtra(EXTRA_BANK_CODE, bank.code)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        rv.adapter = adapter

        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
