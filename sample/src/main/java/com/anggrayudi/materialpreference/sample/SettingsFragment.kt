package com.anggrayudi.materialpreference.sample

import android.Manifest
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.anggrayudi.materialpreference.*
import com.anggrayudi.materialpreference.annotation.PreferenceKeysConfig

/**
 * [PreferenceKeysConfig] annotation creates a constant class which contains all of your
 * **Preference Keys** from file `/xml/preferences.xml`, .i.e [PrefKey] object class.
 * So you don't need to make your own constant class.
 *
 * To use auto-generated [PrefKey] class, add the following configuration to your `build.gradle` in
 * `dependencies` section:
 *
 *      dependencies {
 *          implementation 'com.anggrayudi:materialpreference:3.1.0'
 *          kapt 'com.anggrayudi:materialpreference-compiler:1.0.0'
 *      }
 */
@PreferenceKeysConfig()
class SettingsFragment : PreferenceFragmentMaterial(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(PrefKey.ABOUT)!!.summary = BuildConfig.VERSION_NAME

        val volume = findPreference(PrefKey.NOTIFICATION_VOLUME) as SeekBarPreference
        volume.summaryFormatter = { "$it%" }

        val vibration = findPreference(PrefKey.VIBRATE_DURATION) as SeekBarDialogPreference
        vibration.summaryFormatter = { "${it}ms" }

        val indicatorPreference = findPreference(PrefKey.ACCOUNT_STATUS) as IndicatorPreference
        indicatorPreference.onPreferenceClickListener = {
            MaterialDialog(context!!)
                    .message(text = "Your account has been verified.")
                    .positiveButton(android.R.string.ok)
                    .show()
            true
        }
        indicatorPreference.onPreferenceLongClickListener = {
            Toast.makeText(context, "onLogClick: " + it.title!!, Toast.LENGTH_SHORT).show()
            true
        }

        val colorPreference = findPreference(PrefKey.THEME_COLOR) as ColorPreference
        colorPreference.allowArgb = true
        colorPreference.allowTransparency = true

        // change the last color in this array to colorPrimary
        val colorList = ColorPreference.DEFAULT_COLOR_LIST.copyOf()
        colorList[colorList.size - 1] = ContextCompat.getColor(context!!, R.color.colorPrimary)
        colorPreference.colorList = colorList

        colorPreference.summaryFormatter = {
            Handler().post {
                (activity as SettingsActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor(it)))
            }
            when (it) {
                "#F44336" -> "Red"
                "#E91E63" -> "Pink"
                else -> it
            }
        }

        val folderPreference = findPreference(PrefKey.BACKUP_LOCATION) as FolderPreference
        folderPreference.permissionCallback = object : StoragePermissionCallback {
            override fun onPermissionTrouble(read: Boolean, write: Boolean) {
                ActivityCompat.requestPermissions(activity!!,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }

        findPreference(PrefKey.RESTORE_DEFAULT)!!.onPreferenceClickListener = {
            MaterialDialog(context!!)
                    .message(text = "Are you sure you want to restore default settings?")
                    .negativeButton(android.R.string.cancel)
                    .positiveButton(android.R.string.ok) {
                        App.setDefaultPreferenceValues(context!!)
                        activity!!.recreate()
                    }.show()
            true
        }

        findPreference(PrefKey.ACCOUNT_NAME)!!.onPreferenceChangeListener = { _, newValue ->
            val name = newValue.toString()
            if (name.contains("shit")) {
                Toast.makeText(context, "Use a polite name", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager!!.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager!!.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        when (key) {
            "auto_update" -> {
            }
        }
    }

    companion object {

        private const val TAG = "SettingsFragment"

        fun newInstance(rootKey: String?): SettingsFragment {
            val args = Bundle()
            args.putString(PreferenceFragmentMaterial.ARG_PREFERENCE_ROOT, rootKey)
            val fragment = SettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
