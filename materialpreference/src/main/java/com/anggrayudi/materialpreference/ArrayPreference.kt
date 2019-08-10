package com.anggrayudi.materialpreference

import androidx.annotation.ArrayRes

/**
 * @see ListPreference
 * @see MultiSelectListPreference
 */
internal interface ArrayPreference<EntryValuesType> {

    /**
     * Sets the human-readable entries to be shown in the list. This will be
     * shown in subsequent dialogs.
     *
     * Each entry must have a corresponding index in [entryValues].
     *
     * @return The list of entries to be shown in the list in subsequent dialogs.
     * @see entryValues
     */
    var entries: Array<CharSequence>?

    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     *
     * @return The array of values to be saved for the preference.
     */
    var entryValues: Array<EntryValuesType>?

    /**
     * @param entriesResId The entries array as a resource.
     * @see entries
     */
    fun setEntries(@ArrayRes entriesResId: Int)

    /**
     * @param entryValuesResId The entry values array as a resource.
     * @see entryValues
     */
    fun setEntryValues(@ArrayRes entryValuesResId: Int)

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    fun <T> findIndexOfValue(value: T?): Int {
        if (value != null && entryValues != null) {
            for (i in entryValues!!.indices.reversed()) {
                if (entryValues!![i] == value) {
                    return i
                }
            }
        }
        return -1
    }
}