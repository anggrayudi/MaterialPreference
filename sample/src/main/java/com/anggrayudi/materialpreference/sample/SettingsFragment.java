package com.anggrayudi.materialpreference.sample;

import android.os.Bundle;

import com.anggrayudi.materialpreference.BuildConfig;
import com.anggrayudi.materialpreference.PreferenceFragmentCompat;
import com.anggrayudi.materialpreference.SeekBarDialogPreference;
import com.anggrayudi.materialpreference.SeekBarPreference;

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
    }
}
