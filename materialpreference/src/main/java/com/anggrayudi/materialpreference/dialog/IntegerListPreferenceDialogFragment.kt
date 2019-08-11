package com.anggrayudi.materialpreference.dialog

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.anggrayudi.materialpreference.IntegerListPreference
import com.anggrayudi.materialpreference.dialog.ListPreferenceDialogFragment.Companion.getCharSequenceArray
import com.anggrayudi.materialpreference.dialog.ListPreferenceDialogFragment.Companion.putCharSequenceArray

class IntegerListPreferenceDialogFragment : PreferenceDialogFragment() {

    private var clickedDialogEntryIndex: Int = 0
    private var entries: Array<CharSequence>? = null
    private var entryValues: Array<Int>? = null

    private val listPreference: IntegerListPreference
        get() = preference as IntegerListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                    "IntegerListPreference requires an entries array and an entryValues array.")
            }

            clickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            entryValues = preference.entryValues
            entries = preference.entries
        } else {
            clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            entryValues = savedInstanceState.getIntArray(SAVE_STATE_ENTRY_VALUES)?.toTypedArray()
            entries = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRIES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
        outState.putIntArray(SAVE_STATE_ENTRY_VALUES, entryValues!!.toIntArray())
        putCharSequenceArray(outState, SAVE_STATE_ENTRIES, entries!!)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog.listItemsSingleChoice(
            items = entries!!.map { it.toString() },
            initialSelection = clickedDialogEntryIndex,
            waitForPositiveButton = false,
            disabledIndices = getDisabledIndices()) { _, index, _ ->
            clickedDialogEntryIndex = index
            whichButtonClicked = WhichButton.POSITIVE
            dialog.dismiss()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && clickedDialogEntryIndex >= 0) {
            listPreference.value = entryValues!![clickedDialogEntryIndex]
        }
    }

    private fun getDisabledIndices(): IntArray? {
        val e = listPreference.disabledEntryValues
        if (e != null && e.size <= entryValues!!.size) {
            return entryValues!!.withIndex()
                .filter { e.contains(it.value) }
                .map { it.index }
                .toIntArray()
        }
        return null
    }

    companion object {

        private const val SAVE_STATE_INDEX = "IntegerListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "IntegerListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "IntegerListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): IntegerListPreferenceDialogFragment {
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            val fragment = IntegerListPreferenceDialogFragment()
            fragment.arguments = b
            return fragment
        }
    }
}
