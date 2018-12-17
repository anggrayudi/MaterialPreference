package com.anggrayudi.materialpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 */
@SuppressLint("RestrictedApi")
public class DatePreference extends Preference implements Preference.OnPreferenceClickListener,
        DatePickerDialog.OnDateSetListener {

    private long minDate;
    private long maxDate;
    private DateFormat formatter = SimpleDateFormat.getDateInstance();
    private Calendar[] disabledDays;
    private Calendar[] highlightedDays;

    public DatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnPreferenceClickListener(this);
    }

    public DatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DatePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public DatePreference(Context context) {
        this(context, null);
    }

    @Override
    void onSetupFinished(PreferenceFragmentMaterial fragment) {
        DatePickerDialog dialog = (DatePickerDialog) fragment.getFragmentManager().findFragmentByTag(getKey());
        if (dialog != null) {
            dialog.setOnDateSetListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long millis = getPersistedLong(System.currentTimeMillis());
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(millis);
        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dialog.setVersion(DatePickerDialog.Version.VERSION_2);
        dialog.dismissOnPause(false);

        CharSequence title = getTitle();
        if (title != null)
            dialog.setTitle(title.toString());

        Calendar cal = Calendar.getInstance();

        cal.setTime(getMinDate());
        dialog.setMinDate(cal);

        Date max = getMaxDate();
        if (max != null) {
            cal.setTime(max);
            dialog.setMaxDate(cal);
        }

        if (disabledDays != null)
            dialog.setDisabledDays(disabledDays);

        if (highlightedDays != null)
            dialog.setHighlightedDays(highlightedDays);

        dialog.show(getPreferenceFragment().getFragmentManager(), getKey());
        return true;
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        setValue(calendar.getTimeInMillis());
    }

    public void setMinDate(long millis) {
        minDate = millis;
    }

    public void setMaxDate(long millis) {
        maxDate = millis;
    }

    @Nullable
    public Date getMaxDate() {
        return maxDate > 0 ? new Date(maxDate) : null;
    }

    @NonNull
    public Date getMinDate() {
        return new Date(minDate);
    }

    public void setDisabledDays(Calendar[] disabledDays) {
        this.disabledDays = disabledDays;
    }

    public void setHighlightedDays(Calendar[] highlightedDays) {
        this.highlightedDays = highlightedDays;
    }

    /**
     * @return <code>null</code> if no value saved yet
     */
    @Nullable
    public Date getValue() {
        long millis = getPersistedLong(0);
        return millis > 0 ? new Date(millis) : null;
    }

    public void setValue(long millis) {
        if (callChangeListener(millis)) {
            persistLong(millis);
            if (isBindValueToSummary()) {
                notifyChanged();
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        Date value = getValue();
        return isBindValueToSummary() && value != null ? formatter.format(value) : super.getSummary();
    }

    public void setDateFormatter(@NonNull DateFormat formatter) {
        this.formatter = formatter;
    }
}
