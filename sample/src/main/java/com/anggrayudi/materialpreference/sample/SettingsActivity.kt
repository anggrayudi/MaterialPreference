package com.anggrayudi.materialpreference.sample

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.anggrayudi.materialpreference.PreferenceActivityMaterial
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial

class SettingsActivity : PreferenceActivityMaterial() {

    private var settingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment.newInstance(null)
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, settingsFragment!!, TAG).commit()
        } else {
            settingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
            title = settingsFragment!!.preferenceFragmentTitle
        }
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
        menu.findItem(R.id.action_donate).intent = Intent(this, DonationActivity::class.java)
        return super.onCreateOptionsMenu(menu)
    }

    companion object {

        private const val TAG = "SettingsActivity"
    }
}
