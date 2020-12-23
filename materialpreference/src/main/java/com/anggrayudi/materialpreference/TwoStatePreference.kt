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

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.StringRes

/**
 * Common base class for preferences that have two selectable states, persist a
 * boolean value in SharedPreferences, and may have dependent preferences that are
 * enabled/disabled based on the current state.
 */
abstract class TwoStatePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Sets the summary to be shown when checked.
     *
     * @return The summary to be shown when checked.
     */
    var summaryOn: CharSequence?
        get() = _summaryOn
        set(summary) {
            _summaryOn = summary
            if (isChecked) {
                notifyChanged()
            }
        }
    private var _summaryOn: CharSequence? = null

    /**
     * Sets the summary to be shown when unchecked.
     *
     * @return The summary to be shown when unchecked.
     */
    var summaryOff: CharSequence?
        get() = _summaryOff
        set(summary) {
            _summaryOff = summary
            if (!isChecked) {
                notifyChanged()
            }
        }
    private var _summaryOff: CharSequence? = null

    private var checkedSet = false

    /**
     * Sets whether dependents are disabled when this preference is on (`true`)
     * or when this preference is off (`false`).
     *
     * @return Whether dependents are disabled when this preference is on (`true`)
     * or when this preference is off (`false`).
     */
    var disableDependentsState = false

    /**
     * Sets the checked state and saves it to the [android.content.SharedPreferences].
     *
     * @return The checked state.
     */
    // Always persist/notify the first time; don't assume the field's default of false.
    var isChecked: Boolean
        get() = _checked
        set(checked) {
            val changed = _checked != checked
            if (changed || !checkedSet) {
                _checked = checked
                checkedSet = true
                persistBoolean(checked)
                if (changed) {
                    notifyDependencyChange(shouldDisableDependents())
                    notifyChanged()
                }
            }
        }
    private var _checked = false

    override var isLegacySummary: Boolean
        get() = true
        set(value) {
            super.isLegacySummary = value
        }

    var defaultValue = false
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TwoStatePreference, defStyleAttr, defStyleRes)
        disableDependentsState = a.getBoolean(R.styleable.TwoStatePreference_android_disableDependentsState, false)
        _summaryOn = a.getString(R.styleable.TwoStatePreference_android_summaryOn)
        _summaryOff = a.getString(R.styleable.TwoStatePreference_android_summaryOff)
        defaultValue = a.getBoolean(R.styleable.Preference_android_defaultValue, false)
        a.recycle()
    }

    override fun onClick() {
        super.onClick()

        val newValue = !isChecked
        if (callChangeListener(newValue)) {
            isChecked = newValue
        }
    }

    override fun shouldDisableDependents(): Boolean {
        val shouldDisable = if (disableDependentsState) _checked else !_checked
        return shouldDisable || super.shouldDisableDependents()
    }

    override fun onSetInitialValue() {
        isChecked = getPersistedBoolean(defaultValue)
    }

    /**
     * @see summaryOn
     * @param summaryResId The summary as a resource.
     */
    fun setSummaryOn(@StringRes summaryResId: Int) {
        summaryOn = context.getString(summaryResId)
    }

    /**
     * @see summaryOff
     * @param summaryResId The summary as a resource.
     */
    fun setSummaryOff(@StringRes summaryResId: Int) {
        summaryOff = context.getString(summaryResId)
    }

    /**
     * Sync a summary holder contained within holder's sub-hierarchy with the correct summary text.
     * @param holder [PreferenceViewHolder] which holds a reference to the summary view
     */
    protected fun syncSummaryView(holder: PreferenceViewHolder) {
        // Sync the summary holder
        val view = holder.findViewById(android.R.id.summary)
        syncSummaryView(view)
    }

    @RestrictTo(LIBRARY_GROUP)
    protected fun syncSummaryView(view: View?) {
        if (view !is TextView) {
            return
        }
        val summaryView = view as TextView?
        var useDefaultSummary = true
        if (_checked && !summaryOn.isNullOrEmpty()) {
            summaryView!!.text = summaryOn
            useDefaultSummary = false
        } else if (!_checked && !summaryOff.isNullOrEmpty()) {
            summaryView!!.text = summaryOff
            useDefaultSummary = false
        }
        if (useDefaultSummary) {
            val summary = summary
            if (!summary.isNullOrEmpty()) {
                summaryView!!.text = summary
                useDefaultSummary = false
            }
        }
        var newVisibility = View.GONE
        if (!useDefaultSummary) {
            // Someone has written to it
            newVisibility = View.VISIBLE
        }
        if (newVisibility != summaryView!!.visibility) {
            summaryView.visibility = newVisibility
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState!!)
        myState.checked = isChecked
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        isChecked = myState.checked
    }

    internal class SavedState : BaseSavedState {
        var checked: Boolean = false

        constructor(source: Parcel) : super(source) {
            checked = source.readInt() == 1
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(if (checked) 1 else 0)
        }

        constructor(superState: Parcelable) : super(superState)

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
