package com.anggrayudi.materialpreference;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Toast;

import com.anggrayudi.materialpreference.util.FileUtils;

import androidx.core.app.ActivityCompat;

/**
 * Available for API 21 only. For API 19 and lower is in my backlog.
 *  <table>
 *  <tr>
 *  <th>Attribute</th>
 *  <th>Value Type</th>
 *  </tr><tr>
 *  <td><code>app:defaultFolder</code></td>
 *  <td>{@link FolderType} => external, download, dcim, alarm, movies, music, notifications, pictures, podcasts, ringtones, documents</td>
 *  </tr>
 *  </table>
 */
@TargetApi(21)
@SuppressLint("RestrictedApi")
public class FolderPreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final String TAG = "FolderPreference";

    private int mDefaultFolderType;
    StoragePermissionCallback callback;

    public FolderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnPreferenceClickListener(this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FolderPreference, defStyleAttr, defStyleRes);
        mDefaultFolderType = a.getInt(R.styleable.FolderPreference_defaultFolder, FolderType.EXTERNAL);
        a.recycle();
    }

    public FolderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FolderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderPreference(Context context) {
        this(context, null);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        boolean writeNotGranted = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        boolean readNotGranted = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        if (writeNotGranted || readNotGranted) {
            if (callback != null)
                callback.onPermissionTrouble(!readNotGranted, !writeNotGranted);
            else
                Toast.makeText(getContext(), R.string.please_grant_storage_permission, Toast.LENGTH_SHORT).show();
            return true;
        }
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        PreferenceFragmentMaterial fragment = getPreferenceFragment();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.resolveActivity(getContext().getPackageManager()) != null) {
            fragment.preferenceKeyOnActivityResult = getKey();
            fragment.startActivityForResult(intent, FileUtils.REQUEST_CODE_STORAGE_GET_FOLDER);
            return true;
        }
        // TODO: 16-Dec-18 Add support for API 19 and lower
//        new FolderChooserDialog.Builder(getContext())
//                .tag(TAG)
//                .chooseButton(R.string.choose)
//                .initialPath(getFolder())
//                .show(fragment.getFragmentManager());
        return true;
    }

    @Override
    public boolean isLegacySummary() {
        return true;
    }

    public void setDefaultFolderType(@FolderType.DirectoryType int folderType) {
        mDefaultFolderType = folderType;
    }

    @FolderType.DirectoryType
    public int getDefaultFolderType() {
        return mDefaultFolderType;
    }

    public String getDefaultFolder() {
        return FolderType.get(getDefaultFolderType());
    }

    /**
     * Get current value that is saved by <code>FolderPreference</code>
     */
    public String getFolder() {
        return getPersistedString(getDefaultFolder());
    }

    /**
     * This callback will be triggered when some permissions are missing.
     */
    public void setPermissionCallback(StoragePermissionCallback callback) {
        this.callback = callback;
    }

    @Override
    public CharSequence getSummary() {
        return getFolder();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return FolderType.get(a.getInt(index, 0));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value = restorePersistedValue ? getPersistedString(getDefaultFolder()) : ((String) defaultValue);
        final boolean wasBlocking = shouldDisableDependents();

        persistString(value);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
        if (isBindValueToSummary())
            setSummary(value);
    }
}
