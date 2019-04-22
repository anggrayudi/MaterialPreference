/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License.
 */

package com.anggrayudi.materialpreference

import android.annotation.SuppressLint

/**
 * A data store interface to be implemented and provided to the Preferences framework. This can be
 * used to replace the default [android.content.SharedPreferences], if needed.
 *
 * In most cases you want to use [android.content.SharedPreferences] as it is automatically
 * backed up and migrated to new devices. However, providing custom data store to preferences can be
 * useful if your app stores its preferences in a local db, cloud or they are device specific like
 * "Developer settings". It might be also useful when you want to use the preferences UI but
 * the data are not supposed to be stored at all because they are valid per session only.
 *
 * Once a put method is called it is full responsibility of the data store implementation to
 * safely store the given values. Time expensive operations need to be done in the background to
 * prevent from blocking the UI. You also need to have a plan on how to serialize the data in case
 * the activity holding this object gets destroyed.
 *
 * By default, all "put" methods throw [UnsupportedOperationException].
 *
 * @see Preference.preferenceDataStore
 * @see PreferenceManager.preferenceDataStore
 */
@SuppressLint("RestrictedApi")
abstract class PreferenceDataStore {

    /**
     * Sets a [String] value to the data store.
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param value the new value for the preference
     * @see getString
     */
    fun putString(key: String, value: String?) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Sets a set of Strings to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param values the set of new values for the preference
     * @see getStringSet
     */
    fun putStringSet(key: String, values: Set<String>?) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Sets an [Integer] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param value the new value for the preference
     * @see getInt
     */
    fun putInt(key: String, value: Int) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Sets a [Long] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param value the new value for the preference
     * @see getLong
     */
    fun putLong(key: String, value: Long) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Sets a [Float] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param value the new value for the preference
     * @see getFloat
     */
    fun putFloat(key: String, value: Float) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Sets a [Boolean] value to the data store.
     *
     *
     * Once the value is set the data store is responsible for holding it.
     *
     * @param key the name of the preference to modify
     * @param value the new value for the preference
     * @see getBoolean
     */
    fun putBoolean(key: String, value: Boolean) {
        throw UnsupportedOperationException("Not implemented on this data store")
    }

    /**
     * Retrieves a [String] value from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see putString
     */
    fun getString(key: String, defValue: String? = null): String? = defValue

    /**
     * Retrieves a set of Strings from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValues values to return if this preference does not exist in the storage
     * @return the values from the data store or the default return values
     * @see putStringSet
     */
    fun getStringSet(key: String, defValues: Set<String>? = null): Set<String>? = defValues

    /**
     * Retrieves an [Integer] value from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see putInt
     */
    fun getInt(key: String, defValue: Int): Int = defValue

    /**
     * Retrieves a [Long] value from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see putLong
     */
    fun getLong(key: String, defValue: Long): Long = defValue

    /**
     * Retrieves a [Float] value from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see putFloat
     */
    fun getFloat(key: String, defValue: Float): Float = defValue

    /**
     * Retrieves a [Boolean] value from the data store.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see .getBoolean
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
}

