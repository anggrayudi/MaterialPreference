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

package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * Represents the basic Preference UI building block displayed by a [PreferenceFragmentMaterial]
 * in the form of a [RecyclerView]. This class provides data for the [View] to be displayed
 * in the list and associates with a [SharedPreferences] to store/retrieve the preference data.
 *
 * When specifying a preference hierarchy in XML, each element can point to a
 * subclass of [Preference], similar to the view hierarchy and layouts.
 *
 * This class contains a `key` that will be used as the key into the [SharedPreferences].
 * It is up to the subclass to decide how to store the value.
 *
 * For more information, see [Material Preference Guide](https://github.com/anggrayudi/MaterialPreference)
 *
 *      |          Attribute         |           Value Type           |
 *      |:--------------------------:|:------------------------------:|
 *      | android:key                | String                         |
 *      | android:title              | String                         |
 *      | android:summary            | String                         |
 *      | android:order              | Int                            |
 *      | android:fragment           | androidx.fragment.app.Fragment |
 *      | android:layout             | Layout                         |
 *      | android:widgetLayout       | Layout                         |
 *      | android:icon               | Drawable                       |
 *      | android:enabled            | Boolean                        |
 *      | android:selectable         | Boolean                        |
 *      | android:dependency         | String                         |
 *      | android:persistent         | Boolean                        |
 *      | android:shouldDisableView  | Boolean                        |
 *      | android:singleLineTitle    | Boolean                        |
 *      | android:iconSpaceReserved  | Boolean                        |
 *      | android:fontFamily         | Font                           |
 *      | android:visible            | Boolean                        |
 *      | app:titleTextColor         | Color                          |
 *      | app:tintIcon               | Color                          |
 *      | app:reverseDependencyState | Boolean                        |
 *      | app:legacySummary          | Boolean                        |
 *      | app:bindValueToSummary     | Boolean                        |
 *      | app:iconSize               | normal, large                  |
 */
@SuppressLint("RestrictedApi")
open class Preference @JvmOverloads constructor(
        val context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.preferenceStyle,
                android.R.attr.preferenceStyle), defStyleRes: Int = 0)
    : Comparable<Preference> {

    /**
     * Gets the [PreferenceManager] that manages this Preference object's tree.
     *
     * @return The [PreferenceManager].
     */
    var preferenceManager: PreferenceManager? = null
        private set

    /**
     * By default preferences always use [SharedPreferences]. To make this
     * preference to use the [PreferenceDataStore] you need to assign your implementation
     * to the Preference itself via [preferenceDataStore] or to its
     * [PreferenceManager] via [PreferenceManager.preferenceDataStore].
     *
     * It also sets a [PreferenceDataStore] to be used by this Preference instead of using
     * [SharedPreferences].
     *
     * The data store will remain assigned even if the Preference is moved around the preference
     * hierarchy. It will also override a data store propagated from the [PreferenceManager]
     * that owns this Preference.
     *
     * @return The data store that should be used by this Preference to store / retrieve data. If null then
     * [PreferenceManager.preferenceDataStore] needs to be checked. If that one is null
     * too it means that we are using [SharedPreferences] to store the data. Returns `null` if
     * [SharedPreferences] is used instead.
     *
     * @see PreferenceManager.preferenceDataStore
     */
    var preferenceDataStore: PreferenceDataStore? = null
        get() {
            if (field != null) {
                return field
            } else if (preferenceManager != null) {
                return preferenceManager!!.preferenceDataStore
            }
            return null
        }

    /**
     * Returns a unique ID for this Preference. This ID should be unique across all
     * Preference objects in a hierarchy.
     *
     * @return A unique ID for this Preference.
     */
    internal var id: Long = 0
        private set

    /** Set true temporarily to keep [onAttachedToHierarchy] from overwriting [id] */
    private var hasId: Boolean = false

    /**
     * Sets the callback to be invoked when this Preference is changed by the
     * user (but before the internal state has been updated).
     *
     * @return The callback to be invoked when this Preference is changed by the
     * user (but before the internal state has been updated).
     * @see OnPreferenceChangeListener
     */
    var onPreferenceChangeListener: OnPreferenceChangeListener? = null

    /**
     * Sets the callback to be invoked when this Preference is clicked.
     * @return True if the click was handled.
     * @see OnPreferenceClickListener
     */
    var onPreferenceClickListener: OnPreferenceClickListener? = null

    /**
     * Sets the callback to be invoked when this Preference is long clicked.
     * @see OnPreferenceLongClickListener
     */
    var onPreferenceLongClickListener: OnPreferenceLongClickListener? = null

    /**
     * Set the ID that will be assigned to the overall View representing this
     * preference, once bound.
     *
     * @see View.setId
     */
    private var viewId = 0

    /**
     * Sets an [Intent] to be used for [Context.startActivity] when this Preference is clicked.
     *
     * @return the [Intent] associated with this Preference.
     */
    var intent: Intent? = null

    /**
     * Sets the class name of a fragment to be shown when this Preference is clicked.
     *
     * @return The fragment class name last set via [fragment] or XML.
     */
    var fragment: String? = null

    /**
     * Sets whether this Preference is persistent. When persistent, it stores its value(s) into
     * the persistent [SharedPreferences] storage by default or into [PreferenceDataStore] if assigned.
     *
     * @return Checks whether this Preference is persistent. If it is, it stores its value(s) into
     * the persistent [SharedPreferences] storage by default or into
     * [PreferenceDataStore] if assigned.
     */
    var isPersistent: Boolean = true
        internal set

    /**
     * Sets the default value for this Preference, which will be set either if
     * persistence is off or persistence is on and the preference is not found
     * in the persistent storage.
     */
    @Deprecated("Use your own static method to set the preference's default value")
    var defaultValue: Any? = null

    var tag: Any? = null

    /**
     * Returns true if [onDetached] was called. Used for handling the case when a
     * preference was removed, modified, and re-added to a [PreferenceGroup]
     */
    internal var wasDetached = false

    private var dependencyMet = true
    private var parentDependencyMet = true
    private var hasSingleLineTitleAttr: Boolean = false
    private val fontFamily: Typeface?
    private val titleTextColor: Int

    /**
     * Sets the layout resource that is inflated as the [View] to be shown
     * for this Preference. In most cases, the default layout is sufficient for
     * custom Preference objects and only the widget layout needs to be changed.
     *
     * This layout should contain a [ViewGroup] with ID [android.R.id.widget_frame]
     * to be the parent of the specific widget for this Preference. It should similarly contain
     * [android.R.id.title] and [android.R.id.summary].
     *
     * It is an error to change the layout after adding the preference to a [PreferenceGroup]
     *
     * @return Layout resource that will be shown as the [View] for this Preference.
     * @see widgetLayoutResource
     */
    var layoutResource = R.layout.preference

    /**
     * Sets the layout for the controllable widget portion of this Preference. This
     * is inflated into the main layout. For example, a [CheckBoxPreference]
     * would specify a custom layout (consisting of just the CheckBox) here,
     * instead of creating its own main layout.
     *
     * It is an error to change the layout after adding the preference to a [PreferenceGroup]
     *
     * @return Layout resource for the controllable widget portion of this Preference.
     * @see layoutResource
     */
    var widgetLayoutResource: Int = 0

    /** Sets the internal change listener. */
    internal var onPreferenceChangeInternalListener: OnPreferenceChangeInternalListener? = null

    private var dependents: MutableList<Preference>? = null
    
    /**
     * Returns the [PreferenceGroup] which is this Preference assigned to or null if this
     * preference is not assigned to any group or is a root Preference.
     *
     * @return The parent PreferenceGroup or null if not attached to any.
     */
    @get:Nullable
    var parent: PreferenceGroup? = null
        private set

    internal var preferenceViewHolder: PreferenceViewHolder? = null

    private var baseMethodCalled: Boolean = false

    private val clickListener = View.OnClickListener { v -> performClick(v) }

    private val longClickListener = View.OnLongClickListener {
        onPreferenceLongClickListener != null && onPreferenceLongClickListener!!.invoke(this@Preference) }

    /**
     * Set to `true` if you want to put the summary horizontally with this preference's title.
     *
     * This value always `false` on these preferences:<br>
     *  * [CheckBoxPreference]
     *  * [SwitchPreference]
     *  * [PreferenceScreen]
     *  * [PreferenceCategory]
     *  * [RingtonePreference]
     *  * [MultiSelectListPreference]
     *
     * and always `true` on [SeekBarPreference].
     */
    open var isLegacySummary: Boolean
        get() = _legacySummary
        set(enable) {
            if (javaClass != Preference::class.java) {
                _legacySummary = (this is TwoStatePreference || this is PreferenceGroup
                        || enable && this !is SeekBarPreference)
                notifyChanged()
            }
        }
    private var _legacySummary: Boolean = false

    /**
     * Set `true` if you want to bind this preference's value to summary. Default value is `true`.
     * Set to `false` if you want to set your own custom summary.
     */
    open var isBindValueToSummary: Boolean
        get() = _bindValueToSummary
        set(enable) {
            _bindValueToSummary = enable
            notifyChanged()
        }
    private var _bindValueToSummary: Boolean = true

    var isReverseDependencyState: Boolean
        get() = _reverseDependencyState
        set(reverse) {
            if (reverse != _reverseDependencyState) {
                _reverseDependencyState = reverse
                notifyDependencyChange(shouldDisableDependents())
                notifyChanged()
            }
        }
    private var _reverseDependencyState: Boolean = false

    /**
     * Return the extras [Bundle] object associated with this preference, creating
     * a new `Bundle` if there currently isn't one.  You can use this to get and
     * set individual extra key/value pairs.
     */
    val extras: Bundle
        get() {
            if (_extras == null) {
                _extras = Bundle()
            }
            return _extras!!
        }
    private var _extras: Bundle? = null

    /**
     * Sets the order of this Preference with respect to other
     * Preference objects on the same level. If this is not specified, the
     * default behavior is to sort alphabetically. The
     * [PreferenceGroup.orderingAsAdded] can be used to order
     * Preference objects based on the order they appear in the XML.
     *
     * A lower value will be shown first. Use [DEFAULT_ORDER] to sort alphabetically or allow ordering from XML.
     *
     * @return Gets the order of this Preference with respect to other Preference objects on the same level.
     *
     * @see PreferenceGroup.orderingAsAdded
     * @see DEFAULT_ORDER
     */
    // Reorder the list
    var order: Int
        get() = _order
        set(order) {
            if (order != _order) {
                _order = order
                notifyHierarchyChanged()
            }
        }
    private var _order = DEFAULT_ORDER

    /**
     * Sets the title for this Preference with a CharSequence.
     * This title will be placed into the ID [android.R.id.title]
     * within the View bound by [onBindViewHolder].
     */
    var title: CharSequence?
        get() = _title
        set(title) {
            if (title == null && _title != null || title != null && title != _title) {
                _title = title
                notifyChanged()
            }
        }
    private var _title: CharSequence? = null

    var tintIcon: Int
        get() = _tintIcon
        set(color) {
            _tintIcon = color
            if (_icon != null && color != Color.TRANSPARENT) {
                notifyChanged()
            }
        }
    private var _tintIcon: Int = 0

    /**
     * Sets the icon for this Preference with a [Drawable].
     * This icon will be placed into the ID [android.R.id.icon] within the [View] created by [onBindViewHolder].
     */
    var icon: Drawable?
        get() {
            if (_icon == null && iconResId != 0) {
                _icon = ContextCompat.getDrawable(context, iconResId)
            }
            return _icon
        }
        set(icon) {
            if (icon == null && _icon != null || icon != null && _icon !== icon) {
                _icon = icon
                iconResId = 0
                notifyChanged()
            }
        }
    private var _icon: Drawable? = null

    /** iconResId is overridden by [icon], if [icon] is specified. */
    private var iconResId: Int = 0

    // TODO 31-Dec-18: Icon size
    private val iconSize: Int

    /**
     * Sets the icon for this Preference with a resource ID.
     *
     * @see icon
     * @param iconResId The icon as a resource ID.
     */
    fun setIcon(@DrawableRes iconResId: Int) {
        icon = ContextCompat.getDrawable(context, iconResId)
        this.iconResId = iconResId
    }

    /** Sets the summary for this Preference with a CharSequence. */
    open var summary: CharSequence?
        get() = _summary
        set(summary) {
            if (summary == null && _summary != null || summary != null && summary != _summary) {
                _summary = summary
                notifyChanged()
            }
        }
    private var _summary: CharSequence? = null

    /**
     * Sets whether this Preference is enabled. If disabled, it will not handle clicks.
     *
     * @return True if this Preference is enabled, false otherwise.
     */
    open var isEnabled: Boolean
        get() = _enabled && isReverseDependencyState != dependencyMet && parentDependencyMet
        set(enabled) {
            if (_enabled != enabled) {
                _enabled = enabled
                // Enabled state can change dependent preferences' states, so notify
                notifyDependencyChange(shouldDisableDependents())
                notifyChanged()
            }
        }
    private var _enabled = true

    /** Checks whether this Preference should be selectable in the list. */
    var isSelectable: Boolean
        get() = _selectable
        set(selectable) {
            if (_selectable != selectable) {
                _selectable = selectable
                notifyChanged()
            }
        }
    private var _selectable = true

    /**
     * Sets whether this Preference should disable its view when it gets disabled.
     *
     * For example, set this and [isEnabled] to false for
     * preferences that are only displaying information and (1) should not be
     * clickable. (2) should not have the view set to the disabled state.
     */
    var shouldDisableView: Boolean
        get() = _shouldDisableView
        set(shouldDisableView) {
            _shouldDisableView = shouldDisableView
            notifyChanged()
        }
    private var _shouldDisableView = true

    /**
     * Sets whether this preference should be visible in the list. If false, it is excluded from
     * the adapter, but can still be retrieved using [PreferenceFragmentMaterial.findPreference].
     *
     * @return True if this preference should be displayed.
     */
    var isVisible: Boolean
        get() = _visible
        set(visible) {
            if (_visible != visible) {
                _visible = visible
                if (onPreferenceChangeInternalListener != null) {
                    onPreferenceChangeInternalListener!!.onPreferenceVisibilityChange(this)
                }
            }
        }
    private var _visible = true

    /**
     * Backing property for the [key].
     * See [Backing properties](https://kotlinlang.org/docs/reference/properties.html#backing-properties)
     */
    private var _key: String? = null

    /**
     * Sets the key for this Preference, which is used as a key to the [SharedPreferences] or
     * [PreferenceDataStore]. This should be unique for the package.
     *
     * @return Key for this Preference, which is also the key used for storing values into
     * [SharedPreferences] or [PreferenceDataStore].
     */
    var key: String?
        set(key) {
            _key = key
            if (_requiresKey && !hasKey()) {
                requireKey()
            }
        }
        get() = _key

    /**
     * Sets whether to constrain the title of this Preference to a single line instead of
     * letting it wrap onto multiple lines.
     *
     * @return `true` if the title of this preference is constrained to a single line
     * @attr ref R.styleable#Preference_android_singleLineTitle
     */
    var isSingleLineTitle: Boolean
        get() = _singleLineTitle
        set(singleLineTitle) {
            hasSingleLineTitleAttr = true
            _singleLineTitle = singleLineTitle
        }
    private var _singleLineTitle = true

    /**
     * Sets whether to reserve the space of this Preference icon view when no icon is provided. If
     * set to true, the preference will be offset as if it would have the icon and thus aligned with
     * other preferences having icons.
     *
     * @return `true` if the space of this preference icon view is reserved
     * @attr ref R.styleable#Preference_android_iconSpaceReserved
     */
    var isIconSpaceReserved: Boolean
        get() = _iconSpaceReserved
        set(iconSpaceReserved) {
            _iconSpaceReserved = iconSpaceReserved
            notifyChanged()
        }
    private var _iconSpaceReserved: Boolean = false

    /**
     * Returns the [SharedPreferences] where this Preference can read its
     * value(s). Usually, it's easier to use one of the helper read methods:
     * [getPersistedBoolean], [getPersistedFloat],
     * [getPersistedInt], [getPersistedLong], [getPersistedString].
     *
     * @return the [SharedPreferences] where this Preference reads its value(s). If this
     * preference is not attached to a Preference hierarchy or if a
     * [PreferenceDataStore] has been set, this method returns `null`.
     * @see preferenceDataStore
     */
    val sharedPreferences: SharedPreferences?
        get() = if (preferenceManager == null || preferenceDataStore != null) {
            null
        } else preferenceManager!!.sharedPreferences

    /**
     * Sets the key of a Preference that this Preference will depend on. If that
     * Preference is not set or is off, this Preference will be disabled.
     *
     * @return The key of the dependency on this Preference.
     */
    // Unregister the old dependency, if we had one
    // Register the new
    var dependency: String?
        get() = _dependencyKey
        set(dependencyKey) {
            unregisterDependency()
            _dependencyKey = dependencyKey
            registerDependency()
        }
    private var _dependencyKey: String? = null

    /** @return [PreferenceFragmentMaterial] associated with this preference */
    val preferenceFragment: PreferenceFragmentMaterial?
        get() {
            val base = (context as ContextThemeWrapper).baseContext
            return (base as PreferenceActivityMaterial).visiblePreferenceFragment
        }

    /**
     * Returns the text that will be used to filter this Preference depending on user input.
     *
     * If overriding and calling through to the superclass, make sure to prepend your additions with a space.
     *
     * @return Text as a [StringBuilder] that will be used to filter this
     * preference. By default, this is the title and summary
     * (concatenated with a space).
     */
    // Drop the last space
    internal val filterableStringBuilder: StringBuilder
        get() {
            val sb = StringBuilder()
            val title = title
            if (!TextUtils.isEmpty(title)) {
                sb.append(title).append(' ')
            }
            val summary = summary
            if (!TextUtils.isEmpty(summary)) {
                sb.append(summary).append(' ')
            }
            if (sb.isNotEmpty()) {
                sb.setLength(sb.length - 1)
            }
            return sb
        }

    /**
     * Interface definition for a callback to be invoked when this
     * [Preference] is changed or, if this is a group, there is an
     * addition/removal of [Preference](s). This is used internally.
     */
    internal interface OnPreferenceChangeInternalListener {
        /**
         * Called when this Preference has changed.
         *
         * @param preference This preference.
         */
        fun onPreferenceChange(preference: Preference)

        /**
         * Called when this group has added/removed [Preference](s).
         *
         * @param preference This Preference.
         */
        fun onPreferenceHierarchyChange(preference: Preference)

        /**
         * Called when this preference has changed its visibility.
         *
         * @param preference This Preference.
         */
        fun onPreferenceVisibilityChange(preference: Preference)
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes)
        _key = a.getString(R.styleable.Preference_android_key)
        _title = a.getText(R.styleable.Preference_android_title)
        _summary = a.getText(R.styleable.Preference_android_summary)
        _order = a.getInt(R.styleable.Preference_android_order, DEFAULT_ORDER)
        _enabled = a.getBoolean(R.styleable.Preference_android_enabled, true)
        _selectable = a.getBoolean(R.styleable.Preference_android_selectable, true)
        _visible = a.getBoolean(R.styleable.Preference_android_visible, true)
        _dependencyKey = a.getString(R.styleable.Preference_android_dependency)
        _bindValueToSummary = a.getBoolean(R.styleable.Preference_bindValueToSummary, true)
        _tintIcon = a.getColor(R.styleable.Preference_tintIcon, Color.TRANSPARENT)
        _reverseDependencyState = a.getBoolean(R.styleable.Preference_reverseDependencyState, false)
        _shouldDisableView = a.getBoolean(R.styleable.Preference_android_shouldDisableView, true)
        _iconSpaceReserved = a.getBoolean(R.styleable.Preference_android_iconSpaceReserved, false)
        _legacySummary = a.getBoolean(R.styleable.Preference_legacySummary, false)

        titleTextColor = a.getColor(R.styleable.Preference_titleTextColor, 0)
        iconResId = a.getResourceId(R.styleable.Preference_android_icon, 0)
        fragment = a.getString(R.styleable.Preference_android_fragment)
        widgetLayoutResource = a.getResourceId(R.styleable.Preference_android_widgetLayout, 0)
        layoutResource = a.getResourceId(R.styleable.Preference_android_layout, R.layout.preference)
        isPersistent = a.getBoolean(R.styleable.Preference_android_persistent, true)
        iconSize = a.getInt(R.styleable.Preference_preferenceIconSize, 0)

        val fontResId = a.getResourceId(R.styleable.Preference_android_fontFamily, 0)
        fontFamily = if (fontResId > 0) ResourcesCompat.getFont(context, fontResId) else null

        hasSingleLineTitleAttr = a.hasValue(R.styleable.Preference_android_singleLineTitle)
        if (hasSingleLineTitleAttr) {
            _singleLineTitle = a.getBoolean(R.styleable.Preference_android_singleLineTitle, true)
        }

        a.recycle()
    }

    /**
     * Called when a Preference is being inflated and the default value
     * attribute needs to be read. Since different Preference types have
     * different value types, the subclass should get and return the default
     * value which will be its value type.
     *
     * For example, if the value type is String, the body of the method would
     * proxy to [TypedArray.getString].
     *
     * @sample [com.anggrayudi.materialpreference.sample.App.kt]
     * (https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/java/com/anggrayudi/materialpreference/sample/App.java)
     *
     * @param a The set of attributes.
     * @param index The index of the default value attribute.
     * @return The default value of this preference type.
     */
    @Deprecated("It causes complexity and unreliable for preference's default value. Since v3.0.0 this method always returns null." +
            " Use your own method to set the default values as described in the com.anggrayudi.materialpreference.sample.App.kt",
            ReplaceWith(""))
    protected fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return null
    }

    /**
     * Return the extras Bundle object associated with this preference,
     * returning null if there is not currently one.
     */
    fun peekExtras(): Bundle? {
        return _extras
    }

    /**
     * Binds the created View to the data for this Preference.
     *
     * This is a good place to grab references to custom Views in the layout and
     * set properties on them.
     *
     * Make sure to call through to the superclass's implementation.
     *
     * @param holder The ViewHolder that provides references to the views to fill in. These views
     * will be recycled, so you should not hold a reference to them after this method
     * returns.
     */
    open fun onBindViewHolder(holder: PreferenceViewHolder) {
        holder.itemView.setOnClickListener(clickListener)
        holder.itemView.setOnLongClickListener(longClickListener)

        val titleView = holder.findViewById(android.R.id.title) as? TextView
        if (titleView != null) {
            if (fontFamily != null)
                titleView.typeface = fontFamily

            val title = title
            if (!TextUtils.isEmpty(title)) {
                titleView.text = title
                titleView.visibility = View.VISIBLE
                if (hasSingleLineTitleAttr) {
                    titleView.setSingleLine(isSingleLineTitle)
                }
                if (titleTextColor != 0)
                    titleView.setTextColor(titleTextColor)
            } else {
                titleView.visibility = View.GONE
            }
        }

        val legacySummaryView = holder.findViewById(android.R.id.summary) as? TextView
        if (legacySummaryView != null) {
            if (fontFamily != null)
                legacySummaryView.typeface = fontFamily

            val s = summary
            if (isLegacySummary && !TextUtils.isEmpty(s)) {
                legacySummaryView.text = s
                legacySummaryView.visibility = View.VISIBLE
            } else {
                legacySummaryView.visibility = View.GONE
            }
        }

        val materialSummaryView = holder.findViewById(R.id.material_summary) as? TextView
        if (materialSummaryView != null) {
            if (fontFamily != null)
                materialSummaryView.typeface = fontFamily

            val s = summary
            if (!isLegacySummary && !TextUtils.isEmpty(s)) {
                materialSummaryView.text = s
                materialSummaryView.visibility = View.VISIBLE
            } else {
                materialSummaryView.visibility = View.GONE
            }
        }

        val imageView = holder.findViewById(android.R.id.icon) as? ImageView
        if (imageView != null) {
            if (imageView.tag == null && iconSize == 1) {
                imageView.tag = 1
            }

            var c = icon
            if (iconResId != 0 || c != null) {
                if (c == null) {
                    c = ContextCompat.getDrawable(context, iconResId)
                }
                if (c != null) {
                    if (tintIcon != Color.TRANSPARENT) {
                        c.mutate().setColorFilter(tintIcon, PorterDuff.Mode.SRC_IN)
                    }
                    imageView.setImageDrawable(c)
                }
            }
            if (c != null) {
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = if (isIconSpaceReserved) View.INVISIBLE else View.GONE
            }
        }

        var imageFrame = holder.findViewById(R.id.icon_frame)
        if (imageFrame == null) {
            imageFrame = holder.findViewById(R.id.icon_frame)
        }
        if (imageFrame != null) {
            if (icon != null) {
                imageFrame.visibility = View.VISIBLE
            } else {
                imageFrame.visibility = if (isIconSpaceReserved) View.INVISIBLE else View.GONE
            }
        }

        if (shouldDisableView) {
            setEnabledStateOnViews(holder.itemView, isEnabled)
        } else {
            setEnabledStateOnViews(holder.itemView, true)
        }

        val selectable = isSelectable
        holder.itemView.isFocusable = selectable
        holder.itemView.isClickable = selectable

        preferenceViewHolder = holder
    }

    /** Makes sure the view (and any children) get the enabled state changed. */
    private fun setEnabledStateOnViews(v: View, enabled: Boolean) {
        v.isEnabled = enabled
        if (v is ViewGroup) {
            for (i in v.childCount - 1 downTo 0) {
                setEnabledStateOnViews(v.getChildAt(i), enabled)
            }
        }
    }

    /**
     * Sets the title for this Preference with a resource ID.
     *
     * @see title
     * @param titleResId The title as a resource ID.
     */
    fun setTitle(@StringRes titleResId: Int) {
        title = context.getString(titleResId)
    }

    /**
     * Sets the summary for this Preference with a resource ID.
     *
     * @see summary
     * @param summaryResId The summary as a resource.
     */
    fun setSummary(@StringRes summaryResId: Int) {
        summary = context.getString(summaryResId)
    }

    /**
     * Processes a click on the preference. This includes saving the value to
     * the [SharedPreferences]. However, the overridden method should
     * call [callChangeListener] to make sure the client wants to
     * update the preference's state with the new value.
     */
    protected open fun onClick() {}

    /**
     * Checks whether the key is present, and if it isn't throws an exception. This should be called
     * by subclasses that persist their preferences.
     *
     * @throws IllegalStateException If there is no key assigned.
     */
    internal fun requireKey() {
        if (TextUtils.isEmpty(key)) {
            throw IllegalStateException("Preference does not have a key assigned.")
        }
        _requiresKey = true
    }
    private var _requiresKey: Boolean = false

    /**
     * Checks whether this Preference has a valid key.
     *
     * @return True if the key exists and is not a blank string, false otherwise.
     */
    fun hasKey(): Boolean {
        return !TextUtils.isEmpty(key)
    }

    /**
     * Checks whether, at the given time this method is called, this Preference should store/restore
     * its value(s) into the [SharedPreferences] or into [PreferenceDataStore] if
     * assigned. This, at minimum, checks whether this Preference is persistent and it currently has
     * a key. Before you save/restore from the storage, check this first.
     *
     * @return `true` if it should persist the value
     */
    protected fun shouldPersist(): Boolean {
        return preferenceManager != null && isPersistent && hasKey()
    }

    /**
     * Call this method after the user changes the preference, but before the
     * internal state is set. This allows the client to ignore the user value.
     *
     * @param newValue The new value of this Preference.
     * @return True if the user value should be set as the preference
     * value (and persisted).
     */
    fun callChangeListener(newValue: Any?): Boolean {
        return onPreferenceChangeListener == null || onPreferenceChangeListener!!.invoke(this, newValue)
    }

    @RestrictTo(LIBRARY_GROUP)
    internal open fun performClick(view: View) {
        performClick()
    }

    /** Called when a click should be performed. */
    @RestrictTo(LIBRARY_GROUP)
    internal fun performClick() {
        if (!isEnabled) {
            return
        }

        onClick()

        if (onPreferenceClickListener != null && onPreferenceClickListener!!.invoke(this)) {
            return
        }

        val preferenceManager = preferenceManager
        if (preferenceManager != null) {
            val listener = preferenceManager.onPreferenceTreeClickListener
            if (listener != null && listener.onPreferenceTreeClick(this)) {
                return
            }
        }

        if (intent != null) {
            context.startActivity(intent)
        }
    }

    /**
     * Compares Preference objects based on order (if set), otherwise alphabetically on the titles.
     *
     * @param other The Preference to compare to this one.
     * @return 0 if the same; less than 0 if this Preference sorts ahead of <var>another</var>;
     * greater than 0 if this Preference sorts after <var>another</var>.
     */
    override fun compareTo(other: Preference): Int {
        return when {
            order != other.order -> // Do order comparison
                order - other.order
            _title === other._title -> // If titles are null or share same object comparison
                0
            _title == null -> 1
            other._title == null -> -1
            else -> // Do name comparison
                _title!!.toString().compareTo(other._title!!.toString(), ignoreCase = true)
        }
    }

    /**
     * Should be called when the data of this [Preference] has changed.
     */
    protected open fun notifyChanged() {
        onPreferenceChangeInternalListener?.onPreferenceChange(this)
    }

    /**
     * Should be called when a Preference has been added/removed from this group,
     * or the ordering should be re-evaluated.
     */
    protected fun notifyHierarchyChanged() {
        onPreferenceChangeInternalListener?.onPreferenceHierarchyChange(this)
    }

    /**
     * Called when this Preference has been attached to a Preference hierarchy.
     * Make sure to call the super implementation.
     *
     * @param preferenceManager The PreferenceManager of the hierarchy.
     */
    fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        this.preferenceManager = preferenceManager
        if (!hasId) {
            id = preferenceManager.nextId
        }
        if (shouldPersist())
            onSetInitialValue()
    }

    /** Called from [PreferenceGroup] to pass in an ID for reuse */
    @RestrictTo(LIBRARY_GROUP)
    internal fun onAttachedToHierarchy(preferenceManager: PreferenceManager, id: Long) {
        this.id = id
        hasId = true
        try {
            onAttachedToHierarchy(preferenceManager)
        } finally {
            hasId = false
        }
    }

    /**
     * Assigns a [PreferenceGroup] as the parent of this Preference. Set null to remove
     * the current parent.
     *
     * @param parentGroup Parent preference group of this Preference or null if none.
     */
    internal fun assignParent(parentGroup: PreferenceGroup?) {
        parent = parentGroup
    }

    /**
     * Called when the Preference hierarchy has been attached to the
     * list of preferences. This can also be called when this
     * Preference has been attached to a group that was already attached
     * to the list of preferences.
     */
    open fun onAttached() {
        // At this point, the hierarchy that this preference is in is connected
        // with all other preferences.
        registerDependency()
    }

    /**
     * Called when the Preference hierarchy has been detached from the
     * list of preferences. This can also be called when this
     * Preference has been removed from a group that was attached
     * to the list of preferences.
     */
    open fun onDetached() {
        unregisterDependency()
        wasDetached = true
    }

    private fun registerDependency() {
        if (_dependencyKey.isNullOrBlank()) return
        val preference = findPreferenceInHierarchy(_dependencyKey!!)
        if (preference != null) {
            preference.registerDependent(this)
        } else {
            throw IllegalStateException("Dependency \"$_dependencyKey\" " +
                    "not found for preference \"$key\" (title: \"$_title\"")
        }
    }

    private fun unregisterDependency() {
        if (_dependencyKey != null) {
            val oldDependency = findPreferenceInHierarchy(_dependencyKey!!)
            oldDependency?.unregisterDependent(this)
        }
    }

    /**
     * Finds a Preference in this hierarchy (the whole thing,
     * even above/below your [PreferenceScreen] screen break) with the given key.
     *
     * This only functions after we have been attached to a hierarchy.
     *
     * @param key The key of the Preference to find.
     * @return The Preference that uses the given key.
     */
    protected fun findPreferenceInHierarchy(key: String): Preference? {
        return if (TextUtils.isEmpty(key) || preferenceManager == null) {
            null
        } else preferenceManager!!.findPreference(key)

    }

    /**
     * Adds a dependent Preference on this Preference so we can notify it.
     * Usually, the dependent Preference registers itself (it's good for it to
     * know it depends on something), so please use
     * [Preference.dependency] on the dependent Preference.
     *
     * @param dependent The dependent Preference that will be enabled/disabled
     * according to the state of this Preference.
     */
    private fun registerDependent(dependent: Preference) {
        if (dependents == null) {
            dependents = ArrayList()
        }

        dependents!!.add(dependent)
        dependent.onDependencyChanged(this, shouldDisableDependents())
    }

    /**
     * Removes a dependent Preference on this Preference.
     *
     * @param dependent The dependent Preference that will be enabled/disabled
     * according to the state of this Preference.
     */
    private fun unregisterDependent(dependent: Preference) {
        dependents?.remove(dependent)
    }

    /**
     * Notifies any listening dependents of a change that affects the dependency.
     *
     * @param disableDependents Whether this Preference should disable its dependents.
     */
    open fun notifyDependencyChange(disableDependents: Boolean) {
        val dependents = dependents ?: return
        val dependentsCount = dependents.size
        for (i in 0 until dependentsCount) {
            dependents[i].onDependencyChanged(this, disableDependents)
        }
    }

    /**
     * Called when the dependency changes.
     *
     * @param dependency The Preference that this Preference depends on.
     * @param disableDependent Set true to disable this Preference.
     */
    internal fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        if (dependencyMet == disableDependent) {
            dependencyMet = !disableDependent

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents())

            notifyChanged()
        }
    }

    /**
     * Called when the implicit parent dependency changes.
     *
     * @param parent The Preference that this Preference depends on.
     * @param disableChild Set true to disable this Preference.
     */
    internal fun onParentChanged(parent: Preference, disableChild: Boolean) {
        if (parentDependencyMet == disableChild) {
            parentDependencyMet = !disableChild

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents())

            notifyChanged()
        }
    }

    /**
     * Checks whether this preference's dependents should currently be disabled.
     *
     * @return True if the dependents should be disabled, otherwise false.
     */
    open fun shouldDisableDependents(): Boolean {
        return !isEnabled
    }

    /**
     * Called when this Preference is being removed from the hierarchy. You
     * should remove any references to this Preference that you know about. Make
     * sure to call through to the superclass implementation.
     */
    fun onPrepareForRemoval() {
        unregisterDependency()
    }

    /** Implement this to set the initial value of the Preference. */
    protected open fun onSetInitialValue() {}

    private fun tryCommit(editor: SharedPreferences.Editor) {
        if (preferenceManager!!.shouldCommit()) {
            editor.apply()
        }
    }

    internal open fun onSetupFinished(fragment: PreferenceFragmentMaterial) {
    }

    /**
     * Attempts to persist a [String] if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist.
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see getPersistedString
     */
    fun persistString(value: String?): Boolean {
        if (!shouldPersist()) {
            return false
        }

        // Shouldn't store null
        if (TextUtils.equals(value, getPersistedString(null))) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putString(key!!, value)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putString(key, value)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted set of Strings if this Preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either the
     * Preference is not persistent or the Preference is not in the shared preferences.
     * @return the value from the storage or the default return value
     * @see persistString
     */
    protected fun getPersistedString(defaultReturnValue: String?): String? {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return if (dataStore != null) {
            dataStore.getString(key!!, defaultReturnValue)
        } else preferenceManager!!.sharedPreferences!!.getString(key, defaultReturnValue)
    }

    /**
     * Attempts to persist a set of Strings if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param values the values to persist
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see getPersistedStringSet
     */
    fun persistStringSet(values: Set<String>?): Boolean {
        if (!shouldPersist()) {
            return false
        }

        // Shouldn't store null
        if (values == getPersistedStringSet(null)) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putStringSet(key!!, values)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putStringSet(key, values)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted set of Strings if this Preference is persistent.
     *
     * @param defaultReturnValue the default value to return if either this Preference is not
     * persistent or this Preference is not present
     * @return the value from the storage or the default return value
     * @see persistStringSet
     */
    fun getPersistedStringSet(defaultReturnValue: Set<String>?): Set<String>? {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return if (dataStore != null) {
            dataStore.getStringSet(key!!, defaultReturnValue)
        } else preferenceManager!!.sharedPreferences!!.getStringSet(key, defaultReturnValue)

    }

    /**
     * Attempts to persist an [Integer] if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist.
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see persistString
     * @see getPersistedInt
     */
    protected fun persistInt(value: Int): Boolean {
        if (!shouldPersist()) {
            return false
        }

        if (value == getPersistedInt(value.inv())) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putInt(key!!, value)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putInt(key, value)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted [Integer] if this Preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this
     * Preference is not persistent or this Preference is not in the SharedPreferences.
     * @return the value from the storage or the default return value
     * @see getPersistedString
     * @see persistInt
     */
    protected fun getPersistedInt(defaultReturnValue: Int): Int {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return dataStore?.getInt(key!!, defaultReturnValue)
                ?: preferenceManager!!.sharedPreferences!!.getInt(key, defaultReturnValue)
    }

    /**
     * Attempts to persist a [Float] if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist.
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see persistString
     * @see getPersistedFloat
     */
    protected fun persistFloat(value: Float): Boolean {
        if (!shouldPersist()) {
            return false
        }

        if (value == getPersistedFloat(java.lang.Float.NaN)) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putFloat(key!!, value)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putFloat(key, value)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted [Float] if this Preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this
     * Preference is not persistent or this Preference is not in the SharedPreferences.
     * @return the value from the storage or the default return value
     * @see getPersistedString
     * @see persistFloat
     */
    protected fun getPersistedFloat(defaultReturnValue: Float): Float {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return dataStore?.getFloat(key!!, defaultReturnValue)
                ?: preferenceManager!!.sharedPreferences!!.getFloat(key, defaultReturnValue)
    }

    /**
     * Attempts to persist a [Long] if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist.
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see persistString
     * @see getPersistedLong
     */
    protected fun persistLong(value: Long): Boolean {
        if (!shouldPersist()) {
            return false
        }

        if (value == getPersistedLong(value.inv())) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putLong(key!!, value)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putLong(key, value)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted [Long] if this Preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this
     * Preference is not persistent or this Preference is not in the SharedPreferences.
     * @return the value from the storage or the default return value
     * @see getPersistedString
     * @see persistLong
     */
    protected fun getPersistedLong(defaultReturnValue: Long): Long {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return dataStore?.getLong(key!!, defaultReturnValue)
                ?: preferenceManager!!.sharedPreferences!!.getLong(key, defaultReturnValue)
    }

    /**
     * Attempts to persist a [Boolean] if this Preference is persistent.
     *
     * The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist.
     * @return `true` if the Preference is persistent, `false` otherwise
     * @see persistString
     * @see getPersistedBoolean
     */
    protected fun persistBoolean(value: Boolean): Boolean {
        if (!shouldPersist()) {
            return false
        }

        if (value == getPersistedBoolean(!value)) {
            // It's already there, so the same as persisting
            return true
        }

        val dataStore = preferenceDataStore
        if (dataStore != null) {
            dataStore.putBoolean(key!!, value)
        } else {
            val editor = preferenceManager!!.editor
            editor!!.putBoolean(key, value)
            tryCommit(editor)
        }
        return true
    }

    /**
     * Attempts to get a persisted [Boolean] if this Preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this
     * Preference is not persistent or this Preference is not in the SharedPreferences.
     * @return the value from the storage or the default return value
     * @see getPersistedString
     * @see persistBoolean
     */
    protected fun getPersistedBoolean(defaultReturnValue: Boolean): Boolean {
        if (!shouldPersist()) {
            return defaultReturnValue
        }

        val dataStore = preferenceDataStore
        return dataStore?.getBoolean(key!!, defaultReturnValue)
                ?: preferenceManager!!.sharedPreferences!!.getBoolean(key, defaultReturnValue)
    }

    override fun toString(): String {
        return filterableStringBuilder.toString()
    }

    /**
     * Store this Preference hierarchy's frozen state into the given container.
     *
     * @param container The Bundle in which to save the instance of this Preference.
     * @see restoreHierarchyState
     * @see onSaveInstanceState
     */
    fun saveHierarchyState(container: Bundle) {
        dispatchSaveInstanceState(container)
    }

    /**
     * Called by [saveHierarchyState] to store the instance for this Preference and its
     * children. May be overridden to modify how the save happens for children. For example, some
     * Preference objects may want to not store an instance for their children.
     *
     * @param container The Bundle in which to save the instance of this Preference.
     * @see saveHierarchyState
     * @see onSaveInstanceState
     */
    internal open fun dispatchSaveInstanceState(container: Bundle) {
        if (hasKey()) {
            baseMethodCalled = false
            val state = onSaveInstanceState()
            if (!baseMethodCalled) {
                throw IllegalStateException("Derived class did not call super.onSaveInstanceState()")
            }
            if (state != null) {
                container.putParcelable(key, state)
            }
        }
    }

    /**
     * Hook allowing a Preference to generate a representation of its internal
     * state that can later be used to create a new instance with that same
     * state. This state should only contain information that is not persistent
     * or can be reconstructed later.
     *
     * @return A Parcelable object containing the current dynamic state of
     * this Preference, or null if there is nothing interesting to save.
     * The default implementation returns null.
     * @see onRestoreInstanceState
     * @see saveHierarchyState
     */
    protected open fun onSaveInstanceState(): Parcelable? {
        baseMethodCalled = true
        return AbsSavedState.EMPTY_STATE
    }

    /**
     * Restore this Preference hierarchy's previously saved state from the given container.
     *
     * @param container The Bundle that holds the previously saved state.
     * @see saveHierarchyState
     * @see onRestoreInstanceState
     */
    fun restoreHierarchyState(container: Bundle) {
        dispatchRestoreInstanceState(container)
    }

    /**
     * Called by [restoreHierarchyState] to retrieve the saved state for this
     * Preference and its children. May be overridden to modify how restoring
     * happens to the children of a Preference. For example, some Preference objects may
     * not want to save state for their children.
     *
     * @param container The Bundle that holds the previously saved state.
     * @see restoreHierarchyState
     * @see onRestoreInstanceState
     */
    internal open fun dispatchRestoreInstanceState(container: Bundle) {
        if (hasKey()) {
            val state = container.getParcelable<Parcelable>(key)
            if (state != null) {
                baseMethodCalled = false
                onRestoreInstanceState(state)
                if (!baseMethodCalled) {
                    throw IllegalStateException("Derived class did not call super.onRestoreInstanceState()")
                }
            }
        }
    }

    /**
     * Hook allowing a Preference to re-apply a representation of its internal
     * state that had previously been generated by [onSaveInstanceState].
     * This function will never be called with a null state.
     *
     * @param state The saved state that had previously been returned by [onSaveInstanceState].
     * @see onSaveInstanceState
     * @see restoreHierarchyState
     */
    protected open fun onRestoreInstanceState(state: Parcelable?) {
        baseMethodCalled = true
        if (state !== AbsSavedState.EMPTY_STATE && state != null) {
            throw IllegalArgumentException("Wrong state class -- expecting Preference State")
        }
    }

    /**
     * Initializes an [android.view.accessibility.AccessibilityNodeInfo] with information
     * about the View for this Preference.
     */
    @CallSuper
    open fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfoCompat) {
    }

    /** A base class for managing the instance state of a [Preference]. */
    open class BaseSavedState : AbsSavedState {
        constructor(source: Parcel) : super(source)

        constructor(superState: Parcelable) : super(superState)

        companion object CREATOR : Parcelable.Creator<BaseSavedState> {

            override fun createFromParcel(`in`: Parcel): BaseSavedState {
                return BaseSavedState(`in`)
            }

            override fun newArray(size: Int): Array<BaseSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {

        private const val TAG = "Preference"

        /** Specify for [.setOrder] if a specific order is not required. */
        const val DEFAULT_ORDER = Integer.MAX_VALUE
    }
}
