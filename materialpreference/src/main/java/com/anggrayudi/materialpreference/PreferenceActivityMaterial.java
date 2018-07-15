package com.anggrayudi.materialpreference;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public abstract class PreferenceActivityMaterial extends AppCompatActivity implements
        PreferenceFragmentMaterial.OnPreferenceStartScreenCallback,
        FragmentManager.OnBackStackChangedListener {

    private static final String TAG = "PreferenceActivity";

    public static final ReplaceFragment REPLACE_FRAGMENT_FADE =
            new ReplaceFragment(R.anim.abc_fade_in, R.anim.abc_fade_out, R.anim.abc_fade_in, R.anim.abc_fade_out);

    public static final ReplaceFragment REPLACE_FRAGMENT_SLIDE =
            new ReplaceFragment(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);

    private ReplaceFragment mReplaceFragmentStrategy;
    private CharSequence mActivityLabel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setReplaceFragmentStrategy(REPLACE_FRAGMENT_SLIDE);
        mActivityLabel = savedInstanceState == null ? getTitle() : savedInstanceState.getCharSequence("mActivityLabel");
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("mActivityLabel", mActivityLabel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setReplaceFragmentStrategy(ReplaceFragment replaceFragment) {
        mReplaceFragmentStrategy = replaceFragment;
    }

    public final CharSequence getActivityLabel() {
        return mActivityLabel;
    }

    boolean onCreatePreferences(PreferenceFragmentMaterial f, String rootKey) {
        if (rootKey != null && !ReplaceFragment.DEFAULT_ROOT_KEY.equals(rootKey)) {
            f.setPreferenceScreen((PreferenceScreen) f.findPreference(rootKey));
            return true;
        }
        return false;
    }

    protected abstract PreferenceFragmentMaterial onBuildPreferenceFragment(String rootKey);

    /**
     * Called when the user has clicked on a PreferenceScreen item in order to navigate to a new
     * screen of preferences.
     *
     * @param caller The fragment requesting navigation.
     * @param screen The preference screen to navigate to.
     * @return true if the screen navigation has been handled
     */
    public boolean onPreferenceStartScreen(PreferenceFragmentMaterial caller, PreferenceScreen screen) {
        setTitle(screen.getTitle());
        String key = screen.getKey();
        PreferenceFragmentMaterial f = onBuildPreferenceFragment(key);
        f.getArguments().putCharSequence(PreferenceFragmentMaterial.PREFERENCE_TITLE, screen.getTitle());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mReplaceFragmentStrategy != null) {
            ft.setCustomAnimations(mReplaceFragmentStrategy.mAnimEnter, mReplaceFragmentStrategy.mAnimExit,
                    mReplaceFragmentStrategy.mAnimPopEnter, mReplaceFragmentStrategy.mAnimPopExit);
        }
        ft.hide(caller)
                .add(caller.getId(), f, caller.getTag())
                .addToBackStack(key)
                .commit();
        return true;
    }

    /**
     * This will replace the whole preference fragment while putting it on the backstack.
     * Supports transition animations.
     * <p/>
     * Create this inside your activity or calling fragment and call appropriate methods.
     * <p/>
     * This class uses fragment framework so it does support transition animations and
     * saved states.
     */
    public static class ReplaceFragment {

        static final String DEFAULT_ROOT_KEY = "ReplaceFragment.ROOT";

        private final int mAnimEnter, mAnimExit, mAnimPopEnter, mAnimPopExit;

        /**
         * @param animEnter    Enter animation resource ID.
         * @param animExit     Exit animation resource ID.
         * @param animPopEnter Enter animation resource ID when popped from backstack.
         * @param animPopExit  Enter animation resource ID when popped from backstack.
         */
        public ReplaceFragment(int animEnter, int animExit, int animPopEnter, int animPopExit) {
            mAnimEnter = animEnter;
            mAnimExit = animExit;
            mAnimPopEnter = animPopEnter;
            mAnimPopExit = animPopExit;
        }
    }
}
