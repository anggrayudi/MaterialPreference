/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License.
 */

package com.anggrayudi.materialpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import com.anggrayudi.materialpreference.dialog.DialogPreference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog.
 * <p></p>
 * This preference will store a set of strings into the SharedPreferences.
 * This set will contain one or more values from the
 * {@link #setEntryValues(CharSequence[])} array.
 */
public class MultiSelectListPreference extends DialogPreference {
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Set<String> mValues = new HashSet<>();
    private CharSequence mNothing;

    ListValueEvaluator mEvaluator;

    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_DialogPreference);
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.multiSelectListPreferenceStyle);
    }

    public MultiSelectListPreference(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectListPreference, defStyleAttr, defStyleRes);
        mEntries = a.getTextArray(R.styleable.MultiSelectListPreference_android_entries);
        mEntryValues = a.getTextArray(R.styleable.MultiSelectListPreference_android_entryValues);
        mNothing = a.getText(R.styleable.MultiSelectListPreference_summaryNothing);
        a.recycle();
        Log.d("MultiSelect", "init: " + mNothing);
    }

    /**
     * Sets the human-readable entries to be shown in the list. This will be
     * shown in subsequent dialogs.
     * <p></p>
     * Each entry must have a corresponding index in
     * {@link #setEntryValues(CharSequence[])}.
     *
     * @param entries The entries.
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    /**
     * @param entriesResId The entries array as a resource.
     * @see #setEntries(CharSequence[])
     */
    public void setEntries(int entriesResId) {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    /**
     * The list of entries to be shown in the list in subsequent dialogs.
     *
     * @return The list as an array.
     */
    public CharSequence[] getEntries() {
        return mEntries;
    }

    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     *
     * @param entryValues The array to be used as values to save for the preference.
     */
    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues = entryValues;
    }

    /**
     * @param entryValuesResId The entry values array as a resource.
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntryValues(int entryValuesResId) {
        setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
    }

    /**
     * Returns the array of values to be saved for the preference.
     *
     * @return The array of values.
     */
    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    /**
     * Sets the value of the key. This should contain entries in
     * {@link #getEntryValues()}.
     *
     * @param values The values to set for the key.
     */
    public void setValues(Set<String> values) {
        mValues.clear();
        mValues.addAll(values);
        persistStringSet(values);
        if (isBindValueToSummary()) {
            setSummary(values.isEmpty() ? mNothing
                    : String.format(Locale.US, "%d/%d", values.size(), mEntryValues.length));
        }
    }

    private void setFormattedSummary() {
        Set<String> text = new HashSet<>(mValues.size());
        Set<String> values = new TreeSet<>(mValues);
        CharSequence[] entries = getEntries();
        for (String str : values)
            text.add(entries[findIndexOfValue(str)].toString());

        String summ = text.toString();
        CharSequence summary = mValues.isEmpty() ? mNothing
                : summ.substring(1, summ.length() - 1); // strip []
        setSummary(summary);
    }

    /**
     * Retrieves the current value of the key.
     */
    public Set<String> getValues() {
        return mValues;
    }

    /**
     * When value is bound to the summary and there is nothing selected in this {@link MultiSelectListPreference},
     * the 'Nothing' text will be shown as summary.
     */
    public void setNothing(CharSequence nothing) {
        mNothing = nothing;
        if (isBindValueToSummary()) {
            setSummary(mValues.isEmpty() ? mNothing
                    : String.format(Locale.US, "%d/%d", mValues.size(), mEntryValues.length));
        }
    }

    public CharSequence getNothing() {
        return mNothing;
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean[] getSelectedItems() {
        final CharSequence[] entries = mEntryValues;
        final int entryCount = entries.length;
        final Set<String> values = mValues;
        boolean[] result = new boolean[entryCount];

        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entries[i].toString());
        }

        return result;
    }

    public void setListValueEvaluator(ListValueEvaluator evaluator) {
        mEvaluator = evaluator;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        final Set<String> result = new HashSet<>();
        try {
            final CharSequence[] defaultValues = a.getTextArray(index);
            final int valueCount = defaultValues == null ? 0 : defaultValues.length;

            for (int i = 0; i < valueCount; i++) {
                result.add(defaultValues[i].toString());
            }
        } catch (NullPointerException ignore) {
            // TADA! Now you don't need to specify an empty array in XML.
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValues(restoreValue ? getPersistedStringSet(mValues) : (Set<String>) defaultValue);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        assert superState != null;
        if (isPersistent()) {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.values = getValues();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValues(myState.values);
    }

    private static class SavedState extends BaseSavedState {
        Set<String> values;

        public SavedState(Parcel source) {
            super(source);
            values = new HashSet<>();
            String[] strings = source.createStringArray();
            Collections.addAll(values, strings);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(values.toArray(new String[values.size()]));
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
    }

    public interface ListValueEvaluator {

        /**
         * @return <code>true</code> if item on the list dialog can be chosen
         */
        boolean evaluate(Integer[] which, CharSequence[] text);
    }
}
