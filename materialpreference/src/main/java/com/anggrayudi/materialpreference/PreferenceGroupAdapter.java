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

package com.anggrayudi.materialpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.ListUpdateCallback;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afollestad.materialdialogs.util.DialogUtils;

import java.util.ArrayList;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * An adapter that connects a RecyclerView to the {@link Preference} objects contained in the
 * associated {@link PreferenceGroup}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class PreferenceGroupAdapter implements
        Preference.OnPreferenceChangeInternalListener, ListUpdateCallback {

    private static final String TAG = "PreferenceGroupAdapter";

    /**
     * The group that we are providing data from.
     */
    private PreferenceGroup mPreferenceGroup;

    /**
     * Maps a position into this adapter -> {@link Preference}. These
     * {@link Preference}s don't have to be direct children of this
     * {@link PreferenceGroup}, they can be grand children or younger)
     */
    private List<Preference> mPreferenceList;

    /**
     * Contains a sorted list of all preferences in this adapter regardless of visibility. This is
     * used to construct {@link #mPreferenceList}
     */
    private List<Preference> mPreferenceListInternal;

    /**
     * List of unique Preference and its subclasses' names and layouts.
     */
    private List<PreferenceLayout> mPreferenceLayouts;

    private ViewGroup mRootParent;

    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

    private Handler mHandler = new Handler();

    private Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            syncMyPreferences();
        }
    };

    private static class PreferenceLayout {
        private int resId;
        private int widgetResId;
        private String name;

        PreferenceLayout() {
        }

        PreferenceLayout(PreferenceLayout other) {
            resId = other.resId;
            widgetResId = other.widgetResId;
            name = other.name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PreferenceLayout)) {
                return false;
            }
            final PreferenceLayout other = (PreferenceLayout) o;
            return resId == other.resId
                    && widgetResId == other.widgetResId
                    && TextUtils.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + resId;
            result = 31 * result + widgetResId;
            result = 31 * result + name.hashCode();
            return result;
        }
    }

    PreferenceGroupAdapter(PreferenceGroup preferenceGroup, ViewGroup parent) {
        mRootParent = parent;
        mPreferenceGroup = preferenceGroup;
        // If this group gets or loses any children, let us know
        mPreferenceGroup.setOnPreferenceChangeInternalListener(this);

        mPreferenceList = new ArrayList<>();
        mPreferenceListInternal = new ArrayList<>();
        mPreferenceLayouts = new ArrayList<>();

        syncMyPreferences();
    }

    private void syncMyPreferences() {
        for (final Preference preference : mPreferenceListInternal) {
            // Clear out the listeners in anticipation of some items being removed. This listener
            // will be (re-)added to the remaining prefs when we flatten.
            preference.setOnPreferenceChangeInternalListener(null);
        }
        final List<Preference> fullPreferenceList = new ArrayList<>(mPreferenceListInternal.size());
        flattenPreferenceGroup(fullPreferenceList, mPreferenceGroup);

        mPreferenceList = fullPreferenceList;
        mPreferenceListInternal = fullPreferenceList;

        notifyDataSetChanged();

        for (final Preference preference : fullPreferenceList) {
            preference.clearWasDetached();
        }
    }

    private void flattenPreferenceGroup(List<Preference> preferences, PreferenceGroup group) {
        group.sortPreferences();

        final int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            final Preference preference = group.getPreference(i);

            preferences.add(preference);

            addPreferenceClassName(preference);

            if (preference instanceof PreferenceGroup) {
                final PreferenceGroup preferenceAsGroup = (PreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }

            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    /**
     * Creates a string that includes the preference name, layout id and widget layout id.
     * If a particular preference type uses 2 different resources, they will be treated as
     * different view types.
     */
    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout in) {
        PreferenceLayout pl = in != null? in : new PreferenceLayout();
        pl.name = preference.getClass().getName();
        pl.resId = preference.getLayoutResource();
        pl.widgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    private void addPreferenceClassName(Preference preference) {
        final PreferenceLayout pl = createPreferenceLayout(preference, null);
        if (!mPreferenceLayouts.contains(pl)) {
            mPreferenceLayouts.add(pl);
        }
    }

    public Preference getItem(int position) {
        if (position < 0 || position >= mPreferenceList.size()) return null;
        return mPreferenceList.get(position);
    }

    @Override
    public void onPreferenceChange(Preference preference) {
        final int index = mPreferenceList.indexOf(preference);
        // If we don't find the preference, we don't need to notify anyone
        if (index != -1) {
            // Send the pref object as a placeholder to ensure the view holder is recycled in place
            onItemChanged(index);
        }
    }

    @Override
    public void onPreferenceHierarchyChange(Preference preference) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public void onPreferenceVisibilityChange(Preference preference) {
        if (!mPreferenceListInternal.contains(preference)) {
            return;
        }
        preference.mPreferenceViewHolder.itemView
                .setVisibility(preference.isVisible() ? View.VISIBLE : View.GONE);
    }

    private int getItemViewType(int position) {
        final Preference preference = getItem(position);

        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        int viewType = mPreferenceLayouts.indexOf(mTempPreferenceLayout);
        if (viewType != -1) {
            return viewType;
        } else {
            viewType = mPreferenceLayouts.size();
            mPreferenceLayouts.add(new PreferenceLayout(mTempPreferenceLayout));
            return viewType;
        }
    }

    private PreferenceViewHolder createViewHolder(int viewType, Preference preference) {
        PreferenceLayout pl = mPreferenceLayouts.get(viewType);
        Context context = mPreferenceGroup.getContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.BackgroundStyle);
        Drawable background = a.getDrawable(R.styleable.BackgroundStyle_android_selectableItemBackground);
        if (background == null) {
            background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background);
        }
        a.recycle();

        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(pl.resId, getParentView(preference), false);
        if (view.getBackground() == null) {
            ViewCompat.setBackground(view, background);
        }

        final ViewGroup widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            if (pl.widgetResId != 0) {
                inflater.inflate(pl.widgetResId, widgetFrame);
            }
//            else if (preference.isLegacySummary()){
//                widgetFrame.setVisibility(View.GONE);
//            }
        }

        return new PreferenceViewHolder(view);
    }

    public void onItemChanged(int position) {
        Preference preference = getItem(position);
        if (preference != null) {
            if (preference.mPreferenceViewHolder == null) {
                preference.mPreferenceViewHolder = createViewHolder(getItemViewType(position), preference);
                if (preference instanceof PreferenceScreen) {
                    ImageView summaryIcon = (ImageView) preference.mPreferenceViewHolder.findViewById(R.id.summary_icon);
                    summaryIcon.getDrawable().mutate().setColorFilter(
                            DialogUtils.resolveColor(preference.getContext(),
                                    android.R.attr.textColorSecondary), PorterDuff.Mode.SRC_IN);
                } else if (preference instanceof IndicatorPreference) {
                    IndicatorPreference indicatorPreference = (IndicatorPreference) preference;
                    indicatorPreference.setTint(indicatorPreference.getTint());
                    indicatorPreference.mPreferenceViewHolder.itemView.findViewById(R.id.material_summary).setVisibility(View.GONE);
                }
                getParentView(preference).addView(preference.mPreferenceViewHolder.itemView);
                preference.mPreferenceViewHolder.itemView.setVisibility(preference.isVisible() ? View.VISIBLE : View.GONE);
            }

            preference.onBindViewHolder(preference.mPreferenceViewHolder);
        }
    }

    public void notifyDataSetChanged() {
        for (int i = 0; i < mPreferenceList.size(); i++) {
            onItemChanged(i);
        }
    }

    private ViewGroup getParentView(Preference preference) {
        if (preference.getParent() == null || preference instanceof PreferenceCategory)
            return mRootParent;

        if (preference.getParent().mPreferenceViewHolder.itemView == null)
            throw new IllegalStateException("Make sure that you wrap " + preference.getClass().getSimpleName()
                    + " inside PreferenceCategory from the XML.");

        return preference.getParent().mPreferenceViewHolder.itemView.findViewById(android.R.id.content);
    }

    @Override
    public void onInserted(int position, int count) {
        Log.d(TAG, "onInserted: " + position);
    }

    @Override
    public void onRemoved(int position, int count) {
        Log.d(TAG, "onRemoved: " + position);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        Log.d(TAG, "onMoved: fromPosition " + fromPosition + " => toPosition " + toPosition);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        Log.d(TAG, "onChanged: " + position + ", payload " + payload.toString());
    }
}
