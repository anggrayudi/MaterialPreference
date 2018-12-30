package com.anggrayudi.materialpreference.dialog

import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.anggrayudi.materialpreference.R
import com.anggrayudi.materialpreference.SeekBarDialogPreference
import java.util.*

/**
 * @author Eugen on 7. 12. 2015.
 */
class SeekBarPreferenceDialogFragment : PreferenceDialogFragment(), View.OnKeyListener {

    private var mSeekBar: SeekBar? = null
    private var mTextMin: TextView? = null
    private var mTextMax: TextView? = null
    private var mTextValue: TextView? = null

    private val seekBarDialogPreference: SeekBarDialogPreference
        get() = preference as SeekBarDialogPreference

    private var mKeyProgressIncrement: Int = 0

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val preference = seekBarDialogPreference

        mTextMin = view.findViewById(R.id.seekbar_min)
        mTextMax = view.findViewById(R.id.seekbar_max)
        mTextValue = view.findViewById(R.id.seekbar_value)
        mSeekBar = view.findViewById(R.id.seekbar)

        val max = preference.max
        mTextMax!!.text = String.format(Locale.US, "%d", max)

        val min = preference.min
        mTextMin!!.text = String.format(Locale.US, "%d", min)

        mSeekBar!!.max = max - min
        mSeekBar!!.progress = preference.value - min
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (preference.summaryFormatter != null)
                    mTextValue!!.text = preference.summaryFormatter!!.invoke(progress + min)
                else
                    mTextValue!!.text = String.format(Locale.US, "%d", progress + min)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        listener.onProgressChanged(mSeekBar!!, mSeekBar!!.progress, false)
        mSeekBar!!.setOnSeekBarChangeListener(listener)
        mKeyProgressIncrement = mSeekBar!!.keyProgressIncrement
        mSeekBar!!.setOnKeyListener(this)

        setupAccessibilityDelegate(max, min)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog
                .positiveButton(text = mPositiveButtonText ?: getString(android.R.string.ok)) {
                    mWhichButtonClicked = WhichButton.POSITIVE
                }
                .negativeButton(text = mNegativeButtonText ?: getString(android.R.string.cancel)) {
                    mWhichButtonClicked = WhichButton.NEGATIVE
                }
    }

    private fun setupAccessibilityDelegate(max: Int, min: Int) {
        mSeekBar!!.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                super.onInitializeAccessibilityEvent(host, event)

                val progress = mSeekBar!!.progress + min
                event.contentDescription = progress.toString() + ""

                //                    event.setItemCount(max - min);
                //                    event.setFromIndex(min);
                //                    event.setToIndex(max);
                //                    event.setCurrentItemIndex(progress);
            }

            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                val progress = mSeekBar!!.progress + min
                info.contentDescription = progress.toString() + ""
            }
        })
    }

    private fun hasDialogTitle(): Boolean {
        val preference = preference
        var dialogTitle = preference!!.dialogTitle
        if (dialogTitle == null) dialogTitle = preference.title
        return !TextUtils.isEmpty(dialogTitle)
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) {
            val step = mKeyProgressIncrement
            if (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS) {
                mSeekBar!!.progress = mSeekBar!!.progress + step
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MINUS) {
                mSeekBar!!.progress = mSeekBar!!.progress - step
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        mSeekBar?.setOnKeyListener(null)
        super.onDestroyView()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = seekBarDialogPreference
        if (positiveResult) {
            val progress = mSeekBar!!.progress + preference.min
            if (preference.callChangeListener(progress)) {
                preference.value = progress
            }
        }
    }

    companion object {

        fun newInstance(key: String): SeekBarPreferenceDialogFragment {
            val fragment = SeekBarPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString("key", key)
            fragment.arguments = b
            return fragment
        }
    }
}
