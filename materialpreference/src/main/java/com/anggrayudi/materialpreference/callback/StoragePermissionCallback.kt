package com.anggrayudi.materialpreference.callback

interface StoragePermissionCallback {

    /**
     * Called when some permissions are missing.
     * @param read `true` if granted
     * @param write `true` if granted
     */
    fun onPermissionTrouble(read: Boolean, write: Boolean)
}
