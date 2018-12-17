package com.anggrayudi.materialpreference.sample;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.BuildConfig;
import com.anggrayudi.materialpreference.FolderPreference;
import com.anggrayudi.materialpreference.IndicatorPreference;
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial;
import com.anggrayudi.materialpreference.SeekBarDialogPreference;
import com.anggrayudi.materialpreference.SeekBarPreference;

import androidx.core.app.ActivityCompat;

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
        volume.setValueFormatter(progress -> progress + "%");

        SeekBarDialogPreference vibration = (SeekBarDialogPreference) findPreference("vibrate_duration");
        vibration.setValueFormatter(progress -> progress + "ms");

        IndicatorPreference indicatorPreference = (IndicatorPreference) findPreference("account_status");
        indicatorPreference.setOnPreferenceClickListener(preference -> {
            new MaterialDialog.Builder(getContext())
                    .content("Your account has been verified.")
                    .positiveText(android.R.string.ok)
                    .show();
            return true;
        });
        indicatorPreference.setLongClickListener(preference -> {
            Toast.makeText(preference.getContext(), "onLongClick: " + preference.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });

        FolderPreference folderPreference = (FolderPreference) findPreference("backupLocation");
        folderPreference.setPermissionCallback((read, write) -> ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1));
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
//                boolean visible = preferences.getBoolean(key, true);
//                findPreference("wifi_only").setVisible(visible);
                break;
        }
    }
}
