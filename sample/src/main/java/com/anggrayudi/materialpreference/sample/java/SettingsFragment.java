package com.anggrayudi.materialpreference.sample.java;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.IndicatorPreference;
import com.anggrayudi.materialpreference.Preference;
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial;
import com.anggrayudi.materialpreference.SeekBarDialogPreference;
import com.anggrayudi.materialpreference.annotation.PreferenceKeysConfig;
import com.anggrayudi.materialpreference.sample.BuildConfig;
import com.anggrayudi.materialpreference.sample.PrefKey;
import com.anggrayudi.materialpreference.sample.R;

import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.widget.Toast;

import kotlin.jvm.functions.Function1;

/**
 * Sample class for Java compatibility. This class shows how Kotlin library can be used in Java.
 * For complete sample, see {@link com.anggrayudi.materialpreference.sample.SettingsFragment}
 *
 * @author Anggrayudi H
 */
@PreferenceKeysConfig
public class SettingsFragment extends PreferenceFragmentMaterial {

    public static SettingsFragment newInstance(String rootKey) {
        Bundle args = new Bundle();
        args.putString(ARG_PREFERENCE_ROOT, rootKey);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        Preference preferenceAbout = findPreference(PrefKey.ABOUT);
        preferenceAbout.setSummary(BuildConfig.VERSION_NAME);

        SeekBarDialogPreference vibration = (SeekBarDialogPreference) findPreference(PrefKey.VIBRATE_DURATION);

        // summary formatter example
        vibration.setSummaryFormatter(new Function1<Integer, String>() {
            @Override
            public String invoke(Integer duration) {
                return duration + "ms";
            }
        });

        IndicatorPreference indicatorPreference = (IndicatorPreference) findPreference(PrefKey.ACCOUNT_STATUS);

        // click listener example
        indicatorPreference.setOnPreferenceClickListener(new Function1<Preference, Boolean>() {
            @Override
            public Boolean invoke(Preference preference) {
                new MaterialDialog(getContext())
                    .message(null, "Your account has been verified.", false, 1f)
                    .positiveButton(android.R.string.ok, null, null)
                    .show();
                return true;
            }
        });

        // long click listener example
        indicatorPreference.setOnPreferenceLongClickListener(new Function1<Preference, Boolean>() {
            @Override
            public Boolean invoke(Preference preference) {
                Toast.makeText(getContext(), "onLogClick: " + preference.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}
