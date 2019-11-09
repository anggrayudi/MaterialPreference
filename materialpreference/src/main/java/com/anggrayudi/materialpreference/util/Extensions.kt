package com.anggrayudi.materialpreference.util

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

fun String?.toSafeInt(defaultValue: Int = 0): Int {
    return try {
        this?.toInt() ?: defaultValue
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun Drawable?.applyTint(@ColorInt color: Int): Drawable? =
        apply { this?.mutate()?.setColorFilter(color, PorterDuff.Mode.SRC_IN)  }

fun Context.openWebsite(url: String) {
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (web.resolveActivity(packageManager) != null)
        startActivity(web)
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

fun EditText.onTextChanged(onTextChanged: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            onTextChanged(text?.toString().orEmpty())
        }

        override fun afterTextChanged(editable: Editable?) {
        }
    })
}