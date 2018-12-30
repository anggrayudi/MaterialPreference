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
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import androidx.annotation.ArrayRes
import androidx.core.content.res.TypedArrayUtils
import com.anggrayudi.materialpreference.dialog.DialogPreference
import com.anggrayudi.materialpreference.util.EntrySummaryFormatter

/**
 * A [Preference] that displays a list of entries as a dialog.
 *
 * This preference will store a string into the SharedPreferences.
 * This string will be the value from the [entryValues] array.
 *
 *       |       Attribute     |   Value Type   |
 *       |:-------------------:|:--------------:|
 *       | android:entries     | String array   |
 *       | android:entryValues | String array   |
 *       | app:entryIcons      | Drawable array |
 */
@SuppressLint("RestrictedApi")
class ListPreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle), defStyleRes: Int = 0)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes), ArrayPreference<String> {

    override var entries: Array<CharSequence>? = null
    override var entryValues: Array<CharSequence>? = null

    // TODO 27-Dec-18: Periksa field 'value' pada semua preference
    /**
     * Sets the value of the key. This should be one of the entries in [entryValues].
     *
     * @return The value of the key. This should be one of the entries in [entryValues].
     */
    // Always persist/notify the first time.
    override var value: String?
        get() = _value
        set(v) {
            val changed = !TextUtils.equals(value, v)
            if (changed && callChangeListener(v)) {
                _value = v
                persistString(v)
                if (changed && isBindValueToSummary) {
                    summary = summaryFormatter?.invoke(entry.toString(), v) ?: entry.toString()
                }
            }
        }
    private var _value: String? = null

    /**
     * Lets you control how displaying value to summary. Suppose that the selected entry is
     * **Weekly**, then you set:
     *
     *     listPreference.summaryFormatter = { entry, value -> "Value for $entry is $value" }
     *
     * It will produce **Value for Weekly is 7** to the summary.
     *
     * With this callback you don't need to set "%s" or "%1$s" to summary.
     */
    var summaryFormatter: EntrySummaryFormatter? = null
        set(f) {
            field = f
            if (isBindValueToSummary) {
                summary = f?.invoke(entry.toString(), value) ?: entry.toString()
            }
        }

    var disabledEntryValues: Array<CharSequence>? = null

    /**
     * Sets the summary for this Preference with a CharSequence.
     * If the summary has a [String.format] marker in it (i.e. "%s" or "%1$s"),
     * then the current entry value will be substituted in its place when it's retrieved.
     */
    override var summary: CharSequence?
        get() {
            val entry = entry
            return if (_summary == null) {
                super.summary
            } else {
                String.format(_summary!!, entry ?: "")
            }
        }
        set(summary) {
            super.summary = summary
            if (summary == null && _summary != null) {
                _summary = null
            } else if (summary != null && summary != _summary) {
                _summary = summary.toString()
            }
        }
    private var _summary: String? = null

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or null.
     */
    val entry: CharSequence?
        get() {
            val index = valueIndex
            return if (index >= 0 && entries != null) entries!![index] else null
        }

    /** Sets the value to the given index from the entry values. */
    private var valueIndex: Int
        get() = findIndexOfValue(value)
        set(index) {
            if (entryValues != null) {
                value = entryValues!![index].toString()
            }
        }

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes)
        entries = a.getTextArray(R.styleable.ListPreference_android_entries)
        entryValues = a.getTextArray(R.styleable.ListPreference_android_entryValues)
        a.recycle()

        /* Retrieve the Preference summary attribute since it's private
         * in the Preference class.
         */
        a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes)
        _summary = a.getString(R.styleable.Preference_android_summary)
        if (isBindValueToSummary && TextUtils.isEmpty(_summary))
            _summary = "%s"

        a.recycle()

        negativeButtonText = null
        positiveButtonText = null
    }


    override fun setEntries(@ArrayRes entriesResId: Int) {
        entries = context.resources.getTextArray(entriesResId)
    }

    override fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        entryValues = context.resources.getTextArray(entryValuesResId)
    }

    override fun onSetInitialValue() {
        _value = getPersistedString(value)
        if (isBindValueToSummary)
            summary = summaryFormatter?.invoke(entry.toString(), _value) ?: entry.toString()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState!!)
        myState.value = value
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
        value = myState.value
    }

    private class SavedState : Preference.BaseSavedState {
        internal var value: String? = null

        constructor(source: Parcel) : super(source) {
            value = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        constructor(superState: Parcelable) : super(superState)

        companion object CREATOR: Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
