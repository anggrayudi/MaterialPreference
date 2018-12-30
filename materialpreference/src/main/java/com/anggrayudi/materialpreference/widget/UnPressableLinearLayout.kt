/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.anggrayudi.materialpreference.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Custom LinearLayout that does not propagate the pressed state down to its children.
 * By default, the pressed state is propagated to all the children that are not clickable or long-clickable.
 */
internal class UnPressableLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : LinearLayout(context, attrs) {

    override fun dispatchSetPressed(pressed: Boolean) {
        // Skip dispatching the pressed key state to the children so that they don't trigger any
        // pressed state animation on their stateful drawables.
    }
}
