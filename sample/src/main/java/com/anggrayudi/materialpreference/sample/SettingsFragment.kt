package com.anggrayudi.materialpreference.sample

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.anggrayudi.materialpreference.*
import com.anggrayudi.materialpreference.annotation.PreferencesConfig

/**
 * [PreferencesConfig] annotation creates a constant class which contains all of your
 * **Preference Keys** from file `/xml/preferences.xml`, .i.e [PrefKey] object class.
 * So you don't need to make your own constant class. It also generates [SharedPreferencesHelper].
 *
 * To use auto-generated [PrefKey] and [SharedPreferencesHelper] classes,
 * add the following configuration to your `build.gradle` in `dependencies` section:
 *
 *      dependencies {
 *          implementation 'com.anggrayudi:materialpreference:3.7.0'
 *          kapt 'com.anggrayudi:materialpreference-compiler:1.'
 *      }
 */
@PreferencesConfig
class SettingsFragment : PreferenceFragmentMaterial() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(PrefKey.ABOUT)?.summary = BuildConfig.VERSION_NAME

        val volume = findPreferenceAs<SeekBarPreference>(PrefKey.NOTIFICATION_VOLUME)
        volume?.summaryFormatter = { "$it%" }

        val vibration = findPreferenceAs<SeekBarDialogPreference>(PrefKey.VIBRATE_DURATION)
        vibration?.summaryFormatter = { "${it}ms" }

        val indicatorPreference = findPreferenceAs<IndicatorPreference>(PrefKey.ACCOUNT_STATUS)
        indicatorPreference?.onPreferenceClickListener = {
            MaterialDialog(requireContext())
                .message(text = "Your account has been verified.")
                .positiveButton(android.R.string.ok)
                .show()
            true
        }
        indicatorPreference?.onPreferenceLongClickListener = {
            Toast.makeText(context, "onLogClick: " + it.title!!, Toast.LENGTH_SHORT).show()
            true
        }

        findPreferenceAs<ColorPreference>(PrefKey.THEME_COLOR)?.run {
            allowArgb = true
            allowTransparency = true
            changeToolbarColor(colorHex)

            // change the last color in this array to colorPrimary
            val colors = ColorPreference.DEFAULT_COLOR_LIST.copyOf()
            colors[colorList.size - 1] = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            colorList = colors

            summaryFormatter = {
                changeToolbarColor(it!!)
                when (it) {
                    "#F44336" -> "Red"
                    "#E91E63" -> "Pink"
                    else -> it
                }
            }
        }

        findPreference(PrefKey.RESTORE_DEFAULT)?.onPreferenceClickListener = {
            MaterialDialog(requireContext())
                .message(text = "Are you sure you want to restore default settings?")
                .negativeButton(android.R.string.cancel)
                .positiveButton(android.R.string.ok) {
                    App.setDefaultPreferenceValues(requireContext())
                    requireActivity().recreate()
                }.show()
            true
        }

        findPreference(PrefKey.ACCOUNT_NAME)?.onPreferenceChangeListener = { _, newValue ->
            val name = newValue.toString()
            if (name.contains("shit")) {
                Toast.makeText(context, "Use a polite name", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        }
    }

    private fun changeToolbarColor(colorHex: String) {
        (requireActivity() as SettingsActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor(colorHex)))
    }

    companion object {

        private const val TAG = "SettingsFragment"

        fun newInstance(rootKey: String?) = SettingsFragment().apply {
            arguments = Bundle().also {
                it.putString(ARG_PREFERENCE_ROOT, rootKey)
            }
        }
    }
}
