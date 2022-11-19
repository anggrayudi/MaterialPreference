package com.anggrayudi.materialpreference

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import com.anggrayudi.materialpreference.util.FolderType
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.file.FileFullPath
import com.anggrayudi.storage.file.getAbsolutePath

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
            openFolderSelector()
            true
        }
    }

    private fun openFolderSelector() {
        val fragment = preferenceFragment ?: return
        fragment.storageHelper.run {
            onFolderSelected = { _, folder ->
                this@FolderPreference.folder = folder.getAbsolutePath(context)
            }
            openFolderPicker(
                REQUEST_CODE_STORAGE_GET_FOLDER,
                initialPath = FileFullPath(fragment.requireContext(), folder ?: SimpleStorage.externalStoragePath)
            )
        }
    }

    companion object {
        const val REQUEST_CODE_STORAGE_ACCESS = 111
        const val REQUEST_CODE_STORAGE_GET_FOLDER = 112
    }
}
