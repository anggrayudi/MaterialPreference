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

    private val newValues = HashSet<String>()
    private var preferenceChanged: Boolean = false
    private var entries: Array<CharSequence>? = null
    private var entryValues: Array<String>? = null

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

            newValues.clear()
            newValues.addAll(preference.value!!)
            preferenceChanged = false
            entries = preference.entries
            entryValues = preference.entryValues
        } else {
            newValues.clear()
            newValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES)!!)
            preferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false)
            entries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            entryValues = savedInstanceState.getStringArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(newValues))
        outState.putBoolean(SAVE_STATE_CHANGED, preferenceChanged)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, entries)
        outState.putStringArray(SAVE_STATE_ENTRY_VALUES, entryValues)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        val integers = ArrayList<Int>(entryValues!!.size)
        entryValues!!.indices.forEach {
            if (newValues.contains(entryValues!![it]))
                integers.add(it)
        }
        val checkedItems = IntArray(integers.size) { integers[it] }
        val listItem = entries!!.map { it.toString() }

        preferenceChanged = false
        return dialog
                .positiveButton(text = positiveButtonText ?: getString(android.R.string.ok)) {
                    whichButtonClicked = WhichButton.POSITIVE
                }
                .negativeButton(text = negativeButtonText ?: getString(android.R.string.cancel)) {
                    whichButtonClicked = WhichButton.NEGATIVE
                }
                .listItemsMultiChoice(items = listItem,
                        initialSelection = checkedItems,
                        disabledIndices = getDisabledIndices(),
                        waitForPositiveButton = false,
                        allowEmptySelection = true) { d, _, items ->
                    if (d.isShowing) {
                        preferenceChanged = true
                        newValues.clear()
                        newValues.addAll(items)
                    }
                }
    }

    private fun getDisabledIndices(): IntArray? {
        val e = listPreference.disabledEntryValues
        if (e != null && e.size <= entryValues!!.size) {
            val a = mutableListOf<Int>()
            for (item in entryValues!!.withIndex()) {
                if (e.contains(item.value))
                    a.add(item.index)
            }
            return a.toIntArray()
        }
        return null
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && preferenceChanged) {
            val preference = listPreference
            val values = newValues
            if (preference.callChangeListener(values)) {
                preference.value = values
            }
        }
        preferenceChanged = false
    }

    companion object {

        private const val SAVE_STATE_VALUES = "MultiSelectListPreferenceDialogFragment.values"
        private const val SAVE_STATE_CHANGED = "MultiSelectListPreferenceDialogFragment.changed"
        private const val SAVE_STATE_ENTRIES = "MultiSelectListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "MultiSelectListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): MultiSelectListPreferenceDialogFragment {
            val fragment = MultiSelectListPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
