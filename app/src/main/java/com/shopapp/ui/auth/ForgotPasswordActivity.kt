package com.shopapp.ui.auth

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSendReset.setOnClickListener { sendReset() }
    }

    private fun sendReset() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Please enter your email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email address"
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            // ✅ Firebase sends the reset email automatically — no SMTP needed
            val result = Repository.sendPasswordResetEmail(email)

            setLoading(false)

            if (result.isSuccess) {
                // Show success layout
                binding.layoutForm.visibility    = View.GONE
                binding.layoutSuccess.visibility = View.VISIBLE
                binding.tvSentTo.text            = "Sent to: $email"
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Failed to send reset email: $msg",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text      = if (loading) "Sending..." else "Send Reset Email"
        binding.etEmail.isEnabled      = !loading
    }
}