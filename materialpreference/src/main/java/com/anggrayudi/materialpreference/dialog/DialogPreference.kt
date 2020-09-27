/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.anggrayudi.materialpreference.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.TypedArrayUtils
import com.anggrayudi.materialpreference.Preference
import com.anggrayudi.materialpreference.R
import com.anggrayudi.materialpreference.util.getSupportDrawable

/**
 * A base class for [Preference] objects that are dialog-based.
 * These preferences will, when clicked, open a dialog showing the actual preference controls.
 *
 *      |          Attribute         | Value Type |
 *      |:--------------------------:|:----------:|
 *      | android:dialogTitle        | Dimension  |
 *      | android:dialogMessage      | Dimension  |
 *      | android:dialogIcon         | Drawable   |
 *      | android:dialogLayout       | Layout     |
 *      | android:positiveButtonText | String     |
 *      | android:negativeButtonText | String     |
 */
@SuppressLint("RestrictedApi")
abstract class DialogPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,android.R.attr.dialogPreferenceStyle),
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    /** Sets and gets the title of the dialog. */
    var dialogTitle: CharSequence? = null
        get() = field ?: title

    /**
     * Sets the message of the dialog. This will be shown on subsequent dialogs.
     *
     * This message forms the content View of the dialog and conflicts with
     * list-based dialogs, for example. If setting a custom View on a dialog via
     * [dialogLayoutResource], include a text View with ID [android.R.id.message]
     * and it will be populated with this message.
     *
     * @return Message to be shown on subsequent dialogs.
     */
    var dialogMessage: CharSequence? = null

    /**
     * Sets the icon of the dialog. This will be shown on subsequent dialogs.
     *
     * @return The icon, as a [Drawable].
     */
    var dialogIcon: Drawable? = null

    /**
     * Sets the text of the positive button of the dialog. This will be shown on subsequent dialogs.
     *
     * @return The text of the positive button.
     */
    var positiveButtonText: CharSequence? = null

    /**
     * Sets the text of the negative button of the dialog. This will be shown on subsequent dialogs.
     *
     * @return The text of the negative button.
     */
    var negativeButtonText: CharSequence? = null

    /**
     * Sets the layout resource that is inflated as the [View] to be shown
     * as the content View of subsequent dialogs.
     *
     * @return The layout resource.
     * @see .setDialogMessage
     */
    var dialogLayoutResource: Int = 0

    /**
     * @see dialogTitle
     * @param dialogTitleResId The dialog title as a resource.
     */
    fun setDialogTitle(@StringRes dialogTitleResId: Int) {
        dialogTitle = context.getString(dialogTitleResId)
    }

    /**
     * @see dialogMessage
     * @param dialogMessageResId The dialog message as a resource.
     */
    fun setDialogMessage(@StringRes dialogMessageResId: Int) {
        dialogMessage = context.getString(dialogMessageResId)
    }

    /**
     * Sets the icon (resource ID) of the dialog. This will be shown on subsequent dialogs.
     *
     * @param dialogIconRes The icon, as a resource ID.
     */
    fun setDialogIcon(@DrawableRes dialogIconRes: Int) {
        dialogIcon = context.getSupportDrawable(dialogIconRes)
    }

    /**
     * @see positiveButtonText
     * @param positiveButtonTextResId The positive button text as a resource.
     */
    fun setPositiveButtonText(@StringRes positiveButtonTextResId: Int) {
        positiveButtonText = context.getString(positiveButtonTextResId)
    }

    /**
     * @see negativeButtonText
     * @param negativeButtonTextResId The negative button text as a resource.
     */
    fun setNegativeButtonText(@StringRes negativeButtonTextResId: Int) {
        negativeButtonText = context.getString(negativeButtonTextResId)
    }

    interface TargetFragment {
        fun findPreference(key: CharSequence): Preference?
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DialogPreference, defStyleAttr, defStyleRes)
        dialogTitle = a.getString(R.styleable.DialogPreference_android_dialogTitle) ?: title
        dialogMessage = a.getString(R.styleable.DialogPreference_android_dialogMessage)
        dialogIcon = a.getSupportDrawable(context, R.styleable.DialogPreference_android_dialogIcon)
        positiveButtonText = a.getString(R.styleable.DialogPreference_android_positiveButtonText)
        negativeButtonText = a.getString(R.styleable.DialogPreference_android_negativeButtonText)
        dialogLayoutResource = a.getResourceId(R.styleable.DialogPreference_android_dialogLayout, 0)
        a.recycle()
    }

    override fun onClick() {
        preferenceManager!!.showDialog(this)
    }
}
