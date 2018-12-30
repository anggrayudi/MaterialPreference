package com.anggrayudi.materialpreference

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import java.util.*

/**
 *
 * @see DatePreference
 */
class TimePreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.preferenceStyle, defStyleRes: Int = 0)
    : Preference(context, attrs, defStyleAttr, defStyleRes), TimePickerDialog.OnTimeSetListener {

    var enableMinute = true
    var enableSecond = false
    var minTime: Timepoint? = null
    var maxTime: Timepoint? = null

    var is24HourMode = DateFormat.is24HourFormat(context)

    var timeFormatter: java.text.DateFormat = DateFormat.getTimeFormat(context)
    set(f) {
        field = f
        val v = value
        if (isBindValueToSummary && v != null) {
            summary = f.format(v)
        }
    }

    /**
     * Get saved value from this preference
     * @return `null` if no value saved yet
     */
    var value: Time?
        get() {
            val value = getPersistedString(null)
            return if (value != null) toTime(value) else null
        }
        set(time) {
            if (callChangeListener(time!!)) {
                persistString(time.toString())
                if (isBindValueToSummary) {
                    summary = timeFormatter.format(time.toDate())
                }
            }
        }

    init {
        onPreferenceClickListener = {
            var time = value
            if (time == null) {
                val now = Calendar.getInstance()
                time = Time(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))
            }
            val dialog = TimePickerDialog.newInstance(this,
                    time.hourOfDay, time.minute, time.second, is24HourMode)
            dialog.version = TimePickerDialog.Version.VERSION_2
            dialog.dismissOnPause(false)
            dialog.enableMinutes(enableMinute)
            dialog.enableSeconds(enableSecond)

            val title = title
            if (title != null)
                dialog.title = title.toString()

            if (minTime != null)
                dialog.setMinTime(minTime)

            if (maxTime != null)
                dialog.setMaxTime(maxTime)

            dialog.show(preferenceFragment!!.fragmentManager!!, key)
            true
        }
    }

    override fun onSetupFinished(fragment: PreferenceFragmentMaterial) {
        val dialog = fragment.fragmentManager!!.findFragmentByTag(key) as? TimePickerDialog
        dialog?.onTimeSetListener = this
    }

    override fun onSetInitialValue() {
        val v = value
        if (isBindValueToSummary && v != null) {
            summary = timeFormatter.format(v.toDate())
        }
    }

    override fun onTimeSet(view: TimePickerDialog, hourOfDay: Int, minute: Int, second: Int) {
        value = Time(hourOfDay, minute, second)
    }

    class Time constructor(
            /** 24 hours format  */
            var hourOfDay: Int, var minute: Int, var second: Int) {

        override fun toString(): String {
            return hourOfDay.toString() + ":" + minute + ":" + second
        }

        fun toDate(initDateMillis: Long = 0): Date {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = initDateMillis
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, second)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }
    }

    companion object {

        /**
         * Converts saved time preference value into [Time]
         * @param value Must be separated with **:** char. For example => `13:25:8`
         */
        fun toTime(value: String): Time {
            val s = value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Time(Integer.valueOf(s[0]), Integer.valueOf(s[1]), Integer.valueOf(s[2]))
        }

        fun toTime(date: Date): Time {
            val c = Calendar.getInstance()
            c.time = date
            return toTime("${c.get(Calendar.HOUR_OF_DAY)}:${c.get(Calendar.MINUTE)}:${c.get(Calendar.SECOND)}")
        }
    }
}
