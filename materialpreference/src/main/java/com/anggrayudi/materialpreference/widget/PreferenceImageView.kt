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

    private var maxWidth = Integer.MAX_VALUE
    private var maxHeight = Integer.MAX_VALUE

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceImageView, defStyleAttr, 0)
        maxWidth = a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxWidth, Integer.MAX_VALUE)
        maxHeight = a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxHeight, Integer.MAX_VALUE)
        a.recycle()
    }

    override fun setMaxWidth(maxWidth: Int) {
        this.maxWidth = maxWidth
        super.setMaxWidth(maxWidth)
    }

    override fun getMaxWidth(): Int {
        return maxWidth
    }

    override fun setMaxHeight(maxHeight: Int) {
        this.maxHeight = maxHeight
        super.setMaxHeight(maxHeight)
    }

    override fun getMaxHeight(): Int {
        return maxHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec
        val widthMode = MeasureSpec.getMode(widthSpec)
        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            val widthSize = MeasureSpec.getSize(widthSpec)
            val maxWidth = maxWidth
            if (maxWidth != Integer.MAX_VALUE && (maxWidth < widthSize || widthMode == MeasureSpec.UNSPECIFIED)) {
                widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
            }
        }

        val heightMode = MeasureSpec.getMode(heightSpec)
        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
            val heightSize = MeasureSpec.getSize(heightSpec)
            val maxHeight = maxHeight
            if (maxHeight != Integer.MAX_VALUE && (maxHeight < heightSize || heightMode == MeasureSpec.UNSPECIFIED)) {
                heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            }
        }

        super.onMeasure(widthSpec, heightSpec)
    }
}
