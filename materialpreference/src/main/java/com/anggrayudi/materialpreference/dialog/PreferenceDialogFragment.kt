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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.customview.customView
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial

/**
 * Abstract base class which presents a dialog associated with a
 * [DialogPreference]. Since the preference object may
 * not be available during fragment re-creation, the necessary information for displaying the dialog
 * is read once during the initial call to [.onCreate] and saved/restored in the saved
 * instance state. Custom subclasses should also follow this pattern.
 */
abstract class PreferenceDialogFragment : DialogFragment() {

    private var mPreference: DialogPreference? = null
    private var mDialogTitle: CharSequence? = null
    private var mDialogMessage: CharSequence? = null
    private var mDialogIcon: BitmapDrawable? = null
    @LayoutRes private var mDialogLayoutRes: Int = 0

    protected var mPositiveButtonText: CharSequence? = null
    protected var mNegativeButtonText: CharSequence? = null

    /** Which button was clicked.  */
    protected var mWhichButtonClicked = WhichButton.NEGATIVE

    /**
     * Get the preference that requested this dialog. Available after [.onCreate] has
     * been called on the [PreferenceFragmentMaterial] which launched this dialog.
     *
     * @return The [DialogPreference] associated with this dialog.
     */
    val preference: DialogPreference?
        get() {
            if (mPreference == null) {
                val key = arguments!!.getString(ARG_KEY)!!
                val fragment = targetFragment as DialogPreference.TargetFragment
                mPreference = fragment.findPreference(key) as DialogPreference
            }
            return mPreference
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawFragment = targetFragment as? DialogPreference.TargetFragment
                ?: throw IllegalStateException("Target fragment must implement TargetFragment interface")

        val key = arguments!!.getString(ARG_KEY)!!
        if (savedInstanceState == null) {
            mPreference = rawFragment.findPreference(key) as DialogPreference
            mDialogTitle = mPreference!!.dialogTitle
            mPositiveButtonText = mPreference!!.positiveButtonText
            mNegativeButtonText = mPreference!!.negativeButtonText
            mDialogMessage = mPreference!!.dialogMessage
            mDialogLayoutRes = mPreference!!.dialogLayoutResource

            val icon = mPreference!!.dialogIcon
            mDialogIcon = when {
                icon is BitmapDrawable -> icon
                icon != null -> {
                    val bitmap = Bitmap.createBitmap(icon.intrinsicWidth,
                            icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    BitmapDrawable(resources, bitmap)
                }
                else -> null
            }
        } else {
            mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
            mPositiveButtonText = savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT)
            mNegativeButtonText = savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT)
            mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
            mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0)
            val bitmap = savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_ICON)
            if (bitmap != null) {
                mDialogIcon = BitmapDrawable(resources, bitmap)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle)
        outState.putCharSequence(SAVE_STATE_POSITIVE_TEXT, mPositiveButtonText)
        outState.putCharSequence(SAVE_STATE_NEGATIVE_TEXT, mNegativeButtonText)
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage)
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes)
        if (mDialogIcon != null) {
            outState.putParcelable(SAVE_STATE_ICON, mDialogIcon!!.bitmap)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity!!
        var dialog = MaterialDialog(context)
                .title(text = mDialogTitle.toString())

        if (mDialogIcon != null)
            dialog.icon(drawable = mDialogIcon)

        val contentView = createDialogView(context)
        if (contentView != null) {
            onBindDialogView(contentView)
            dialog = dialog.customView(view = contentView, scrollable = true)
        } else if (mDialogMessage != null) {
            // normal dialog
            dialog.message(text = mDialogMessage)

            if (mPositiveButtonText != null)
                dialog.positiveButton(text = mPositiveButtonText) {
                    mWhichButtonClicked = WhichButton.POSITIVE
                }

            if (mNegativeButtonText != null)
                dialog.negativeButton(text = mNegativeButtonText) {
                    mWhichButtonClicked = WhichButton.NEGATIVE
                }
        }

        dialog = onPrepareDialog(dialog)

        // Create the dialog
        if (needInputMethod()) {
            requestInputMethod(dialog)
        }

        return dialog
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked.
     * Use this to set custom properties on the dialog.
     */
    protected open fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     */
    @RestrictTo(LIBRARY_GROUP)
    protected open fun needInputMethod(): Boolean {
        return false
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private fun requestInputMethod(dialog: Dialog) {
        val window = dialog.window
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    /**
     * Creates the content view for the dialog (if a custom content view is required).
     * By default, it inflates the dialog layout resource if it is set.
     *
     * @return The content View for the dialog.
     * @see DialogPreference.layoutResource
     */
    private fun createDialogView(context: Context): View? {
        val resId = mDialogLayoutRes
        if (resId == 0) {
            return null
        }
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(resId, null)
    }

    /**
     * Binds views in the content View of the dialog to data.
     *
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content View of the dialog, if it is custom.
     */
    protected open fun onBindDialogView(view: View) {
        val dialogMessageView = view.findViewById<View>(android.R.id.message)

        if (dialogMessageView != null) {
            val message = mDialogMessage
            var newVisibility = View.GONE

            if (!message.isNullOrEmpty()) {
                if (dialogMessageView is TextView) {
                    dialogMessageView.text = message
                }

                newVisibility = View.VISIBLE
            }

            if (dialogMessageView.visibility != newVisibility) {
                dialogMessageView.visibility = newVisibility
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogClosed(mWhichButtonClicked == WhichButton.POSITIVE)
    }

    abstract fun onDialogClosed(positiveResult: Boolean)

    companion object {

        internal const val ARG_KEY = "key"

        private const val SAVE_STATE_TITLE = "PreferenceDialogFragment.title"
        private const val SAVE_STATE_POSITIVE_TEXT = "PreferenceDialogFragment.positiveText"
        private const val SAVE_STATE_NEGATIVE_TEXT = "PreferenceDialogFragment.negativeText"
        private const val SAVE_STATE_MESSAGE = "PreferenceDialogFragment.message"
        private const val SAVE_STATE_LAYOUT = "PreferenceDialogFragment.layout"
        private const val SAVE_STATE_ICON = "PreferenceDialogFragment.icon"
    }
}
