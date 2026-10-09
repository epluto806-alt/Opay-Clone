package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class FingerprintActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)

        val name = AppPrefs.getName(this).trim()
        val phone = AppPrefs.getPhone(this).trim()

        val first = Safe.splitName(name).first.takeIf { it.isNotEmpty() } ?: "User"
        val masked = maskPhone(phone)
        findViewById<TextView>(R.id.tvUserLabel).text = "$first($masked)"

        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val lockMode = intent.getBooleanExtra("lock_mode", false)
                    if (lockMode) {
                        // Coming from background lock — just close and let previous screen resume
                        finish()
                    } else {
                        val phone = AppPrefs.getPhone(this@FingerprintActivity).trim()
                        if (phone.isNotEmpty() && !AccountStore.isDeviceAllowed(this@FingerprintActivity, phone)) {
                            android.app.AlertDialog.Builder(this@FingerprintActivity)
                                .setTitle("Account Locked")
                                .setMessage("This account is bound to another device. Please sign in from the original device or create a new account.")
                                .setPositiveButton("OK") { _, _ ->
                                    startActivity(Intent(this@FingerprintActivity, LoginActivity::class.java))
                                    finishAffinity()
                                }
                                .setCancelable(false)
                                .show()
                        } else {
                            startActivity(Intent(this@FingerprintActivity, HomeActivity::class.java))
                            finishAffinity()
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@FingerprintActivity,
                        "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@FingerprintActivity,
                        "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        val canAuth = BiometricManager.from(this)
            .canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

        promptInfo = if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify it's you")
                .setSubtitle("Log in with fingerprint")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify it's you")
                .setSubtitle("Confirm your lockscreen")
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        }

        findViewById<MaterialButton>(R.id.btnVerifyFingerprint).setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        findViewById<TextView>(R.id.tvSwitchAccount)?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        findViewById<TextView>(R.id.tvLoginPassword).setOnClickListener {
            val lockMode = intent.getBooleanExtra("lock_mode", false)
            val i = Intent(this, PasswordLoginActivity::class.java)
            i.putExtra("lock_mode", lockMode)
            startActivity(i)
        }

        // Device-binding gate BEFORE biometric prompt
        val _phone = AppPrefs.getPhone(this).trim()
        if (_phone.isNotEmpty() && !AccountStore.isDeviceAllowed(this, _phone)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Account Locked")
                .setMessage("This account is bound to another device. Please sign in from the original device or create a new account.")
                .setPositiveButton("OK") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setCancelable(false)
                .show()
            return
        }

        // Auto-trigger
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun maskPhone(p: String): String {
        val clean = p.replace("+234", "").replace(" ", "").trim()
        if (clean.length < 10) return clean
        val first3 = clean.take(3)
        val last3 = clean.takeLast(3)
        return "$first3****$last3"
    }
}
