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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import com.anggrayudi.materialpreference.dialog.DialogPreference;

/**
 * A {@link Preference} that allows for string
 * input. It is a subclass of {@link DialogPreference} and shows the {@link EditText}
 * in a dialog. This preference will store a string into the SharedPreferences.
 * <hr>
<table>
    <tr>
      <th>Attribute</th>
      <th>Value Type</th>
    </tr>
    <tr>
        <td><code>android:hint</code></td>
        <td>String</td>
    </tr>
    <tr>
        <td><code>android:inputType</code></td>
        <td>{@link InputType}</td>
    </tr>
    <tr>
        <td><code>android:dialogMessage</code></td>
        <td>String</td>
    </tr>
    <tr>
        <td><code>android:maxLength</code></td>
        <td>Int</td>
    </tr>
    <tr>
       <td><code>app:minLength</code></td>
        <td>Int</td>
    </tr>
    <tr>
        <td><code>app:counterEnabled</code></td>
        <td>Boolean</td>
    </tr>
  </table>
 */
@SuppressLint("RestrictedApi")
public class EditTextPreference extends DialogPreference {

    private String mText;
    private String mHint;
    private String mMessage;
    private int mInputType;
    private int mMaxLength;
    private int mMinLength;
    private boolean mCounterEnabled;

    InputFilter[] mInputFilters;

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle,
                android.R.attr.editTextPreferenceStyle));
    }

    public EditTextPreference(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditTextPreference, defStyleAttr, defStyleRes);
        mCounterEnabled = a.getBoolean(R.styleable.EditTextPreference_counterEnabled, true);
        mHint = a.getString(R.styleable.EditTextPreference_android_hint);
        mMessage = a.getString(R.styleable.EditTextPreference_android_dialogMessage);
        mMaxLength = a.getInt(R.styleable.EditTextPreference_android_maxLength, 100);
        mMinLength = a.getInt(R.styleable.EditTextPreference_minLength, 0);
        mInputType = a.getInt(R.styleable.EditTextPreference_android_inputType, InputType.TYPE_CLASS_TEXT);
        a.recycle();
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMinLength(int minLength) {
        mMinLength = minLength;
    }

    public int getMinLength() {
        return mMinLength;
    }

    public void setMaxLength(int maxLength) {
        mMaxLength = maxLength;
    }

    public int getMaxLength() {
        return mMaxLength;
    }

    public void setInputFilters(InputFilter[] inputFilters) {
        mInputFilters = inputFilters;
    }

    public void setInputType(int inputType) {
        mInputType = inputType;
    }

    public int getInputType() {
        return mInputType;
    }

    public void setHint(String hint) {
        mHint = hint;
    }

    public String getHint() {
        return mHint;
    }

    public void setCounterEnabled(boolean counterEnabled) {
        mCounterEnabled = counterEnabled;
    }

    public boolean isCounterEnabled() {
        return mCounterEnabled;
    }

    /**
     * Saves the text to the {@link android.content.SharedPreferences}.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
        if (isBindValueToSummary())
            setSummary(text);
    }

    /**
     * Gets the text from the {@link android.content.SharedPreferences}.
     *
     * @return The current preference value.
     */
    public String getText() {
        return mText;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.text = getText();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setText(myState.text);
    }

    private static class SavedState extends BaseSavedState {
        String text;

        SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
