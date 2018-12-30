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
 * limitations under the License.
 */

package com.anggrayudi.materialpreference

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.anggrayudi.materialpreference.util.IntSummaryFormatter

/**
 * The seekbar within the preference can be defined adjustable or not by setting `adjustable` attribute.
 * If adjustable, the preference will be responsive to DPAD left/right keys.
 * Otherwise, it skips those keys.
 *
 *      |       Attribute      | Value Type |
 *      |:--------------------:|:----------:|
 *      | android:max          | Int        |
 *      | app:min              | Int        |
 *      | app:seekBarIncrement | Int        |
 *      | app:adjustable       | Boolean    |
 *
 * @see SeekBarDialogPreference
 */
class SeekBarPreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.seekBarPreferenceStyle, defStyleRes: Int = 0)
    : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var mSeekBarValue: Int = 0
    private var mTrackingTouch: Boolean = false
    private var mSeekBar: SeekBar? = null
    var isAdjustable: Boolean = false // whether the seekbar should respond to the left/right keys

    var summaryFormatter: IntSummaryFormatter? = null
        set(f) {
            field = f
            if (isBindValueToSummary)
                summary = f?.invoke(mSeekBarValue) ?: mSeekBarValue.toString()
        }

    override var isLegacySummary: Boolean
        get() = false
        set(value) {
            super.isLegacySummary = value
        }

    /** Listener reacting to the SeekBar changing value by the user */
    private val mSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser && !mTrackingTouch) {
                syncValueInternal(seekBar)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            mTrackingTouch = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mTrackingTouch = false
            if (seekBar.progress + mMin != mSeekBarValue) {
                syncValueInternal(seekBar)
            }
        }
    }

    /**
     * Listener reacting to the user pressing DPAD left/right keys if `adjustable` attribute is set to true;
     * it transfers the key presses to the `SeekBar` to be handled accordingly.
     */
    private val mSeekBarKeyListener = View.OnKeyListener { v, keyCode, event ->
        if (event.action != KeyEvent.ACTION_DOWN) {
            return@OnKeyListener false
        }

        if (!isAdjustable && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            // Right or left keys are pressed when in non-adjustable mode; Skip the keys.
            return@OnKeyListener false
        }

        // We don't want to propagate the click keys down to the seekbar view since it will
        // create the ripple effect for the thumb.
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            return@OnKeyListener false
        }

        if (mSeekBar == null) {
            Log.e(TAG, "SeekBar view is null and hence cannot be adjusted.")
            return@OnKeyListener false
        }
        mSeekBar!!.onKeyDown(keyCode, event)
    }

    var min: Int
        get() = mMin
        set(min) {
            var min = min
            if (min > mMax) {
                min = mMax
            }
            if (min != mMin) {
                mMin = min
                notifyChanged()
            }
        }
    private var mMin: Int = 0

    /**
     * Sets the increment amount on the SeekBar for each arrow key press.
     *
     * @return The amount of increment change via each arrow key click. This value is derived from
     * user's specified increment value if it's not zero. Otherwise, the default value is picked
     * from the default `mKeyProgressIncrement` value in [android.widget.AbsSeekBar].
     */
    var seekBarIncrement: Int
        get() = mSeekBarIncrement
        set(seekBarIncrement) {
            if (seekBarIncrement != mSeekBarIncrement) {
                mSeekBarIncrement = Math.min(mMax - mMin, Math.abs(seekBarIncrement))
                notifyChanged()
            }
        }
    private var mSeekBarIncrement: Int = 0

    var max: Int
        get() = mMax
        set(max) {
            var max = max
            if (max < mMin) {
                max = mMin
            }
            if (max != mMax) {
                mMax = max
                notifyChanged()
            }
        }
    private var mMax: Int = 0

    var value: Int
        get() = mSeekBarValue
        set(seekBarValue) = setValueInternal(seekBarValue, true)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes)
        /*
         * The ordering of these two statements are important. If we want to set max first, we need
         * to perform the same steps by changing min/max to max/min as following:
         * mMax = a.getInt(...) and setMin(...).
         */
        mMin = a.getInt(R.styleable.SeekBarPreference_min, 0)
        max = a.getInt(R.styleable.SeekBarPreference_android_max, 100)
        seekBarIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 0)
        isAdjustable = a.getBoolean(R.styleable.SeekBarPreference_adjustable, true)
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnKeyListener(mSeekBarKeyListener)
        mSeekBar = holder.findViewById(R.id.seekbar) as SeekBar

        if (mSeekBar == null) {
            Log.e(TAG, "SeekBar view is null in onBindViewHolder.")
            return
        }
        mSeekBar!!.setOnSeekBarChangeListener(mSeekBarChangeListener)
        mSeekBar!!.max = mMax - mMin
        // If the increment is not zero, use that. Otherwise, use the default mKeyProgressIncrement
        // in AbsSeekBar when it's zero. This default increment value is set by AbsSeekBar
        // after calling setMax. That's why it's important to call setKeyProgressIncrement after
        // calling setMax() since setMax() can change the increment value.
        if (mSeekBarIncrement != 0) {
            mSeekBar!!.keyProgressIncrement = mSeekBarIncrement
        } else {
            mSeekBarIncrement = mSeekBar!!.keyProgressIncrement
        }

        mSeekBar!!.progress = mSeekBarValue - mMin
        if (isBindValueToSummary)
            summary = summaryFormatter?.invoke(mSeekBarValue) ?: mSeekBarValue.toString()

        mSeekBar!!.isEnabled = isEnabled
    }

    private fun setValueInternal(seekBarValue: Int, notifyChanged: Boolean) {
        var value = seekBarValue
        if (value < mMin) {
            value = mMin
        }
        if (value > mMax) {
            value = mMax
        }

        if (value != mSeekBarValue) {
            mSeekBarValue = value
            persistInt(value)
            if (isBindValueToSummary || notifyChanged)
                summary = summaryFormatter?.invoke(mSeekBarValue) ?: mSeekBarValue.toString()
        }
    }

    /**
     * Persist the seekBar's seekbar value if [callChangeListener]
     * returns true, otherwise set the seekBar's value to the stored value
     */
    private fun syncValueInternal(seekBar: SeekBar) {
        val seekBarValue = mMin + seekBar.progress
        if (seekBarValue != mSeekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false)
            } else {
                seekBar.progress = mSeekBarValue - mMin
            }
        }
    }

    override fun onSetInitialValue() {
        mSeekBarValue = getPersistedInt(value)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        // Save the instance state
        val myState = SavedState(superState!!)
        myState.seekBarValue = mSeekBarValue
        myState.min = mMin
        myState.max = mMax
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state!!.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        // Restore the instance state
        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        mSeekBarValue = myState.seekBarValue
        mMin = myState.min
        mMax = myState.max
        notifyChanged()
    }

    /**
     * SavedState, a subclass of [Preference.BaseSavedState], will store the state
     * of MyPreference, a subclass of Preference.
     *
     * It is important to always call through to super methods.
     */
    private class SavedState : Preference.BaseSavedState {
        internal var seekBarValue: Int = 0
        internal var min: Int = 0
        internal var max: Int = 0

        constructor(source: Parcel) : super(source) {
            // Restore the click counter
            seekBarValue = source.readInt()
            min = source.readInt()
            max = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            // Save the click counter
            dest.writeInt(seekBarValue)
            dest.writeInt(min)
            dest.writeInt(max)
        }

        internal constructor(superState: Parcelable) : super(superState)

        companion object CREATOR: Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {

        private const val TAG = "SeekBarPreference"
    }
}
