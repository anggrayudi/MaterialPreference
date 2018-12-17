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

package com.anggrayudi.materialpreference;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.dialog.PreferenceDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

public class EditTextPreferenceDialogFragment extends PreferenceDialogFragment {

    private static final String SAVE_STATE_TEXT = "EditTextPreferenceDialogFragment.text";

    private EditText mEditText;
    private TextInputLayout mTextInputLayout;
    private TextView mTextMessage;

    private CharSequence mText;

    public static EditTextPreferenceDialogFragment newInstance(String key) {
        final EditTextPreferenceDialogFragment fragment = new EditTextPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mText = getEditTextPreference().getText();
        } else {
            mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(SAVE_STATE_TEXT, mText);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final EditTextPreference preference = getEditTextPreference();
        mTextMessage = view.findViewById(android.R.id.message);
        mTextInputLayout = view.findViewById(android.R.id.inputArea);
        mEditText = view.findViewById(android.R.id.edit);

        mTextMessage.setText(preference.getMessage());
        if (TextUtils.isEmpty(preference.getMessage()))
            mTextMessage.setVisibility(View.GONE);

        mTextInputLayout.setCounterEnabled(preference.isCounterEnabled());
        mTextInputLayout.setCounterMaxLength(preference.getMaxLength());

        if (preference.mInputFilters != null)
            mEditText.setFilters(preference.mInputFilters);

        mEditText.setHint(preference.getHint());
        mEditText.setInputType(preference.getInputType());
        mEditText.setText(mText);

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence text, int i, int i1, int i2) {
                boolean underMinChars = text == null || preference.getMinLength() > 0
                        && text.length() < preference.getMinLength();

                ((MaterialDialog) getDialog()).getActionButton(DialogAction.POSITIVE)
                        .setEnabled(!underMinChars && text.length() <= preference.getMaxLength());

                mTextInputLayout.setError(underMinChars
                        ? getString(R.string.min_preference_input_chars_, preference.getMinLength()) : null);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        mEditText.addTextChangedListener(textWatcher);
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                textWatcher.onTextChanged(mText, 0, 0, 0);
            }
        });
    }

    private EditTextPreference getEditTextPreference() {
        return (EditTextPreference) getPreference();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    protected boolean needInputMethod() {
        // We want the input method to show, if possible, when dialog is displayed
        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (getEditTextPreference().callChangeListener(value)) {
                getEditTextPreference().setText(value);
            }
        }
    }


}
