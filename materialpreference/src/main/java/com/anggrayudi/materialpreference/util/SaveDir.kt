package com.anggrayudi.materialpreference.util

import android.os.Build
import android.os.Environment
import java.io.File
import java.io.IOException

/**
 * @author Anggrayudi H on 12/02/2016.
 *
 * Memngambil direktori path berdasarkan tipe.
 */
object SaveDir {
    // 0
    val EXTERNAL = Environment.getExternalStorageDirectory().absolutePath!!
    // 1
    val ALARM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).absolutePath!!
    // 2
    val DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath!!
    // 3
    val DOWNLOADS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath!!
    //    public static final String DOWNLOADS    = "/mnt/shared";
    // 4
    val MOVIES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath!!
    // 5
    val MUSIC = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath!!
    // 6
    val NOTIFICATIONS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).absolutePath!!
    // 7
    val PICTURES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath!!
    // 8
    val PODCASTS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).absolutePath!!
    // 9
    val RINGTONES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).absolutePath!!
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
