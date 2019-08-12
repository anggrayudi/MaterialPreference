package com.anggrayudi.materialpreference

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.files.selectedFolder
import com.anggrayudi.materialpreference.callback.StoragePermissionCallback
import com.anggrayudi.materialpreference.util.FileUtils
import com.anggrayudi.materialpreference.util.FolderType
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
open class FolderPreference @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0, defStyleRes: Int = 0)
    : Preference(context, attrs, defStyleAttr, defStyleRes) {

    @get:FolderType
    var defaultFolderType: Int = 0

    /** This callback will be triggered when some permissions are missing. */
    var permissionCallback: StoragePermissionCallback? = null

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
            val writeNotGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            val readNotGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            if (writeNotGranted || readNotGranted) {
                if (permissionCallback != null)
                    permissionCallback!!.onPermissionTrouble(!readNotGranted, !writeNotGranted)
                else
                    Toast.makeText(context, R.string.please_grant_storage_permission, Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent("android.intent.action.OPEN_DOCUMENT_TREE")
                val fragment = preferenceFragment
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.resolveActivity(context.packageManager) != null) {
                    fragment!!.preferenceKeyOnActivityResult = key
                    fragment.startActivityForResult(intent, FileUtils.REQUEST_CODE_STORAGE_GET_FOLDER)
                } else {
                    val args = Bundle()
                    args.putString("folder", folder)
                    val dialog = FolderPreferenceDialog()
                    dialog.arguments = args
                    dialog.show(fragment!!.fragmentManager!!, key)
                }
            }
            true
        }
    }

    class FolderPreferenceDialog : DialogFragment() {

        private var dialog: MaterialDialog? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            dialog = MaterialDialog(context!!)
                    .negativeButton(android.R.string.cancel)
                    .folderChooser(File(arguments!!.getString("folder")), allowFolderCreation = true){ _, file ->
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
}
