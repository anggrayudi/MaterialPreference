package com.anggrayudi.materialpreference.dialog

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.color.colorChooser
import com.anggrayudi.materialpreference.ColorPreference

class ColorPreferenceDialogFragment : PreferenceDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val colorPreference = preference as ColorPreference
        return MaterialDialog(requireContext())
                .title(text = colorPreference.dialogTitle.toString())
                .negativeButton(android.R.string.cancel)
                .colorChooser(colorPreference.colorList,
                        subColors = colorPreference.subColorList,
                        initialSelection = colorPreference.color,
                        allowCustomArgb = colorPreference.allowArgb,
                        showAlphaSelector = colorPreference.allowTransparency) { _, color ->
                    whichButtonClicked = WhichButton.POSITIVE
                    colorPreference.color = color
                }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
    }

    companion object {

        fun newInstance(key: String): ColorPreferenceDialogFragment {
            val b = Bundle(2)
            b.putString(ARG_KEY, key)
            val fragment = ColorPreferenceDialogFragment()
            fragment.arguments = b
            return fragment
        }
    }
}