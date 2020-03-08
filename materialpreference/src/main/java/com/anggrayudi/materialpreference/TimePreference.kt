package com.anggrayudi.materialpreference

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import com.anggrayudi.materialpreference.util.toSafeInt
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import java.util.*

/**
 *
 * @see DatePreference
 */
open class TimePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes), TimePickerDialog.OnTimeSetListener {

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
            return if (value != null) toTime(value) else defaultValue
        }
        set(time) {
            if (callChangeListener(time!!)) {
                persistString(time.toString())
                if (isBindValueToSummary) {
                    summary = timeFormatter.format(time.toDate())
                }
            }
        }

    /**
     * For example:
     * ```xml
     * <TimePreference
     *     android:defaultValue="14:08" />
     * ```
     */
    var defaultValue: Time? = null
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes)
        defaultValue = a.getString(R.styleable.Preference_android_defaultValue)?.let { toTime(it) }
        a.recycle()

        onPreferenceClickListener = {
            var time = value
            if (time == null) {
                val now = Calendar.getInstance()
                time = Time(now[Calendar.HOUR_OF_DAY], now[Calendar.MINUTE])
            }
            val dialog = TimePickerDialog.newInstance(this,
                time.hourOfDay, time.minute, is24HourMode)
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
        val dialog = fragment.fragmentManager?.findFragmentByTag(key) as? TimePickerDialog
        dialog?.onTimeSetListener = this
    }

    override fun onSetInitialValue() {
        val v = value
        if (isBindValueToSummary && v != null) {
            summary = timeFormatter.format(v.toDate())
        }
    }

    override fun onTimeSet(view: TimePickerDialog, hourOfDay: Int, minute: Int, second: Int) {
        value = Time(hourOfDay, minute)
    }

    class Time constructor(
        /** 24 hours format  */
        var hourOfDay: Int,
        var minute: Int
    ) {

        override fun toString(): String = "$hourOfDay:$minute"

        fun toDate(initialDateMillis: Long = 0): Date = Calendar.getInstance().run {
            timeInMillis = initialDateMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time
        }
    }

    companion object {

        /**
         * Converts saved time preference value into [Time]
         * @param value Must be separated with **:** char. For example => `13:25:8`
         */
        fun toTime(value: String): Time {
            val s = value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Time(s[0].toSafeInt(), s[1].toSafeInt())
        }

        fun toTime(date: Date): Time {
            val c = Calendar.getInstance()
            c.time = date
            return toTime("${c[Calendar.HOUR_OF_DAY]}:${c[Calendar.MINUTE]}:${c[Calendar.SECOND]}")
        }
    }
}
