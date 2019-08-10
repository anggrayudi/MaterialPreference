package com.anggrayudi.materialpreference.callback

import com.anggrayudi.materialpreference.Preference
import com.google.android.material.textfield.TextInputLayout

/**
 * Called when a Preference has been clicked.
 *
 * @return True if the click was handled.
 */
typealias OnPreferenceClickListener = (preference: Preference) -> Boolean

/** Sets the callback to be invoked when this Preference is long clicked. */
typealias OnPreferenceLongClickListener = (preference: Preference) -> Boolean

/**
 * Called when a Preference has been changed by the user. This is
 * called before the state of the Preference is about to be updated and
 * before the state is persisted. This gives the client a chance
 * to prevent setting and/or persisting the value.
 *
 * @return True to update the state of the Preference with the new value.
 */
typealias OnPreferenceChangeListener = (preference: Preference, newValue: Any?) -> Boolean

/**
 * Called when the dialog view for this preference has been bound,
 * allowing you to customize the [TextInputLayout] displayed in the dialog.
 * It has the same function with [OnBindEditTextListener](https://developer.android.com/reference/androidx/preference/EditTextPreference.OnBindEditTextListener),
 * except it uses [TextInputLayout] as the callback's parameter.
 */
typealias OnBindTextInputLayoutListener = (textInputLayout: TextInputLayout) -> Unit