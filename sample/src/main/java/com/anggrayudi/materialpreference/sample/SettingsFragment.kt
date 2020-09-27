package com.anggrayudi.materialpreference.sample

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
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
 *          kapt 'com.anggrayudi:materialpreference-compiler:1.7'
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
            MaterialDialog(context!!)
                .message(text = "Your account has been verified.")
                .positiveButton(android.R.string.ok)
                .show()
            true
        }
        indicatorPreference?.onPreferenceLongClickListener = {
            Toast.makeText(context, "onLogClick: " + it.title!!, Toast.LENGTH_SHORT).show()
            true
        }

        val colorPreference = findPreferenceAs<ColorPreference>(PrefKey.THEME_COLOR)
        colorPreference?.allowArgb = true
        colorPreference?.allowTransparency = true

        // change the last color in this array to colorPrimary
        val colorList = ColorPreference.DEFAULT_COLOR_LIST.copyOf()
        colorList[colorList.size - 1] = ContextCompat.getColor(context!!, R.color.colorPrimary)
        colorPreference?.colorList = colorList

        colorPreference?.summaryFormatter = {
            Handler().post {
                (activity as SettingsActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor(it)))
            }
            when (it) {
                "#F44336" -> "Red"
                "#E91E63" -> "Pink"
                else -> it
            }
        }

        findPreference(PrefKey.RESTORE_DEFAULT)?.onPreferenceClickListener = {
            MaterialDialog(context!!)
                .message(text = "Are you sure you want to restore default settings?")
                .negativeButton(android.R.string.cancel)
                .positiveButton(android.R.string.ok) {
                    App.setDefaultPreferenceValues(context!!)
                    activity!!.recreate()
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

    companion object {

        private const val TAG = "SettingsFragment"

        fun newInstance(rootKey: String?) = SettingsFragment().apply {
            arguments = Bundle()
            arguments!!.putString(ARG_PREFERENCE_ROOT, rootKey)
        }
    }
}
