package com.anggrayudi.materialpreference.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by Anggrayudi on 12/02/2016.
 * Memngambil direktori path berdasarkan tipe.
 */
public final class SaveDir {
    // 0
    public static final String EXTERNAL = Environment.getExternalStorageDirectory().getAbsolutePath();
    // 1
    public static final String ALARM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).getAbsolutePath();
    // 2
    public static final String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
    // 3
    public static final String DOWNLOADS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    //    public static final String DOWNLOADS    = "/mnt/shared";
    // 4
    public static final String MOVIES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
    // 5
    public static final String MUSIC = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
    // 6
    public static final String NOTIFICATIONS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath();
    // 7
    public static final String PICTURES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    // 8
    public static final String PODCASTS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).getAbsolutePath();
    // 9
    public static final String RINGTONES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getAbsolutePath();
    // 10
    @SuppressLint("InlinedApi")
    public static final String DOCUMENTS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()
            : EXTERNAL;

    public static final String[] arrays = {
            EXTERNAL,
            ALARM,
            DCIM,
            DOWNLOADS,
            MOVIES,
            MUSIC,
            NOTIFICATIONS,
            PICTURES,
            PODCASTS,
            RINGTONES,
            DOCUMENTS
    };

    public static final String[] NON_WRITABLE_DIR = {
            "/proc", "/cache", "/acct", "/data", "/sys", "/dev", "/kernel", "/var", "/mnt/shell",
            "/dsp", "/cust", "/persist", "/firmware", "/storage/self"
    };

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isNotWritable(File folder) {
        try {
            File file = new File(folder, "anggrayudi.test");
            file.delete();
            boolean ok = file.createNewFile();
            file.delete();
            return !ok;
        } catch (IOException e) {
            return true;
        }
    }
}
