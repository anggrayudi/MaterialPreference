package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.util.Log
import androidx.annotation.RestrictTo
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * @author Eugen on 14.12.2015.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("PrivateApi")
internal class RingtoneManagerCompat : RingtoneManager {

    constructor(activity: Activity) : super(activity)

    constructor(context: Context) : super(context)

    private val internalRingtones: Cursor
        get() {
            try {
                return METHOD_GET_INTERNAL_RINGTONES!!.invoke(this) as Cursor
            } catch (e: Exception) {
                throw IllegalStateException("getInternalRingtones not available.", e)
            }
        }

    private fun setCursor(cursor: Cursor) {
        try {
            FIELD_CURSOR!!.set(this, cursor)
        } catch (e: Exception) {
            throw IllegalStateException("setCursor not available.", e)
        }
    }

    override fun getCursor(): Cursor {
        return try {
            super.getCursor()
        } catch (ex: SecurityException) {
            Log.w(TAG, "No READ_EXTERNAL_STORAGE permission, ignoring ringtones on ext storage")
            @Suppress("DEPRECATION")
            if (includeDrm) {
                Log.w(TAG, "DRM ringtones are ignored.")
            }

            val cursor = internalRingtones
            setCursor(cursor)
            cursor
        }
    }

    companion object {
        private val TAG = RingtoneManagerCompat::class.java.simpleName

        private val FIELD_CURSOR: Field?
        private val METHOD_GET_INTERNAL_RINGTONES: Method?

        init {
            var cursor: Field? = null
            try {
                cursor = RingtoneManager::class.java.getDeclaredField("mCursor")
                cursor!!.isAccessible = true
            } catch (e: Exception) {
                Log.e(TAG, "mCursor not available.", e)
            }

            FIELD_CURSOR = cursor

            var getInternalRingtones: Method? = null
            try {
                getInternalRingtones = RingtoneManager::class.java.getDeclaredMethod("getInternalRingtones")
                getInternalRingtones!!.isAccessible = true
            } catch (e: Exception) {
                Log.e(TAG, "getInternalRingtones not available.", e)
            }

            METHOD_GET_INTERNAL_RINGTONES = getInternalRingtones
        }
    }
}
