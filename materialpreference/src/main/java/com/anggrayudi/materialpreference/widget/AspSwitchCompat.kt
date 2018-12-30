package com.anggrayudi.materialpreference.widget

import android.content.Context
import android.util.AttributeSet

import androidx.annotation.RestrictTo
import androidx.appcompat.widget.SwitchCompat

/**
 * Works around https://code.google.com/p/android/issues/detail?id=196652
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AspSwitchCompat : SwitchCompat {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var isInSetChecked = false

    override fun isShown(): Boolean = if (isInSetChecked) { visibility == VISIBLE } else super.isShown()

    override fun setChecked(checked: Boolean) {
        isInSetChecked = true
        super.setChecked(checked)
        isInSetChecked = false
    }
}
