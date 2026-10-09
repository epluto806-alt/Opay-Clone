package com.opay.clone

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import org.json.JSONObject

class ResidentialAddressActivity : AppCompatActivity() {

    private lateinit var regions: JSONObject
    private var selectedState: String? = null
    private var selectedLga: String? = null
    private var selectedArea: String? = null

    private lateinit var tvState: TextView
    private lateinit var tvLga: TextView
    private lateinit var tvArea: TextView
    private lateinit var etAddress: EditText
    private lateinit var btnNext: AppCompatButton
    private lateinit var rowLga: LinearLayout
    private lateinit var rowArea: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read current phone (returns "" if none)
        val phone = AccountStore.getCurrentPhone(this)
        val pwSet = phone.isNotEmpty() && !(AccountStore.getAccount(this, phone)?.password ?: "").isEmpty()
        if (phone.isNotEmpty() && AccountStore.hasAddress(this, phone) && pwSet) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else if (phone.isNotEmpty() && AccountStore.hasAddress(this, phone)) {
            // Address done but password missing — resume from password
            startActivity(Intent(this, CreatePasswordActivity::class.java))
            finish()
            finishAffinity()
            return
        }

        setContentView(R.layout.activity_residential_address)

        regions = JSONObject(
            assets.open("nigeria_regions.json").bufferedReader().use { it.readText() }
        )

        tvState   = findViewById(R.id.tvState)
        tvLga     = findViewById(R.id.tvLga)
        tvArea    = findViewById(R.id.tvArea)
        etAddress = findViewById(R.id.etAddress)
        btnNext   = findViewById(R.id.btnNext)
        rowLga    = findViewById(R.id.rowLga)
        rowArea   = findViewById(R.id.rowArea)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setRowEnabled(rowLga, false)
        setRowEnabled(rowArea, false)

        findViewById<LinearLayout>(R.id.rowState).setOnClickListener {
            val states = regions.keys().asSequence().toList().sorted()
            showPicker("Select State", states) { picked ->
                selectedState = picked
                selectedLga = null
                selectedArea = null
                tvState.text = picked
                tvState.setTextColor(Color.parseColor("#1A1A1A"))
                tvLga.text = "Select LGA"
                tvLga.setTextColor(Color.parseColor("#8E8E8E"))
                tvArea.text = "Select area"
                tvArea.setTextColor(Color.parseColor("#8E8E8E"))
                setRowEnabled(rowLga, true)
                setRowEnabled(rowArea, false)
                updateNextState()
            }
        }

        rowLga.setOnClickListener {
            val st = selectedState ?: return@setOnClickListener
            val lgas = regions.getJSONObject(st).keys().asSequence().toList().sorted()
            showPicker("Select LGA", lgas) { picked ->
                selectedLga = picked
                selectedArea = null
                tvLga.text = picked
                tvLga.setTextColor(Color.parseColor("#1A1A1A"))
                tvArea.text = "Select area"
                tvArea.setTextColor(Color.parseColor("#8E8E8E"))
                setRowEnabled(rowArea, true)
                updateNextState()
            }
        }

        rowArea.setOnClickListener {
            val st = selectedState ?: return@setOnClickListener
            val lg = selectedLga ?: return@setOnClickListener
            val areasJson = regions.getJSONObject(st).getJSONArray(lg)
            val areas = (0 until areasJson.length()).map { areasJson.getString(it) }.sorted()
            showPicker("Select Area", areas) { picked ->
                selectedArea = picked
                tvArea.text = picked
                tvArea.setTextColor(Color.parseColor("#1A1A1A"))
                updateNextState()
            }
        }

        etAddress.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { updateNextState() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnNext.setOnClickListener {
            val street = etAddress.text.toString().trim()
            if (selectedState.isNullOrEmpty() || selectedLga.isNullOrEmpty() ||
                selectedArea.isNullOrEmpty() || street.isEmpty()) {
                return@setOnClickListener
            }
            val phoneNow = AccountStore.getCurrentPhone(this)
            if (phoneNow.isEmpty()) return@setOnClickListener
            AccountStore.saveAddress(
                this, phoneNow,
                selectedState!!, selectedLga!!, selectedArea!!, street
            )
            showSavedDialog()
        }

        updateNextState()
    }

    private fun setRowEnabled(row: LinearLayout, enabled: Boolean) {
        row.isEnabled = enabled
        row.alpha = if (enabled) 1f else 0.55f
        row.isClickable = enabled
    }

    private fun updateNextState() {
        val ready = !selectedState.isNullOrEmpty() &&
                !selectedLga.isNullOrEmpty() &&
                !selectedArea.isNullOrEmpty() &&
                etAddress.text.toString().trim().isNotEmpty()
        btnNext.isEnabled = ready
        btnNext.alpha = if (ready) 1f else 0.5f
    }

    private fun showPicker(title: String, items: List<String>, onPick: (String) -> Unit) {
        val arr = items.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(arr) { _, which -> onPick(arr[which]) }
            .show()
    }

    private fun showSavedDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_address_saved)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<AppCompatButton>(R.id.btnContinue).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, CreatePasswordActivity::class.java))
            finishAffinity()
        }
        dialog.show()
    }
}
