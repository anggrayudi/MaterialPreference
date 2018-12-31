/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.anggrayudi.materialpreference.dialog

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.anggrayudi.materialpreference.MultiSelectListPreference
import java.util.*

class MultiSelectListPreferenceDialogFragment : PreferenceDialogFragment() {

    private val mNewValues = HashSet<String>()
    private var mPreferenceChanged: Boolean = false
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null
    private var selectedIndices: IntArray? = null

    private val listPreference: MultiSelectListPreference
        get() = preference as MultiSelectListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val preference = listPreference

            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                        "MultiSelectListPreference requires an entries array and an entryValues array.")
            }

            mNewValues.clear()
            mNewValues.addAll(preference.value!!)
            mPreferenceChanged = false
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mNewValues.clear()
            mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES)!!)
            mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false)
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(mNewValues))
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        val integers = ArrayList<Int>(mEntryValues!!.size)
        for (i in mEntryValues!!.indices) {
            if (mNewValues.contains(mEntryValues!![i].toString()))
                integers.add(i)
        }
        val checkedItems = IntArray(integers.size) {integers[it]}

        val listItem = mutableListOf<String>()
        mEntries!!.forEach { listItem.add(it.toString()) }

        mPreferenceChanged = false
        return dialog
                .positiveButton(text = mPositiveButtonText ?: getString(android.R.string.ok)) {
                    mWhichButtonClicked = WhichButton.POSITIVE
                }
                .negativeButton(text = mNegativeButtonText ?: getString(android.R.string.cancel)) {
                    mWhichButtonClicked = WhichButton.NEGATIVE
                }
                .listItemsMultiChoice(items = listItem,
                        initialSelection = checkedItems,
                        disabledIndices = getDisabledIndices(),
                        waitForPositiveButton = false,
                        allowEmptySelection = true) { _, _, items ->
                    mPreferenceChanged = true
                    mNewValues.clear()
                    mNewValues.addAll(items)
                }
    }

    private fun getDisabledIndices(): IntArray? {
        val e = listPreference.disabledEntryValues
        if (e != null && e.size <= mEntryValues!!.size) {
            val a = mutableListOf<Int>()
            for (item in mEntryValues!!.withIndex()) {
                if (e.contains(item.value))
                    a.add(item.index)
            }
            return a.toIntArray()
        }
        return null
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mPreferenceChanged) {
            val preference = listPreference
            val values = mNewValues
            if (preference.callChangeListener(values)) {
                preference.value = values
            }
        }
        mPreferenceChanged = false
    }

    companion object {

        private const val SAVE_STATE_VALUES = "MultiSelectListPreferenceDialogFragment.values"
        private const val SAVE_STATE_CHANGED = "MultiSelectListPreferenceDialogFragment.changed"
        private const val SAVE_STATE_ENTRIES = "MultiSelectListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "MultiSelectListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): MultiSelectListPreferenceDialogFragment {
            val fragment = MultiSelectListPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragment.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
