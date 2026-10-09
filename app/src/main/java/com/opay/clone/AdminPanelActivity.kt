package com.opay.clone

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class AdminPanelActivity : AppCompatActivity() {

    private val banks = arrayOf(
        "Access Bank", "Ecobank Nigeria Plc", "FCMB MFB", "Fairmoney MFB",
        "First Bank of Nigeria Plc", "Fidelity Bank", "Flutterwave MFB",
        "Guaranty Trust Bank", "Keystone Bank", "Kuda MFB", "MONIE POINT",
        "Opay", "PAYSTACK-TITAN", "Paga", "Palmpay", "Polaris Bank Plc",
        "Providus Bank Plc", "SmartCash Payment Service Bank",
        "Stanbic-ibtc Bank Plc", "Sterling Bank", "Union Bank Of Nigeria",
        "United Bank For Africa", "Unity Bank Plc", "Wema Bank Plc", "Zenith Bank"
    )

    private var selectedBank: String? = null
    private var pickedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            findViewById<TextView>(R.id.tvAdminChosenImage).text = uri.lastPathSegment ?: "image"
            findViewById<TextView>(R.id.tvAdminChosenImage).setTextColor(0xFF0A0A0A.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        findViewById<ImageView>(R.id.btnAdminBack).setOnClickListener { finish() }

        // Prefill
        findViewById<EditText>(R.id.etAdminFullName).setText(AppPrefs.getName(this))
        findViewById<EditText>(R.id.etAdminUsername).setText(AppPrefs.getName(this))

        // EDIT DETAILS
        findViewById<TextView>(R.id.btnAdminEditDetails).setOnClickListener {
            val fullName = findViewById<EditText>(R.id.etAdminFullName).text.toString().trim()
            val username = findViewById<EditText>(R.id.etAdminUsername).text.toString().trim()
            val acct = findViewById<EditText>(R.id.etAdminAccount).text.toString().trim()
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Enter a full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppPrefs.setName(this, fullName)
            AppPrefs.setBalance(this, AppPrefs.getBalance(this)) // no-op
            // Also update the multi-account store so Home/Me pick up the new name
            val phone = AccountStore.getCurrentPhone(this)
            if (phone.isNotEmpty()) {
                AccountStore.updateName(this, phone, fullName)
            }
            if (username.isNotEmpty()) {
                val p = getSharedPreferences("opay_clone_prefs", MODE_PRIVATE)
                p.edit().putString("username", username).apply()
            }
            if (acct.isNotEmpty()) {
                val p = getSharedPreferences("opay_clone_prefs", MODE_PRIVATE)
                p.edit().putString("account_number", acct).apply()
            }
            Toast.makeText(this, "Profile Edited", Toast.LENGTH_SHORT).show()
        }

        // ADD PROFILE IMAGE  picker
        findViewById<TextView>(R.id.btnAdminChooseImage).setOnClickListener {
            pickImage.launch("image/*")
        }

        // ADD PROFILE IMAGE  save to prefs
        findViewById<TextView>(R.id.btnAdminAddProfile).setOnClickListener {
            val uri = pickedImageUri
            if (uri == null) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val p = getSharedPreferences("opay_clone_prefs", MODE_PRIVATE)
            p.edit().putString("profile_image_uri", uri.toString()).apply()
            Toast.makeText(this, "Profile image saved", Toast.LENGTH_SHORT).show()
        }

    }
}

