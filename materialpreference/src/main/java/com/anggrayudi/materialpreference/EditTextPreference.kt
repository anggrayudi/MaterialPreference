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
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import androidx.annotation.Keep
import androidx.core.content.res.TypedArrayUtils
import com.anggrayudi.materialpreference.callback.OnBindTextInputLayoutListener
import com.anggrayudi.materialpreference.dialog.DialogPreference
import com.anggrayudi.materialpreference.util.StringSummaryFormatter

/*
 * HOW TO CREATE TABLE IN KOTLIN DOCS, DAMMIT? IT DOES NOT WORK AT ALL.
 * [https://stackoverflow.com/q/53894870/3922207]
 * Workaround => https://www.tablesgenerator.com/markdown_tables
 */
/**
 * A [Preference] that allows for string input.
 * It is a subclass of [DialogPreference] and shows the [EditText] in a dialog.
 * This preference will store a string into the SharedPreferences.
 *
 * All attributes from [DialogPreference] are usable here.
 *
 *      |       Attribute       |         Value Type         |
 *      |:---------------------:|:--------------------------:|
 *      | android:hint          | String                     |
 *      | android:inputType     | android.text.InputType Int |
 *      | android:dialogMessage | String                     |
 *      | android:maxLength     | Int                        |
 *      | app:minLength         | Int                        |
 *      | app:counterEnabled    | Boolean                    |
 */
@SuppressLint("RestrictedApi")
open class EditTextPreference @Keep @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle, android.R.attr.editTextPreferenceStyle),
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Saves the text to the [android.content.SharedPreferences].
     * @return The current preference value.
     */
    var value: String?
        get() = _text
        set(value) {
            if (callChangeListener(value)) {
                val wasBlocking = shouldDisableDependents()

                _text = value

                persistString(value)

                val isBlocking = shouldDisableDependents()
                if (isBlocking != wasBlocking) {
                    notifyDependencyChange(isBlocking)
                }
                updateSummary()
            }
        }
    private var _text: String? = null

    /**
     * Lets you control how displaying value to summary. Suppose that the given value is
     * **Anggrayudi H**, then you set:
     *
     *     editTextPreference.summaryFormatter = { "Your name is $it" }
     *
     * It will produce **Your name is Anggrayudi H** to the summary.
     */
    var summaryFormatter: StringSummaryFormatter? = null
        set(f) {
            field = f
            updateSummary()
        }

    var onBindTextInputLayoutListener: OnBindTextInputLayoutListener? = null

    var hint: String? = null

    var message: String? = null

    var inputType: Int = 0

    var maxLength: Int = 0

    var minLength: Int = 0

    var isCounterEnabled: Boolean = false

    var inputFilters: Array<InputFilter>? = null

    var defaultValue: String? = null
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.EditTextPreference, defStyleAttr, defStyleRes)
        isCounterEnabled = a.getBoolean(R.styleable.EditTextPreference_counterEnabled, true)
        hint = a.getString(R.styleable.EditTextPreference_android_hint)
        message = a.getString(R.styleable.EditTextPreference_android_dialogMessage)
        maxLength = a.getInt(R.styleable.EditTextPreference_android_maxLength, 100)
        minLength = a.getInt(R.styleable.EditTextPreference_minLength, 0)
        inputType = a.getInt(R.styleable.EditTextPreference_android_inputType, InputType.TYPE_CLASS_TEXT)
        defaultValue = a.getString(R.styleable.Preference_android_defaultValue)
        a.recycle()
    }

    private fun updateSummary() {
        if (isBindValueToSummary) {
            summary = summaryFormatter?.invoke(value) ?: value
        }
    }

    override fun onSetInitialValue() {
        _text = getPersistedString(defaultValue)
        updateSummary()
    }

    override fun shouldDisableDependents(): Boolean {
        return value.isNullOrEmpty() || super.shouldDisableDependents()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState!!)
        myState.text = value
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
        _text = myState.text
    }

    private class SavedState : BaseSavedState {
        internal var text: String? = null

        internal constructor(source: Parcel) : super(source) {
            text = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(text)
        }

        internal constructor(superState: Parcelable) : super(superState)

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
