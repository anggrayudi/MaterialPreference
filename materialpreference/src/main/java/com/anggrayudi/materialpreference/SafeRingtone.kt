package com.anggrayudi.materialpreference

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.Settings
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import com.anggrayudi.materialpreference.SafeRingtone.Companion.obtain

/**
 * Ringtone provides a quick method for playing a ringtone, notification, or other similar types of sounds.
 *
 * Retrieve [SafeRingtone] objects by [obtain].
 *
 * This class works around some platform limitations:
 *
 *  * Any ringtone can get title on API 23, otherwise external ringtone can only get title when
 * [android.Manifest.permission.READ_EXTERNAL_STORAGE] is granted.
 *  * Any ringtone can play on API 16, otherwise external ringtone can only play when
 * [android.Manifest.permission.READ_EXTERNAL_STORAGE] is granted.
 *
 * Instead of throwing a [SecurityException]
 *
 *  * if a sound cannot be played, there will be silence,
 *  * if a title cannot be obtained, localized "Unknown" will be returned.
 *
 * @see RingtoneManager
 */
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SafeRingtone private constructor(
        private val context: Context, @param:Nullable private val uri: Uri?) {

    private val ringtone: Ringtone?
        get() {
            if (_ringtone == null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                if (ringtone != null) {
                    if (_streamType != STREAM_NULL) {
                        ringtone.streamType = _streamType
                    }
                }
                _ringtone = ringtone
            }
            return _ringtone
        }
    private var _ringtone: Ringtone? = null

    val isPlaying: Boolean
        get() = _ringtone != null && _ringtone!!.isPlaying

    // On API 23+ reading ringtone title is safe.
    // On API 16-22 Ringtone does nothing when reading from SD card without permission.
    // Below API 16 Ringtone crashes when we try to read from SD card without permission.
    // But that's just AOSP. Let's enforce any SecurityException here.
    val title: String
        get() {
            val ringtone = ringtone
            if (ringtone != null) {
                return if (Build.VERSION.SDK_INT >= 23) {
                    ringtone.getTitle(context)
                } else {
                    try {
                        if (uri != null) {
                            peek(context, uri)
                        }
                        ringtone.getTitle(context)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Cannot get title of ringtone at $uri.")
                        RingtonePreference.getRingtoneUnknownString(context)
                    }
                }
            } else {
                Log.w(TAG, "Cannot get title of ringtone at $uri.")
                return RingtonePreference.getRingtoneUnknownString(context)
            }
        }

    fun canPlay(): Boolean = uri != null

    fun play() {
        if (canPlay()) {
            ringtone?.play() ?: Log.w(TAG, "Ringtone at $uri cannot be played.")
        } else {
            Log.w(TAG, "Ringtone at $uri cannot be played.")
        }
    }

    fun stop() {
        _ringtone?.stop()
    }

    /**
     * Sets the stream type where this ringtone will be played.
     *
     * @see [AudioManager]
     */
    internal var streamType: Int
        get() = _streamType
        set(value) {
            if (value < -1) {
                throw IllegalArgumentException("Invalid stream type: $value")
            }
            _streamType = value
            _ringtone?.streamType = value
        }
    private var _streamType: Int = 0

    fun canGetTitle(): Boolean {
        return canGetTitle(context, uri)
    }

    companion object {
        private val TAG = SafeRingtone::class.java.simpleName

        private const val STREAM_NULL = Integer.MIN_VALUE

        private val COLUMNS = arrayOf(BaseColumns._ID)

        fun obtain(context: Context, uri: Uri?): SafeRingtone {
            return SafeRingtone(context.applicationContext, uri)
        }

        fun obtain(context: Context, uri: Uri?, streamType: Int): SafeRingtone {
            val ringtone = SafeRingtone(context.applicationContext, uri)
            ringtone.streamType = streamType
            return ringtone
        }

        private fun peek(context: Context, uri: Uri) {
            if (Settings.AUTHORITY == uri.authority) {
                val type = RingtoneManager.getDefaultType(uri)
                // This can throw a SecurityException.
                val actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type)
                if (actualUri != null) {
                    // Actual Uri may be null on Android 4 emulators, where there are no ringtones.
                    // Plus silent default ringtone sounds like a valid case.
                    peek(context, actualUri)
                }
                return
            }

            // This can throw a SecurityException.
            val res = context.contentResolver
            val cursor = res.query(uri, COLUMNS, null, null, null)
            cursor?.close()
        }

        fun canGetTitle(context: Context, uri: Uri?): Boolean {
            if (uri == null) {
                // We can display "None".
                return true
            }
            if (Build.VERSION.SDK_INT >= 23) {
                return true
            }
            return try {
                peek(context, uri)
                true
            } catch (e: SecurityException) {
                false
            }
        }
    }
}
