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
import androidx.annotation.StringRes
import com.anggrayudi.materialpreference.widget.AspSwitchCompat

/**
 * A [Preference] that provides a two-state toggleable option.
 *
 * This preference will store a boolean into the SharedPreferences.
 *
 *      |            Attribute           | Value Type |
 *      |:------------------------------:|:----------:|
 *      | android:summaryOff             | String     |
 *      | android:summaryOn              | String     |
 *      | android:disableDependentsState | Boolean    |
 *      | android:switchTextOff          | String     |
 *      | android:switchTextOn           | String     |
 *
 * @see TwoStatePreference
 * @see CheckBoxPreference
 */
@SuppressLint("RestrictedApi")
open class SwitchPreference @Keep @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.switchPreferenceStyle,
    defStyleRes: Int = 0
) : TwoStatePreference(context, attrs, defStyleAttr, defStyleRes) {

    private val listener = Listener()

    /**
     * Set the text displayed on the switch widget in the on state.
     * This should be a very short string; one word if possible.
     *
     * @return The text that will be displayed on the switch widget in the on state
     */
    var switchTextOn: CharSequence?
        get() = _switchTextOn
        set(onText) {
            _switchTextOn = onText
            notifyChanged()
        }
    private var _switchTextOn: CharSequence? = null

    /**
     * Set the text displayed on the switch widget in the off state.
     * This should be a very short string; one word if possible.
     *
     * @return The text that will be displayed on the switch widget in the off state
     */
    var switchTextOff: CharSequence?
        get() = _switchTextOff
        set(offText) {
            _switchTextOff = offText
            notifyChanged()
        }
    private var _switchTextOff: CharSequence? = null

    private inner class Listener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.isChecked = !isChecked
                return
            }
            this@SwitchPreference.isChecked = isChecked
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreference,
            defStyleAttr, defStyleRes)
        _switchTextOn = a.getString(R.styleable.SwitchPreference_android_switchTextOn)
        _switchTextOff = a.getString(R.styleable.SwitchPreference_android_switchTextOff)
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val switchView = holder.findViewById(R.id.switchWidget)
        syncSwitchView(switchView)
        syncSummaryView(holder)
    }

    /**
     * Set the text displayed on the switch widget in the on state.
     * This should be a very short string; one word if possible.
     *
     * @param resId The text as a string resource ID
     */
    fun setSwitchTextOn(@StringRes resId: Int) {
        switchTextOn = context.getText(resId)
    }

    /**
     * Set the text displayed on the switch widget in the off state.
     * This should be a very short string; one word if possible.
     *
     * @param resId The text as a string resource ID
     */
    fun setSwitchTextOff(@StringRes resId: Int) {
        switchTextOff = context.getText(resId)
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

        val switchView = view.findViewById<View>(R.id.switchWidget)
        syncSwitchView(switchView)

        val summaryView = view.findViewById<View>(android.R.id.summary)
        syncSummaryView(summaryView)
    }

    private fun syncSwitchView(view: View?) {
        if (view is AspSwitchCompat) {
            val switchView = view as AspSwitchCompat?
            switchView!!.setOnCheckedChangeListener(null)
        }
        if (view is Checkable) {
            (view as Checkable).isChecked = isChecked
        }
        if (view is AspSwitchCompat) {
            val switchView = view as AspSwitchCompat?
            switchView!!.textOn = switchTextOn
            switchView.textOff = switchTextOff
            switchView.setOnCheckedChangeListener(listener)
        }
    }
}
