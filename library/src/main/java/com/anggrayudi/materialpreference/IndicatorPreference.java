package com.anggrayudi.materialpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

@SuppressLint("RestrictedApi")
public class IndicatorPreference extends Preference {

    private static final String TAG = "IndicatorPreference";

    private int mTint;
    private Drawable mDrawable;

    public IndicatorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IndicatorPreference, defStyleAttr, defStyleRes);
        mTint = a.getColor(R.styleable.IndicatorPreference_tint, 0);
        mDrawable = a.getDrawable(R.styleable.IndicatorPreference_android_src);
        a.recycle();
    }

    public IndicatorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IndicatorPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.indicatorPreferenceStyle, 0));
    }

    public IndicatorPreference(Context context) {
        this(context, null);
    }

    @Override
    public final boolean isPersistent() {
        return false;
    }

    @Override
    public final void setPersistent(boolean persistent) {
        Log.i(TAG, "IndicatorPreference.setPersistent() always false");
    }

    @Override
    public final boolean isLegacySummary() {
        return true;
    }

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
        setTint(getTint());
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setTint(int tint) {
        mTint = tint;
        if (getDrawable() != null)
            getDrawable().mutate().setColorFilter(mTint, PorterDuff.Mode.SRC_IN);

        ImageView imageView = mPreferenceViewHolder.itemView.findViewById(android.R.id.icon1);
        imageView.setImageDrawable(getDrawable());
    }

    public int getTint() {
        return mTint;
    }
}
