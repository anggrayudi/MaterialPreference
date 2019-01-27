package com.anggrayudi.materialpreference.callback

import android.content.Context
import com.anggrayudi.materialpreference.R

import java.io.IOException

class StoragePermissionDenialException(context: Context) : IOException(context.getString(R.string.permission_denial_runtime))
