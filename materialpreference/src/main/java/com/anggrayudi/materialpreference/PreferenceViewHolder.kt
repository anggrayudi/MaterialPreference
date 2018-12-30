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

import android.util.SparseArray
import android.view.View

import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.ViewHolder] class which caches views associated with the default [Preference] layouts.
 * Cached views can be retrieved by calling [findViewById].
 */
class PreferenceViewHolder internal constructor(internal val itemView: View) {
    private val mCachedViews = SparseArray<View>(5)

    init {
        // Pre-cache the views that we know in advance we'll want to find
        mCachedViews.put(android.R.id.title, itemView.findViewById(android.R.id.title))
        mCachedViews.put(android.R.id.summary, itemView.findViewById(android.R.id.summary))
        mCachedViews.put(R.id.material_summary, itemView.findViewById(R.id.material_summary))
        mCachedViews.put(android.R.id.icon, itemView.findViewById(android.R.id.icon))
        mCachedViews.put(R.id.icon_frame, itemView.findViewById(R.id.icon_frame))
        mCachedViews.put(android.R.id.widget_frame, itemView.findViewById(android.R.id.widget_frame))
    }

    /**
     * Returns a cached reference to a subview managed by this object. If the view reference is not
     * yet cached, it falls back to calling [View.findViewById] and caches the result.
     *
     * @param id Resource ID of the view to find
     * @return The view, or null if no view with the requested ID is found.
     */
    fun findViewById(@IdRes id: Int): View? {
        val cachedView = mCachedViews.get(id)
        return if (cachedView != null) {
            cachedView
        } else {
            val v = itemView.findViewById<View>(id)
            if (v != null) {
                mCachedViews.put(id, v)
            }
            v
        }
    }

    companion object {

        @RestrictTo(RestrictTo.Scope.TESTS)
        fun createInstanceForTests(itemView: View): PreferenceViewHolder {
            return PreferenceViewHolder(itemView)
        }
    }
}
