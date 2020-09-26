package com.anggrayudi.materialpreference

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.XmlRes
import androidx.core.app.ActivityCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.anggrayudi.materialpreference.dialog.*
import com.anggrayudi.materialpreference.util.FileUtils

/**
 * A fragment class to manage and display all preferences.
 *
 * For more information, see [Material Preference Guide](https://github.com/anggrayudi/MaterialPreference)
 *
 * @see PreferenceActivityMaterial
 * @author Anggrayudi H on July 1st, 2018.
 */
@SuppressLint("RestrictedApi", "PrivateResource")
abstract class PreferenceFragmentMaterial : Fragment(),
        PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceManager.OnDisplayPreferenceDialogListener,
        PreferenceManager.OnNavigateToScreenListener,
        DialogPreference.TargetFragment {

    /** @return The [PreferenceManager] used by this fragment. */
    var preferenceManager: PreferenceManager? = null
        private set

    private var styledContext: Context? = null
    private var scrollView: NestedScrollView? = null
    private var listContainer: LinearLayout? = null
    private var havePrefs: Boolean = false
    private var initDone: Boolean = false
    internal var preferenceKeyOnActivityResult: String? = null

    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_BIND_PREFERENCES -> bindPreferences()
            }
        }
    }

    private val requestFocus = Runnable { scrollView!!.focusableViewAvailable(scrollView) }

    private var selectPreferenceRunnable: Runnable? = null

    val preferenceFragmentTitle: String?
        get() = arguments!!.getString(PREFERENCE_TITLE)

    /**
     * Sets the root of the preference hierarchy that this fragment is showing.
     *
     * @return The [PreferenceScreen] that is the root of the preference hierarchy.
     */
    var preferenceScreen: PreferenceScreen?
        get() = preferenceManager!!.preferenceScreen
        set(preferenceScreen) {
            if (preferenceScreen != null && preferenceManager!!.setPreferences(preferenceScreen)) {
                onUnbindPreferences()
                havePrefs = true
                if (initDone) {
                    postBindPreferences()
                }
            }
        }

    private var adapter: PreferenceGroupAdapter? = null

    /** @return Fragment to possibly use as a callback */
    private val callbackFragment: Fragment
        @RestrictTo(LIBRARY_GROUP)
        get() = this

    /**
     * Interface that PreferenceFragment's containing activity should
     * implement to be able to process preference items that wish to
     * switch to a specified fragment.
     */
    interface OnPreferenceStartFragmentCallback {
        /**
         * Called when the user has clicked on a Preference that has
         * a fragment class name associated with it.  The implementation
         * should instantiate and switch to an instance of the given fragment.
         *
         * @param caller The fragment requesting navigation.
         * @param pref   The preference requesting the fragment.
         * @return true if the fragment creation has been handled
         */
        fun onPreferenceStartFragment(caller: PreferenceFragmentMaterial, pref: Preference): Boolean
    }

    /**
     * Interface that PreferenceFragment's containing activity should
     * implement to be able to process preference items that wish to
     * switch to a new screen of preferences.
     */
    interface OnPreferenceStartScreenCallback {
        /**
         * Called when the user has clicked on a PreferenceScreen item in order to navigate to a new
         * screen of preferences.
         *
         * @param caller The fragment requesting navigation.
         * @param screen   The preference screen to navigate to.
         * @return true if the screen navigation has been handled
         */
        fun onPreferenceStartScreen(caller: PreferenceFragmentMaterial, screen: PreferenceScreen): Boolean
    }

    interface OnPreferenceDisplayDialogCallback {

        /**
         * @param caller The fragment containing the preference requesting the dialog.
         * @param pref   The preference requesting the dialog.
         * @return true if the dialog creation has been handled.
         */
        fun onPreferenceDisplayDialog(caller: PreferenceFragmentMaterial, pref: Preference): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        val theme = tv.resourceId
        if (theme == 0) {
            throw IllegalStateException("Must specify preferenceTheme in theme. " +
                    "Read this sample project: https://github.com/anggrayudi/MaterialPreference/tree/master/sample")
        }
        if (arguments == null) {
            throw IllegalStateException("Must specify non-null PreferenceFragmentMaterial arguments")
        }
        styledContext = ContextThemeWrapper(activity, theme)
        preferenceManager = PreferenceManager(styledContext!!)
        preferenceManager!!.onNavigateToScreenListener = this
        val rootKey = arguments!!.getString(ARG_PREFERENCE_ROOT)
        if (rootKey == null && savedInstanceState == null)
            arguments!!.putCharSequence(PREFERENCE_TITLE, (activity as PreferenceActivityMaterial).activityLabel)

        onCreatePreferences(savedInstanceState, rootKey)
        (activity as PreferenceActivityMaterial).onCreatePreferences(this, rootKey)
    }

    /**
     * Called during [onCreate] to supply the preferences for this fragment.
     * Subclasses are expected to call [preferenceScreen] either
     * directly or via helper methods such as [addPreferencesFromResource].
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     * @param rootKey If non-null, this preference fragment should be rooted at the [PreferenceScreen] with this key.
     */
    abstract fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
