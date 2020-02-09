package com.anggrayudi.materialpreference.processor

enum class PreferenceDataType {
    NOTHING,

    /**
     * Default data type. For `Preference`, `EditTextPreference`, `FolderPreference`,
     * `RingtonePreference` and `ListPreference`.
     */
    STRING,

    /**
     * For `CheckBoxPreference` and `SwitchPreference`.
     */
    BOOLEAN,

    /**
     * For `TimePreference`, `ColorPreference`, `IntegerListPreference`, `SeekBarDialogPreference` and `SeekBarPreference`.
     */
    INTEGER,

    /**
     * For `DatePreference`.
     */
    LONG,

    /**
     * For `MultiSelectListPreference`.
     */
    STRING_SET,

    // For future development
    /**
     * For `MultiSelectIntegerListPreference`.
     */
    INTEGER_SET,
    FLOAT
}