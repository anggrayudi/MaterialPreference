package com.anggrayudi.materialpreference.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PreferenceKeysConfig(
    val xmlResName: String = "preferences.xml",
    /** String res file. Change it if you put preference keys in different file. */
    val stringResName: String = "strings.xml",
    val className: String = "PrefKey",
    /** True if you want to use Java constant style. */
    val capitalStyle: Boolean = true
)
