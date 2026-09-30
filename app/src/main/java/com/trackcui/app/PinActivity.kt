package com.trackcui.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.trackcui.app.databinding.ActivityPinBinding

class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private lateinit var prefs: SharedPreferences

    private var isChangePin = false
    private var isVerifyingOldPinForChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("trackcui_prefs", MODE_PRIVATE)
        val savedPin = prefs.getString("user_pin", null)

        isChangePin = intent.getBooleanExtra("IS_CHANGE_PIN", false)

        if (isChangePin) {
            isVerifyingOldPinForChange = true
            binding.tvPinTitle.text = "Masukkan PIN Lama"
            binding.btnConfirmPin.text = "Verifikasi PIN"
            binding.btnBiometric.visibility = View.GONE
        } else if (savedPin == null) {
            binding.tvPinTitle.text = "Buat PIN Baru"
            binding.btnConfirmPin.text = "Simpan PIN"
            binding.btnBiometric.visibility = View.GONE
        } else {
            binding.tvPinTitle.text = "Masukkan PIN"
            binding.btnConfirmPin.text = "Lanjut"

            if (isBiometricAvailable()) {
                binding.btnBiometric.visibility = View.VISIBLE
                binding.btnBiometric.setOnClickListener {
                    showBiometricPrompt()
                }
                // Tampilkan prompt sidik jari otomatis saat aplikasi dibuka
                showBiometricPrompt()
            } else {
                binding.btnBiometric.visibility = View.GONE
            }
        }

        binding.btnConfirmPin.setOnClickListener {
            val inputPin = binding.etPin.text.toString().trim()

            if (inputPin.length < 4) {
                binding.tvPinError.text = "PIN minimal 4 digit"
                binding.tvPinError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (isChangePin) {
                if (isVerifyingOldPinForChange) {
                    if (inputPin == savedPin) {
                        isVerifyingOldPinForChange = false
                        binding.tvPinTitle.text = "Masukkan PIN Baru"
                        binding.btnConfirmPin.text = "Simpan PIN Baru"
                        binding.tvPinError.visibility = View.INVISIBLE
                        binding.etPin.setText("")
                    } else {
                        binding.tvPinError.text = "PIN lama salah"
                        binding.tvPinError.visibility = View.VISIBLE
                        binding.etPin.setText("")
                    }
                } else {
                    prefs.edit().putString("user_pin", inputPin).apply()
                    Toast.makeText(this, "PIN berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else if (savedPin == null) {
                prefs.edit().putString("user_pin", inputPin).apply()
                goToMain()
            } else {
                if (inputPin == savedPin) {
                    goToMain()
                } else {
                    binding.tvPinError.text = "PIN salah, coba lagi"
                    binding.tvPinError.visibility = View.VISIBLE
                    binding.etPin.setText("")
                }
            }
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Autentikasi Berhasil!", Toast.LENGTH_SHORT).show()
                    goToMain()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Sidik jari tidak cocok", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentikasi Sidik Jari")
            .setSubtitle("Gunakan sidik jari untuk masuk ke TrackCui")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}