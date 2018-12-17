package com.anggrayudi.materialpreference;

public interface StoragePermissionCallback {

    /**
     * Called when some permissions are missing.
     * @param read <code>true</code> if granted
     * @param write <code>true</code> if granted
     */
    void onPermissionTrouble(boolean read, boolean write);
}
