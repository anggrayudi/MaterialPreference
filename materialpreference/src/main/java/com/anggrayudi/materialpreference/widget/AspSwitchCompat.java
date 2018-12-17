package com.anggrayudi.materialpreference.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Works around https://code.google.com/p/android/issues/detail?id=196652.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AspSwitchCompat extends SwitchCompat {
    private boolean isInSetChecked = false;

    public AspSwitchCompat(Context context) {
        super(context);
    }

    public AspSwitchCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspSwitchCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setChecked(boolean checked) {
        isInSetChecked = true;
        super.setChecked(checked);
        isInSetChecked = false;
    }

    @Override
    public boolean isShown() {
        if (isInSetChecked) {
            return getVisibility() == VISIBLE;
        }
        return super.isShown();
    }
}
