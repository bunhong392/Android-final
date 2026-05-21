package com.shopapp.ui.auth

import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shopapp.databinding.ActivityRegisterBinding
import com.shopapp.data.repository.Repository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val name            = binding.etName.text.toString().trim()
            val email           = binding.etEmail.text.toString().trim()
            val phone           = binding.etPhone.text.toString().trim()
            val password        = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val result = Repository.registerUser(name, email, password, phone)
                setLoading(false)
                result.onSuccess {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created! Please sign in.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ── Go back to LoginActivity with email pre-filled ────────
                    // so the user sees their real profile data immediately on
                    // the login screen without having to type the email again.
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(LoginActivity.EXTRA_PREFILL_EMAIL, email)
                        putExtra(LoginActivity.EXTRA_PREFILL_NAME,  name)
                    }
                    startActivity(intent)
                    finish()

                }.onFailure {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration failed: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled  = !loading
    }
}