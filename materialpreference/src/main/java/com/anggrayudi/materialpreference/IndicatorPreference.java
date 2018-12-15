package com.anggrayudi.materialpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Used to indicate status of something, e.g. verified account is shown as check icon.
 * <hr>
 *  <table>
 *  <tr>
 *  <th>Attribute</th>
 *  <th>Value Type</th>
 *  </tr><tr>
 *  <td><code>android:src</code></td>
 *  <td>Drawable</td>
 *  </tr><tr>
 *  <td><code>app:tint</code></td>
 *  <td>Color</td>
 *  </tr><tr>
 *  </tr>
 *  </table>
 */
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
    public final boolean isLegacySummary() {
        return true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon1);
        imageView.setImageDrawable(getDrawable());
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

        notifyChanged();
    }

    public int getTint() {
        return mTint;
    }
}
