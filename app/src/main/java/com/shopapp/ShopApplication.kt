package com.shopapp

import android.app.Application
import android.content.Context
import com.shopapp.data.repository.Repository
import com.shopapp.util.LocaleHelper
import com.shopapp.util.ThemeHelper

class ShopApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()

        // ✅ Repository.init() only sets up SharedPreferences now.
        //    Firebase Auth & Firestore initialise themselves automatically
        //    via google-services.json — no manual setup needed here.
        Repository.init(this)

        ThemeHelper.applySavedTheme(this)

        // ❌ REMOVED: Room database seed for admin account.
        //    Create your admin account in Firebase Console instead.
        //    (See Step 6 in the migration guide.)
    }
}