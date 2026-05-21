package com.shopapp.ui.auth

import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shopapp.R
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityLoginBinding
import com.shopapp.ui.admin.AdminActivity
import com.shopapp.ui.home.MainActivity
import com.shopapp.ui.staff.StaffActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var selectedRole = "user"   // "user" | "admin" | "staff"

    companion object {
        /** Passed from RegisterActivity so we can pre-fill email + greet by name */
        const val EXTRA_PREFILL_EMAIL = "prefill_email"
        const val EXTRA_PREFILL_NAME  = "prefill_name"
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        handlePrefill()
    }

    // ── Pre-fill email & show welcome name after successful registration ──
    private fun handlePrefill() {
        val email = intent.getStringExtra(EXTRA_PREFILL_EMAIL) ?: return
        val name  = intent.getStringExtra(EXTRA_PREFILL_NAME)  ?: ""

        // Pre-fill email field
        binding.etEmail.setText(email)
        binding.etPassword.requestFocus()

        // Show a personalised welcome message under the subtitle
        if (name.isNotEmpty()) {
            binding.tvWelcomeName.visibility = View.VISIBLE
            binding.tvWelcomeName.text       = "👋 Welcome, $name! Please sign in to continue."
        }
    }

    private fun setupClickListeners() {
        binding.btnTabUser.setOnClickListener  { selectTab("user") }
        binding.btnTabAdmin.setOnClickListener { selectTab("admin") }
        binding.btnTabStaff.setOnClickListener { selectTab("staff") }
        binding.btnLogin.setOnClickListener    { performLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun selectTab(role: String) {
        selectedRole = role
        // Reset all tabs to unselected style
        binding.btnTabUser.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.btnTabAdmin.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.btnTabStaff.setBackgroundResource(R.drawable.bg_tab_unselected)
        when (role) {
            "user"  -> {
                binding.btnTabUser.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvLoginTitle.text = "User Login"
            }
            "admin" -> {
                binding.btnTabAdmin.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvLoginTitle.text = "Admin Login"
            }
            "staff" -> {
                binding.btnTabStaff.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvLoginTitle.text = "Staff Login"
            }
        }
    }

    private fun performLogin() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (email.isEmpty())    { binding.etEmail.error = "Email is required";    return }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; return }

        setLoading(true)
        lifecycleScope.launch {
            val result = Repository.loginUser(email, password)
            if (result.isFailure) {
                setLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val user = result.getOrNull()!!
            setLoading(false)

            if (selectedRole == "admin" && user.role != "admin") {
                Repository.logout()
                Toast.makeText(this@LoginActivity, "This account is not an Admin account.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (selectedRole == "staff" && user.role != "staff") {
                Repository.logout()
                Toast.makeText(this@LoginActivity, "This account is not a Staff account.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (selectedRole == "user" && (user.role == "admin" || user.role == "staff")) {
                Repository.logout()
                Toast.makeText(this@LoginActivity, "Please use the correct tab to login.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val intent = when (user.role) {
                "admin" -> Intent(this@LoginActivity, AdminActivity::class.java)
                "staff" -> Intent(this@LoginActivity, StaffActivity::class.java)
                else    -> Intent(this@LoginActivity, MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled     = !loading
        binding.etEmail.isEnabled      = !loading
        binding.etPassword.isEnabled   = !loading
    }
}