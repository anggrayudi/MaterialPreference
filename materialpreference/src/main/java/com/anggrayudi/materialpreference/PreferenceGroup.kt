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
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.collection.SimpleArrayMap
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList

/**
 * A container for multiple [Preference] objects. It is a base class for  Preference objects that are
 * parents, such as [PreferenceCategory] and [PreferenceScreen].
 *
 *      |        Attribute        | Value Type |
 *      |:-----------------------:|:----------:|
 *      | android:orderingFromXml | Boolean    |
 */
@SuppressLint("RestrictedApi")
abstract class PreferenceGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * The container for child [Preference]s. This is sorted based on the
     * ordering, please use [addPreference] instead of adding to this directly.
     */
    private val preferenceList: MutableList<Preference> = ArrayList()

    /**
     * Whether to order the [Preference] children of this group as they
     * are added. If this is false, the ordering will follow each Preference
     * order and default to alphabetic for those without an order.
     *
     * If this is called after preferences are added, they will not be
     * re-ordered in the order they were added, hence call this method early on.
     *
     * @return Whether this group orders based on the order the children are added.
     * @see Preference.order
     */
    var isOrderingAsAdded = true

    private var currentPreferenceOrder = 0

    /** Returns true if we're between [onAttached] and [onPrepareForRemoval] */
    @get:RestrictTo(LIBRARY_GROUP)
    var isAttached = false
        private set

    private val idRecycleCache = SimpleArrayMap<String, Long>()
    private val handler = Handler()
    private val clearRecycleCacheRunnable = object : Runnable {
        override fun run() {
            synchronized(this) {
                idRecycleCache.clear()
            }
        }
    }

    /**
     * Returns the number of children [Preference]s.
     * @return The number of preference children in this group.
     */
    val preferenceCount: Int
        get() = preferenceList.size

    /**
     * Whether this preference group should be shown on the same screen as its contained preferences.
     *
     * @return True if the contained preferences should be shown on the same screen as this preference.
     */
    internal open var isOnSameScreenAsChildren = true

    override var isLegacySummary: Boolean
        get() = true
        set(value) {
            super.isLegacySummary = value
        }

    init {
        isPersistent = false
        val a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceGroup,
            defStyleAttr, defStyleRes)
        isOrderingAsAdded = a.getBoolean(R.styleable.PreferenceGroup_android_orderingFromXml, true)
        a.recycle()
    }

    /** Called by the inflater to add an item to this group.  */
    fun addItemFromInflater(preference: Preference) {
        addPreference(preference)
    }

    /**
     * Returns the [Preference] at a particular index.
     *
     * @param index The index of the [Preference] to retrieve.
     * @return The [Preference].
     */
    fun getPreference(index: Int): Preference {
        return preferenceList[index]
    }

    /**
     * Adds a [Preference] at the correct position based on the preference's order.
     *
     * @param preference The preference to add.
     * @return Whether the preference is now in this group.
     */
    fun addPreference(preference: Preference): Boolean {
        if (preferenceList.contains(preference)) {
            // Exists
            return true
        }

        if (preference.order == DEFAULT_ORDER) {
            if (isOrderingAsAdded) {
                preference.order = currentPreferenceOrder++
            }

            if (preference is PreferenceGroup) {
                // TODO: fix (method is called tail recursively when inflating,
                // so we won't end up properly passing this flag down to children
                preference.isOrderingAsAdded = isOrderingAsAdded
            }
        }

        var insertionIndex = Collections.binarySearch(preferenceList, preference)
        if (insertionIndex < 0) {
            insertionIndex = insertionIndex * -1 - 1
        }

        if (!onPrepareAddPreference(preference)) {
            return false
        }

        synchronized(this) {
            preferenceList.add(insertionIndex, preference)
        }

        val preferenceManager = preferenceManager
        val key = preference.key
        val id = if (key != null && idRecycleCache.containsKey(key)) {
            idRecycleCache.remove(key)!!
        } else {
            preferenceManager!!.nextId
        }
        preference.onAttachedToHierarchy(preferenceManager!!, id)
        preference.assignParent(this)

        if (isAttached) {
            preference.onAttached()
        }

        notifyHierarchyChanged()
        return true
    }

    /**
     * Removes a [Preference] from this group.
     *
     * @param preference The preference to remove.
     * @return Whether the preference was found and removed.
     */
    fun removePreference(preference: Preference): Boolean {
        val returnValue = removePreferenceInt(preference)
        notifyHierarchyChanged()
        return returnValue
    }

    private fun removePreferenceInt(preference: Preference): Boolean {
        synchronized(this) {
            preference.onPrepareForRemoval()
            if (preference.parent === this) {
                preference.assignParent(null)
            }
            val success = preferenceList.remove(preference)
            if (success) {
                // If this preference, or another preference with the same key, gets re-added
                // immediately, we want it to have the same id so that it can be correctly tracked
                // in the adapter by RecyclerView, to make it appear as if it has only been
                // seamlessly updated. If the preference is not re-added by the time the handler
                // runs, we take that as a signal that the preference will not be re-added soon
                // in which case it does not need to retain the same id.

                // If two (or more) preferences have the same (or null) key and both are removed
                // and then re-added, only one id will be recycled and the second (and later)
                // preferences will receive a newly generated id. This use pattern of the preference
                // API is strongly discouraged.
                val key = preference.key
                if (key != null) {
                    idRecycleCache.put(key, preference.id)
                    handler.removeCallbacks(clearRecycleCacheRunnable)
                    handler.post(clearRecycleCacheRunnable)
                }
                if (isAttached) {
                    preference.onDetached()
                }
            }
            return success
        }
    }

    /** Removes all [Preference]s from this group. */
    fun removeAll() {
        synchronized(this) {
            val preferenceList = preferenceList
            for (i in preferenceList.indices.reversed()) {
                removePreferenceInt(preferenceList[0])
            }
        }
        notifyHierarchyChanged()
    }

    /**
     * Prepares a [Preference] to be added to the group.
     *
     * @param preference The preference to add.
     * @return Whether to allow adding the preference (true), or not (false).
     */
    protected open fun onPrepareAddPreference(preference: Preference): Boolean {
        preference.onParentChanged(this, shouldDisableDependents())
        return true
    }

    /**
     * Finds a [Preference] based on its key. If two [Preference]
     * share the same key (not recommended), the first to appear will be
     * returned (to retrieve the other preference with the same key, call this
     * method on the first preference). If this preference has the key, it will not be returned.
     *
     * This will recursively search for the preference into children that are also [PreferenceGroup]s.
     *
     * @param key The key of the preference to retrieve.
     * @return The [Preference] with the key, or null.
     */
    fun findPreference(key: CharSequence): Preference? {
        if (key == this.key) {
            return this
        }
        for (i in 0 until preferenceCount) {
            val preference = getPreference(i)
            val curKey = preference.key

            if (curKey != null && curKey == key) {
                return preference
            }

            if (preference is PreferenceGroup) {
                val returnedPreference = preference.findPreference(key)
                if (returnedPreference != null) {
                    return returnedPreference
                }
            }
        }
        return null
    }

    override fun onAttached() {
        super.onAttached()

        // Mark as attached so if a preference is later added to this group, we
        // can tell it we are already attached
        isAttached = true

        // Dispatch to all contained preferences
        for (i in 0 until preferenceCount) {
            getPreference(i).onAttached()
        }
    }

    override fun onDetached() {
        super.onDetached()

        // We won't be attached to the activity anymore
        isAttached = false

        // Dispatch to all contained preferences
        for (i in 0 until preferenceCount) {
            getPreference(i).onDetached()
        }
    }

    override fun notifyDependencyChange(disableDependents: Boolean) {
        super.notifyDependencyChange(disableDependents)

        // Child preferences have an implicit dependency on their containing
        // group. Dispatch dependency change to all contained preferences.
        for (i in 0 until preferenceCount) {
            getPreference(i).onParentChanged(this, disableDependents)
        }
    }

    internal fun sortPreferences() {
        synchronized(this) {
            preferenceList.sort()
        }
    }

    override fun dispatchSaveInstanceState(container: Bundle) {
        super.dispatchSaveInstanceState(container)

        // Dispatch to all contained preferences
        for (i in 0 until preferenceCount) {
            getPreference(i).dispatchSaveInstanceState(container)
        }
    }

    override fun dispatchRestoreInstanceState(container: Bundle) {
        super.dispatchRestoreInstanceState(container)

        // Dispatch to all contained preferences
        for (i in 0 until preferenceCount) {
            getPreference(i).dispatchRestoreInstanceState(container)
        }
    }

    /**
     * Interface for PreferenceGroup Adapters to implement so that
     * [PreferenceFragmentMaterial.scrollToPreference] and
     * [PreferenceFragmentMaterial.scrollToPreference] or
     * [PreferenceFragmentMaterial.scrollToPreference] and
     * [PreferenceFragmentMaterial.scrollToPreference]
     * can determine the correct scroll position to request.
     */
    interface PreferencePositionCallback {

        /**
         * Return the adapter position of the first [Preference] with the specified key
         * @param key Key of [Preference] to find
         * @return Adapter position of the [Preference] or [RecyclerView.NO_POSITION] if not found
         */
        fun getPreferenceAdapterPosition(key: String): Int

        /**
         * Return the adapter position of the specified [Preference] object
         * @param preference [Preference] object to find
         * @return Adapter position of the [Preference] or [RecyclerView.NO_POSITION] if not found
         */
        fun getPreferenceAdapterPosition(preference: Preference): Int
    }
}
