package com.shopapp.ui

import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shopapp.R
import com.shopapp.data.repository.Repository
import com.shopapp.ui.auth.LoginActivity
import com.shopapp.ui.admin.AdminActivity
import com.shopapp.ui.home.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({ checkLoginState() }, 2000)
    }

    private fun checkLoginState() {
        if (!Repository.isLoggedIn()) { goToLogin(); return }
        lifecycleScope.launch {
            try {
                val user = Repository.getCurrentUser()
                when {
                    user == null -> { Repository.logout(); goToLogin() }
                    user.role == "admin" -> goToAdmin()
                    else -> goToMain()
                }
            } catch (e: Exception) { Repository.logout(); goToLogin() }
        }
    }

    private fun goToLogin() = go(LoginActivity::class.java)
    private fun goToMain()  = go(MainActivity::class.java)
    private fun goToAdmin() = go(AdminActivity::class.java)

    private fun <T> go(cls: Class<T>) {
        startActivity(Intent(this, cls).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
