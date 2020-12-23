package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.files.selectedFolder
import com.anggrayudi.materialpreference.util.FolderType
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.absolutePath
import java.io.File

/**
 *      |     Attribute     |                        Value Type                       |
 *      |:-----------------:|:-------------------------------------------------------:|
 *      | app:defaultFolder | external, download, dcim, alarm, movies, music,         |
 *      |                   | notifications, pictures, podcasts, ringtones, documents |
 *
 * @see FolderType
 */
@TargetApi(21)
@SuppressLint("RestrictedApi")
open class FolderPreference @Keep @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    @get:FolderType
    var defaultFolderType: Int = 0

    override var isLegacySummary: Boolean
        get() = true
        set(value) {
            super.isLegacySummary = value
        }

    val defaultFolder: String
        get() = FolderType[defaultFolderType]

    /** Get or set value that is saved by `FolderPreference` */
    var folder: String?
        get() = getPersistedString(defaultFolder)
        set(value) {
            if (callChangeListener(value)) {
                persistString(value)
                summary = value
            }
        }

    override var summary: CharSequence?
        get() = folder
        set(value) {
            super.summary = value
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FolderPreference, defStyleAttr, defStyleRes)
        defaultFolderType = a.getInt(R.styleable.FolderPreference_defaultFolder, FolderType.EXTERNAL)
        a.recycle()

        onPreferenceClickListener = {
            SimpleStorageHelper.requestStoragePermission(context) { openFolderSelector() }
            true
        }
    }

    private fun openFolderSelector() {
        val fragment = preferenceFragment ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FolderPreferenceDialog().apply {
                arguments = Bundle().apply { putString("folder", folder) }
                show(fragment.fragmentManager!!, key)
            }
        } else {
            preferenceFragment!!.storageHelper.run {
                requestCodeFolderPicker = REQUEST_CODE_STORAGE_GET_FOLDER
                onFolderSelected = { _, folder ->
                    this@FolderPreference.folder = folder.absolutePath
                }
                openFolderPicker()
            }
        }
    }

    class FolderPreferenceDialog : DialogFragment() {

        private var dialog: MaterialDialog? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            dialog = MaterialDialog(context!!)
                .negativeButton(android.R.string.cancel)
                .folderChooser(File(arguments!!.getString("folder")!!), allowFolderCreation = true) { _, file ->
                    val preference = (activity as PreferenceActivityMaterial)
                        .visiblePreferenceFragment!!.findPreference(tag!!) as FolderPreference
                    preference.folder = file.absolutePath
                }
            return dialog as MaterialDialog
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            arguments!!.putString("folder", dialog!!.selectedFolder()!!.absolutePath)
        }
    }

    companion object {
        const val REQUEST_CODE_STORAGE_ACCESS = 111
        const val REQUEST_CODE_STORAGE_GET_FOLDER = 112
    }
}
