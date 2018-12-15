package com.anggrayudi.materialpreference;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class FolderType {

    private FolderType(){}

    /** Equals to <code>Environment.getExternalStorageDirectory().getAbsolutePath()</code> */
    public static final int EXTERNAL = 0;
    public static final int ALARM = 1;
    public static final int DCIM = 2;
    public static final int DOWNLOADS = 3;
    public static final int MOVIES = 4;
    public static final int MUSIC = 5;
    public static final int NOTIFICATIONS = 6;
    public static final int PICTURES = 7;
    public static final int PODCASTS = 8;
    public static final int RINGTONES = 9;
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static final int DOCUMENTS = 10;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EXTERNAL, ALARM, DCIM, DOWNLOADS, MOVIES, MUSIC, NOTIFICATIONS, PICTURES, PODCASTS, RINGTONES, DOCUMENTS})
    @interface DirectoryType{}

    @SuppressLint("SwitchIntDef")
    public static String get(@DirectoryType int folderType) {
        String dir = null;
        switch (folderType) {
            case ALARM:
                dir = Environment.DIRECTORY_ALARMS;
                break;
            case DCIM:
                dir = Environment.DIRECTORY_DCIM;
                break;
            case DOWNLOADS:
                dir = Environment.DIRECTORY_DOWNLOADS;
                break;
            case MOVIES:
                dir = Environment.DIRECTORY_MOVIES;
                break;
            case MUSIC:
                dir = Environment.DIRECTORY_MUSIC;
                break;
            case NOTIFICATIONS:
                dir = Environment.DIRECTORY_NOTIFICATIONS;
                break;
            case PICTURES:
                dir = Environment.DIRECTORY_PICTURES;
                break;
            case PODCASTS:
                dir = Environment.DIRECTORY_PODCASTS;
                break;
            case RINGTONES:
                dir = Environment.DIRECTORY_RINGTONES;
                break;
            case DOCUMENTS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    dir = Environment.DIRECTORY_DOCUMENTS;
                }
                break;
        }
        return dir == null
                ? Environment.getExternalStorageDirectory().getAbsolutePath()
                : Environment.getExternalStoragePublicDirectory(dir).getAbsolutePath();
    }
}
