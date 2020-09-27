package com.anggrayudi.materialpreference.util

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.anggrayudi.storage.extension.startActivitySafely

fun String?.toSafeInt(defaultValue: Int = 0): Int = this?.toIntOrNull() ?: defaultValue

@Suppress("DEPRECATION")
fun Drawable?.applyTint(@ColorInt color: Int): Drawable? = apply {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        this?.mutate()?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    } else {
        this?.setTint(color)
    }
}

fun Context.openWebsite(url: String) {
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivitySafely(web)
}

fun Context.getAttrColor(@AttrRes attr: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attr))
    return try {
        a.getColor(0, 0)
    } finally {
        a.recycle()
    }
}

fun CharSequence?.toUri() = if (this != null) Uri.parse(toString()) else null

inline fun EditText.onTextChanged(crossinline onTextChanged: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            // no-op
        }

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            onTextChanged(text?.toString().orEmpty())
        }

        override fun afterTextChanged(editable: Editable?) {
            // no-op
        }
    })
}

fun Context.getSupportDrawable(@DrawableRes resId: Int) = if (resId != 0) AppCompatResources.getDrawable(this, resId) else null

fun TypedArray.getSupportDrawable(context: Context, index: Int): Drawable? {
    val resId = getResourceId(index, 0)
    return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
}