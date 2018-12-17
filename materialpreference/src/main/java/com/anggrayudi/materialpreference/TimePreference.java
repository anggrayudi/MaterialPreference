package com.anggrayudi.materialpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import com.wdullaer.materialdatetimepicker.time.Timepoint;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 */
public class TimePreference extends Preference implements Preference.OnPreferenceClickListener,
        TimePickerDialog.OnTimeSetListener {

    private java.text.DateFormat formatter;
    private boolean enableMinute = true;
    private boolean enableSecond;
    private boolean mode24Hour;
    private Timepoint minTime;
    private Timepoint maxTime;

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnPreferenceClickListener(this);
        formatter = DateFormat.getTimeFormat(context);
        mode24Hour = DateFormat.is24HourFormat(context);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public TimePreference(Context context) {
        this(context, null);
    }

    @Override
    void onSetupFinished(PreferenceFragmentMaterial fragment) {
        TimePickerDialog dialog = (TimePickerDialog) fragment.getFragmentManager().findFragmentByTag(getKey());
        if (dialog != null) {
            dialog.setOnTimeSetListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Time time = getValue();
        if (time == null) {
            Calendar now = Calendar.getInstance();
            time = new Time(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        }
        TimePickerDialog dialog = TimePickerDialog.newInstance(this, time.hourOfDay, time.minute, is24HourMode());
        dialog.setVersion(TimePickerDialog.Version.VERSION_2);
        dialog.dismissOnPause(false);
        dialog.enableMinutes(enableMinute);
        dialog.enableSeconds(enableSecond);

        CharSequence title = getTitle();
        if (title != null)
            dialog.setTitle(title.toString());

        if (getMinTime() != null)
            dialog.setMinTime(getMinTime());

        if (getMaxTime() != null)
            dialog.setMaxTime(getMaxTime());

        dialog.show(getPreferenceFragment().getFragmentManager(), getKey());
        return true;
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        setValue(new Time(hourOfDay, minute, second));
    }

    public void setPickerEnabled(boolean enableMinute, boolean enableSecond) {
        this.enableMinute = enableMinute;
        this.enableSecond = enableSecond;
    }

    public void set24HourMode(boolean mode24Hour) {
        this.mode24Hour = mode24Hour;
    }

    public boolean is24HourMode() {
        return mode24Hour;
    }

    public void setMinTime(@Nullable Timepoint minTime){
        this.minTime = minTime;
    }

    @Nullable
    public Timepoint getMinTime() {
        return minTime;
    }

    public void setMaxTime(@Nullable Timepoint maxTime) {
        this.maxTime = maxTime;
    }

    @Nullable
    public Timepoint getMaxTime() {
        return maxTime;
    }

    @Override
    public CharSequence getSummary() {
        Time value = getValue();
        return isBindValueToSummary() && value != null ? formatter.format(value.toDate()) : super.getSummary();
    }

    public void setTimeFormatter(@NonNull java.text.DateFormat formatter) {
        this.formatter = formatter;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value = restorePersistedValue ? getPersistedString(null) : ((String) defaultValue);
        final boolean wasBlocking = shouldDisableDependents();

        persistString(value);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
        if (isBindValueToSummary())
            notifyChanged();
    }

    public void setValue(Time time) {
        if (callChangeListener(time)) {
            persistString(time.toString());
            if (isBindValueToSummary()) {
                notifyChanged();
            }
        }
    }

    /**
     * Get saved value from this preference
     * @return <code>null</code> if no value saved yet
     */
    @Nullable
    public Time getValue() {
        String value = getPersistedString(null);
        return value != null ? toTime(value) : null;
    }

    /**
     * Converts saved time preference value into {@link Time}
     * @param value Must be separated with <b>:</b><br>For example => <code>13:25:8</code>
     */
    public static Time toTime(String value) {
        String[] s = value.split(":");
        return new Time(Integer.valueOf(s[0]), Integer.valueOf(s[1]), Integer.valueOf(s[2]));
    }

    public static class Time {
        /** 24 hours format */
        public final int hourOfDay;
        public final int minute, second;

        Time(int hourOfDay, int minute, int second) {
            this.hourOfDay = hourOfDay;
            this.minute = minute;
            this.second = second;
        }

        @NonNull
        @Override
        public String toString() {
            return hourOfDay + ":" + minute + ":" + second;
        }

        public Date toDate(long initDateMillis) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(initDateMillis);
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        }

        public Date toDate() {
            return toDate(System.currentTimeMillis());
        }
    }
}
