package com.fkg002c.networkutils

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

fun Context.hasGranted(permission: String) = checkSelfPermission(permission) == PERMISSION_GRANTED

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
internal val API_Q: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
internal val API_S: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
