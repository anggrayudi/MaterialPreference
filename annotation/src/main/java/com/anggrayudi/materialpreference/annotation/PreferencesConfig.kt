package com.anggrayudi.materialpreference.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PreferencesConfig(
        val preferencesXmlRes: String = "preferences.xml",
        val prefKeyClassName: String = "PrefKey",
        val prefHelperClassName: String = "SharedPreferencesHelper",
        /** True if you want to use Java constant style. */
        val capitalStyle: Boolean = true
)
