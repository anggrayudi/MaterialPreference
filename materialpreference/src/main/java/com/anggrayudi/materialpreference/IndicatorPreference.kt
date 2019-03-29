package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.TypedArrayUtils
import com.anggrayudi.materialpreference.util.applyTint

/**
 * Used to indicate status of something, e.g. verified account is shown as check icon.
 *
 *      |  Attribute  | Value Type |
 *      |:-----------:|:----------:|
 *      | android:src | Drawable   |
 *      | app:tint    | Color      |
 */
@SuppressLint("RestrictedApi")
class IndicatorPreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.indicatorPreferenceStyle, 0),
        defStyleRes: Int = 0)
    : Preference(context, attrs, defStyleAttr, defStyleRes) {

    override var isLegacySummary: Boolean
        get() = true
        set(value) {
            super.isLegacySummary = value
        }

    var drawable: Drawable?
        get() = _drawable
        set(drawable) {
            _drawable = drawable
            tint = tint
        }
    private var _drawable: Drawable? = null

    var tint: Int
        get() = _tint
        set(tint) {
            _tint = tint
            drawable?.applyTint(_tint)
            notifyChanged()
        }
    private var _tint: Int = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.IndicatorPreference, defStyleAttr, defStyleRes)
        _tint = a.getColor(R.styleable.IndicatorPreference_tint, 0)
        _drawable = a.getDrawable(R.styleable.IndicatorPreference_android_src)
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val imageView = holder.findViewById(android.R.id.icon1) as ImageView
        imageView.setImageDrawable(drawable)
    }

    override fun onSetupFinished(fragment: PreferenceFragmentMaterial) {
        tint = _tint
        preferenceViewHolder?.itemView?.findViewById<View>(R.id.material_summary)!!.visibility = View.GONE
    }
}
