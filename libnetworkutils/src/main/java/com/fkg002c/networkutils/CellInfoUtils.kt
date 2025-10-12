@file:Suppress("DEPRECATION")

package com.fkg002c.networkutils

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.fkg002c.networkutils.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

internal class CellInfoUtils {
    @SuppressLint("MissingPermission")
    suspend fun collectCellInfo(context: Context, consumer: String, timeoutInMillis: Long, count: Int): List<CellInfo> {
        val timestamp = System.currentTimeMillis()
        Logger.d(TAG, "collectCellInfo() called: $consumer, timeout: $timeoutInMillis, count: $count")
        val commonCellInfoList = mutableListOf<CellInfo>()
        if (!context.hasGranted(ACCESS_FINE_LOCATION)) {
            Logger.e(TAG, "No location permissions!")
            return listOf()
        }
        if (API_Q) {
            val tm = context.getSystemService(TelephonyManager::class.java)
            var callback: Any? = null
            var reason = "timeout"

            try {
                val res = withTimeout(timeoutInMillis) {
                    suspendCancellableCoroutine { continuation ->
                        val resume: (List<CellInfo>) -> Unit = {
                            try {
                                if (continuation.isActive) continuation.resume(it)
                                else continuation.cancel()
                            } catch (e: Exception) {
                                Logger.e(TAG, "collectCellInfo resume error: ${e.message}")
                            }
                        }
                        callback = if (API_S)
                            object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                                override fun onCellInfoChanged(cellInfoList: MutableList<CellInfo>) {
                                    Logger.d(TAG, "onCellInfoChanged() count: ${cellInfoList.size}")
                                    commonCellInfoList.addAll(cellInfoList)
                                    if (commonCellInfoList.filter { it.isRegistered }.size >= count) {
                                        resume(commonCellInfoList)
                                    }
                                }
                            }
                        else
                            object : PhoneStateListener(Dispatchers.Main.asExecutor()) {
                                @Deprecated("Deprecated in Java")
                                override fun onCellInfoChanged(cellInfoList: MutableList<CellInfo>?) {
                                    Logger.d(TAG, "onCellInfoChanged() count: ${cellInfoList?.size}")
                                    cellInfoList?.let {
                                        commonCellInfoList.addAll(cellInfoList)
                                        if (commonCellInfoList.filter { it.isRegistered }.size >= count) {
                                            resume(commonCellInfoList)
                                        }
                                    }
                                }
                            }

                        if (API_S) {
                            tm.registerTelephonyCallback(Dispatchers.Main.asExecutor(), callback as TelephonyCallback)
                        } else {
                            tm.listen(callback as PhoneStateListener, PhoneStateListener.LISTEN_CELL_INFO)
                        }
                        startRequester(tm)
                    }
                }
                Logger.d(TAG, "collectCellInfo() completed by count: ${System.currentTimeMillis() - timestamp}, all: ${commonCellInfoList.size}")
                return res
            } catch (e: Exception) {
                Logger.e(TAG, "error: ${e.message}")
                if (e !is TimeoutCancellationException) reason = "error"
            } finally {
                if (callback != null) {
                    stopRequester()
                    if (API_S) tm.unregisterTelephonyCallback(callback as TelephonyCallback)
                    else tm.listen(callback as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
                }
            }

            Logger.d(TAG, "collectCellInfo() completed by ${reason}: ${System.currentTimeMillis() - timestamp}, all: ${commonCellInfoList.size}")
            return commonCellInfoList.toList()
        } else {
            Logger.e(TAG, "Android 10 API is required. Current is ${Build.VERSION.SDK_INT} ")
            return listOf()
        }
    }

    // The code below is for forced requests to TelephonyManager for CellInfo.
    // Without these requests the listener's callback happen very rare: much less than each 2 seconds with display is on or each 10 second when display is off
    private var coroutineScope: CoroutineScope? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(value = "android.permission.ACCESS_FINE_LOCATION")
    private fun startRequester(telephonyManager: TelephonyManager) {
        coroutineScope = CoroutineScope(Dispatchers.IO).apply {
            launch {
                while (isActive) {
                    try {
                        // This request is needed to get more cell info updates in cell info listeners
                        telephonyManager.requestCellInfoUpdate(Dispatchers.IO.asExecutor(), object : TelephonyManager.CellInfoCallback() {
                            override fun onCellInfo(cellInfoList: MutableList<CellInfo>) {
                            }
                        })
                    } catch (_: Exception) {
                    }
                    delay(CELL_INFO_FORCED_REQUEST_PERIOD)
                }
            }
        }
    }

    private fun stopRequester() {
        try {
            coroutineScope?.coroutineContext?.apply {
                cancelChildren()
                cancel()
            }
        } catch (_: Exception) {
        }
        coroutineScope = null
    }

    private companion object {
        private const val TAG = "CellInfoUtils"
        private const val CELL_INFO_FORCED_REQUEST_PERIOD = 1000L
    }
}