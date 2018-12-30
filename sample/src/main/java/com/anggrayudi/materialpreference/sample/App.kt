package com.anggrayudi.materialpreference.sample

import android.app.Application
import android.content.Context
import android.graphics.Color
import com.anggrayudi.materialpreference.PreferenceManager
import com.anggrayudi.materialpreference.PreferenceManager.Companion.KEY_HAS_SET_DEFAULT_VALUES
import com.anggrayudi.materialpreference.util.SaveDir

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        /*
        DO NOT USE THIS METHOD to set your preferences' default value. It is inefficient!!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        USE THE FOLLOWING TECHNIQUE INSTEAD
         */
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            preferences.edit().putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true).apply()
            setDefaultPreferenceValues(this)
        }
    }

    companion object {

        fun setDefaultPreferenceValues(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit()
                    .putBoolean("auto_update", true)
                    .putBoolean("wifi_only", true)
                    .putString("update_interval", "Weekly")
                    .putInt("vibrate_duration", 200)
                    .putString("backupLocation", SaveDir.DOWNLOADS)
                    .putInt("themeColor", Color.parseColor("#37474F"))
                    .apply()
        }
    }
}
