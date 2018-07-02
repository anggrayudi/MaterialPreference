package com.anggrayudi.materialpreference.sample;

import android.os.Bundle;

import com.anggrayudi.materialpreference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static SettingsFragment newInstance(String rootKey) {
        Bundle args = new Bundle();
        args.putString(ARG_PREFERENCE_ROOT, rootKey);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}
