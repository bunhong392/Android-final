package com.shopapp.ui.staff

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shopapp.R
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityStaffBinding
import com.shopapp.ui.admin.AdminAddProductFragment
import com.shopapp.ui.admin.AdminOrdersFragment
import com.shopapp.ui.admin.AdminProductsFragment
import com.shopapp.ui.auth.LoginActivity

/**
 * StaffActivity — hosts the three screens a Staff member can access:
 *   1. Home        → AdminProductsFragment  (view product list)
 *   2. Add Product → AdminAddProductFragment (add new product)
 *   3. Orders      → AdminOrdersFragment     (view order messages)
 */
class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding

    /** Exposed so fragments can update the top-bar title (same pattern as AdminActivity) */
    val tvPageTitle: TextView get() = binding.tvPageTitle

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment — product list (Home)
        loadFragment(AdminProductsFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_staff_home    -> { loadFragment(AdminProductsFragment());   true }
                R.id.nav_staff_add     -> { loadFragment(AdminAddProductFragment()); true }
                R.id.nav_staff_orders  -> { loadFragment(AdminOrdersFragment());     true }
                R.id.nav_staff_profile -> { loadFragment(StaffProfileFragment());    true }
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
        finish()
    }
}
