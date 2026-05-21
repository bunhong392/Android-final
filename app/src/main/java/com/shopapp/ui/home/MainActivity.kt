package com.shopapp.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shopapp.R
import com.shopapp.databinding.ActivityMainBinding
import com.shopapp.ui.auth.LoginActivity
import com.shopapp.util.LocaleHelper
import com.shopapp.util.ThemeHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Key used to remember which tab was active across recreate()
    companion object {
        private const val KEY_NAV_ID = "active_nav_id"
    }

    override fun attachBaseContext(base: android.content.Context) {
        // Always apply the saved locale so every recreate() picks it up
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE super.onCreate / setContentView
        ThemeHelper.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore the last-active tab (default: Home)
        val savedNavId = savedInstanceState?.getInt(KEY_NAV_ID, R.id.nav_profile)
            ?: R.id.nav_home

        // Load the correct fragment without triggering the listener
        loadFragment(fragmentForId(savedNavId))
        binding.bottomNav.selectedItemId = savedNavId

        binding.bottomNav.setOnItemSelectedListener { item ->
            loadFragment(fragmentForId(item.itemId))
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Persist the active tab so recreate() comes back to the same screen
        outState.putInt(KEY_NAV_ID, binding.bottomNav.selectedItemId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun fragmentForId(id: Int): Fragment = when (id) {
        R.id.nav_cart    -> CartFragment()
        R.id.nav_orders  -> OrdersFragment()
        R.id.nav_profile -> ProfileFragment()
        else             -> HomeFragment()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun logout() {
        com.shopapp.data.repository.Repository.logout()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}