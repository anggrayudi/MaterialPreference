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
import java.util.*

class ListPreferenceDialogFragment : PreferenceDialogFragment() {

    private var clickedDialogEntryIndex: Int = 0
    private var entries: Array<CharSequence>? = null
    private var entryValues: Array<CharSequence>? = null

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException("ListPreference requires an entries array and an entryValues array.")
            }

            clickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            entryValues = preference.entryValues
            entries = preference.entries
        } else {
            clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            entryValues = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRY_VALUES)
            entries = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRIES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
        putCharSequenceArray(outState, SAVE_STATE_ENTRY_VALUES, entryValues!!)
        putCharSequenceArray(outState, SAVE_STATE_ENTRIES, entries!!)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        val listItem = mutableListOf<String>()
        entries!!.forEach { listItem.add(it.toString()) }
        return dialog.listItemsSingleChoice(items = listItem, initialSelection = clickedDialogEntryIndex,
                        waitForPositiveButton = false, disabledIndices = getDisabledIndices()) { _, index, _ ->
            clickedDialogEntryIndex = index
            whichButtonClicked = WhichButton.POSITIVE
            dialog.dismiss()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && clickedDialogEntryIndex >= 0) {
            listPreference.value = entryValues!![clickedDialogEntryIndex].toString()
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

        private fun putCharSequenceArray(out: Bundle, key: String, entries: Array<CharSequence>) {
            val stored = ArrayList<String>(entries.size)
            entries.forEach { stored.add(it.toString()) }
            out.putStringArrayList(key, stored)
        }

        private fun getCharSequenceArray(`in`: Bundle, key: String): Array<CharSequence>? {
            val stored = `in`.getStringArrayList(key)
            return stored?.toTypedArray()
        }
    }
}
