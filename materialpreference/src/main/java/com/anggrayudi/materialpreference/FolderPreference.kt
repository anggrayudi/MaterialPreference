package com.anggrayudi.materialpreference

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.files.selectedFolder
import com.anggrayudi.materialpreference.util.FolderType
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.FolderPickerCallback
import com.anggrayudi.storage.callback.StorageAccessCallback
import com.anggrayudi.storage.file.StorageType
import com.anggrayudi.storage.file.fullPath
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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
            requestStoragePermission()
            true
        }
    }

    private fun requestStoragePermission() {
        Dexter.withContext(context)
            .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        openFolderSelector()
                    } else {
                        Toast.makeText(context, R.string.please_grant_storage_permission, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                    // no-op
                }
            }).check()
    }

    private fun openFolderSelector() {
        val fragment = preferenceFragment ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FolderPreferenceDialog().apply {
                arguments = Bundle().apply { putString("folder", folder) }
                show(fragment.fragmentManager!!, key)
            }
        } else {
            setupSimpleStorage()
            storage.openFolderPicker(REQUEST_CODE_STORAGE_GET_FOLDER)
        }
    }

    private fun setupSimpleStorage() {
        storage.storageAccessCallback = object : StorageAccessCallback {
            override fun onCancelledByUser() {
                // no-op
            }

            override fun onRootPathNotSelected(rootPath: String, rootStorageType: StorageType) {
                MaterialDialog(context)
                    .message(text = context.getString(R.string.please_select_root_storage, rootPath))
                    .negativeButton(android.R.string.cancel)
                    .positiveButton {
                        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS, rootStorageType)
                    }
                    .show()
            }

            override fun onRootPathPermissionGranted(root: DocumentFile) {
                storage.openFolderPicker(REQUEST_CODE_STORAGE_GET_FOLDER)
                Toast.makeText(context, context.getString(R.string.selecting_root_path_success, root.fullPath), Toast.LENGTH_LONG).show()
            }

            override fun onStoragePermissionDenied() {
                requestStoragePermission()
            }
        }

        storage.folderPickerCallback = object : FolderPickerCallback {
            override fun onCancelledByUser(requestCode: Int) {
                // no-op
            }

            override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {
                this@FolderPreference.folder = folder.fullPath
            }

            override fun onStorageAccessDenied(requestCode: Int, folder: DocumentFile?, storageType: StorageType?) {
                if (storageType == null) {
                    requestStoragePermission()
                    return
                }
                MaterialDialog(context)
                    .message(R.string.storage_access_denied_confirm)
                    .negativeButton(android.R.string.cancel)
                    .positiveButton {
                        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS, storageType)
                    }
                    .show()
            }

            override fun onStoragePermissionDenied(requestCode: Int) {
                requestStoragePermission()
            }
        }
    }

    private val storage: SimpleStorage
        get() = preferenceFragment!!.storage

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
