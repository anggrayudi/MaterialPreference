package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @see TimePreference
 */
@SuppressLint("RestrictedApi")
open class DatePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes), DatePickerDialog.OnDateSetListener {

    var minDate = 0L

    var maxDate = 0L

    var disabledDays: Array<Calendar>? = null

    var highlightedDays: Array<Calendar>? = null

    var dateFormatter: DateFormat = SimpleDateFormat.getDateInstance()
        set(f) {
            field = f
            val v = value
            if (isBindValueToSummary && v != null) {
                summary = f.format(v)
            }
        }

    /** Null if no value saved yet */
    var value: Date?
        get() {
            val millis = getPersistedLong(defaultValue)
            return if (millis > 0) Date(millis) else null
        }
        set(date) {
            val millis = date?.time ?: 0
            if (callChangeListener(millis)) {
                persistLong(millis)
                if (isBindValueToSummary) {
                    summary = dateFormatter.format(millis)
                }
            }
        }

    /**
     * Unix timestamp in millisecond. For example:
     * ```xml
     * <DatePreference
     *     android:defaultValue="1583621381235L" />
     * ```
     *
     * You have to use letter `L` as the suffix, or it will throw error like =>
     * `NumberFormatException: For input string: "1.58362134E12"`
     */
    var defaultValue = 0L
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes)
        defaultValue = a.getString(R.styleable.Preference_android_defaultValue)?.replace("L", "")?.toLong() ?: 0
        a.recycle()

        onPreferenceClickListener = {
            val millis = getPersistedLong(System.currentTimeMillis())
            val now = Calendar.getInstance()
            now.timeInMillis = millis
            val dialog = DatePickerDialog.newInstance(this,
                now[Calendar.YEAR],
                now[Calendar.MONTH],
                now[Calendar.DAY_OF_MONTH])
            dialog.version = DatePickerDialog.Version.VERSION_2
            dialog.dismissOnPause(false)

            val title = title
            if (title != null)
                dialog.setTitle(title.toString())

            val cal = Calendar.getInstance()

            cal.timeInMillis = minDate
            dialog.minDate = cal

            if (maxDate > minDate) {
                cal.timeInMillis = maxDate
                dialog.maxDate = cal
            }

            if (disabledDays != null)
                dialog.disabledDays = disabledDays

            if (highlightedDays != null)
                dialog.highlightedDays = highlightedDays

            dialog.show(preferenceFragment!!.fragmentManager!!, key)
            true
        }
    }

    override fun onSetupFinished(fragment: PreferenceFragmentMaterial) {
        val dialog = fragment.fragmentManager?.findFragmentByTag(key) as? DatePickerDialog
        dialog?.onDateSetListener = this
    }

    override fun onSetInitialValue() {
        val v = value
        if (isBindValueToSummary && v != null) {
            summary = dateFormatter.format(v)
        }
    }

    override fun onDateSet(view: DatePickerDialog, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthOfYear)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        value = calendar.time
    }
}
