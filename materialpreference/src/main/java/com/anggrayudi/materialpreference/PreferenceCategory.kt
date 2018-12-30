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

import androidx.core.content.res.TypedArrayUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat

/**
 * Used to group [Preference] objects and provide a disabled title above the group.
 */
@SuppressLint("RestrictedApi")
class PreferenceCategory @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.preferenceCategoryStyle,
                android.R.attr.preferenceCategoryStyle), defStyleRes: Int = 0)
    : PreferenceGroup(context, attrs, defStyleAttr, defStyleRes) {

    override var isEnabled: Boolean
        get() = false
        set(value) {
            super.isEnabled = value
        }

    override fun onPrepareAddPreference(preference: Preference): Boolean {
        if (preference is PreferenceCategory) {
            throw IllegalArgumentException("Cannot add a $TAG directly to a $TAG")
        }
        return super.onPrepareAddPreference(preference)
    }

    override fun shouldDisableDependents(): Boolean {
        return !super.isEnabled
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfo(info)
        val existingItemInfo = info.collectionItemInfo ?: return
        val newItemInfo = CollectionItemInfoCompat.obtain(
                existingItemInfo.rowIndex,
                existingItemInfo.rowSpan,
                existingItemInfo.columnIndex,
                existingItemInfo.columnSpan,
                true /* heading */,
                existingItemInfo.isSelected)
        info.setCollectionItemInfo(newItemInfo)
    }

    companion object {
        private const val TAG = "PreferenceCategory"
    }
}
