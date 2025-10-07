package com.fkg002c.networkutils.data.enum

import android.os.Build
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.fkg002c.networkutils.log.Logger

// Wireless network technologies
enum class Wnt {
    UNKNOWN,
    TWO_G,
    THREE_G,
    FOUR_G,
    FOUR_G_ADVANCED_PRO,
    FOUR_G_CA,
    FIVE_G_NSA,
    FIVE_G,
    FIVE_G_ADVANCED,
    FIVE_G_NSA_MMWAVE;

    companion object {
        private const val TAG = "Wnt"

        fun networkTypeToWnt(networkType: Int) =
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> UNKNOWN
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_GSM -> TWO_G

                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> THREE_G

                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_IWLAN,
                19/*TelephonyManager.NETWORK_TYPE_LTE_CA*/ -> FOUR_G

                TelephonyManager.NETWORK_TYPE_NR -> FIVE_G
                else -> {
                    Logger.w(TAG, "networkTypeToWnt(): undefined network type: $networkType")
                    UNKNOWN
                }
            }

        fun networkTypeToWnt(networkType: Int, overrideNetworkType: Int): Wnt =
            when (overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> networkTypeToWnt(networkType)
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> FOUR_G_CA
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> FOUR_G_ADVANCED_PRO
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> FIVE_G_NSA
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> FIVE_G_NSA_MMWAVE
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> FIVE_G_ADVANCED
                else -> {
                    Logger.w(TAG, "networkTypeToWnt(): undefined override network type: $overrideNetworkType")
                    networkTypeToWnt(networkType)
                }
            }

        @RequiresApi(Build.VERSION_CODES.R)
        fun displayInfoToWnt(displayInfo: TelephonyDisplayInfo) = networkTypeToWnt(displayInfo.networkType, displayInfo.overrideNetworkType)
    }
}