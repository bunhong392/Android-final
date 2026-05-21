package com.shopapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shopapp.R
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityAdminBinding
import com.shopapp.ui.auth.LoginActivity

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    val tvPageTitle: TextView get() = binding.tvPageTitle

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment
        loadFragment(AdminProductsFragment())

        // ── FIX 1: Toolbar menu button → open Orders fragment ─────────────────
        binding.btnMenu.setOnClickListener {
            loadFragment(AdminOrdersFragment())
            binding.bottomNav.selectedItemId = -1  // deselect current tab visually
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home     -> { loadFragment(AdminProductsFragment()); true }
                R.id.nav_admin_add      -> { loadFragment(AdminAddProductFragment()); true }
                R.id.nav_admin_finance  -> { loadFragment(AdminFinanceFragment()); true }
                R.id.nav_admin_settings -> { loadFragment(AdminSettingsFragment()); true }
                else -> false
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun logout() {
        Repository.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
