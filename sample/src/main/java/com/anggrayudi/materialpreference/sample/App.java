package com.anggrayudi.materialpreference.sample;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.anggrayudi.materialpreference.PreferenceManager;
import com.anggrayudi.materialpreference.util.SaveDir;

public class App extends Application {

    private static final String KEY_HAS_SET_DEFAULT_VALUES = "hasSetDefaultValues";

    @Override
    public void onCreate() {
        super.onCreate();
        // Run this method to set your preferences' default value.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            preferences.edit().putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true).apply();
            setDefaultPreferenceValues(this);
        }
        /*
        DO NOT USE THIS METHOD to set your preferences' default value.
        It is inefficient!!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
         */
    }

    public static void setDefaultPreferenceValues(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit()
                .putBoolean("auto_update", true)
                .putBoolean("wifi_only", true)
                .putString("update_interval", "Weekly")
                .putInt("vibrate_duration", 200)
                .putString("backupLocation", SaveDir.DOWNLOADS)
                .apply();
    }
}
