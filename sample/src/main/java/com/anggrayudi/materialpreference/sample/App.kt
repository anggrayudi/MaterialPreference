package com.anggrayudi.materialpreference.sample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Environment
import com.anggrayudi.materialpreference.PreferenceManager
import com.anggrayudi.materialpreference.PreferenceManager.Companion.KEY_HAS_SET_DEFAULT_VALUES
import com.anggrayudi.materialpreference.migration.MigrationPlan
import com.anggrayudi.materialpreference.migration.PreferenceMigration
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
        initSharedPreferences()
    }

    private fun initSharedPreferences() {
        /*
        DO NOT USE THIS METHOD to set your preferences' default value. It is inefficient!!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        USE THE FOLLOWING TECHNIQUE INSTEAD
         */
        val preferences = get<SharedPreferences>()
        if (preferences.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            /*
            You can use PreferenceMigration if you want to update your SharedPreferences.
            This commented method migrates your SharedPreferences in background thread,
            hence there'is no guarantee that it will be completed before activity creation.
            Since my main activity is SettingsActivity, I wont call this method here because it may
            causes crash while SettingsActivity is rendering preferences layout and the migration
            is in progress. In real case, your main activity might not be a settings activity,
            so you should not worry about this.

            PreferenceMigration.setupMigration(MyPreferenceMigration(), preferences, PREFERENCE_VERSION)
             */
        } else {
            preferences.edit()
                .putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true)
                // Always set preference version to the latest for the first time
                .putInt(PreferenceMigration.DEFAULT_PREFERENCE_VERSION_KEY, PREFERENCE_VERSION)
                .apply()

            setDefaultPreferenceValues(this)
        }
    }

    private fun initKoin() {
        // Koin Dependency Injection
        startKoin {
            androidContext(applicationContext)

            val preferencesHelperModule = module {
                factory { PreferenceManager.getDefaultSharedPreferences(get()) }
                factory { SharedPreferencesHelper(get()) }
            }

            modules(preferencesHelperModule)
        }
    }

    private inner class MyPreferenceMigration : PreferenceMigration {

        override fun migrate(plan: MigrationPlan, currentPreferenceVersion: Int) {
            var currentVersionTemp = currentPreferenceVersion

            if (currentVersionTemp == 0) {
                plan.updateValue(PrefKey.ENABLE_DARK_THEME, false)
                currentVersionTemp++
            }

            if (currentVersionTemp == 1) {
                plan.updateValue(PrefKey.ENABLE_DARK_THEME, "yes")
                currentVersionTemp++
            }

            // Last IF condition must be "PREFERENCE_VERSION - 1", i.e. 2
            if (currentVersionTemp == 2) {
                plan.renameKey(PrefKey.ENABLE_DARK_THEME, "useDarkTheme")
                currentVersionTemp++
            }
        }

        override fun onNewOS(plan: MigrationPlan, previousOSVersion: Int) {
            /*
            For example:

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                // do your action, such as removing preferences
            }
             */
        }

        override fun onMigrationCompleted(preferences: SharedPreferences) {
            //no-op
        }
    }

    companion object {

        private const val PREFERENCE_VERSION = 3

        /**
         * Create custom `setDefaultPreferenceValues()` where setting some default values require
         * logic and does not covered by [SharedPreferencesHelper.setDefaultPreferenceValues].
         */
        @Suppress("DEPRECATION")
        fun setDefaultPreferenceValues(context: Context) {
            SharedPreferencesHelper.setDefaultPreferenceValues(context)

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit()
                .putString("backupLocation", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
                .putInt("themeColor", Color.parseColor("#37474F"))
                .apply()
        }
    }
}
