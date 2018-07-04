/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.dialog.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiSelectListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_VALUES =
            "MultiSelectListPreferenceDialogFragmentCompat.values";
    private static final String SAVE_STATE_CHANGED =
            "MultiSelectListPreferenceDialogFragmentCompat.changed";
    private static final String SAVE_STATE_ENTRIES =
            "MultiSelectListPreferenceDialogFragmentCompat.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "MultiSelectListPreferenceDialogFragmentCompat.entryValues";

    private Set<String> mNewValues = new HashSet<>();
    private boolean mPreferenceChanged;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public static MultiSelectListPreferenceDialogFragmentCompat newInstance(String key) {
        final MultiSelectListPreferenceDialogFragmentCompat fragment =
                new MultiSelectListPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final MultiSelectListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException(
                        "MultiSelectListPreference requires an entries array and " +
                                "an entryValues array.");
            }

            mNewValues.clear();
            mNewValues.addAll(preference.getValues());
            mPreferenceChanged = false;
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mNewValues.clear();
            mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES));
            mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    private MultiSelectListPreference getListPreference() {
        return (MultiSelectListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(MaterialDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        List<Integer> integers = new ArrayList<>(mEntryValues.length);
        for (int i = 0; i < mEntryValues.length; i++) {
            if (mNewValues.contains(mEntryValues[i].toString()))
                integers.add(i);
        }
        Integer[] checkedItems = new Integer[integers.size()];
        for (int i = 0; i < integers.size(); i++) {
            checkedItems[i] = integers.get(i);
        }

        final MultiSelectListPreference.ListValueEvaluator evaluator = getListPreference().mEvaluator;
        builder.autoDismiss(false)
                .neutralText(R.string.clear)
                .items(mEntries)
                .alwaysCallMultiChoiceCallback()
                .itemsCallbackMultiChoice(checkedItems, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        if (evaluator == null || evaluator.evaluate(which, text)) {
                            mPreferenceChanged = true;
                            mNewValues.clear();
                            for (int i : which)
                                mNewValues.add(mEntryValues[i].toString());

                            return true;
                        }
                        return false;
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mNewValues.clear();
                        mPreferenceChanged = true;
                        dialog.setSelectedIndices(new Integer[0]);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mPreferenceChanged = false;
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        MultiSelectListPreferenceDialogFragmentCompat.this.onClick(dialog, DialogAction.POSITIVE);
                        dialog.dismiss();
                    }
                });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final MultiSelectListPreference preference = getListPreference();
        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (preference.callChangeListener(values)) {
                preference.setValues(values);
            }
        }
        mPreferenceChanged = false;
    }
}
