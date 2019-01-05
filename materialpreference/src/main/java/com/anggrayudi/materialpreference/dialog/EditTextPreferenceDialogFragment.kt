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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.anggrayudi.materialpreference.EditTextPreference
import com.anggrayudi.materialpreference.R
import com.google.android.material.textfield.TextInputLayout

class EditTextPreferenceDialogFragment : PreferenceDialogFragment() {

    private var mEditText: EditText? = null
    private var mTextInputLayout: TextInputLayout? = null
    private var mTextMessage: TextView? = null

    private var mText: CharSequence? = null

    private val editTextPreference: EditTextPreference
        get() = preference as EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mText = if (savedInstanceState == null) {
            editTextPreference.value
        } else {
            savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TEXT, mText)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog
                .positiveButton(text = mPositiveButtonText ?: getString(android.R.string.ok)) {
                    mWhichButtonClicked = WhichButton.POSITIVE
                }.negativeButton(text = mNegativeButtonText ?: getString(android.R.string.cancel)) {
                    mWhichButtonClicked = WhichButton.NEGATIVE
                }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val preference = editTextPreference
        mTextMessage = view.findViewById(android.R.id.message)
        mTextInputLayout = view.findViewById(android.R.id.inputArea)
        mEditText = view.findViewById(android.R.id.edit)

        mTextMessage!!.text = preference.message
        if (TextUtils.isEmpty(preference.message))
            mTextMessage!!.visibility = View.GONE

        mTextInputLayout!!.isCounterEnabled = preference.isCounterEnabled
        mTextInputLayout!!.counterMaxLength = preference.maxLength

        if (preference.inputFilters != null)
            mEditText!!.filters = preference.inputFilters

        mEditText!!.hint = preference.hint
        mEditText!!.inputType = preference.inputType
        mEditText!!.setText(mText)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(text: CharSequence?, i: Int, i1: Int, i2: Int) {
                val underMinChars = text == null || preference.minLength > 0 && text.length < preference.minLength

                (dialog as MaterialDialog).getActionButton(WhichButton.POSITIVE).isEnabled =
                        !underMinChars && text!!.length <= preference.maxLength

                mTextInputLayout!!.error = if (underMinChars && preference.minLength > 0)
                    getString(R.string.min_preference_input_chars_, preference.minLength)
                else
                    null
            }

            override fun afterTextChanged(editable: Editable) {}
        }
        mEditText!!.addTextChangedListener(textWatcher)
        mEditText!!.post { textWatcher.onTextChanged(mText, 0, 0, 0) }
    }

    @RestrictTo(LIBRARY_GROUP)
    override fun needInputMethod(): Boolean {
        // We want the input method to show, if possible, when dialog is displayed
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            editTextPreference.value = mEditText!!.text.toString()
        }
    }

    companion object {

        private const val SAVE_STATE_TEXT = "EditTextPreferenceDialogFragment.text"

        fun newInstance(key: String): EditTextPreferenceDialogFragment {
            val fragment = EditTextPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragment.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
