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

import android.os.Handler
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ListUpdateCallback
import com.anggrayudi.materialpreference.util.applyTint
import com.anggrayudi.materialpreference.util.getAttrColor
import com.anggrayudi.materialpreference.util.getSupportDrawable
import java.util.*

/**
 * An adapter that connects a RecyclerView to the [Preference] objects contained in the
 * associated [PreferenceGroup].
 */
@RestrictTo(LIBRARY_GROUP)
internal class PreferenceGroupAdapter internal constructor(
        private val fragment: PreferenceFragmentMaterial,
        /** The group that we are providing data from. */
        private val preferenceGroup: PreferenceGroup,
        private val rootParent: ViewGroup)
    : Preference.OnPreferenceChangeInternalListener, ListUpdateCallback {

    /**
     * Maps a position into this adapter -> [Preference]. These
     * [Preference]s don't have to be direct children of this
     * [PreferenceGroup], they can be grand children or younger)
     */
    private var preferenceList: List<Preference>? = null

    /**
     * Contains a sorted list of all preferences in this adapter regardless of visibility. This is
     * used to construct [preferenceList]
     */
    private var preferenceListInternal: List<Preference>? = null

    /** List of unique Preference and its subclasses' names and layouts. */
    private val preferenceLayouts: MutableList<PreferenceLayout>

    private var tempPreferenceLayout = PreferenceLayout()

    private val handler = Handler()

    private val syncRunnable = { syncMyPreferences() }

    private class PreferenceLayout {
        internal var resId: Int = 0
        internal var widgetResId: Int = 0
        internal var name: String? = null

        internal constructor()

        internal constructor(other: PreferenceLayout) {
            resId = other.resId
            widgetResId = other.widgetResId
            name = other.name
        }

        override fun equals(other: Any?): Boolean {
            return other is PreferenceLayout
                    && resId == other.resId
                    && widgetResId == other.widgetResId
                    && name == other.name
        }

        override fun hashCode(): Int {
            var result = 17
            result = 31 * result + resId
            result = 31 * result + widgetResId
            result = 31 * result + name.hashCode()
            return result
        }
    }

    init {
        // If this group gets or loses any children, let us know
        preferenceGroup.onPreferenceChangeInternalListener = this
        preferenceList = ArrayList()
        preferenceListInternal = ArrayList()
        preferenceLayouts = ArrayList()
        syncMyPreferences()
    }

    private fun syncMyPreferences() {
        preferenceListInternal!!.forEach {
            // Clear out the listeners in anticipation of some items being removed. This listener
            // will be (re-)added to the remaining prefs when we flatten.
            it.onPreferenceChangeInternalListener = null
        }
        val fullPreferenceList = ArrayList<Preference>(preferenceListInternal!!.size)
        flattenPreferenceGroup(fullPreferenceList, preferenceGroup)

        preferenceList = fullPreferenceList
        preferenceListInternal = fullPreferenceList

        notifyDataSetChanged()

        fullPreferenceList.forEach { it.wasDetached = false }
    }

    private fun flattenPreferenceGroup(preferences: MutableList<Preference>, group: PreferenceGroup) {
        group.sortPreferences()

        for (i in 0 until group.preferenceCount) {
            val preference = group.getPreference(i)

            preferences.add(preference)

            addPreferenceClassName(preference)

            if (preference is PreferenceGroup) {
                if (preference.isOnSameScreenAsChildren) {
                    flattenPreferenceGroup(preferences, preference)
                }
            }

            preference.onPreferenceChangeInternalListener = this
        }
    }

    /**
     * Creates a string that includes the preference name, layout id and widget layout id.
     * If a particular preference type uses 2 different resources, they will be treated as
     * different view types.
     */
    private fun createPreferenceLayout(preference: Preference, `in`: PreferenceLayout?): PreferenceLayout {
        val pl = `in` ?: PreferenceLayout()
        pl.name = preference.javaClass.name
        pl.resId = preference.layoutResource
        pl.widgetResId = preference.widgetLayoutResource
        return pl
    }

    private fun addPreferenceClassName(preference: Preference) {
        val pl = createPreferenceLayout(preference, null)
        if (!preferenceLayouts.contains(pl)) {
            preferenceLayouts.add(pl)
        }
    }

    private fun getItem(position: Int): Preference? {
        return if (position < 0 || position >= preferenceList!!.size) null else preferenceList!![position]
    }

    override fun onPreferenceChange(preference: Preference) {
        val index = preferenceList!!.indexOf(preference)
        // If we don't find the preference, we don't need to notify anyone
        if (index != -1) {
            // Send the pref object as a placeholder to ensure the view holder is recycled in place
            onItemChanged(index)
        }
    }

    override fun onPreferenceHierarchyChange(preference: Preference) {
        handler.removeCallbacks(syncRunnable)
        handler.post(syncRunnable)
    }

    override fun onPreferenceVisibilityChange(preference: Preference) {
        if (!preferenceListInternal!!.contains(preference)) {
            return
        }
        preference.preferenceViewHolder?.itemView?.visibility = if (preference.isVisible) View.VISIBLE else View.GONE
    }

    private fun getItemViewType(position: Int): Int {
        val preference = getItem(position)

        tempPreferenceLayout = createPreferenceLayout(preference!!, tempPreferenceLayout)

        var viewType = preferenceLayouts.indexOf(tempPreferenceLayout)
        return if (viewType != -1) {
            viewType
        } else {
            viewType = preferenceLayouts.size
            preferenceLayouts.add(PreferenceLayout(tempPreferenceLayout))
            viewType
        }
    }

    private fun createViewHolder(viewType: Int, preference: Preference): PreferenceViewHolder {
        val pl = preferenceLayouts[viewType]
        val context = preferenceGroup.context
        val a = context.obtainStyledAttributes(null, R.styleable.BackgroundStyle)
        val background = a.getSupportDrawable(context, R.styleable.BackgroundStyle_android_selectableItemBackground)
                ?: context.getSupportDrawable(android.R.drawable.list_selector_background)
        a.recycle()

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(pl.resId, getParentView(preference), false)
        if (view.background == null) {
            ViewCompat.setBackground(view, background)
        }

        val widgetFrame = view.findViewById<ViewGroup>(android.R.id.widget_frame)
        if (widgetFrame != null) {
            if (pl.widgetResId != 0) {
                inflater.inflate(pl.widgetResId, widgetFrame)
            }
        }

        return PreferenceViewHolder(view)
    }

    private fun onItemChanged(position: Int) {
        getItem(position)?.run {
            if (preferenceViewHolder == null) {
                preferenceViewHolder = createViewHolder(getItemViewType(position), this)
                if (this is PreferenceScreen) {
                    val summaryIcon = preferenceViewHolder!!.findViewById(R.id.summary_icon) as ImageView
                    summaryIcon.drawable.applyTint(context.getAttrColor(android.R.attr.textColorSecondary))
                }
                preferenceViewHolder!!.itemView.visibility = if (isVisible) View.VISIBLE else View.GONE
                getParentView(this).addView(preferenceViewHolder!!.itemView)
                onBindViewHolder(preferenceViewHolder!!)
                onSetupFinished(this@PreferenceGroupAdapter.fragment)
                return
            }
            onBindViewHolder(preferenceViewHolder!!)
        }
    }

    private fun notifyDataSetChanged() {
        preferenceList?.indices?.forEach { onItemChanged(it) }
    }

    private fun getParentView(preference: Preference): ViewGroup {
        if (preference.parent == null || preference is PreferenceCategory)
            return rootParent

        if (preference.parent!!.preferenceViewHolder == null) {
            var message = ("Make sure that you wrap ${preference.javaClass.simpleName}"
                    + " inside PreferenceCategory in the XML.")
            if (preference.key != null)
                message += " Key=\"${preference.key}\""

            throw InflateException(message)
        }

        return preference.parent!!.preferenceViewHolder!!.itemView.findViewById(android.R.id.content)
    }

    override fun onInserted(position: Int, count: Int) {
        Log.d(TAG, "onInserted: $position")
    }

    override fun onRemoved(position: Int, count: Int) {
        Log.d(TAG, "onRemoved: $position")
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        Log.d(TAG, "onMoved: fromPosition $fromPosition => toPosition $toPosition")
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        Log.d(TAG, "onChanged: " + position + ", payload " + payload.toString())
    }

    companion object {

        private const val TAG = "PreferenceGroupAdapter"
    }
}
