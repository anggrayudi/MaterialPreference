package com.anggrayudi.materialpreference.util

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.IOException

/**
 * @author Anggrayudi H on 12/02/2016.
 *
 * Memngambil direktori path berdasarkan tipe.
 */
@SuppressLint("SdCardPath")
object SaveDir {
    // 0
    val EXTERNAL: String = Environment.getExternalStorageDirectory().absolutePath ?: "/mnt/sdcard"
    // 1
    val ALARM: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).absolutePath ?: "/mnt/sdcard/Alarms"
    // 2
    val DCIM: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath ?: "/mnt/sdcard/DCIM"
    // 3
    val DOWNLOADS: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath ?: "/mnt/sdcard/Downloads"
    //    public static final String DOWNLOADS    = "/mnt/shared";
    // 4
    val MOVIES: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath ?: "/mnt/sdcard/Movies"
    // 5
    val MUSIC: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath ?: "/mnt/sdcard/Music"
    // 6
    val NOTIFICATIONS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).absolutePath ?: "/mnt/sdcard/Notifications"
    // 7
    val PICTURES: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath ?: "/mnt/sdcard/Pictures"
    // 8
    val PODCASTS: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).absolutePath ?: "/mnt/sdcard/Podcasts"
    // 9
    val RINGTONES: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).absolutePath ?: "/mnt/sdcard/Ringtones"
    // 10
    val DOCUMENTS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath!!
    else
        "$EXTERNAL/Documents"

    val arrays = arrayOf(EXTERNAL, ALARM, DCIM, DOWNLOADS, MOVIES, MUSIC, NOTIFICATIONS, PICTURES, PODCASTS, RINGTONES, DOCUMENTS)

    val NON_WRITABLE_DIR = arrayOf("/proc", "/cache", "/acct", "/data", "/sys", "/dev", "/kernel", "/var",
            "/mnt/shell", "/dsp", "/cust", "/persist", "/firmware", "/storage/self")

    fun isNotWritable(folder: File): Boolean {
        return try {
            val file = File(folder, "anggrayudi.test")
            file.delete()
            val ok = file.createNewFile()
            file.delete()
            !ok
        } catch (e: IOException) {
            true
        }
    }
}