//        val a = styledContext!!.obtainStyledAttributes(null,
//                R.styleable.PreferenceFragmentMaterial,
//                R.attr.preferenceFragmentCompatStyle,
//                0)

        // Need to theme the inflater to pick up the preferenceFragmentListStyle
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        val themedContext = ContextThemeWrapper(inflater.context, tv.resourceId)
        val themedInflater = inflater.cloneInContext(themedContext)
        scrollView = themedInflater.inflate(R.layout.preference_scrollview, container, false) as NestedScrollView
        listContainer = scrollView!!.findViewById(R.id.list_container)
        handler.post(requestFocus)
        return scrollView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (havePrefs) {
            bindPreferences()
            selectPreferenceRunnable?.run()
            selectPreferenceRunnable = null
        }
        initDone = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            preferenceKeyOnActivityResult = savedInstanceState.getString("preferenceKeyOnActivityResult")
            val container = savedInstanceState.getBundle(PREFERENCES_TAG)
            if (container != null) {
                preferenceScreen?.restoreHierarchyState(container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        preferenceManager!!.onPreferenceTreeClickListener = this
        preferenceManager!!.onDisplayPreferenceDialogListener = this
    }

    override fun onStop() {
        super.onStop()
        preferenceManager!!.onPreferenceTreeClickListener = null
        preferenceManager!!.onDisplayPreferenceDialogListener = null
    }

    override fun onDestroyView() {
        handler.removeCallbacks(requestFocus)
        handler.removeMessages(MSG_BIND_PREFERENCES)
        if (havePrefs) {
            unbindPreferences()
        }
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("preferenceKeyOnActivityResult", preferenceKeyOnActivityResult)
        val preferenceScreen = preferenceScreen
        if (preferenceScreen != null) {
            val container = Bundle()
            preferenceScreen.saveHierarchyState(container)
            outState.putBundle(PREFERENCES_TAG, container)
        }
    }

    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current preference hierarchy.
     *
     * @param preferencesResId The XML resource ID to inflate.
     */
    fun addPreferencesFromResource(@XmlRes preferencesResId: Int) {
        requirePreferenceManager()
        preferenceScreen = preferenceManager!!.inflateFromResource(styledContext!!,
                preferencesResId, preferenceScreen)
    }

    /**
     * Inflates the given XML resource and replaces the current preference hierarchy (if any) with
     * the preference hierarchy rooted at `key`.
     *
     * @param preferencesResId The XML resource ID to inflate.
     * @param key The preference key of the [PreferenceScreen]
     * to use as the root of the preference hierarchy, or null to use the root [PreferenceScreen].
     */
    fun setPreferencesFromResource(@XmlRes preferencesResId: Int, key: String?) {
        requirePreferenceManager()
        val xmlRoot = preferenceManager!!.inflateFromResource(styledContext!!,
                preferencesResId, null)

        val root: Preference?
        if (key != null) {
            root = xmlRoot.findPreference(key)
            if (root !is PreferenceScreen) {
                throw IllegalArgumentException("Preference object with key $key is not a PreferenceScreen")
            }
        } else {
            root = xmlRoot
        }
        preferenceScreen = root
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.fragment != null) {
            var handled = false
            if (callbackFragment is OnPreferenceStartFragmentCallback) {
                handled = (callbackFragment as OnPreferenceStartFragmentCallback)
                        .onPreferenceStartFragment(this, preference)
            }
            if (!handled && activity is OnPreferenceStartFragmentCallback) {
                handled = (activity as OnPreferenceStartFragmentCallback)
                        .onPreferenceStartFragment(this, preference)
            }
            return handled
        }
        return false
    }

    /**
     * Called by [PreferenceScreen.onClick] in order to navigate to a new screen of preferences.
     * Calls [PreferenceFragmentMaterial.OnPreferenceStartScreenCallback.onPreferenceStartScreen]
     * if the target fragment or containing activity implements
     * [PreferenceFragmentMaterial.OnPreferenceStartScreenCallback].
     *
     * @param preferenceScreen The [PreferenceScreen] to navigate to.
     */
    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        var handled = false
        if (callbackFragment is OnPreferenceStartScreenCallback) {
            handled = (callbackFragment as OnPreferenceStartScreenCallback)
                    .onPreferenceStartScreen(this, preferenceScreen)
        }
        if (!handled && activity is OnPreferenceStartScreenCallback) {
            (activity as OnPreferenceStartScreenCallback)
                    .onPreferenceStartScreen(this, preferenceScreen)
        }
    }

    /**
     * Finds a [Preference] based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The [Preference] with the key, or null.
     * @see PreferenceGroup.findPreference
     */
    override fun findPreference(key: CharSequence): Preference? = preferenceManager?.findPreference(key)

    inline fun <reified T : Preference> findPreferenceAs(key: CharSequence): T? = findPreference(key) as? T

    private fun requirePreferenceManager() {
        if (preferenceManager == null) {
            throw RuntimeException("This should be called after super.onCreate.")
        }
    }

    private fun postBindPreferences() {
        if (handler.hasMessages(MSG_BIND_PREFERENCES)) return
        handler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget()
    }

    private fun bindPreferences() {
        val preferenceScreen = preferenceScreen
        if (preferenceScreen != null) {
            attachPreferences(preferenceScreen)
            preferenceScreen.onAttached()
        }
        onBindPreferences()
    }

    private fun unbindPreferences() {
        val preferenceScreen = preferenceScreen
        preferenceScreen?.onDetached()
        onUnbindPreferences()
    }

    @RestrictTo(LIBRARY_GROUP)
    protected fun onBindPreferences() {
    }

    @RestrictTo(LIBRARY_GROUP)
    protected fun onUnbindPreferences() {
    }

    private fun attachPreferences(screen: PreferenceScreen) {
        // TODO: 01/07/18 Attach to cardviews
        adapter = PreferenceGroupAdapter(this, screen, listContainer!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            preferenceKeyOnActivityResult = null
            return
        }

        if (requestCode == FileUtils.REQUEST_CODE_STORAGE_GET_FOLDER) {
            val path = FileUtils.resolvePathFromUri(data!!)
            if (path!!.startsWith("/") || FileUtils.isSdCardUriPermissionsGranted(context!!, data)) {
                if (preferenceKeyOnActivityResult != null) {
                    val preference = findPreference(preferenceKeyOnActivityResult!!) as FolderPreference
                    if (preference.callChangeListener(path)) {
                        preference.persistString(path)
                        preference.summary = path
                    }
                }
                preferenceKeyOnActivityResult = null
            } else {
                Toast.makeText(context, R.string.please_select_sdcard_root, Toast.LENGTH_LONG).show()
                startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT_TREE"),
                        FileUtils.REQUEST_CODE_REQUIRE_SDCARD_ROOT_PATH_PERMISSIONS)
            }
        } else if (requestCode == FileUtils.REQUEST_CODE_REQUIRE_SDCARD_ROOT_PATH_PERMISSIONS && FileUtils.saveUriPermission(context!!, data!!)) {
            Toast.makeText(context, R.string.you_can_select_sdcard, Toast.LENGTH_SHORT).show()
            startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT_TREE"),
                    FileUtils.REQUEST_CODE_STORAGE_GET_FOLDER)
        } else {
            preferenceKeyOnActivityResult = null
        }
    }

    /**
     * Called when a preference in the tree requests to display a dialog. Subclasses should
     * override this method to display custom dialogs or to handle dialogs for custom preference
     * classes.
     *
     * @param preference The Preference object requesting the dialog.
     */
    override fun onDisplayPreferenceDialog(preference: Preference) {

        var handled = false
        if (callbackFragment is OnPreferenceDisplayDialogCallback) {
            handled = (callbackFragment as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
        }
        if (!handled && activity is OnPreferenceDisplayDialogCallback) {
            handled = (activity as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
        }

        if (handled) {
            return
        }

        // check if dialog is already showing
        if (fragmentManager!!.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }

        if (preference is RingtonePreference && preference.permissionCallback != null) {
            val readNotGranted = ActivityCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            if (readNotGranted) {
                val writeGranted = ActivityCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                preference.permissionCallback!!.onPermissionTrouble(!readNotGranted, writeGranted)
                return
            }
        }

        val f: DialogFragment = when (preference) {
            is EditTextPreference -> EditTextPreferenceDialogFragment.newInstance(preference.key!!)
            is ListPreference -> ListPreferenceDialogFragment.newInstance(preference.key!!)
            is IntegerListPreference -> IntegerListPreferenceDialogFragment.newInstance(preference.key!!)
            is MultiSelectListPreference -> MultiSelectListPreferenceDialogFragment.newInstance(preference.key!!)
            is SeekBarDialogPreference -> SeekBarPreferenceDialogFragment.newInstance(preference.key!!)
            is ColorPreference -> ColorPreferenceDialogFragment.newInstance(preference.key!!)
            is RingtonePreference -> RingtonePreferenceDialogFragment.newInstance(preference.key!!)
            else -> throw IllegalArgumentException("Tried to display dialog for unknown "
                    + "preference type. Did you forget to override onDisplayPreferenceDialog()?")
        }
        f.setTargetFragment(this, 0)
        f.show(fragmentManager!!, DIALOG_FRAGMENT_TAG)
    }

    fun scrollToPreference(key: String) {
        scrollToPreferenceInternal(null, key)
    }

    fun scrollToPreference(preference: Preference) {
        scrollToPreferenceInternal(preference, null)
    }

    private fun scrollToPreferenceInternal(preference: Preference?, key: String?) {
        // TODO: 01/07/18 Lakukan dengan NestedScrollView
        //        final Runnable r = new Runnable() {
        //            @Override
        //            public void run() {
        //                final RecyclerView.Adapter adapter = mList.getAdapter();
        //                if (!(adapter instanceof
        //                        PreferenceGroup.PreferencePositionCallback)) {
        //                    if (adapter != null) {
        //                        throw new IllegalStateException("Adapter must implement "
        //                                + "PreferencePositionCallback");
        //                    } else {
        //                        // Adapter was set to null, so don't scroll I guess?
        //                        return;
        //                    }
        //                }
        //                final int position;
        //                if (preference != null) {
        //                    position = ((PreferenceGroup.PreferencePositionCallback) adapter)
        //                            .getPreferenceAdapterPosition(preference);
        //                } else {
        //                    position = ((PreferenceGroup.PreferencePositionCallback) adapter)
        //                            .getPreferenceAdapterPosition(key);
        //                }
        //                if (position != RecyclerView.NO_POSITION) {
        //                    mList.scrollToPosition(position);
        //                } else {
        //                    // Item not found, wait for an update and try again
        //                    adapter.registerAdapterDataObserver(
        //                            new PreferenceFragmentMaterial.ScrollToPreferenceObserver(adapter, mList, preference, key));
        //                }
        //            }
        //        };
        //        if (mList == null) {
        //            selectPreferenceRunnable = r;
        //        } else {
        //            r.run();
        //        }
    }

//    private class ScrollToPreferenceObserver(private val adapter: RecyclerView.Adapter,
//                                             private val mList: RecyclerView,
//                                             private val mPreference: Preference?,
//                                             private val mKey: String)
//        : RecyclerView.AdapterDataObserver() {
//
//        private fun scrollToPreference() {
//            adapter.unregisterAdapterDataObserver(this)
//            val position = if (mPreference != null) {
//                (adapter as PreferenceGroup.PreferencePositionCallback)
//                        .getPreferenceAdapterPosition(mPreference)
//            } else {
//                (adapter as PreferenceGroup.PreferencePositionCallback)
//                        .getPreferenceAdapterPosition(mKey)
//            }
//            if (position != RecyclerView.NO_POSITION) {
//                mList.scrollToPosition(position)
//            }
//        }
//
//        override fun onChanged() {
//            scrollToPreference()
//        }
//
//        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
//            scrollToPreference()
//        }
//
//        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
//            scrollToPreference()
//        }
//
//        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//            scrollToPreference()
//        }
//
//        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
//            scrollToPreference()
//        }
//
//        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
//            scrollToPreference()
//        }
//    }

    companion object {
        private const val TAG = "PreferenceFragment"

        /** Fragment argument used to specify the tag of the desired root [PreferenceScreen] object. */
        const val ARG_PREFERENCE_ROOT = "com.anggrayudi.materialpreference.PreferenceFragmentMaterial.PREFERENCE_ROOT"
        private const val PREFERENCES_TAG = "android:preferences"
        internal const val DIALOG_FRAGMENT_TAG = "com.anggrayudi.materialpreference.PreferenceFragment.DIALOG"
        internal const val PREFERENCE_TITLE = "com.anggrayudi.materialpreference.PreferenceFragment.TITLE"
        private const val MSG_BIND_PREFERENCES = 1
    }
}
