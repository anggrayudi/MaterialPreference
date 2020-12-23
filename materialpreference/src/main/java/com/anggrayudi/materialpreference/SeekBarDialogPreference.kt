/*
 * Copyright (C) 2007 The Android Open Source Project
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
import androidx.annotation.Keep
import com.anggrayudi.materialpreference.dialog.DialogPreference
import com.anggrayudi.materialpreference.util.IntSummaryFormatter

/**
 *
 * @see SeekBarPreference
 */
open class SeekBarDialogPreference @Keep @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.seekBarDialogPreferenceStyle,
    defStyleRes: Int = R.style.Preference_DialogPreference_SeekBarDialogPreference
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var progress: Int = 0
    private var preferredMax = 100
    private var preferredMin = 0

    var summaryFormatter: IntSummaryFormatter? = null
        set(f) {
            field = f
            if (isBindValueToSummary)
                summary = f?.invoke(value) ?: value.toString()
        }

    var value: Int
        get() = progress
        set(progress) {
            if (callChangeListener(progress)) {
                setProgress(progress, true)
            }
        }

    var max: Int
        get() = preferredMax
        set(max) {
            if (max != preferredMax) {
                preferredMax = max
                notifyChanged()
            }
        }

    var min: Int
        get() = preferredMin
        set(min) {
            if (min != preferredMin) {
                preferredMin = min
                notifyChanged()
            }
        }

    var defaultValue = 0
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes)
        preferredMax = a.getInt(R.styleable.SeekBarPreference_android_max, preferredMax)
        val hasAspMin = a.hasValue(R.styleable.SeekBarPreference_min)
        if (hasAspMin) {
            preferredMin = a.getInt(R.styleable.SeekBarPreference_min, preferredMin)
        }
        defaultValue = a.getInt(R.styleable.Preference_android_defaultValue, preferredMin)
        a.recycle()
    }

    override fun shouldDisableDependents(): Boolean {
        return progress == 0 || super.shouldDisableDependents()
    }

    private fun setProgress(preferredProgress: Int, notifyChanged: Boolean) {
        var p = preferredProgress
        val wasBlocking = shouldDisableDependents()

        if (p > preferredMax) {
            p = preferredMax
        }
        if (p < preferredMin) {
            p = preferredMin
        }
        if (p != progress) {
            progress = p
            persistInt(p)
            //            Log.d("SBDP", "p=" + p);
            if (notifyChanged) {
                notifyChanged()
            }
        }

        val isBlocking = shouldDisableDependents()
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking)
        }
        summary = summaryFormatter?.invoke(p) ?: p.toString()
    }

    override fun onSetInitialValue() {
        value = getPersistedInt(defaultValue)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()!!
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState)
        myState.progress = progress
        myState.max = preferredMax
        myState.min = preferredMin
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state!!.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        preferredMax = myState.max
        preferredMin = myState.min
        setProgress(myState.progress, true)
    }

    private class SavedState : BaseSavedState {
        internal var progress: Int = 0
        internal var max: Int = 0
        internal var min: Int = 0

        internal constructor(source: Parcel) : super(source) {
            progress = source.readInt()
            max = source.readInt()
            min = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(progress)
            dest.writeInt(max)
            dest.writeInt(min)
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

    companion object {
        private const val TAG = "SeekBarDialogPreference"
    }
}
