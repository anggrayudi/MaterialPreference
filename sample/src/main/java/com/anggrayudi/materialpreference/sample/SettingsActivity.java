package com.anggrayudi.materialpreference.sample;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.anggrayudi.materialpreference.PreferenceActivityCompat;
import com.anggrayudi.materialpreference.PreferenceFragmentCompat;

public class SettingsActivity extends PreferenceActivityCompat {

    private static final String TAG = "SettingsActivity";

    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mSettingsFragment = SettingsFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mSettingsFragment, "Settings").commit();
        } else {
            mSettingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("Settings");
            setTitle(mSettingsFragment.getPreferenceFragmentTitle());
        }
    }

    @Override
    protected PreferenceFragmentCompat onBuildPreferenceFragment(String rootKey) {
        return SettingsFragment.newInstance(rootKey);
    }

    @Override
    public void onBackStackChanged() {
        mSettingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("Settings");
        setTitle(mSettingsFragment.getPreferenceFragmentTitle());
    }
}
