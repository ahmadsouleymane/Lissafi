package com.lissafi.app.service

import android.util.Log
import com.lissafi.app.BuildConfig

/**
 * Wrapper de logging : les logs verbeux (d/w) sont coupés en release — ils ne
 * contiennent pas de secret, mais autant ne rien laisser dans logcat par hygiène.
 * Les erreurs (e) restent actives, utiles pour un diagnostic via adb en support.
 */
object AppLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
