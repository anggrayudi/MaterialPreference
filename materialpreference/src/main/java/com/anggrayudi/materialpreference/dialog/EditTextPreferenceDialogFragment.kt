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

    private var editText: EditText? = null
    private var textInputLayout: TextInputLayout? = null
    private var textMessage: TextView? = null
    private var text: CharSequence? = null

    private val editTextPreference: EditTextPreference
        get() = preference as EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = if (savedInstanceState == null) {
            editTextPreference.value
        } else {
            savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TEXT, text)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog
                .positiveButton(text = positiveButtonText ?: getString(android.R.string.ok)) {
                    whichButtonClicked = WhichButton.POSITIVE
                }.negativeButton(text = negativeButtonText ?: getString(android.R.string.cancel)) {
                    whichButtonClicked = WhichButton.NEGATIVE
                }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val preference = editTextPreference
        textMessage = view.findViewById(android.R.id.message)
        textInputLayout = view.findViewById(android.R.id.inputArea)
        editText = view.findViewById(android.R.id.edit)

        textMessage!!.text = preference.message
        if (preference.message.isNullOrEmpty())
            textMessage!!.visibility = View.GONE

        textInputLayout!!.isCounterEnabled = preference.isCounterEnabled
        textInputLayout!!.counterMaxLength = preference.maxLength

        if (preference.inputFilters != null)
            editText!!.filters = preference.inputFilters

        editText!!.hint = preference.hint
        editText!!.inputType = preference.inputType
        editText!!.setText(text)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(text: CharSequence?, i: Int, i1: Int, i2: Int) {
                val underMinChars = text == null || preference.minLength > 0 && text.length < preference.minLength

                (dialog as MaterialDialog).getActionButton(WhichButton.POSITIVE).isEnabled =
                        !underMinChars && text!!.length <= preference.maxLength

                textInputLayout!!.error = if (underMinChars && preference.minLength > 0)
                    getString(R.string.min_preference_input_chars_, preference.minLength)
                else
                    null
            }

            override fun afterTextChanged(editable: Editable) {}
        }
        editText!!.addTextChangedListener(textWatcher)
        editText!!.post { textWatcher.onTextChanged(text, 0, 0, 0) }
    }

    @RestrictTo(LIBRARY_GROUP)
    override fun needInputMethod(): Boolean {
        // We want the input method to show, if possible, when dialog is displayed
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            editTextPreference.value = editText!!.text.toString()
        }
    }

    companion object {

        private const val SAVE_STATE_TEXT = "EditTextPreferenceDialogFragment.text"

        fun newInstance(key: String): EditTextPreferenceDialogFragment {
            val fragment = EditTextPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
