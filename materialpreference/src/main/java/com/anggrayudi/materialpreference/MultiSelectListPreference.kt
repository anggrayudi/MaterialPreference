/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License.
 */

package com.anggrayudi.materialpreference

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.ArrayRes
import com.anggrayudi.materialpreference.dialog.DialogPreference
import com.anggrayudi.materialpreference.util.ArraySummaryFormatter
import java.util.*
import kotlin.collections.HashSet

/**
 * A [Preference] that displays a list of entries as a dialog.
 *
 * This preference will store a set of strings into the SharedPreferences.
 * This set will contain one or more values from the [entryValues] array.
 *
 *      |      Attribute      |   Value Type   |
 *      |:-------------------:|:--------------:|
 *      | android:entries     | String array   |
 *      | android:entryValues | String array   |
 *      | app:entryIcons      | Drawable array |
 *      | app:summaryNothing  | String         |
 */
class MultiSelectListPreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.multiSelectListPreferenceStyle,
        defStyleRes: Int = R.style.Preference_DialogPreference)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes), ArrayPreference<Set<String>> {

    override var entries: Array<CharSequence>? = null
    override var entryValues: Array<CharSequence>? = null
    var disabledEntryValues: Array<CharSequence>? = null

    private val values = HashSet<String>()
    private var _nothingText: CharSequence? = null

    override var value: Set<String>?
        get() = values
        set(v) {
            values.clear()
            if (v != null) {
                values.addAll(v)
            }
            if (persistStringSet(v ?: HashSet()) && isBindValueToSummary) {
                summary = when {
                    value!!.isEmpty() -> if (summaryFormatter == null) nothingText else summaryFormatter!!.invoke(value!!.toTypedArray())
                    summaryFormatter != null -> summaryFormatter!!.invoke(value!!.toTypedArray())
                    else -> "${value!!.size}/${entryValues!!.size}"
                }
            }
        }

    /**
     * Lets you control how value should be displayed to summary.
     */
    var summaryFormatter: ArraySummaryFormatter? = null
        set(f) {
            field = f
            if (isBindValueToSummary) {
                summary = when {
                    value!!.isEmpty() -> if (f == null) nothingText else f.invoke(value!!.toTypedArray())
                    f != null -> f.invoke(value!!.toTypedArray())
                    else -> "${value!!.size}/${entryValues!!.size}"
                }
            }
        }

    /**
     * When value is bound to the summary and there is nothing selected in this [MultiSelectListPreference],
     * the 'Nothing' text will be shown as summary.
     */
    var nothingText: CharSequence?
        get() = _nothingText
        set(nothing) {
            _nothingText = nothing
            if (isBindValueToSummary) {
                summary = when {
                    value!!.isEmpty() -> if (summaryFormatter == null) nothing else summaryFormatter!!.invoke(value!!.toTypedArray())
                    summaryFormatter != null -> summaryFormatter!!.invoke(value!!.toTypedArray())
                    else -> "${value!!.size}/${entryValues!!.size}"
                }
            }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectListPreference, defStyleAttr, defStyleRes)
        entries = a.getTextArray(R.styleable.MultiSelectListPreference_android_entries)
        entryValues = a.getTextArray(R.styleable.MultiSelectListPreference_android_entryValues)
        _nothingText = a.getText(R.styleable.MultiSelectListPreference_summaryNothing)
        a.recycle()
    }

    /**
     * @param entriesResId The entries array as a resource.
     * @see entries
     */
    override fun setEntries(@ArrayRes entriesResId: Int) {
        entries = context.resources.getTextArray(entriesResId)
    }

    /**
     * @param entryValuesResId The entry values array as a resource.
     * @see entryValues
     */
    override fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        entryValues = context.resources.getTextArray(entryValuesResId)
    }

    fun setPrettySummaryFormatter() {
        val text = HashSet<String>(values.size)
        val values = TreeSet(values)
        values.forEach { text.add(entries!![findIndexOfValue(it)].toString()) }
        summaryFormatter = {
            val summ = text.toString()
            if (values.isEmpty() && nothingText != null)
                nothingText.toString()
            else
                summ.substring(1, summ.length - 1)
        }
    }

    override fun onSetInitialValue() {
        values.addAll(getPersistedStringSet(value)!!)
        if (isBindValueToSummary) {
            summary = when {
                value!!.isEmpty() -> if (summaryFormatter == null) nothingText else summaryFormatter!!.invoke(value!!.toTypedArray())
                summaryFormatter != null -> summaryFormatter!!.invoke(value!!.toTypedArray())
                else -> "${value!!.size}/${entryValues!!.size}"
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()!!
        if (isPersistent) {
            // No need to save instance state
            return superState
        }

        val myState = SavedState(superState)
        myState.values = value
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state!!.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        value = myState.values
    }

    private class SavedState : BaseSavedState {
        internal var values: Set<String>? = null

        constructor(source: Parcel) : super(source) {
            values = HashSet()
            val strings = source.createStringArray()
            Collections.addAll(values as? HashSet<String>, *strings)
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeStringArray(values?.toTypedArray())
        }

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
