@file:Suppress("DEPRECATION")

package com.fkg002c.networkutils

import android.Manifest.permission.READ_BASIC_PHONE_STATE
import android.Manifest.permission.READ_PHONE_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.fkg002c.networkutils.data.enum.Wnt
import com.fkg002c.networkutils.log.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

private const val TAG = "TelephonyUtils"

@SuppressLint("MissingPermission")
internal suspend fun getWnt(context: Context, timeout: Long = 100): Wnt {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return context.getSystemService(TelephonyManager::class.java)?.let {
            try {
                if (context.hasGranted(READ_PHONE_STATE) || context.hasGranted(READ_BASIC_PHONE_STATE)) {
                    Wnt.networkTypeToWnt(it.dataNetworkType)
                } else {
                    Logger.e(TAG, "getWnt() error: no read phone state permission")
                    Wnt.UNKNOWN
                }
            } catch (e: Exception) {
                Logger.e(TAG, "getWnt() error: ${e.message}")
                Wnt.UNKNOWN
            }
        } ?: run {
            Logger.e(TAG, "getWnt() error:: no telephony manager")
            Wnt.UNKNOWN
        }
    } else {
        var unregisterListener: () -> Unit = {}
        var cancellableContinuation: CancellableContinuation<Wnt>? = null
        var callback: Any?

        return try {
            withTimeout(timeout) {
                suspendCancellableCoroutine { task ->
                    cancellableContinuation = task
                    val resume: (Wnt) -> Unit = {
                        try {
                            unregisterListener()
                            if (task.isActive) task.resume(it) else task.cancel()
                        } catch (_: Exception) {
                        }
                    }

                    val telephonyManager = context.getSystemService(TelephonyManager::class.java)
                    callback =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                                    resume(Wnt.displayInfoToWnt(telephonyDisplayInfo))
                                }
                            }
                        else
                            object : PhoneStateListener(Dispatchers.Main.asExecutor()) {
                                @Deprecated("Deprecated in Java")
                                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                                    resume(Wnt.displayInfoToWnt(telephonyDisplayInfo))
                                }
                            }
                    unregisterListener = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            telephonyManager.unregisterTelephonyCallback(callback as TelephonyCallback)
                        } else {
                            telephonyManager.listen(callback as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
                        }
                        Logger.v(TAG, "unregister callback")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        telephonyManager.registerTelephonyCallback(Dispatchers.Main.asExecutor(), callback as TelephonyCallback)
                    } else {
                        telephonyManager.listen(callback as PhoneStateListener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
                    }
                    Logger.v(TAG, "register callback")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "getWnt() error: ${e.message}")
            unregisterListener()
            try {
                cancellableContinuation?.apply { if (isActive) cancel() }
            } catch (_: Exception) {
            }
            Wnt.UNKNOWN
        }
    }
}
