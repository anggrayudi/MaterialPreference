package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
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
 *       |       Attribute     |   Value Type              |
 *       |:-------------------:|:-------------------------:|
 *       | android:entries     | String or integer array   |
 *       | android:entryValues | Integer array             |
 *       | app:entryIcons      | Drawable array            |
 */
@SuppressLint("RestrictedApi")
open class IntegerListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
        android.R.attr.dialogPreferenceStyle),
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes), ArrayPreference<Int> {

    override var entries: Array<CharSequence>?
        get() = _entries
        set(value) {
            _entries = value
        }
    private var _entries: Array<CharSequence>? = null

    override var entryValues: Array<Int>?
        get() = _entryValues
        set(value) {
            _entryValues = value
        }
    private var _entryValues: Array<Int>? = null

    /** Get or set value to this preference */
    var value: Int
        get() = _value
        set(v) {
            if (_value != v && callChangeListener(v)) {
                _value = v
                // Always persist/notify the first time.
                persistInt(v)
                if (isBindValueToSummary) {
                    summary = summaryFormatter?.invoke(entry, v.toString()) ?: entry
                }
            }
        }
    private var _value: Int = 0

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
                summary = f?.invoke(entry, value.toString()) ?: entry
            }
        }

    var disabledEntryValues: Array<Int>? = null

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or null.
     */
    val entry: CharSequence?
        get() {
            val index = findIndexOfValue(value)
            return if (index >= 0 && entries != null) entries!![index] else null
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListPreference,
            defStyleAttr, defStyleRes)

        val entries = a.getTextArray(R.styleable.ListPreference_android_entries)
        _entries = if (entries != null && entries[0] != null)
            entries
        else {
            val entriesResId = a.getResourceId(R.styleable.ListPreference_android_entries, 0)
            context.resources.getIntArray(entriesResId)
                .map { it.toString() }
                .toTypedArray()
        }

        val entryValuesResId = a.getResourceId(R.styleable.ListPreference_android_entryValues, 0)
        _entryValues = context.resources.getIntArray(entryValuesResId).toTypedArray()

        a.recycle()

        negativeButtonText = null
        positiveButtonText = null
    }

    override fun setEntries(@ArrayRes entriesResId: Int) {
        val e = context.resources.getTextArray(entriesResId)
        entries = if (e[0] != null)
            e
        else {
            // the array resource ID contains integer-array
            context.resources.getIntArray(entriesResId)
                .map { it.toString() }
                .toTypedArray()
        }
    }

    override fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        entryValues = context.resources.getIntArray(entryValuesResId).toTypedArray()
    }

    override fun onSetInitialValue() {
        _value = getPersistedInt(value)
        if (isBindValueToSummary)
            summary = summaryFormatter?.invoke(entry, _value.toString()) ?: entry
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

    private class SavedState : BaseSavedState {
        internal var value: Int = 0

        constructor(source: Parcel) : super(source) {
            value = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(value)
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
