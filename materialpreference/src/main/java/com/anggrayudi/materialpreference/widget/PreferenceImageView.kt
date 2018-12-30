/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.anggrayudi.materialpreference.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.anggrayudi.materialpreference.R

/**
 * Extension of [ImageView] that correctly applies `maxWidth` and `maxHeight`.
 *
 *      |   Attribute   | Value Type |
 *      |:-------------:|:----------:|
 *      | app:maxWidth  | Dimension  |
 *      | app:maxHeight | Dimension  |
 */
internal class PreferenceImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ImageView(context, attrs, defStyleAttr) {

    private var mMaxWidth = Integer.MAX_VALUE
    private var mMaxHeight = Integer.MAX_VALUE

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceImageView, defStyleAttr, 0)
        maxWidth = a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxWidth, Integer.MAX_VALUE)
        maxHeight = a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxHeight, Integer.MAX_VALUE)
        a.recycle()
    }

    override fun setMaxWidth(maxWidth: Int) {
        mMaxWidth = maxWidth
        super.setMaxWidth(maxWidth)
    }

    override fun getMaxWidth(): Int {
        return mMaxWidth
    }

    override fun setMaxHeight(maxHeight: Int) {
        mMaxHeight = maxHeight
        super.setMaxHeight(maxHeight)
    }

    override fun getMaxHeight(): Int {
        return mMaxHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        if (widthMode == View.MeasureSpec.AT_MOST || widthMode == View.MeasureSpec.UNSPECIFIED) {
            val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
            val maxWidth = maxWidth
            if (maxWidth != Integer.MAX_VALUE && (maxWidth < widthSize || widthMode == View.MeasureSpec.UNSPECIFIED)) {
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST)
            }
        }

        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode == View.MeasureSpec.AT_MOST || heightMode == View.MeasureSpec.UNSPECIFIED) {
            val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
            val maxHeight = maxHeight
            if (maxHeight != Integer.MAX_VALUE && (maxHeight < heightSize || heightMode == View.MeasureSpec.UNSPECIFIED)) {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
