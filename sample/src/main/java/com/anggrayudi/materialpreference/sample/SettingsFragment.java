package com.anggrayudi.materialpreference.sample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.BuildConfig;
import com.anggrayudi.materialpreference.IndicatorPreference;
import com.anggrayudi.materialpreference.Preference;
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial;
import com.anggrayudi.materialpreference.SeekBarDialogPreference;
import com.anggrayudi.materialpreference.SeekBarPreference;

public class SettingsFragment extends PreferenceFragmentMaterial implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";

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

        findPreference("about").setSummary(BuildConfig.VERSION_NAME);

        SeekBarPreference volume = (SeekBarPreference) findPreference("notification_volume");
        volume.setValueFormatter(new SeekBarPreference.ValueFormatter() {
            @Override
            public String getValue(int progress) {
                return progress + "%";
            }
        });

        SeekBarDialogPreference vibration = (SeekBarDialogPreference) findPreference("vibrate_duration");
        vibration.setValueFormatter(new SeekBarPreference.ValueFormatter() {
            @Override
            public String getValue(int progress) {
                return progress + "ms";
            }
        });

        IndicatorPreference indicatorPreference = (IndicatorPreference) findPreference("account_status");
        indicatorPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new MaterialDialog.Builder(getContext())
                        .content("Your account has been verified.")
                        .positiveText(android.R.string.ok)
                        .show();
                return true;
            }
        });
        indicatorPreference.setLongClickListener(new Preference.OnPreferenceLongClickListener() {
            @Override
            public boolean onPreferenceLongClick(Preference preference) {
                Toast.makeText(preference.getContext(), "onLongClick: " + preference.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        switch (key) {
            case "auto_update":
                boolean visible = preferences.getBoolean(key, true);
                findPreference("wifi_only").setVisible(visible);
                break;
        }
    }
}
