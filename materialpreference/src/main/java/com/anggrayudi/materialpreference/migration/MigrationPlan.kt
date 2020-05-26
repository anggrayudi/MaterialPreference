package com.anggrayudi.materialpreference.migration

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

@SuppressLint("ApplySharedPref")
class MigrationPlan internal constructor(
    val preferences: SharedPreferences,
    private val preferenceVersionKey: String,
    private val newVersion: Int
) {

    private val editor = preferences.edit()

    internal fun updateVersion(migration: PreferenceMigration) {
        editor.putInt(preferenceVersionKey, newVersion).apply()
        val previousOSVersion = preferences.getInt(PreferenceMigration.OS_VERSION, 0)
        if (previousOSVersion != Build.VERSION.SDK_INT) {
            if (previousOSVersion != 0) {
                migration.onNewOS(this, previousOSVersion)
            }
            editor.putInt(PreferenceMigration.OS_VERSION, Build.VERSION.SDK_INT).apply()
        }
    }

    /**
     * Clear this `SharedPreferences`.
     */
    fun clear(): MigrationPlan {
        editor.clear().commit()
        return this
    }

    /**
     * Delete a preference by its key.
     */
    fun remove(key: String): MigrationPlan {
        editor.remove(key).commit()
        return this
    }

    fun updateValue(key: String, newValue: Boolean): MigrationPlan {
        editor.putBoolean(key, newValue).commit()
        return this
    }

    fun updateValue(key: String, newValue: Int): MigrationPlan {
        editor.putInt(key, newValue).commit()
        return this
    }

    fun updateValue(key: String, newValue: Long): MigrationPlan {
        editor.putLong(key, newValue).commit()
        return this
    }

    fun updateValue(key: String, newValue: Float): MigrationPlan {
        editor.putFloat(key, newValue).commit()
        return this
    }

    fun updateValue(key: String, newValue: String?): MigrationPlan {
        editor.putString(key, newValue).commit()
        return this
    }

    fun updateValue(key: String, newValue: Set<String>?): MigrationPlan {
        editor.putStringSet(key, newValue).commit()
        return this
    }

    /**
     * Rename a preference's key.
     */
    fun renameKey(oldKey: String, newKey: String): MigrationPlan {
        when (findValueType(oldKey)) {
            ValueType.BOOLEAN -> {
                val oldValue = preferences.getBoolean(oldKey, false)
                editor.remove(oldKey).putBoolean(newKey, oldValue).commit()
            }

            ValueType.INTEGER -> {
                val oldValue = preferences.getInt(oldKey, 0)
                editor.remove(oldKey).putInt(newKey, oldValue).commit()
            }

            ValueType.LONG -> {
                val oldValue = preferences.getLong(oldKey, 0)
                editor.remove(oldKey).putLong(newKey, oldValue).commit()
            }

            ValueType.FLOAT -> {
                val oldValue = preferences.getFloat(oldKey, 0f)
                editor.remove(oldKey).putFloat(newKey, oldValue).commit()
            }

            ValueType.STRING -> {
                val oldValue = preferences.getString(oldKey, null)
                editor.remove(oldKey).putString(newKey, oldValue).commit()
            }

            ValueType.STRING_SET -> {
                val oldValue = preferences.getStringSet(oldKey, null)
                editor.remove(oldKey).putStringSet(newKey, oldValue).commit()
            }

            else -> Log.d(TAG, "Cannot find old preference key: $oldKey")
        }
        return this
    }

    private fun findValueType(key: String): ValueType {
        if (!preferences.contains(key)) {
            return ValueType.UNDEFINED
        }

        val testValueType: (ValueType) -> ValueType = { typeTest ->
            when (typeTest) {
                ValueType.BOOLEAN -> {
                    preferences.getBoolean(key, false)
                    ValueType.BOOLEAN
                }

                ValueType.INTEGER -> {
                    preferences.getInt(key, 0)
                    ValueType.INTEGER
                }

                ValueType.LONG -> {
                    preferences.getLong(key, 0)
                    ValueType.LONG
                }

                ValueType.FLOAT -> {
                    preferences.getFloat(key, 0f)
                    ValueType.FLOAT
                }

                ValueType.STRING -> {
                    preferences.getString(key, null)
                    ValueType.STRING
                }

                ValueType.STRING_SET -> {
                    preferences.getStringSet(key, null)
                    ValueType.STRING_SET
                }

                else -> ValueType.UNDEFINED
            }
        }

        for (i in 1 until ValueType.values().size) {
            try {
                return testValueType(ValueType.values()[i])
            } catch (e: ClassCastException) {
                // ignore
            }
        }

        return ValueType.UNDEFINED
    }

    companion object {
        private const val TAG = "MigrationPlan"
    }
}
