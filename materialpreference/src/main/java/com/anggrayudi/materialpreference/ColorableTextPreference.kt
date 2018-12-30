package com.anggrayudi.materialpreference

import android.content.res.ColorStateList

import androidx.annotation.ColorInt

interface ColorableTextPreference {
    fun setTitleTextColor(@ColorInt colorRes: Int = 0, stateList: ColorStateList? = null)

    fun setSummaryTextColor(@ColorInt colorRes: Int = 0, stateList: ColorStateList? = null)

    fun setTitleTextAppearance(titleTextAppearance: Int)

    fun setSummaryTextAppearance(summaryTextAppearance: Int)
}
