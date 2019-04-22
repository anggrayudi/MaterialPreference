package com.anggrayudi.materialpreference.dialog

import android.os.Bundle
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

    private var seekBar: SeekBar? = null
    private var textMin: TextView? = null
    private var textMax: TextView? = null
    private var textValue: TextView? = null
    private var keyProgressIncrement: Int = 0

    private val seekBarDialogPreference: SeekBarDialogPreference
        get() = preference as SeekBarDialogPreference

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val preference = seekBarDialogPreference

        textMin = view.findViewById(R.id.seekbar_min)
        textMax = view.findViewById(R.id.seekbar_max)
        textValue = view.findViewById(R.id.seekbar_value)
        seekBar = view.findViewById(R.id.seekbar)

        val max = preference.max
        textMax!!.text = String.format(Locale.US, "%d", max)

        val min = preference.min
        textMin!!.text = String.format(Locale.US, "%d", min)

        seekBar!!.max = max - min
        seekBar!!.progress = preference.value - min
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (preference.summaryFormatter != null)
                    textValue!!.text = preference.summaryFormatter!!.invoke(progress + min)
                else
                    textValue!!.text = String.format(Locale.US, "%d", progress + min)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        listener.onProgressChanged(seekBar!!, seekBar!!.progress, false)
        seekBar!!.setOnSeekBarChangeListener(listener)
        keyProgressIncrement = seekBar!!.keyProgressIncrement
        seekBar!!.setOnKeyListener(this)

        setupAccessibilityDelegate(max, min)
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        return dialog
                .positiveButton(text = positiveButtonText ?: getString(android.R.string.ok)) {
                    whichButtonClicked = WhichButton.POSITIVE
                }
                .negativeButton(text = negativeButtonText ?: getString(android.R.string.cancel)) {
                    whichButtonClicked = WhichButton.NEGATIVE
                }
    }

    private fun setupAccessibilityDelegate(max: Int, min: Int) {
        seekBar!!.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                super.onInitializeAccessibilityEvent(host, event)

                val progress = seekBar!!.progress + min
                event.contentDescription = progress.toString() + ""

                //                    event.setItemCount(max - min);
                //                    event.setFromIndex(min);
                //                    event.setToIndex(max);
                //                    event.setCurrentItemIndex(progress);
            }

            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)

                val progress = seekBar!!.progress + min
                info.contentDescription = progress.toString() + ""
            }
        })
    }

    private fun hasDialogTitle(): Boolean {
        val preference = preference
        val dialogTitle = preference!!.dialogTitle ?: preference.title
        return !dialogTitle.isNullOrEmpty()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) {
            val step = keyProgressIncrement
            if (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS) {
                seekBar!!.progress = seekBar!!.progress + step
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MINUS) {
                seekBar!!.progress = seekBar!!.progress - step
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        seekBar?.setOnKeyListener(null)
        super.onDestroyView()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = seekBarDialogPreference
        if (positiveResult) {
            val progress = seekBar!!.progress + preference.min
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
