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
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.content.res.TypedArrayUtils

/**
 * Represents a top-level [Preference] that is the root of a Preference hierarchy.
 * A [PreferenceFragmentMaterial] points to an instance of this class to show the preferences.
 * To instantiate this class, use [PreferenceManager.createPreferenceScreen].
 *
 * This class can appear in two places:
 *  *  When a [PreferenceFragmentMaterial] points to this, it is used as the root
 * and is not shown (only the contained preferences are shown).
 *  *  When it appears inside another preference hierarchy, it is shown and
 * serves as the gateway to another screen of preferences (either by showing
 * another screen of preferences as a [android.app.Dialog] or via a
 * [Context.startActivity] from the [Preference.intent]). The children of this [PreferenceScreen]
 * are NOT shown in the screen that this [PreferenceScreen] is shown in.
 * Instead, a separate screen will be shown when this preference is clicked.
 */
@SuppressLint("RestrictedApi")
class PreferenceScreen
/** Do NOT use this constructor, use [PreferenceManager.createPreferenceScreen]. */
@RestrictTo(LIBRARY_GROUP)
@Keep constructor(
    context: Context,
    attrs: AttributeSet?
) : PreferenceGroup(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceScreenStyle, android.R.attr.preferenceScreenStyle)) {

    override var isOnSameScreenAsChildren = false
        get() = false

    public override fun onClick() {
        if (intent != null || fragment != null || preferenceCount == 0) {
            return
        }
        preferenceManager!!.onNavigateToScreenListener?.onNavigateToScreen(this)
    }

    override fun toString(): String = title.toString()
}
