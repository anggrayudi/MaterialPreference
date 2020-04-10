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

package com.anggrayudi.materialpreference.dialog

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.anggrayudi.materialpreference.ListPreference

class ListPreferenceDialogFragment : PreferenceDialogFragment() {

    private var clickedDialogEntryIndex: Int = 0
    private lateinit var entries: Array<CharSequence>
    private lateinit var entryValues: Array<String>

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.")
            }

            clickedDialogEntryIndex = preference.findIndexOfValue(preference.value, preference.defaultValue)
            entryValues = preference.entryValues!!
            entries = preference.entries!!
        } else {
            clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            entryValues = savedInstanceState.getStringArrayList(SAVE_STATE_ENTRY_VALUES)!!.toTypedArray()
            entries = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRIES)!!
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
        outState.putStringArrayList(SAVE_STATE_ENTRY_VALUES, ArrayList(entryValues.toList()))
        putCharSequenceArray(outState, SAVE_STATE_ENTRIES, entries)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog.listItemsSingleChoice(
            items = entries.map { it.toString() },
            initialSelection = clickedDialogEntryIndex,
            waitForPositiveButton = false,
            disabledIndices = getDisabledIndices(listPreference.disabledEntryValues, entryValues)) { _, index, _ ->
            clickedDialogEntryIndex = index
            whichButtonClicked = WhichButton.POSITIVE
            dialog.dismiss()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && clickedDialogEntryIndex >= 0) {
            listPreference.value = entryValues[clickedDialogEntryIndex]
        }
    }

    companion object {

        private const val SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): ListPreferenceDialogFragment {
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            val fragment = ListPreferenceDialogFragment()
            fragment.arguments = b
            return fragment
        }

        internal fun getDisabledIndices(e: Array<CharSequence>?, entryValues: Array<String>): IntArray? {
            if (e != null && e.size <= entryValues.size) {
                return entryValues.withIndex()
                    .filter { e.contains(it.value) }
                    .map { it.index }
                    .toIntArray()
            }
            return null
        }

        internal fun putCharSequenceArray(out: Bundle, key: String, entries: Array<CharSequence>) {
            val stored = ArrayList<String>(entries.size)
            entries.forEach { stored.add(it.toString()) }
            out.putStringArrayList(key, stored)
        }

        internal fun getCharSequenceArray(`in`: Bundle, key: String): Array<CharSequence>? =
            `in`.getStringArrayList(key)?.toTypedArray()
    }
}
