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
@Suppress("DEPRECATION")
class RingtonePreferenceDialogFragment : PreferenceDialogFragment(), Runnable {

    private var ringtoneManager: RingtoneManager? = null
    private var type: Int = 0

    private var cursor: Cursor? = null
    private var handler: Handler? = null

    private var unknownPos = POS_UNKNOWN

    /** The position in the list of the 'Silent' item. */
    private var silentPos = POS_UNKNOWN

    /** The position in the list of the 'Default' item. */
    private var defaultRingtonePos = POS_UNKNOWN

    /** The position in the list of the last clicked item. */
    internal var clickedPos = POS_UNKNOWN

    /** The position in the list of the ringtone to sample. */
    private var sampleRingtonePos = POS_UNKNOWN

    /** Whether this list has the 'Silent' item. */
    private var hasSilentItem: Boolean = false

    /** The Uri to place a checkmark next to. */
    private var existingUri: Uri? = null

    /** The number of static items in the list. */
    private val staticItems = ArrayList<CharSequence>()

    /** Whether this list has the 'Default' item. */
    private var hasDefaultItem: Boolean = false

    /** The Uri to play when the 'Default' item is clicked. */
    private var uriForDefaultItem: Uri? = null

    private var unknownRingtone: Ringtone? = null

    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private var defaultRingtone: Ringtone? = null

