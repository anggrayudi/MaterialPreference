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

    private var mClickedDialogEntryIndex: Int = 0
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException("ListPreference requires an entries array and an entryValues array.")
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntryValues = preference.entryValues
            mEntries = preference.entries
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntryValues = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRY_VALUES)
            mEntries = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRIES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        putCharSequenceArray(outState, SAVE_STATE_ENTRY_VALUES, mEntryValues!!)
        putCharSequenceArray(outState, SAVE_STATE_ENTRIES, mEntries!!)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        val listItem = mutableListOf<String>()
        mEntries!!.forEach { listItem.add(it.toString()) }
        return dialog.listItemsSingleChoice(items = listItem, initialSelection = mClickedDialogEntryIndex,
                        waitForPositiveButton = false, disabledIndices = getDisabledIndices()) { _, index, _ ->
            mClickedDialogEntryIndex = index
            mWhichButtonClicked = WhichButton.POSITIVE
            dialog.dismiss()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            listPreference.value = mEntryValues!![mClickedDialogEntryIndex].toString()
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

    companion object {

        private const val SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): ListPreferenceDialogFragment {
            val b = Bundle(1)
            b.putString(PreferenceDialogFragment.ARG_KEY, key)
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
