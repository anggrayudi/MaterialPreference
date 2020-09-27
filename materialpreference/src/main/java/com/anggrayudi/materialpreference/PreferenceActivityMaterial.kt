package com.anggrayudi.materialpreference

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

/**
 * Your `Activity` should extends from this abstract class and uses [PreferenceFragmentMaterial]
 * to display all preferences.
 *
 * For more information, see [Material Preference Guide](https://github.com/anggrayudi/MaterialPreference)
 *
 * @see PreferenceFragmentMaterial
 * @author Anggrayudi H
 */
abstract class PreferenceActivityMaterial : AppCompatActivity(),
    PreferenceFragmentMaterial.OnPreferenceStartScreenCallback,
    FragmentManager.OnBackStackChangedListener {

    // TODO 24-Jan-19: Create preference header for tablet in two columns mode

    var replaceFragmentStrategy: ReplaceFragment? = null

    var activityLabel: CharSequence? = null
        private set

    val visiblePreferenceFragment: PreferenceFragmentMaterial?
        get() {
            supportFragmentManager.fragments.forEach {
                if (it is PreferenceFragmentMaterial && it.isVisible())
                    return it
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        replaceFragmentStrategy = REPLACE_FRAGMENT_SLIDE
        activityLabel = if (savedInstanceState == null) title else savedInstanceState.getCharSequence("activityLabel")
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("activityLabel", activityLabel)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    internal fun onCreatePreferences(f: PreferenceFragmentMaterial, rootKey: String?): Boolean {
        if (rootKey != null && ReplaceFragment.DEFAULT_ROOT_KEY != rootKey) {
            f.preferenceScreen = f.findPreference(rootKey) as PreferenceScreen
            return true
        }
        return false
    }

    protected abstract fun onBuildPreferenceFragment(rootKey: String?): PreferenceFragmentMaterial

    /**
     * Called when the user has clicked on a PreferenceScreen item in order to navigate to a new
     * screen of preferences.
     *
     * @param caller The fragment requesting navigation.
     * @param screen The preference screen to navigate to.
     * @return true if the screen navigation has been handled
     */
    override fun onPreferenceStartScreen(caller: PreferenceFragmentMaterial, screen: PreferenceScreen): Boolean {
        title = screen.title
        val key = screen.key
        val f = onBuildPreferenceFragment(key)
        f.arguments!!.putCharSequence(PreferenceFragmentMaterial.PREFERENCE_TITLE, screen.title)
        val ft = supportFragmentManager.beginTransaction()
        replaceFragmentStrategy?.run {
            ft.setCustomAnimations(animEnter, animExit, animPopEnter, animPopExit)
        }
        ft.hide(caller)
            .add(caller.id, f, caller.tag)
            .addToBackStack(key)
            .commit()
        return true
    }

    /**
     * This will replace the whole preference fragment while putting it on the backstack.
     * Supports transition animations.
     *
     *
     * Create this inside your activity or calling fragment and call appropriate methods.
     *
     *
     * This class uses fragment framework so it does support transition animations and
     * saved states.
     */
    class ReplaceFragment
    /**
     * @param animEnter    Enter animation resource ID.
     * @param animExit     Exit animation resource ID.
     * @param animPopEnter Enter animation resource ID when popped from backstack.
     * @param animPopExit  Enter animation resource ID when popped from backstack.
     */
        (
        internal val animEnter: Int,
        internal val animExit: Int,
        internal val animPopEnter: Int,
        internal val animPopExit: Int
    ) {
        companion object {

            internal const val DEFAULT_ROOT_KEY = "ReplaceFragment.ROOT"
        }
    }

    companion object {

        private const val TAG = "PreferenceActivity"

        val REPLACE_FRAGMENT_FADE = ReplaceFragment(R.anim.abc_fade_in, R.anim.abc_fade_out, R.anim.abc_fade_in, R.anim.abc_fade_out)

        val REPLACE_FRAGMENT_SLIDE = ReplaceFragment(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right)
    }
}
