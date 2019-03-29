package com.anggrayudi.materialpreference.dialog

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.anggrayudi.materialpreference.RingtoneManagerCompat
import com.anggrayudi.materialpreference.RingtonePreference
import com.anggrayudi.materialpreference.SafeRingtone
import java.util.*

/**
 * Created by Eugen on 07.12.2015.
 */
class RingtonePreferenceDialogFragment : PreferenceDialogFragment(), Runnable {

    private var mRingtoneManager: RingtoneManager? = null
    private var mType: Int = 0

    private var mCursor: Cursor? = null
    private var mHandler: Handler? = null

    private var mUnknownPos = POS_UNKNOWN

    /**
     * The position in the list of the 'Silent' item.
     */
    private var mSilentPos = POS_UNKNOWN

    /**
     * The position in the list of the 'Default' item.
     */
    private var mDefaultRingtonePos = POS_UNKNOWN

    /**
     * The position in the list of the last clicked item.
     */
    internal var mClickedPos = POS_UNKNOWN

    /**
     * The position in the list of the ringtone to sample.
     */
    private var mSampleRingtonePos = POS_UNKNOWN

    /**
     * Whether this list has the 'Silent' item.
     */
    private var mHasSilentItem: Boolean = false

    /**
     * The Uri to place a checkmark next to.
     */
    private var mExistingUri: Uri? = null

    /**
     * The number of static items in the list.
     */
    private val mStaticItems = ArrayList<CharSequence>()

    /**
     * Whether this list has the 'Default' item.
     */
    private var mHasDefaultItem: Boolean = false

    /**
     * The Uri to play when the 'Default' item is clicked.
     */
    private var mUriForDefaultItem: Uri? = null

    private var mUnknownRingtone: Ringtone? = null

    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private var mDefaultRingtone: Ringtone? = null

    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private var mCurrentRingtone: Ringtone? = null