    /** The ringtone that's currently playing, unless the currently playing one is the default ringtone. */
    private var currentRingtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler()
        loadRingtoneManager(savedInstanceState)
    }

    private fun loadRingtoneManager(savedInstanceState: Bundle?) {
        // Give the Activity so it can do managed queries
        ringtoneManager = RingtoneManagerCompat(activity!!)

        val fallbackRingtonePicker: Boolean
        if (savedInstanceState != null) {
            clickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN)
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
            hasDefaultItem = preference.showDefault
            uriForDefaultItem = RingtoneManager.getDefaultUri(preference.ringtoneType)

            // Get whether to show the 'Silent' item
            hasSilentItem = preference.showSilent

            // Get the types of ringtones to show
            type = preference.ringtoneType
            if (type != -1) {
                ringtoneManager!!.setType(type)
            }

            // Get the URI whose list item should have a checkmark
            existingUri = preference.onRestoreRingtone()

            try {
                cursor = ringtoneManager!!.cursor

                // Check if cursor is valid.
                cursor!!.columnNames
            } catch (ex: IllegalStateException) {
                recover(preference, ex)
            } catch (ex: IllegalArgumentException) {
                recover(preference, ex)
            }
        }
    }

    private fun recover(preference: RingtonePreference, ex: Throwable) {
        Log.e(TAG, "RingtoneManager returned unexpected cursor.", ex)

        cursor = null
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
     * this fragment. The result must contain [RingtoneManager.EXTRA_RINGTONE_PICKED_URI] extra.
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
        activity?.volumeControlStream = ringtoneManager!!.inferStreamType()

        dialog.title(text = preference.nonEmptyDialogTitle.toString())

        if (hasDefaultItem) {
            defaultRingtonePos = addDefaultRingtoneItem()

            if (clickedPos == POS_UNKNOWN && RingtoneManager.isDefault(existingUri)) {
                clickedPos = defaultRingtonePos
            }
        }
        if (hasSilentItem) {
            silentPos = addSilentItem()

            // The 'Silent' item should use a null Uri
            if (clickedPos == POS_UNKNOWN && existingUri == null) {
                clickedPos = silentPos
            }
        }

        if (clickedPos == POS_UNKNOWN) {
            clickedPos = getListPosition(ringtoneManager!!.getRingtonePosition(existingUri))
        }

        // If we still don't have selected item, but we're not silent, show the 'Unknown' item.
        if (clickedPos == POS_UNKNOWN && existingUri != null) {
            val ringtoneTitle: String?
            val ringtone = SafeRingtone.obtain(dialog.context, existingUri)
            try {
                // We may not be able to list external ringtones
                // but we may be able to show selected external ringtone title.
                ringtoneTitle = if (ringtone.canGetTitle())
                    ringtone.title
                else
                    null
            } finally {
                ringtone.stop()
            }
            unknownPos = if (ringtoneTitle == null) {
                addUnknownItem()
            } else {
                addStaticItem(ringtoneTitle)
            }
            clickedPos = unknownPos
        }

        val titles = ArrayList<String>()
        staticItems.forEach { titles.add(it.toString()) }
        if (cursor!!.moveToFirst()) {
            val index = cursor!!.getColumnIndex(MediaStore.Audio.Media.TITLE)
            do {
                titles.add(cursor!!.getString(index))
            } while (cursor!!.moveToNext())
        }

        return dialog.noAutoDismiss()
                .positiveButton(text = positiveButtonText ?: getText(android.R.string.ok)) {
                    whichButtonClicked = WhichButton.POSITIVE
                    it.dismiss()
                }
                .negativeButton(text = negativeButtonText ?: getText(android.R.string.cancel)) {
                    whichButtonClicked = WhichButton.NEGATIVE
                    it.dismiss()
                }
                .listItemsSingleChoice(items = titles, waitForPositiveButton = false, initialSelection = clickedPos) { d, index, _ ->
                    if (d.isShowing) {
                        clickedPos = index
                        d.getActionButton(WhichButton.POSITIVE).isEnabled = true
                        playRingtone(index, DELAY_MS_SELECTION_PLAYED)
                    }
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
        staticItems.add(text)
        return staticItems.size - 1
    }

    private fun addDefaultRingtoneItem(): Int {
        return when (type) {
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
        return if (ringtoneManagerPos < 0) POS_UNKNOWN else ringtoneManagerPos + staticItems.size
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
        outState.putInt(SAVE_CLICKED_POS, clickedPos)
        outState.putBoolean(KEY_FALLBACK_RINGTONE_PICKER, !showsDialog)
    }

    protected fun requireRingtonePreference(): RingtonePreference {
        return preference as? RingtonePreference
                ?: throw IllegalStateException("RingtonePreference[${arguments?.getString(ARG_KEY)}] not available (yet).")
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // Stop playing the previous ringtone
        if (sPlayingRingtone == null) {
            ringtoneManager?.stopPreviousRingtone()
        }

        // The volume keys will control the default stream
        activity?.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

        if (positiveResult) {
            val uri = when (clickedPos) {
                defaultRingtonePos -> // Set it to the default Uri that they originally gave us
                    uriForDefaultItem
                silentPos -> // A null Uri is for the 'Silent' item
                    null
                unknownPos -> // 'Unknown' was shown because it was persisted before showing the picker.
                    // There's no change to persist, return immediately.
                    return
                else -> ringtoneManager?.getRingtoneUri(getRingtoneManagerPosition(clickedPos))
            }

            requireRingtonePreference().saveRingtone(uri)
        }
    }

    internal fun playRingtone(position: Int, delayMs: Int) {
        handler!!.removeCallbacks(this)
        sampleRingtonePos = position
        handler!!.postDelayed(this, delayMs.toLong())
    }

    override fun run() {
        stopAnyPlayingRingtone()
        if (sampleRingtonePos == silentPos) {
            return
        }

        //        final int oldSampleRingtonePos = sampleRingtonePos;
        try {
            var ringtone: Ringtone? = null
            when (sampleRingtonePos) {
                defaultRingtonePos -> {
                    if (defaultRingtone == null) {
                        try {
                            defaultRingtone = RingtoneManager.getRingtone(context!!, uriForDefaultItem)
                        } catch (ex: SecurityException) {
                            Log.e(TAG, "Failed to create default Ringtone from $uriForDefaultItem.", ex)
                        }
                    }
                    /*
                     * Stream type of defaultRingtone is not set explicitly here.
                     * It should be set in accordance with ringtoneManager of this Activity.
                     */
                    defaultRingtone?.streamType = ringtoneManager!!.inferStreamType()
                    ringtone = defaultRingtone
                    currentRingtone = null
                }
                unknownPos -> {
                    if (unknownRingtone == null) {
                        try {
                            unknownRingtone = RingtoneManager.getRingtone(context!!, existingUri)
                        } catch (ex: SecurityException) {
                            Log.e(TAG, "Failed to create unknown Ringtone from $existingUri.", ex)
                        }
                    }
                    unknownRingtone?.streamType = ringtoneManager!!.inferStreamType()
                    ringtone = unknownRingtone
                    currentRingtone = null
                }
                else -> {
                    val position = getRingtoneManagerPosition(sampleRingtonePos)
                    try {
                        ringtone = ringtoneManager!!.getRingtone(position)
                    } catch (ex: SecurityException) {
                        Log.e(TAG, "Failed to create selected Ringtone from " + ringtoneManager!!.getRingtoneUri(position) + ".", ex)
                    }
    
                    currentRingtone = ringtone
                }
            }

            ringtone?.play()
        } catch (ex: SecurityException) {
            // Don't play the inaccessible default ringtone.
            Log.e(TAG, "Failed to play Ringtone.", ex)
            //            sampleRingtonePos = oldSampleRingtonePos;
        }
    }

    private fun saveAnyPlayingRingtone() {
        if (defaultRingtone != null && defaultRingtone!!.isPlaying) {
            sPlayingRingtone = defaultRingtone
        } else if (unknownRingtone != null && unknownRingtone!!.isPlaying) {
            sPlayingRingtone = unknownRingtone
        } else if (currentRingtone != null && currentRingtone!!.isPlaying) {
            sPlayingRingtone = currentRingtone
        }
    }

    private fun stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone!!.isPlaying) {
            sPlayingRingtone!!.stop()
        }
        sPlayingRingtone = null

        if (defaultRingtone != null && defaultRingtone!!.isPlaying) {
            defaultRingtone!!.stop()
        }

        if (unknownRingtone != null && unknownRingtone!!.isPlaying) {
            unknownRingtone!!.stop()
        }

        ringtoneManager?.stopPreviousRingtone()
    }

    private fun getRingtoneManagerPosition(listPos: Int): Int = listPos - staticItems.size

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
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
