package com.fkg002c.networkutils

import android.content.Context
import android.telephony.CellInfo
import android.telephony.SubscriptionInfo
import com.fkg002c.networkutils.log.Logger

object Api {

    fun getLogLevel() = Logger.LOG_LEVEL

    fun setLogLevel(logLevel: Int) {
        Logger.LOG_LEVEL = logLevel
    }

    fun getActiveSubscriptions(context: Context, dataOnly: Boolean = true, timeout: Long = DEFAULT_TIMEOUT): List<SubscriptionInfo> {
        // TODO return list of active subscriptions
        return listOf()
    }

    suspend fun getCellInfo(context: Context, consumer: String): List<CellInfo> {
        return CellInfoUtils().collectCellInfo(context, consumer, DEFAULT_COLLECT_CELLINFO_TIMEOUT, 1)
    }

    suspend fun getWirelessNetworkType(context: Context, timeout: Long = DEFAULT_TIMEOUT) = getWnt(context, timeout)

    private const val DEFAULT_TIMEOUT = 1000L
    private const val DEFAULT_COLLECT_CELLINFO_TIMEOUT = 10_000L
}