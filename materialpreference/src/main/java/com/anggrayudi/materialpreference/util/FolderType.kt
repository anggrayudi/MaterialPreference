package com.anggrayudi.materialpreference.util

import android.os.Build
import android.os.Environment
import androidx.annotation.IntDef

/**
 * @author Anggrayudi H
 */
@IntDef(
    FolderType.EXTERNAL,
    FolderType.ALARM,
    FolderType.DCIM,
    FolderType.DOWNLOADS,
    FolderType.MOVIES,
    FolderType.MUSIC,
    FolderType.NOTIFICATIONS,
    FolderType.PICTURES,
    FolderType.PODCASTS,
    FolderType.RINGTONES,
    FolderType.DOCUMENTS
)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class FolderType {

    companion object {
        /** Equals to [Environment.getExternalStorageDirectory]  */
        const val EXTERNAL = 0
        const val ALARM = 1
        const val DCIM = 2
        const val DOWNLOADS = 3
        const val MOVIES = 4
        const val MUSIC = 5
        const val NOTIFICATIONS = 6
        const val PICTURES = 7
        const val PODCASTS = 8
        const val RINGTONES = 9
        const val DOCUMENTS = 10

        @Suppress("DEPRECATION")
        operator fun get(@FolderType folderType: Int): String {
            var dir: String? = null
            when (folderType) {
                ALARM -> dir = Environment.DIRECTORY_ALARMS
                DCIM -> dir = Environment.DIRECTORY_DCIM
                DOWNLOADS -> dir = Environment.DIRECTORY_DOWNLOADS
                MOVIES -> dir = Environment.DIRECTORY_MOVIES
                MUSIC -> dir = Environment.DIRECTORY_MUSIC
                NOTIFICATIONS -> dir = Environment.DIRECTORY_NOTIFICATIONS
                PICTURES -> dir = Environment.DIRECTORY_PICTURES
                PODCASTS -> dir = Environment.DIRECTORY_PODCASTS
                RINGTONES -> dir = Environment.DIRECTORY_RINGTONES
                DOCUMENTS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    dir = Environment.DIRECTORY_DOCUMENTS
                else
                    return Environment.getExternalStorageDirectory().absolutePath + "/Documents"
            }
            return if (dir == null)
                Environment.getExternalStorageDirectory().absolutePath
            else
                Environment.getExternalStoragePublicDirectory(dir).absolutePath
        }
    }
}
