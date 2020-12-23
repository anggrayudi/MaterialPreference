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
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Checkable
import android.widget.CompoundButton
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.content.res.TypedArrayUtils

/**
 * A [Preference] that provides checkbox widget functionality.
 *
 * This preference will store a boolean into the SharedPreferences.
 *
 *      |            Attribute           | Value Type |
 *      |:------------------------------:|:----------:|
 *      | android:summaryOff             | String     |
 *      | android:summaryOn              | String     |
 *      | android:disableDependentsState | Boolean    |
 *
 * @see TwoStatePreference
 * @see SwitchPreference
 */
@SuppressLint("RestrictedApi")
open class CheckBoxPreference @Keep @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.checkBoxPreferenceStyle,
        android.R.attr.checkBoxPreferenceStyle),
    defStyleRes: Int = 0
) : TwoStatePreference(context, attrs, defStyleAttr, defStyleRes) {

    private val listener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (!callChangeListener(isChecked)) {
            // Listener didn't like it, change it back.
            // CompoundButton will make sure we don't recurse.
            buttonView.isChecked = !isChecked
            return@OnCheckedChangeListener
        }
        this@CheckBoxPreference.isChecked = isChecked
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        syncCheckboxView(holder.findViewById(android.R.id.checkbox))
        syncSummaryView(holder)
    }

    @RestrictTo(LIBRARY_GROUP)
    override fun performClick(view: View) {
        super.performClick(view)
        syncViewIfAccessibilityEnabled(view)
    }

    private fun syncViewIfAccessibilityEnabled(view: View) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!accessibilityManager.isEnabled) {
            return
        }

        val checkboxView = view.findViewById<View>(android.R.id.checkbox)
        syncCheckboxView(checkboxView)

        val summaryView = view.findViewById<View>(android.R.id.summary)
        syncSummaryView(summaryView)
    }

    private fun syncCheckboxView(view: View?) {
        if (view is CompoundButton) {
            view.setOnCheckedChangeListener(null)
        }
        if (view is Checkable) {
            (view as Checkable).isChecked = isChecked
        }
        if (view is CompoundButton) {
            view.setOnCheckedChangeListener(listener)
        }
    }
}
