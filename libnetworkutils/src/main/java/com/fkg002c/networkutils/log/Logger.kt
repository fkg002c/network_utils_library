package com.fkg002c.networkutils.log

import com.fkg002c.networkutils.BuildConfig
import android.util.Log as Logcat

interface ILogger {
    fun verbose(log: Pair<String, String>)
    fun debug(log: Pair<String, String>)
    fun info(log: Pair<String, String>)
    fun warn(log: Pair<String, String>)
    fun error(log: Pair<String, String>, throwable: Throwable?)
}

object Log {
    const val VERBOSE = Logcat.VERBOSE // 2
    const val DEBUG = Logcat.DEBUG // 3
    const val INFO = Logcat.INFO // 4
    const val WARN = Logcat.WARN // 5
    const val ERROR = Logcat.ERROR // 6
    const val DISABLED = 7
}

internal object LogcatLogger : ILogger {
    override fun verbose(log: Pair<String, String>) {
        Logcat.v(log.first, log.second)
    }

    override fun debug(log: Pair<String, String>) {
        Logcat.d(log.first, log.second)
    }

    override fun info(log: Pair<String, String>) {
        Logcat.i(log.first, log.second)
    }

    override fun warn(log: Pair<String, String>) {
        Logcat.w(log.first, log.second)
    }

    override fun error(log: Pair<String, String>, throwable: Throwable?) {
        Logcat.e(log.first, log.second, throwable)
    }
}

object Logger {
    private const val SINGLE_TAG = "NetworkUtilsLib"
    private var externalLogger: ILogger? = null
    internal var LOG_LEVEL = if (BuildConfig.DEBUG) Log.VERBOSE else Log.DISABLED
        set(value) {
            field = value.coerceIn(Log.VERBOSE, Log.DISABLED)
        }

    fun injectLogger(logger: ILogger) {
        externalLogger = logger
    }

    private fun getLog(tag: String, message: String): Pair<String, String> {
        return if (BuildConfig.SINGLE_LOG_TAG) Pair(SINGLE_TAG, "$tag: $message") else Pair(tag, message)
    }

    private fun getLogger(): ILogger = externalLogger ?: LogcatLogger
    fun v(tag: String, message: String) {
        if (LOG_LEVEL <= Log.VERBOSE) getLogger().verbose(getLog(tag, message))
    }

    fun d(tag: String, message: String) {
        if (LOG_LEVEL <= Log.DEBUG) getLogger().debug(getLog(tag, message))
    }

    fun i(tag: String, message: String) {
        if (LOG_LEVEL <= Log.INFO) getLogger().info(getLog(tag, message))
    }

    fun w(tag: String, message: String) {
        if (LOG_LEVEL <= Log.WARN) getLogger().warn(getLog(tag, message))
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (LOG_LEVEL <= Log.ERROR) getLogger().error(getLog(tag, message), throwable)
    }
}