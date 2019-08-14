package com.anggrayudi.materialpreference.sample.java;

import com.anggrayudi.materialpreference.PreferenceActivityMaterial;
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial;
import com.anggrayudi.materialpreference.sample.DonationActivity;
import com.anggrayudi.materialpreference.sample.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_donate).setIntent(new Intent(this, DonationActivity.class));
        return super.onCreateOptionsMenu(menu);
    }
}
