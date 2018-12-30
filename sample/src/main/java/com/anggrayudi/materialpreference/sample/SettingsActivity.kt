package com.anggrayudi.materialpreference.sample

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.anggrayudi.materialpreference.PreferenceActivityMaterial
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial

class SettingsActivity : PreferenceActivityMaterial() {

    private var mSettingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            mSettingsFragment = SettingsFragment.newInstance(null)
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, mSettingsFragment!!, TAG).commit()
        } else {
            mSettingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
            title = mSettingsFragment!!.preferenceFragmentTitle
        }
    }

    override fun onBuildPreferenceFragment(rootKey: String?): PreferenceFragmentMaterial {
        return SettingsFragment.newInstance(rootKey)
    }

    override fun onBackStackChanged() {
        mSettingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
        title = mSettingsFragment!!.preferenceFragmentTitle
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
