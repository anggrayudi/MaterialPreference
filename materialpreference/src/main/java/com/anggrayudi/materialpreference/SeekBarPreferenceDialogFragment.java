package com.anggrayudi.materialpreference;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import android.widget.TextView;

import com.anggrayudi.materialpreference.dialog.DialogPreference;
import com.anggrayudi.materialpreference.dialog.PreferenceDialogFragmentCompat;

import java.util.Locale;

/**
 * @author Eugen on 7. 12. 2015.
 */
public class SeekBarPreferenceDialogFragment extends PreferenceDialogFragmentCompat
        implements View.OnKeyListener {

    private SeekBar mSeekBar;
    private TextView mTextMin;
    private TextView mTextMax;
    private TextView mTextValue;

    public static SeekBarPreferenceDialogFragment newInstance(String key) {
        SeekBarPreferenceDialogFragment fragment = new SeekBarPreferenceDialogFragment();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    public SeekBarPreferenceDialogFragment() {
    }

    public SeekBarDialogPreference getSeekBarDialogPreference() {
        return (SeekBarDialogPreference) getPreference();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final SeekBarDialogPreference preference = getSeekBarDialogPreference();

        mTextMin = view.findViewById(R.id.seekbar_min);
        mTextMax = view.findViewById(R.id.seekbar_max);
        mTextValue = view.findViewById(R.id.seekbar_value);
        mSeekBar = view.findViewById(R.id.seekbar);

        int max = preference.getMax();
        mTextMax.setText(String.format(Locale.US, "%d", max));

        final int min = preference.getMin();
        mTextMin.setText(String.format(Locale.US, "%d", min));

        mSeekBar.setMax(max - min);
        mSeekBar.setProgress(preference.getProgress() - min);
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (preference.mFormatter != null)
                    mTextValue.setText(preference.mFormatter.getValue(progress + min));
                else
                    mTextValue.setText(String.format(Locale.US, "%d", progress + min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        listener.onProgressChanged(mSeekBar, mSeekBar.getProgress(), false);
        mSeekBar.setOnSeekBarChangeListener(listener);
        mKeyProgressIncrement = mSeekBar.getKeyProgressIncrement();
        mSeekBar.setOnKeyListener(this);

        setupAccessibilityDelegate(max, min);
    }

    private void setupAccessibilityDelegate(final int max, final int min) {
        mSeekBar.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityEvent(final View host, final AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);

                final int progress = mSeekBar.getProgress() + min;
                event.setContentDescription(progress + "");

//                    event.setItemCount(max - min);
//                    event.setFromIndex(min);
//                    event.setToIndex(max);
//                    event.setCurrentItemIndex(progress);
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(final View host, final AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

                int progress = mSeekBar.getProgress() + min;
                info.setContentDescription(progress + "");
            }
        });
    }

    private boolean hasDialogTitle() {
        DialogPreference preference = getPreference();
        CharSequence dialogTitle = preference.getDialogTitle();
        if (dialogTitle == null) dialogTitle = preference.getTitle();
        return !TextUtils.isEmpty(dialogTitle);
    }

    private int mKeyProgressIncrement;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            final int step = mKeyProgressIncrement;
            if (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS) {
                mSeekBar.setProgress(mSeekBar.getProgress() + step);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MINUS) {
                mSeekBar.setProgress(mSeekBar.getProgress() - step);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        mSeekBar.setOnKeyListener(null);
        super.onDestroyView();
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        SeekBarDialogPreference preference = getSeekBarDialogPreference();
        if (positiveResult) {
            int progress = mSeekBar.getProgress() + preference.getMin();
            if (preference.callChangeListener(progress)) {
                preference.setProgress(progress);
            }
        }
    }
}
