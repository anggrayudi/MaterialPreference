package com.anggrayudi.materialpreference.util

import android.annotation.TargetApi
import android.os.Build
import android.os.Environment
import androidx.annotation.IntDef

/**
 * @author Anggrayudi H
 */
object FolderType {

    /** Equals to [Environment.getExternalStorageDirectory().getAbsolutePath()]  */
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
    @TargetApi(Build.VERSION_CODES.KITKAT)
    const val DOCUMENTS = 10

    @IntDef(EXTERNAL, ALARM, DCIM, DOWNLOADS, MOVIES, MUSIC, NOTIFICATIONS, PICTURES, PODCASTS, RINGTONES, DOCUMENTS)
    internal annotation class DirectoryType

    operator fun get(@DirectoryType folderType: Int): String {
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
            DOCUMENTS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                dir = Environment.DIRECTORY_DOCUMENTS
            } else
                return SaveDir.EXTERNAL + "/Documents"
        }
        return if (dir == null)
            Environment.getExternalStorageDirectory().absolutePath
        else
            Environment.getExternalStoragePublicDirectory(dir).absolutePath
    }
}
