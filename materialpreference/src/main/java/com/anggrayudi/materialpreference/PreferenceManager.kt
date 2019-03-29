/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Used to help create [Preference] hierarchies from activities or XML.
 */
class PreferenceManager
@RestrictTo(LIBRARY_GROUP)
constructor(val context: Context) {

    /** The counter for unique IDs. */
    private var mNextId: Long = 0

    /** Cached shared preferences. */
    private var mSharedPreferences: SharedPreferences? = null

    /**
     * Sets a [PreferenceDataStore] to be used by all Preferences associated with this manager
     * that don't have a custom [PreferenceDataStore] assigned via [Preference.preferenceDataStore].
     * Also if the data store is set, the child preferences won't use [SharedPreferences] as long as
     * they are assigned to this manager.
     *
     * @return The [PreferenceDataStore] associated with this manager or `null` if none.
     * @see Preference.preferenceDataStore
     */
    var preferenceDataStore: PreferenceDataStore? = null

    /**
     * If in no-commit mode, the shared editor to give out (which will be
     * committed when exiting no-commit mode).
     */
    private var mEditor: SharedPreferences.Editor? = null

    /**
     * Blocks commits from happening on the shared editor. This is used when
     * inflating the hierarchy. Do not set this directly, use [setNoCommit]
     */
    private var mNoCommit: Boolean = false

    /**
     * Sets the name of the [SharedPreferences] file that preferences managed by this will use.
     *
     * If custom [PreferenceDataStore] is set, this won't override its usage.
     *
     * @return The name that can be passed to [Context.getSharedPreferences].
     * @see Context.getSharedPreferences
     * @see preferenceDataStore
     */
    var sharedPreferencesName: String? = null
        set(sharedPreferencesName) {
            field = sharedPreferencesName
            mSharedPreferences = null
        }

    /**
     * Sets the mode of the SharedPreferences file that preferences managed by this will use.
     *
     * @return Current mode of the SharedPreferences file that preferences managed by this will use.
     * @see Context.getSharedPreferences
     */
    var sharedPreferencesMode: Int = 0
        set(sharedPreferencesMode) {
            field = sharedPreferencesMode
            mSharedPreferences = null
        }

    private var mStorage = STORAGE_DEFAULT

    /**
     * Returns the root of the preference hierarchy managed by this class.
     *
     * @return The [PreferenceScreen] object that is at the root of the hierarchy.
     */
    var preferenceScreen: PreferenceScreen? = null
        private set

    var preferenceComparisonCallback: PreferenceComparisonCallback? = null

    /**
     * Sets the callback to be invoked when a [Preference] in the
     * hierarchy rooted at this [PreferenceManager] is clicked.
     */
    var onPreferenceTreeClickListener: OnPreferenceTreeClickListener? = null

    var onDisplayPreferenceDialogListener: OnDisplayPreferenceDialogListener? = null

    /**
     * Sets the callback to be invoked when a [PreferenceScreen] in the hierarchy rooted at
     * this [PreferenceManager] is clicked.
     */
    var onNavigateToScreenListener: OnNavigateToScreenListener? = null

    /**
     * Called by a preference to get a unique ID in its hierarchy.
     *
     * @return A unique ID.
     */
    internal val nextId: Long
        get() = synchronized(this) {
            return mNextId++
        }

    /**
     * Indicates if the storage location used internally by this class is the
     * default provided by the hosting [Context].
     *
     * @see setStorageDefault
     * @see setStorageDeviceProtected
     */
    val isStorageDefault: Boolean
        get() = if (Build.VERSION.SDK_INT >= 24) {
            mStorage == STORAGE_DEFAULT
        } else {
            true
        }

    /**
     * Indicates if the storage location used internally by this class is backed
     * by device-protected storage.
     *
     * @see setStorageDefault
     * @see setStorageDeviceProtected
     */
    val isStorageDeviceProtected: Boolean
        get() = if (Build.VERSION.SDK_INT >= 24) {
            mStorage == STORAGE_DEVICE_PROTECTED
        } else {
            false
        }

    /**
     * Gets a [SharedPreferences] instance that preferences managed by this will use.
     *
     * @return a [SharedPreferences] instance pointing to the file that contain the values of
     * preferences that are managed by this PreferenceManager. If a [PreferenceDataStore]
     * has been set, this method returns `null`.
     */
    val sharedPreferences: SharedPreferences?
        get() {
            if (preferenceDataStore != null) {
                return null
            }

            if (mSharedPreferences == null) {
                val storageContext = when (mStorage) {
                    STORAGE_DEVICE_PROTECTED -> ContextCompat.createDeviceProtectedStorageContext(context)
                    else -> context
                }
                mSharedPreferences = storageContext!!.getSharedPreferences(sharedPreferencesName,
                        sharedPreferencesMode)
            }

            return mSharedPreferences
        }

    /**
     * Returns an editor to use when modifying the shared preferences.
     *
     * Do NOT commit unless [shouldCommit] returns true.
     *
     * @return an editor to use to write to shared preferences. If a [PreferenceDataStore] has
     * been set, this method returns `null`.
     * @see shouldCommit
     */
    internal val editor: SharedPreferences.Editor?
        @SuppressLint("CommitPrefEdits")
        get() {
            if (preferenceDataStore != null) {
                return null
            }

            return if (mNoCommit) {
                if (mEditor == null) {
                    mEditor = sharedPreferences!!.edit()
                }
                mEditor
            } else {
                sharedPreferences!!.edit()
            }
        }

    init {
        sharedPreferencesName = getDefaultSharedPreferencesName(context)
    }

    /**
     * Inflates a preference hierarchy from XML. If a preference hierarchy is
     * given, the new preference hierarchies will be merged in.
     *
     * @param context The context of the resource.
     * @param resId The resource ID of the XML to inflate.
     * @param rootPreferences Optional existing hierarchy to merge the new hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's root).
     */
    @RestrictTo(LIBRARY_GROUP)
    fun inflateFromResource(context: Context, resId: Int, rootPreferences: PreferenceScreen?): PreferenceScreen {
        var rootPreferences = rootPreferences
        // Block commits
        setNoCommit(true)

        val inflater = PreferenceInflater(context, this)
        rootPreferences = inflater.inflate(resId, rootPreferences) as PreferenceScreen
        rootPreferences.onAttachedToHierarchy(this)

        // Unblock commits
        setNoCommit(false)

        return rootPreferences
    }

    fun createPreferenceScreen(context: Context): PreferenceScreen {
        val preferenceScreen = PreferenceScreen(context, null)
        preferenceScreen.onAttachedToHierarchy(this)
        return preferenceScreen
    }

    /**
     * Sets the storage location used internally by this class to be the default
     * provided by the hosting [Context].
     */
    fun setStorageDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            mStorage = STORAGE_DEFAULT
            mSharedPreferences = null
        }
    }

    /**
     * Explicitly set the storage location used internally by this class to be
     * device-protected storage.
     *
     * On devices with direct boot, data stored in this location is encrypted
     * with a key tied to the physical device, and it can be accessed
     * immediately after the device has booted successfully, both
     * *before and after* the user has authenticated with their
     * credentials (such as a lock pattern or PIN).
     *
     * Because device-protected data is available without user authentication,
     * you should carefully limit the data you store using this Context. For
     * example, storing sensitive authentication tokens or passwords in the
     * device-protected area is strongly discouraged.
     *
     * Prior to API 24 this method has no effect,
     * since device-protected storage is not available.
     *
     * @see Context.createDeviceProtectedStorageContext
     */
    fun setStorageDeviceProtected() {
        if (Build.VERSION.SDK_INT >= 24) {
            mStorage = STORAGE_DEVICE_PROTECTED
            mSharedPreferences = null
        }
    }

    /**
     * Sets the root of the preference hierarchy.
     *
     * @param preferenceScreen The root [PreferenceScreen] of the preference hierarchy.
     * @return Whether the [PreferenceScreen] given is different than the previous.
     */
    fun setPreferences(preferenceScreen: PreferenceScreen): Boolean {
        if (preferenceScreen != this.preferenceScreen) {
            this.preferenceScreen?.onDetached()
            this.preferenceScreen = preferenceScreen
            return true
        }

        return false
    }

    /**
     * Finds a [Preference] based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The [Preference] with the key, or null.
     * @see PreferenceGroup.findPreference
     */
    fun findPreference(key: CharSequence): Preference? {
        return if (preferenceScreen == null) {
            null
        } else preferenceScreen!!.findPreference(key)
    }

    /**
     * Whether it is the client's responsibility to commit on the
     * [editor]. This will return false in cases where the writes
     * should be batched, for example when inflating preferences from XML.
     *
     * If preferences are using [PreferenceDataStore] this value is irrelevant.
     *
     * @return Whether the client should commit.
     */
    internal fun shouldCommit(): Boolean {
        return !mNoCommit
    }

    private fun setNoCommit(noCommit: Boolean) {
        if (!noCommit && mEditor != null) {
            mEditor!!.apply()
        }
        mNoCommit = noCommit
    }

    /**
     * Called when a preference requests that a dialog be shown to complete a user interaction.
     *
     * @param preference The preference requesting the dialog.
     */
    fun showDialog(preference: Preference) {
        onDisplayPreferenceDialogListener?.onDisplayPreferenceDialog(preference)
    }

    /**
     * Callback class to be used by the [RecyclerView.Adapter]
     * associated with the [PreferenceScreen], used to determine when two [Preference]
     * objects are semantically and visually the same.
     */
    abstract class PreferenceComparisonCallback {
        /**
         * Called to determine if two [Preference] objects represent the same item
         *
         * @param p1 [Preference] object to compare
         * @param p2 [Preference] object to compare
         * @return `true` if the objects represent the same item
         */
        abstract fun arePreferenceItemsTheSame(p1: Preference, p2: Preference): Boolean

        /**
         * Called to determine if two [Preference] objects will display the same data
         *
         * @param p1 [Preference] object to compare
         * @param p2 [Preference] object to compare
         * @return `true` if the objects are visually identical
         */
        abstract fun arePreferenceContentsTheSame(p1: Preference, p2: Preference): Boolean
    }

    /**
     * A basic implementation of [PreferenceComparisonCallback] suitable for use with the
     * default [Preference] classes. If the [PreferenceScreen] contains custom
     * [Preference] subclasses, you must override
     * [.arePreferenceContentsTheSame]
     */
    class SimplePreferenceComparisonCallback : PreferenceComparisonCallback() {
        /**
         * This method will not be able to track replaced [Preference] objects if they
         * do not have a unique key.
         *
         * @see Preference.key
         */
        override fun arePreferenceItemsTheSame(p1: Preference, p2: Preference): Boolean {
            return p1.id == p2.id
        }

        /**
         * The result of this method is only valid for the default [Preference] objects,
         * and custom subclasses which do not override [Preference.onBindViewHolder].
         * This method also assumes that if a preference object is being replaced by a new instance,
         * the old instance was not modified after being removed from its containing [PreferenceGroup].
         */
        override fun arePreferenceContentsTheSame(p1: Preference, p2: Preference): Boolean {
            if (p1.javaClass != p2.javaClass) {
                return false
            }
            if (p1 === p2 && p1.wasDetached) {
                // Defensively handle the case where a preference was removed, updated and re-added.
                // Hopefully this is rare.
                return false
            }
            if (!TextUtils.equals(p1.title, p2.title)) {
                return false
            }
            if (!TextUtils.equals(p1.summary, p2.summary)) {
                return false
            }
            val p1Icon = p1.icon
            val p2Icon = p2.icon
            if (p1Icon !== p2Icon && (p1Icon == null || p1Icon !== p2Icon)) {
                return false
            }
            if (p1.isEnabled != p2.isEnabled) {
                return false
            }
            if (p1.isSelectable != p2.isSelectable) {
                return false
            }
            if (p1 is TwoStatePreference) {
                if (p1.isChecked != (p2 as TwoStatePreference).isChecked) {
                    return false
                }
            }
//            return if (p1 is DropDownPreference && p1 !== p2) {
//                // Different object, must re-bind spinner adapter
//                false
//            } else true
            return true
        }
    }

    /**
     * Interface definition for a callback to be invoked when a
     * [Preference] in the hierarchy rooted at this [PreferenceScreen] is clicked.
     */
    interface OnPreferenceTreeClickListener {
        /**
         * Called when a preference in the tree rooted at this [PreferenceScreen] has been clicked.
         *
         * @param preference The preference that was clicked.
         * @return Whether the click was handled.
         */
        fun onPreferenceTreeClick(preference: Preference): Boolean
    }

    /**
     * Interface definition for a class that will be called when a [Preference] requests to display a dialog.
     */
    interface OnDisplayPreferenceDialogListener {

        /**
         * Called when a preference in the tree requests to display a dialog.
         *
         * @param preference The Preference object requesting the dialog.
         */
        fun onDisplayPreferenceDialog(preference: Preference)
    }

    /**
     * Interface definition for a class that will be called when a [PreferenceScreen] requests navigation.
     */
    interface OnNavigateToScreenListener {

        /**
         * Called when a PreferenceScreen in the tree requests to navigate to its contents.
         *
         * @param preferenceScreen The PreferenceScreen requesting navigation.
         */
        fun onNavigateToScreen(preferenceScreen: PreferenceScreen)
    }

    companion object {

        const val KEY_HAS_SET_DEFAULT_VALUES = "hasSetDefaultValues"
        private const val STORAGE_DEFAULT = 0
        private const val STORAGE_DEVICE_PROTECTED = 1

        /**
         * Gets a SharedPreferences instance that points to the default file that is
         * used by the preference framework in the given context.
         *
         * @param context The context of the preferences whose values are wanted.
         * @return A `SharedPreferences` instance that can be used to retrieve and
         * listen to values of the preferences.
         */
        fun getDefaultSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                    defaultSharedPreferencesMode)
        }

        private fun getDefaultSharedPreferencesName(context: Context): String {
            return context.packageName + "_preferences"
        }

        private val defaultSharedPreferencesMode: Int
            get() = Context.MODE_PRIVATE
    }
}
