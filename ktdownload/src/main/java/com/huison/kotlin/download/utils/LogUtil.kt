package com.huison.kotlin.download.utils

import android.util.Log

/**
 * Created by huisonma 2019/9/3.
 */
class LogUtil {

    companion object {

        private const val TAG = "LogUtil"

        private var sEnable = false

        @JvmStatic
        fun enable() {
            sEnable = true
        }

        @JvmStatic
        fun d(msg: String) {
            d(TAG, msg)
        }

        @JvmStatic
        fun d(tag: String, msg: String) {
            if (sEnable) {
                Log.d(tag, msg)
            }
        }

        @JvmStatic
        fun d(tag: String, msg: String, tr: Throwable) {
            if (sEnable) {
                Log.d(tag, msg, tr)
            }
        }

        @JvmStatic
        fun e(msg: String) {
            e(TAG, msg)
        }

        @JvmStatic
        fun e(tag: String, msg: String) {
            if (sEnable) {
                Log.e(tag, msg)
            }
        }

        @JvmStatic
        fun e(tag: String, msg: String, tr: Throwable) {
            if (sEnable) {
                Log.e(tag, msg, tr)
            }
        }
    }
}