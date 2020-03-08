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
open class SeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.seekBarPreferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var seekBarValue: Int = 0
    private var trackingTouch: Boolean = false
    private var seekBar: SeekBar? = null

    var isAdjustable: Boolean = false // whether the seekbar should respond to the left/right keys

    var summaryFormatter: IntSummaryFormatter? = null
        set(f) {
            field = f
            if (isBindValueToSummary)
                summary = f?.invoke(seekBarValue) ?: seekBarValue.toString()
        }

    override var isLegacySummary: Boolean
        get() = false
        set(value) {
            super.isLegacySummary = value
        }

    /** Listener reacting to the SeekBar changing value by the user */
    private val seekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser && !trackingTouch) {
                syncValueInternal(seekBar)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            trackingTouch = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            trackingTouch = false
            if (seekBar.progress + _min != seekBarValue) {
                syncValueInternal(seekBar)
            }
        }
    }

    /**
     * Listener reacting to the user pressing DPAD left/right keys if `adjustable` attribute is set to true;
     * it transfers the key presses to the `SeekBar` to be handled accordingly.
     */
    private val seekBarKeyListener = View.OnKeyListener { _, keyCode, event ->
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

        if (seekBar == null) {
            Log.e(TAG, "SeekBar view is null and hence cannot be adjusted.")
            return@OnKeyListener false
        }
        seekBar!!.onKeyDown(keyCode, event)
    }

    var min: Int
        get() = _min
        set(value) {
            var min = value
            if (min > _max) {
                min = _max
            }
            if (min != _min) {
                _min = min
                notifyChanged()
            }
        }
    private var _min: Int = 0

    /**
     * Sets the increment amount on the SeekBar for each arrow key press.
     *
     * @return The amount of increment change via each arrow key click. This value is derived from
     * user's specified increment value if it's not zero. Otherwise, the default value is picked
     * from the default `mKeyProgressIncrement` value in [android.widget.AbsSeekBar].
     */
    var seekBarIncrement: Int
        get() = _seekBarIncrement
        set(seekBarIncrement) {
            if (seekBarIncrement != _seekBarIncrement) {
                _seekBarIncrement = Math.min(_max - _min, Math.abs(seekBarIncrement))
                notifyChanged()
            }
        }
    private var _seekBarIncrement: Int = 0

    var max: Int
        get() = _max
        set(value) {
            var max = value
            if (max < _min) {
                max = _min
            }
            if (max != _max) {
                _max = max
                notifyChanged()
            }
        }
    private var _max: Int = 0

    var value: Int
        get() = seekBarValue
        set(seekBarValue) = setValueInternal(seekBarValue, true)

    var defaultValue = 0
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference,
            defStyleAttr, defStyleRes)
        /*
         * The ordering of these two statements are important. If we want to set max first, we need
         * to perform the same steps by changing min/max to max/min as following:
         * _max = a.getInt(...) and setMin(...).
         */
        _min = a.getInt(R.styleable.SeekBarPreference_min, 0)
        _max = a.getInt(R.styleable.SeekBarPreference_android_max, 100)
        _seekBarIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 0)
        isAdjustable = a.getBoolean(R.styleable.SeekBarPreference_adjustable, true)
        defaultValue = a.getInt(R.styleable.Preference_android_defaultValue, _min)
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnKeyListener(seekBarKeyListener)
        seekBar = holder.findViewById(R.id.seekbar) as SeekBar

        if (seekBar == null) {
            Log.e(TAG, "SeekBar view is null in onBindViewHolder.")
            return
        }
        seekBar!!.setOnSeekBarChangeListener(seekBarChangeListener)
        seekBar!!.max = _max - _min
        // If the increment is not zero, use that. Otherwise, use the default mKeyProgressIncrement
        // in AbsSeekBar when it's zero. This default increment value is set by AbsSeekBar
        // after calling setMax. That's why it's important to call setKeyProgressIncrement after
        // calling setMax() since setMax() can change the increment value.
        if (_seekBarIncrement != 0) {
            seekBar!!.keyProgressIncrement = _seekBarIncrement
        } else {
            _seekBarIncrement = seekBar!!.keyProgressIncrement
        }

        seekBar!!.progress = seekBarValue - _min
        if (isBindValueToSummary)
            summary = summaryFormatter?.invoke(seekBarValue) ?: seekBarValue.toString()

        seekBar!!.isEnabled = isEnabled
    }

    private fun setValueInternal(seekBarValue: Int, notifyChanged: Boolean) {
        var value = seekBarValue
        if (value < _min) {
            value = _min
        }
        if (value > _max) {
            value = _max
        }

        if (value != this.seekBarValue) {
            this.seekBarValue = value
            persistInt(value)
            if (isBindValueToSummary || notifyChanged)
                summary = summaryFormatter?.invoke(this.seekBarValue) ?: this.seekBarValue.toString()
        }
    }

    /**
     * Persist the seekBar's seekbar value if [callChangeListener]
     * returns true, otherwise set the seekBar's value to the stored value
     */
    private fun syncValueInternal(seekBar: SeekBar) {
        val seekBarValue = _min + seekBar.progress
        if (seekBarValue != this.seekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false)
            } else {
                seekBar.progress = this.seekBarValue - _min
            }
        }
    }

    override fun onSetInitialValue() {
        seekBarValue = getPersistedInt(defaultValue)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        // Save the instance state
        val myState = SavedState(superState!!)
        myState.seekBarValue = seekBarValue
        myState.min = _min
        myState.max = _max
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
        seekBarValue = myState.seekBarValue
        _min = myState.min
        _max = myState.max
        notifyChanged()
    }

    /**
     * SavedState, a subclass of [Preference.BaseSavedState], will store the state
     * of MyPreference, a subclass of Preference.
     *
     * It is important to always call through to super methods.
     */
    private class SavedState : BaseSavedState {

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