    val ringtonePreference: RingtonePreference?
        get() = preference as RingtonePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler()
        loadRingtoneManager(savedInstanceState)
    }

    private fun loadRingtoneManager(savedInstanceState: Bundle?) {
        // Give the Activity so it can do managed queries
        mRingtoneManager = RingtoneManagerCompat(activity!!)

        val fallbackRingtonePicker: Boolean
        if (savedInstanceState != null) {
            mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN)
            fallbackRingtonePicker = savedInstanceState.getBoolean(KEY_FALLBACK_RINGTONE_PICKER)
        } else {
            fallbackRingtonePicker = false
        }

        if (fallbackRingtonePicker) {
            showsDialog = false
        } else {
            val preference = requireRingtonePreference()

            /*
             * Get whether to show the 'Default' item, and the URI to play when the
             * default is clicked
             */
            mHasDefaultItem = preference.showDefault
            mUriForDefaultItem = RingtoneManager.getDefaultUri(preference.ringtoneType)

            // Get whether to show the 'Silent' item
            mHasSilentItem = preference.showSilent

            // Get the types of ringtones to show
            mType = preference.ringtoneType
            if (mType != -1) {
                mRingtoneManager!!.setType(mType)
            }

            // Get the URI whose list item should have a checkmark
            mExistingUri = preference.onRestoreRingtone()

            try {
                mCursor = mRingtoneManager!!.cursor

                // Check if cursor is valid.
                mCursor!!.columnNames
            } catch (ex: IllegalStateException) {
                recover(preference, ex)
            } catch (ex: IllegalArgumentException) {
                recover(preference, ex)
            }
        }
    }

    private fun recover(preference: RingtonePreference, ex: Throwable) {
        Log.e(TAG, "RingtoneManager returned unexpected cursor.", ex)

        mCursor = null
        showsDialog = false

        // Alternatively try starting system picker.
        val i = preference.buildRingtonePickerIntent()
        try {
            startActivityForResult(i, RC_FALLBACK_RINGTONE_PICKER)
        } catch (ex2: ActivityNotFoundException) {
            onRingtonePickerNotFound(RC_FALLBACK_RINGTONE_PICKER)
        }
    }

    /**
     * Called when there's no ringtone picker available in the system.
     * Let the user know (using e.g. a Toast).
     * Just dismisses this fragment by default.
     *
     * @param requestCode You can use this code to launch another activity instead of dismissing
     * this fragment. The result must contain
     * [RingtoneManager.EXTRA_RINGTONE_PICKED_URI] extra.
     */
    fun onRingtonePickerNotFound(requestCode: Int) {
        dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_FALLBACK_RINGTONE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                requireRingtonePreference().onActivityResult(data)
            }
            dismiss()
        }
    }

    override fun onPrepareDialog(dialog: MaterialDialog): MaterialDialog {
        val preference = requireRingtonePreference()

        // The volume keys will control the stream that we are choosing a ringtone for
        activity?.volumeControlStream = mRingtoneManager!!.inferStreamType()

        dialog.title(text = preference.nonEmptyDialogTitle.toString())

        val context = dialog.context

        if (mHasDefaultItem) {
            mDefaultRingtonePos = addDefaultRingtoneItem()

            if (mClickedPos == POS_UNKNOWN && RingtoneManager.isDefault(mExistingUri)) {
                mClickedPos = mDefaultRingtonePos
            }
        }
        if (mHasSilentItem) {
            mSilentPos = addSilentItem()

            // The 'Silent' item should use a null Uri
            if (mClickedPos == POS_UNKNOWN && mExistingUri == null) {
                mClickedPos = mSilentPos
            }
        }

        if (mClickedPos == POS_UNKNOWN) {
            mClickedPos = getListPosition(mRingtoneManager!!.getRingtonePosition(mExistingUri))
        }

        // If we still don't have selected item, but we're not silent, show the 'Unknown' item.
        if (mClickedPos == POS_UNKNOWN && mExistingUri != null) {
            val ringtoneTitle: String?
            val ringtone = SafeRingtone.obtain(context, mExistingUri)
            try {
                // We may not be able to list external ringtones
                // but we may be able to show selected external ringtone title.
                ringtoneTitle = if (ringtone.canGetTitle()) {
                    ringtone.title
                } else {
                    null
                }
            } finally {
                ringtone.stop()
            }
            mUnknownPos = if (ringtoneTitle == null) {
                addUnknownItem()
            } else {
                addStaticItem(ringtoneTitle)
            }
            mClickedPos = mUnknownPos
        }

        val titles = ArrayList<String>()
        mStaticItems.forEach { titles.add(it.toString()) }
        if (mCursor!!.moveToFirst()) {
            val index = mCursor!!.getColumnIndex(MediaStore.Audio.Media.TITLE)
            do {
                titles.add(mCursor!!.getString(index))
            } while (mCursor!!.moveToNext())
        }

        return dialog.noAutoDismiss()
                .positiveButton(text = mPositiveButtonText ?: getText(android.R.string.ok)) {
                    mWhichButtonClicked = WhichButton.POSITIVE
                    it.dismiss()
                }
                .negativeButton(text = mNegativeButtonText ?: getText(android.R.string.cancel)) {
                    mWhichButtonClicked = WhichButton.NEGATIVE
                    it.dismiss()
                }
                .listItemsSingleChoice(items = titles, waitForPositiveButton = false) { d, index, _ ->
                    mClickedPos = index
                    d.getActionButton(WhichButton.POSITIVE).isEnabled = true
                    playRingtone(index, DELAY_MS_SELECTION_PLAYED)
                }
    }

    /**
     * Adds a static item to the top of the list. A static item is one that is not from the
     * [RingtoneManager].
     *
     * @param text Text for the item.
     * @return The position of the inserted item.
     */
    private fun addStaticItem(text: CharSequence): Int {
        mStaticItems.add(text)
        return mStaticItems.size - 1
    }

    private fun addDefaultRingtoneItem(): Int {
        return when (mType) {
            RingtoneManager.TYPE_NOTIFICATION -> addStaticItem(RingtonePreference.getNotificationSoundDefaultString(context!!))
            RingtoneManager.TYPE_ALARM -> addStaticItem(RingtonePreference.getAlarmSoundDefaultString(context!!))
            else -> addStaticItem(RingtonePreference.getRingtoneDefaultString(context!!))
        }
    }

    private fun addSilentItem(): Int {
        return addStaticItem(RingtonePreference.getRingtoneSilentString(context!!))
    }

    private fun addUnknownItem(): Int {
        return addStaticItem(RingtonePreference.getRingtoneUnknownString(context!!))
    }

    private fun getListPosition(ringtoneManagerPos: Int): Int {
        // If the manager position is -1 (for not found), return that
        return if (ringtoneManagerPos < 0) POS_UNKNOWN else ringtoneManagerPos + mStaticItems.size
    }

    override fun onPause() {
        super.onPause()
        if (!activity!!.isChangingConfigurations) {
            stopAnyPlayingRingtone()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!activity!!.isChangingConfigurations) {
            stopAnyPlayingRingtone()
        } else {
            saveAnyPlayingRingtone()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_CLICKED_POS, mClickedPos)
        outState.putBoolean(KEY_FALLBACK_RINGTONE_PICKER, !showsDialog)
    }

    protected fun requireRingtonePreference(): RingtonePreference {
        val preference = ringtonePreference
        if (preference == null) {
            val key = arguments!!.getString(PreferenceDialogFragment.ARG_KEY)
            throw IllegalStateException("RingtonePreference[$key] not available (yet).")
        }
        return preference
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // Stop playing the previous ringtone
        if (sPlayingRingtone == null) {
            mRingtoneManager!!.stopPreviousRingtone()
        }

        // The volume keys will control the default stream
        activity?.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

        if (positiveResult) {
            val uri = when (mClickedPos) {
                mDefaultRingtonePos -> // Set it to the default Uri that they originally gave us
                    mUriForDefaultItem
                mSilentPos -> // A null Uri is for the 'Silent' item
                    null
                mUnknownPos -> // 'Unknown' was shown because it was persisted before showing the picker.
                    // There's no change to persist, return immediately.
                    return
                else -> mRingtoneManager!!.getRingtoneUri(getRingtoneManagerPosition(mClickedPos))
            }

            requireRingtonePreference().saveRingtone(uri)
        }
    }

    internal fun playRingtone(position: Int, delayMs: Int) {
        mHandler!!.removeCallbacks(this)
        mSampleRingtonePos = position
        mHandler!!.postDelayed(this, delayMs.toLong())
    }

    override fun run() {
        stopAnyPlayingRingtone()
        if (mSampleRingtonePos == mSilentPos) {
            return
        }

        //        final int oldSampleRingtonePos = mSampleRingtonePos;
        try {
            var ringtone: Ringtone? = null
            when (mSampleRingtonePos) {
                mDefaultRingtonePos -> {
                    if (mDefaultRingtone == null) {
                        try {
                            mDefaultRingtone = RingtoneManager.getRingtone(context!!, mUriForDefaultItem)
                        } catch (ex: SecurityException) {
                            Log.e(TAG, "Failed to create default Ringtone from $mUriForDefaultItem.", ex)
                        }
                    }
                    /*
                     * Stream type of mDefaultRingtone is not set explicitly here.
                     * It should be set in accordance with mRingtoneManager of this Activity.
                     */
                    mDefaultRingtone?.streamType = mRingtoneManager!!.inferStreamType()
                    ringtone = mDefaultRingtone
                    mCurrentRingtone = null
                }
                mUnknownPos -> {
                    if (mUnknownRingtone == null) {
                        try {
                            mUnknownRingtone = RingtoneManager.getRingtone(context!!, mExistingUri)
                        } catch (ex: SecurityException) {
                            Log.e(TAG, "Failed to create unknown Ringtone from $mExistingUri.", ex)
                        }
                    }
                    mUnknownRingtone?.streamType = mRingtoneManager!!.inferStreamType()
                    ringtone = mUnknownRingtone
                    mCurrentRingtone = null
                }
                else -> {
                    val position = getRingtoneManagerPosition(mSampleRingtonePos)
                    try {
                        ringtone = mRingtoneManager!!.getRingtone(position)
                    } catch (ex: SecurityException) {
                        Log.e(TAG, "Failed to create selected Ringtone from " + mRingtoneManager!!.getRingtoneUri(position) + ".", ex)
                    }
    
                    mCurrentRingtone = ringtone
                }
            }

            ringtone?.play()
        } catch (ex: SecurityException) {
            // Don't play the inaccessible default ringtone.
            Log.e(TAG, "Failed to play Ringtone.", ex)
            //            mSampleRingtonePos = oldSampleRingtonePos;
        }
    }

    private fun saveAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone!!.isPlaying) {
            sPlayingRingtone = mDefaultRingtone
        } else if (mUnknownRingtone != null && mUnknownRingtone!!.isPlaying) {
            sPlayingRingtone = mUnknownRingtone
        } else if (mCurrentRingtone != null && mCurrentRingtone!!.isPlaying) {
            sPlayingRingtone = mCurrentRingtone
        }
    }

    private fun stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone!!.isPlaying) {
            sPlayingRingtone!!.stop()
        }
        sPlayingRingtone = null

        if (mDefaultRingtone != null && mDefaultRingtone!!.isPlaying) {
            mDefaultRingtone!!.stop()
        }

        if (mUnknownRingtone != null && mUnknownRingtone!!.isPlaying) {
            mUnknownRingtone!!.stop()
        }

        mRingtoneManager?.stopPreviousRingtone()
    }

    private fun getRingtoneManagerPosition(listPos: Int): Int {
        return listPos - mStaticItems.size
    }

    companion object {

        private const val TAG = "RingtonePreference"

        private const val RC_FALLBACK_RINGTONE_PICKER = 0xff00 // <0; 0xffff>

        private const val KEY_FALLBACK_RINGTONE_PICKER = "com.anggrayudi.materialpreference.FALLBACK_RINGTONE_PICKER"

        private const val POS_UNKNOWN = -1

        private const val DELAY_MS_SELECTION_PLAYED = 300

        private const val SAVE_CLICKED_POS = "clicked_pos"

        /**
         * Keep the currently playing ringtone around when changing orientation, so that it
         * can be stopped later, after the activity is recreated.
         */
        private var sPlayingRingtone: Ringtone? = null

        fun newInstance(key: String): RingtonePreferenceDialogFragment {
            val fragment = RingtonePreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragment.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
