package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.TypedArrayUtils
import com.anggrayudi.materialpreference.dialog.DialogPreference
import com.anggrayudi.materialpreference.util.StringSummaryFormatter
import com.anggrayudi.materialpreference.widget.ColorCircleView

/**
 * A [Preference] class that provides color picker.
 *
 *      |     Attribute    | Value Type |
 *      |:----------------:|:----------:|
 *      | app:defaultColor | Color      |
 */
@SuppressLint("RestrictedApi")
open class ColorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.colorPreferenceStyle,
        android.R.attr.dialogPreferenceStyle),
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    var colorList = DEFAULT_COLOR_LIST
        set(a) {
            if (a.isEmpty())
                throw IllegalArgumentException("Empty array is not allowed")

            field = a
        }

    var subColorList: Array<IntArray>? = null

    var allowArgb = false

    var allowTransparency = false

    var defaultColor: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ColorPreference,
            defStyleAttr, defStyleRes)
        defaultColor = a.getColor(R.styleable.ColorPreference_defaultColor, colorList[0])
        a.recycle()
    }

    override var isLegacySummary: Boolean
        get() = true
        set(value) {
            super.isLegacySummary = value
        }

    /**
     * Lets you control how displaying value to summary. Suppose that the given value is
     * **#F44336**, then you set:
     *
     *       colorPreference.summaryFormatter = {
     *          val colorInt = Color.parseColor(it)
     *
     *          when (it) {
     *              "#F44336" -> "Red"
     *              "#E91E63" -> "Pink"
     *              else -> it
     *          }
     *       }
     *
     * It will produce **Red** to the summary.
     */
    var summaryFormatter: StringSummaryFormatter? = null
        set(f) {
            field = f
            updateSummary()
        }

    /** Get or set color, and then save it to preference */
    var color: Int
        get() = _color
        set(v) {
            if (v != _color && callChangeListener(v)) {
                _color = v
                _colorHex = String.format("#%06X", 0xFFFFFF and v)
                persistInt(v)
                if (isBindValueToSummary) {
                    updateSummary()
                } else {
                    notifyChanged()
                }
            }
        }
    private var _color: Int = defaultColor

    /** Get or set color in HEX, and then save it to preference */
    var colorHex: String
        get() = _colorHex
        set(value) {
            if (value != _colorHex && callChangeListener(value)) {
                _colorHex = value
                _color = Color.parseColor(value)
                persistInt(_color)
                if (isBindValueToSummary) {
                    updateSummary()
                } else {
                    notifyChanged()
                }
            }
        }
    private var _colorHex: String = String.format("#%06X", 0xFFFFFF and defaultColor)

    /** Border color will not be saved to preference */
    @ColorInt
    var border: Int = Color.TRANSPARENT
        set(value) {
            field = value
            notifyChanged()
        }

    override fun onSetInitialValue() {
        _color = getPersistedInt(color)
        _colorHex = String.format("#%06X", 0xFFFFFF and _color)
        updateSummary()
    }

    private fun updateSummary() {
        if (isBindValueToSummary) {
            summary = summaryFormatter?.invoke(colorHex) ?: colorHex
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val colorView = holder.findViewById(R.id.color_view) as ColorCircleView
        colorView.border = border
        colorView.color = color
    }

    companion object {

        val DEFAULT_COLOR_LIST = intArrayOf(
            Color.parseColor("#F44336"), // red
            Color.parseColor("#E91E63"), // pink
            Color.parseColor("#9C27B0"), // purple
            Color.parseColor("#673AB7"), // deep purple
            Color.parseColor("#3F51B5"), // indigo
            Color.parseColor("#2196F3"), // blue
            Color.parseColor("#03A9F4"), // light blue
            Color.parseColor("#00BCD4"), // cyan
            Color.parseColor("#009688"), // teal
            Color.parseColor("#4CAF50"), // green
            Color.parseColor("#8BC34A"), // light green
            Color.parseColor("#CDDC39"), // lime
            Color.parseColor("#FFEB3B"), // yellow
            Color.parseColor("#FFC107"), // amber
            Color.parseColor("#FF9800"), // orange
            Color.parseColor("#FF5722"), // deep orange
            Color.parseColor("#795548"), // brown
            Color.parseColor("#9E9E9E"), // gray
            Color.parseColor("#607D8B"), // blue gray
            Color.parseColor("#000000")) // black
    }
}