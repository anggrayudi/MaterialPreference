package com.anggrayudi.materialpreference.callback

import com.anggrayudi.materialpreference.Preference

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