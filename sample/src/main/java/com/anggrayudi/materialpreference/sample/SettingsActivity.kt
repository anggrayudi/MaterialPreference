package com.anggrayudi.materialpreference.sample

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.anggrayudi.materialpreference.PreferenceActivityMaterial
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial
import org.koin.android.ext.android.inject

class SettingsActivity : PreferenceActivityMaterial(), SharedPreferences.OnSharedPreferenceChangeListener {

    // Since we use Koin, we don't need to init SharedPreferencesHelper manually.
    // Just use Android Application Context provided by Koin to inject the required parameter of SharedPreferencesHelper.
    // The main advantage is we can use SharedPreferencesHelper everywhere without worrying to satisfy its constructor parameter.
    private val preferencesHelper: SharedPreferencesHelper by inject()

    private var settingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDayNightTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment.newInstance(null)
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, settingsFragment!!, TAG).commit()
        } else {
            onBackStackChanged()
        }
    }

    private fun applyDayNightTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (preferencesHelper.isEnableDarkTheme)
                AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onBuildPreferenceFragment(rootKey: String?): PreferenceFragmentMaterial {
        return SettingsFragment.newInstance(rootKey)
    }

    override fun onBackStackChanged() {
        settingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
        title = settingsFragment!!.preferenceFragmentTitle
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_donate) {
            MaterialDialog(this)
                .title(text = "Donation")
                .message(text = "Select your donation method.")
                .listItems(items = listOf("PayPal", "Saweria")) { _, index, _ ->
                    val url = when (index) {
                        0 -> "https://www.paypal.com/paypalme/hardiannicko"
                        else -> "https://saweria.co/hardiannicko"
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                .show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        preferencesHelper.preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        preferencesHelper.preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        if (key == PrefKey.ENABLE_DARK_THEME) {
            recreate()
        }
    }

    companion object {

        private const val TAG = "SettingsActivity"
    }
}
