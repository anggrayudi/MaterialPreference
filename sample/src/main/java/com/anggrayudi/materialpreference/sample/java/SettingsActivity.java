package com.anggrayudi.materialpreference.sample.java;

import android.os.Bundle;

import com.anggrayudi.materialpreference.PreferenceActivityMaterial;
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial;
import com.anggrayudi.materialpreference.sample.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anggrayudi H
 */
public class SettingsActivity extends PreferenceActivityMaterial {

    private static final String TAG = "Settings";

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, TAG)
                    .commit();
        } else {
            onBackStackChanged();
        }
    }

    @NotNull
    @Override
    protected PreferenceFragmentMaterial onBuildPreferenceFragment(@Nullable String rootKey) {
        return SettingsFragment.newInstance(rootKey);
    }

    @Override
    public void onBackStackChanged() {
        settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG);
        setTitle(settingsFragment.getPreferenceFragmentTitle());
    }
}
