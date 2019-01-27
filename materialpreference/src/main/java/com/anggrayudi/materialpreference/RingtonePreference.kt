/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anggrayudi.materialpreference

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings.System
import android.text.TextUtils
import android.util.AttributeSet
import com.anggrayudi.materialpreference.callback.StoragePermissionCallback
import com.anggrayudi.materialpreference.dialog.DialogPreference

/**
 * A [Preference] that allows the user to choose a ringtone from those on the device.
 * The chosen ringtone's URI will be persisted as a string.
 *
 * If the user chooses the "Default" item, the saved string will be one of:
 * * [System.DEFAULT_RINGTONE_URI]
 * * [System.DEFAULT_NOTIFICATION_URI]
 * * [System.DEFAULT_ALARM_ALERT_URI].
 *
 * If the user chooses the "Silent" item, the saved string will be an empty string.
 * See issue [No support for RingtonePreference in support library](https://code.google.com/p/android/issues/detail?id=183255)
 *
 *      |       Attribute      |             Value Type             |
 *      |:--------------------:|:----------------------------------:|
 *      | android:ringtoneType | alarm, all, notification, ringtone |
 *      | android:showDefault  | Boolean                            |
 *      | android:showSilent   | Boolean                            |
 */
class RingtonePreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.ringtonePreferenceStyle,
        defStyleRes: Int = R.style.Preference_DialogPreference_RingtonePreference)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Sets the sound type(s) that are shown in the picker.
     *
     * @return The sound type(s) that are shown in the picker.
     * @see RingtoneManager.EXTRA_RINGTONE_TYPE
     */
    var ringtoneType: Int = 0

    /**
     * Sets whether to show an item for the default sound/ringtone. The default
     * to use will be deduced from the sound type(s) being shown.
     *
     * @return Whether to show an item for the default sound/ringtone.
     * @see RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT
     */
    var showDefault: Boolean = false

    /**
     * Sets whether to show an item for 'Silent'.
     *
     * @return Whether to show an item for 'Silent'.
     * @see RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT
     */
    var showSilent: Boolean = false

    /**
     * When attempting to play a ringtone from external storage without the
     * [android.Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * the picker would crash. Now you get a chance at custom handling.
     *
     * You can use the [buildRingtonePickerIntent] and [onActivityResult]
     * methods to open the system ringtone picker and process its result.
     * System ringtone picker has access to external storage but does not use your app's theme.
     *
     * Default behavior would still open the in-app picker dialog but
     * * you won't hear any inaccessible ringtones,
     * * inaccessible ringtones will have placeholder titles in the list.
     *
     * You can also open the in-app picker dialog manually by calling
     * [showDialogFragment] if you want to ignore the error.
     */
    var permissionCallback: StoragePermissionCallback? = null

    // FIXME Adjust logic once strings are bundled.
    internal val nonEmptyDialogTitle: CharSequence
        get() {
            var title = dialogTitle
            if (title == null) {
                title = super.title
            }
            if (TextUtils.isEmpty(title)) {
                when (ringtoneType) {
                    RingtoneManager.TYPE_NOTIFICATION -> title = getRingtonePickerTitleNotificationString(context)
                    RingtoneManager.TYPE_ALARM -> title = getRingtonePickerTitleAlarmString(context)
                }
            }
            if (TextUtils.isEmpty(title)) {
                title = getRingtonePickerTitleString(context)
            }
            return title!!
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RingtonePreference, defStyleAttr, defStyleRes)
        ringtoneType = a.getInt(R.styleable.RingtonePreference_android_ringtoneType, RingtoneManager.TYPE_RINGTONE)
        showDefault = a.getBoolean(R.styleable.RingtonePreference_android_showDefault, true)
        showSilent = a.getBoolean(R.styleable.RingtonePreference_android_showSilent, true)
        a.recycle()
    }

    override fun onSetInitialValue() {
        if (isBindValueToSummary) {
            val uri = getPersistedString(null)
            summary = if (uri == null)
                context.getString(R.string.ringtone_silent)
            else
                getRingtoneTitle(context, Uri.parse(uri))
        }
    }

    fun canPlayDefaultRingtone(context: Context): Boolean {
        val defaultUri = RingtoneManager.getDefaultUri(ringtoneType)
        val ringtone = SafeRingtone.obtain(context, defaultUri)
        try {
            return ringtone.canPlay()
        } finally {
            ringtone.stop()
        }
    }

    fun canShowSelectedRingtoneTitle(context: Context): Boolean {
        val currentUri = onRestoreRingtone()
        val ringtone = SafeRingtone.obtain(context, currentUri)
        try {
            return ringtone.canGetTitle()
        } finally {
            ringtone.stop()
        }
    }

    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     *
     * By default, this restores the previous ringtone URI from the persistent storage.
     *
     * @return The ringtone to be marked as the current ringtone.
     */
    fun onRestoreRingtone(): Uri? {
        val uriString = getPersistedString(null)
        return if (!TextUtils.isEmpty(uriString)) Uri.parse(uriString) else null
    }

    /** Creates system ringtone picker intent for manual use. */
    fun buildRingtonePickerIntent(): Intent {
        val type = ringtoneType
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, onRestoreRingtone())
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(type))
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, showDefault)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, showSilent)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, nonEmptyDialogTitle)
        return i
    }

    /**
     * Use this method to process selected ringtone if you manually opened system ringtone picker
     * by [RingtoneManager.ACTION_RINGTONE_PICKER].
     */
    fun onActivityResult(data: Intent?) {
        if (data != null) {
            val uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            saveRingtone(uri)
        }
    }

    internal fun saveRingtone(uri: Uri?) {
        if (callChangeListener(uri?.toString() ?: "")) {
            persistString(uri?.toString() ?: "")
            if (isBindValueToSummary)
                summary = if (uri == null)
                    context.getString(R.string.ringtone_silent)
                else
                    getRingtoneTitle(context, uri)
        }
    }

    interface OnFailedToReadRingtoneListener {
        fun onFailedToReadRingtone(ringtonePreference: RingtonePreference, cannotPlayDefault: Boolean, cannotShowTitleSelected: Boolean)
    }

    companion object {
        private const val TAG = "RingtonePreference"
        private const val MEDIA_PROVIDER_PACKAGE_NAME = "com.android.providers.media"

        fun getRingtoneTitle(context: Context, uri: Uri?): String {
            val ringtone = SafeRingtone.obtain(context, uri)
            try {
                return ringtone.title
            } finally {
                ringtone.stop()
            }
        }

        fun getNotificationSoundDefaultString(context: Context): String {
            return context.getString(R.string.notification_sound_default)
        }

        fun getAlarmSoundDefaultString(context: Context): String {
            return context.getString(R.string.alarm_sound_default)
        }

        fun getRingtoneDefaultString(context: Context): String {
            return context.getString(R.string.ringtone_default)
        }

        fun getRingtoneDefaultWithActualString(context: Context, actual: String): String {
            return context.getString(R.string.ringtone_default_with_actual, actual)
        }

        fun getRingtoneSilentString(context: Context): String {
            return context.getString(R.string.ringtone_silent)
        }

        fun getRingtoneUnknownString(context: Context): String {
            return context.getString(R.string.ringtone_unknown)
        }

        fun getRingtonePickerTitleString(context: Context): String {
            return context.getString(R.string.ringtone_picker_title)
        }

        fun getRingtonePickerTitleAlarmString(context: Context): String {
            return context.applicationContext.getString(R.string.ringtone_picker_title_alarm)
        }

        fun getRingtonePickerTitleNotificationString(context: Context): String {
            return context.applicationContext.getString(R.string.ringtone_picker_title_notification)
        }
    }
}
